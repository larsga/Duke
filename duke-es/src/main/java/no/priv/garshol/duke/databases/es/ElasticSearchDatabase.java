package no.priv.garshol.duke.databases.es;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.utils.Utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;

public class ElasticSearchDatabase implements Database {

	private static final int HOST_PORT_DEFAULT = 9300;
	private static final String[] DATA_SUBDIRS = { "data", "work", "logs" };

	private Configuration config;
	private Property idProperty;
	private boolean overwrite;

	private Client client;
	private Node node;
	private String cluster;
	private boolean clientOnly;
	private boolean local;
	private boolean clientSniff;
	private StorageType storageType;
	private String dataFolder;
	private Collection<String> tAddresses;

	private Analyzer analyzer;
	private BulkRequestBuilder bulkRequest;
	private int bulkRequestCounter;
	private int bulkSize;

	private String indexName;
	private String indexType;
	private int maxSearchHits;

	public ElasticSearchDatabase() {
		this.cluster = "duke-es";

		// remote client defaults
		this.clientSniff = true;

		// local client/node defaults
		this.storageType = StorageType.MEMORY;
		this.clientOnly = false;
		this.local = true;

		// index and search stuff
		this.indexName = "duke";
		this.indexType = "record"; // TODO: figure out if we can do something
									// smarter with this
		this.maxSearchHits = 100;
		this.bulkSize = 5000;

		this.analyzer = new StandardAnalyzer();
	}

	private void init() {
		this.setupConnection();

		// Create index if it does not already exist
		IndicesExistsResponse response = client.admin().indices()
				.exists(new IndicesExistsRequest(indexName)).actionGet();

		boolean forceCreate = false;
		if (response.isExists() && !this.overwrite) {
			client.admin().indices().prepareDelete(this.indexName).execute()
					.actionGet();
			forceCreate = true;
		}

		if (!response.isExists() || forceCreate) {
			CreateIndexResponse create = client.admin().indices()
					.prepareCreate(indexName).execute().actionGet();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				throw new RuntimeException(
						"Interrupted while waiting for index to settle in", e);
			}
			if (!create.isAcknowledged()) {
				throw new IllegalArgumentException("Could not create index: "
						+ indexName);
			}

			// create mapping

			// XContentBuilder builder = null;
			// try {
			// builder =
			// XContentFactory.jsonBuilder().startObject().startObject(this.indexType).startObject("properties");
			// for (Property p : config.getProperties()) {
			// if (!p.isIdProperty()) {
			// // TODO: experiment similarity OKAPY BM25 for short
			// // fields
			// //
			// (http://info.elasticsearch.com/rs/elasticsearch/images/What's%20new%20in%200.90%205-3-12.pdf)
			// builder.startObject(p.getName()).field("type",
			// "string").field("store", "yes").field("index", "analyzed")
			// .endObject();
			// }
			// }
			// builder.endObject().endObject().endObject();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			// PutMappingResponse pmrb =
			// client.admin().indices().preparePutMapping(this.indexName).setType(this.indexType).setSource(builder)
			// .execute().actionGet();
		}

		// find id property
		Collection<Property> identityProperties = this.config
				.getIdentityProperties();
		if (identityProperties == null || identityProperties.size() != 1) {
			throw new java.lang.IllegalStateException(
					"Unable to handle entities without single id");
		}
		this.idProperty = Iterables.get(identityProperties, 0);

		// disable index refresh interval to improve indexing performance
		// this is enabled back in commit()
		ImmutableSettings.Builder indexSettings = ImmutableSettings
				.settingsBuilder();
		indexSettings.put("refresh_interval", -1);
		this.client.admin().indices().prepareUpdateSettings(this.indexName)
				.setSettings(indexSettings).execute().actionGet();

