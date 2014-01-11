
package no.priv.garshol.duke.databases;

import org.apache.lucene.search.Filter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.comparators.GeopositionComparator;

/**
 * All spatial Lucene search functionality is located in this class.
 * The class serves no natural design function, but moving all the
 * dependencies out here ensures that people who don't need the
 * geosearch can use Duke without the spatial libraries.
 */
public class GeoProperty {
  private Property prop; // the property containing coordinates
  private SpatialContext spatialctx;
  private SpatialStrategy strategy;

  public GeoProperty(Property prop) {
    this.prop = prop;
    this.spatialctx = SpatialContext.GEO;

    int maxlevels = 11; // FIXME: how to compute?
    GeohashPrefixTree grid = new GeohashPrefixTree(spatialctx, maxlevels);
    this.strategy = new RecursivePrefixTreeStrategy(grid, prop.getName());
  }

  /**
   * Returns the name of the property.
   */
  public String getName() {
    return prop.getName();
  }

  /**
   * Geoindexes the coordinates.
   */
  public IndexableField[] createIndexableFields(String value) {
    return strategy.createIndexableFields(parsePoint(value));
  }

  /**
   * Returns a geoquery.
   */
  public Filter geoSearch(String value) {
    GeopositionComparator comp = (GeopositionComparator) prop.getComparator();
    double dist = comp.getMaxDistance();
    double degrees = DistanceUtils.dist2Degrees(dist, DistanceUtils.EARTH_MEAN_RADIUS_KM * 1000.0);
    Shape circle = spatialctx.makeCircle(parsePoint(value), degrees);
    SpatialArgs args = new SpatialArgs(SpatialOperation.Intersects, circle);
    return strategy.makeFilter(args);
  }
  
  /**
   * Parses coordinates into a Spatial4j point shape.
   */
  private Point parsePoint(String point) {
    int comma = point.indexOf(',');
    if (comma == -1)
      return null;

    float lat = Float.valueOf(point.substring(0, comma));
    float lng = Float.valueOf(point.substring(comma + 1));
    return spatialctx.makePoint(lng, lat);
  }
}