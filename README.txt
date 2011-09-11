
  ========
    DUKE
  ========


Duke is a fast deduplication and record linkage engine written in
Java, based on Lucene. No documentation is included in the
distribution. To see how to use it, see

  http://code.google.com/p/duke/wiki/GettingStarted

instead.

You may also want to look at the examples in doc/example-data,
particularly dogfood.xml and countries.xml.


For a description of what's new in release 0.3, see
  http://code.google.com/p/duke/wiki/ReleaseNotes


--- EXAMPLES

In the doc/examples directory are two examples. One finding duplicates
and one doing record linkage.


dogfood.ntriples contains data about papers presented at Semantic Web
conferences, with some inadvertent duplicates. Running

java no.priv.garshol.duke.Duke --testdebug --testfile=dogfood-test.txt dogfood.xml

shows the results of running deduplication.


countries-mondial.csv and countries-dbpedia.csv both contain basic
data about countries. Running countries.xml makes Duke pair each
country from one file with the corresponding country in the other.
Run:

java no.priv.garshol.duke.Duke --testdebug --testfile=countries-test.txt countries.xml