		this.bulkRequest = this.client.prepareBulk();
	}

	private void setupConnection() {
		ImmutableSettings.Builder settings = ImmutableSettings
				.settingsBuilder();
		settings.put("cluster.name", this.cluster);

		if (this.tAddresses == null) {
			NodeBuilder builder = NodeBuilder.nodeBuilder();

			File dFolder = null;
			if (this.dataFolder == null) {
				dFolder = Utils.createTempDirectory("duke-es");
			} else {
				dFolder = new File(this.dataFolder);
				if (!dFolder.exists()) {
					dFolder.mkdirs();
				}
			}
			System.out.println("ElasicSearch node folder " + dFolder);
			for (String sub : DATA_SUBDIRS) {
				String subdir = dFolder.getPath() + File.separator + sub;
				File f = new File(subdir);
				if (!f.exists()) {
					f.mkdirs();
				}
				settings.put("path." + sub, subdir);
			}

			if (this.storageType == StorageType.MEMORY) {
				settings.put("index.store.type", "memory");
			}

			builder.settings(settings.build());
			this.node = builder.client(this.clientOnly).local(this.local)
					.node();
			this.client = this.node.client();

		} else {

			settings.put("client.transport.sniff", this.clientSniff);

			this.client = new TransportClient(settings.build());

			for (String address : this.tAddresses) {
				String[] hostparts = address.split(":");
				String hostname = hostparts[0];
				int hostport = HOST_PORT_DEFAULT;
				if (hostparts.length == 2) {
					hostport = Integer.parseInt(hostparts[1]);
				}
				((TransportClient) client)
						.addTransportAddress(new InetSocketTransportAddress(
								hostname, hostport));
			}
		}

		ClusterHealthResponse actionGet = this.client.admin().cluster()
				.prepareHealth().setWaitForYellowStatus().execute().actionGet();
		System.out.println("ElasticSearch Health Check " + actionGet);
	}

	/**
	 * Returns true if the ES index is held in memory rather than on disk.
	 */
	@Override
	public boolean isInMemory() {
		return this.storageType == StorageType.MEMORY;
	}

	/**
	 * Add the record to the index.
	 */
	@Override
	public void index(Record record) {
		if (this.client == null) {
			this.init();
		}

		String id = null;
		Map<String, Object> json = new HashMap<String, Object>();
		for (String propname : record.getProperties()) {
			Property prop = config.getPropertyByName(propname);

			if (prop == null) {
				throw new DukeConfigException("Record has property " + propname
						+ " for which there is no configuration");
			}

			if (prop.isIdProperty()) {
				id = record.getValue(propname);
			} else {

				Collection<String> values = record.getValues(propname);
				if (values != null && !values.isEmpty()) {
					if (values.size() == 1) {
						json.put(propname, Iterables.get(values, 0));
					} else {
						json.put(propname, values);
					}
				}
			}

		}

		this.addToIndex(id, json);
	}

	@Override
	public void commit() {
		if (this.client != null) {
			this.flushIndex(true);
			this.client.admin().indices()
					.refresh(new RefreshRequest(this.indexName)).actionGet();

			// enable index auto refresh
			ImmutableSettings.Builder indexSettings = ImmutableSettings
					.settingsBuilder();
			indexSettings.put("refresh_interval", 1);
			this.client.admin().indices().prepareUpdateSettings(this.indexName)
					.setSettings(indexSettings).execute().actionGet();

			this.client.admin().indices().prepareOptimize(this.indexName)
					.setMaxNumSegments(5).execute().actionGet();
		}
	}

	@Override
	public Record findRecordById(String id) {
		GetResponse getResponse = client
				.prepareGet(this.indexName, this.indexType, id).execute()
				.actionGet();
		return this
				.readFromSource(getResponse.getId(), getResponse.getSource());
	}

	@Override
	public Collection<Record> findCandidateMatches(Record record) {
		Collection<Record> records = new ArrayList<Record>();

		BoolQueryBuilder bqb = QueryBuilders.boolQuery();

		for (Property prop : config.getLookupProperties()) {
			String propName = prop.getName();
			boolean required = prop.getLookupBehaviour() == Property.Lookup.REQUIRED;
			Collection<String> values = record.getValues(propName);
			if (values == null) {
				continue;
			}

			StringBuilder queryString = new StringBuilder();
			for (String v : values) {
				try {
					TokenStream tokenStream = analyzer.tokenStream(propName,
							new StringReader(v));
					tokenStream.reset();
					CharTermAttribute attr = tokenStream
							.getAttribute(CharTermAttribute.class);

					while (tokenStream.incrementToken()) {
						queryString.append(attr.toString()).append(" ");
					}
					tokenStream.close();
				} catch (IOException e) {
					throw new DukeException("Error parsing input string '" + v
							+ "' " + "in field " + propName);
				}
			}

			Float boostFactor = this.getBoostFactor(prop.getHighProbability());
			if (queryString.length() <= 0)
				continue;
			QueryStringQueryBuilder qsqb = QueryBuilders
					.queryString(queryString.toString().trim())
					.defaultField(propName).boost(boostFactor);
			bqb = required ? bqb.must(qsqb) : bqb.should(qsqb);

		}

		SearchResponse response = this.client.prepareSearch(this.indexName)
				.setTypes(this.indexType)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(bqb)
				.setSize(this.maxSearchHits).execute().actionGet();

		SearchHit[] results = response.getHits().getHits();
		for (SearchHit hit : results) {
			records.add(this.readFromSource(hit.getId(), hit.getSource()));
		}

		return records;
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.close();
			this.client = null;
		}
		if (this.node != null && !this.node.isClosed()) {
			this.node.close();
			this.node = null;
		}
	}

	@Override
	public String toString() {
		return "ElasticSearchDatabase [idProperty=" + idProperty
				+ ", overwrite=" + overwrite + ", client=" + client + ", node="
				+ node + ", cluster=" + cluster + ", clientOnly=" + clientOnly
				+ ", local=" + local + ", clientSniff=" + clientSniff
				+ ", storageType=" + storageType + ", dataFolder=" + dataFolder
				+ ", tAddresses=" + tAddresses + ", bulkSize=" + bulkSize
				+ ", indexName=" + indexName + ", indexType=" + indexType
				+ ", maxSearchHits=" + maxSearchHits + "]";
	}

	private Record readFromSource(String id, Map<String, Object> source) {
		RecordImpl record = null;
		if (source != null) {
			record = new RecordImpl();
			// add the id first ...
			record.addValue(this.idProperty.getName(), id);
			// ... then the other fields
			for (String key : source.keySet()) {
				Object value = source.get(key);
				if (value instanceof Collection<?>) {
					for (Object v : (Collection<?>) value) {
						record.addValue(key, v.toString());
					}
				} else {
					record.addValue(key, value.toString());
				}
			}
		}
		return record;
	}

	private void addToIndex(String id, Map<String, Object> json) {
		this.bulkRequest.add(this.client.prepareIndex(this.indexName,
				this.indexType, id).setSource(json));
		this.bulkRequestCounter++;

		this.flushIndex(false);
	}

	private void flushIndex(boolean force) {
		if ((force && this.bulkRequestCounter > 0)
				|| this.bulkRequestCounter >= this.bulkSize) {
			BulkResponse bulkResponse = this.bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				throw new DukeException(bulkResponse.buildFailureMessage());
			}
			// reset bulk
			this.bulkRequestCounter = 0;
			this.bulkRequest = this.client.prepareBulk();
		}
	}

	private Float getBoostFactor(double probability) {
		return (float) Math.sqrt(1.0 / ((1.0 - probability) * 2.0));
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	@Override
	public void setConfiguration(Configuration config) {
		this.config = config;
	}

	@Override
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public void setMaxSearchHits(int maxSearchHits) {
		this.maxSearchHits = maxSearchHits;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
}
