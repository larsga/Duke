# Duke

Duke is a fast and flexible deduplication (or entity resolution, or
record linkage) engine written in Java on top of Lucene.  The latest
version is 1.2 (see ReleaseNotes).

Duke can find duplicate customer records, or other kinds of records in
your database. Or you can use it to connect records in one data set
with other records representing the same thing in another data set.
Duke has sophisticated comparators that can handle spelling
differences, numbers, geopositions, and more. Using a probabilistic
model Duke can handle noisy data with good accuracy.

Features

  * High performance.
  * Highly configurable.
  * Support for CSV, JDBC, SPARQL, and NTriples [DataSources].
  * Many built-in [Comparator comparators].
  * Plug in your own data sources, comparators, and [Cleaner cleaners].
  * GeneticAlgorithm for automatically tuning configurations.
  * Command-line client for getting started.
  * API for embedding into any kind of application.
  * Support for batch processing and continuous processing.
  * Can maintain database of links found via JNDI/JDBC.
  * Can run in multiple threads.

The GettingStarted page explains how to get started and has links to
further documentation. The ExamplesOfUse page lists real examples of
using Duke, complete with data and configurations. [This presentation](http://www.slideshare.net/larsga/linking-data-without-common-identifiers)
has more the big picture and background.

Contributions, whether issue reports or patches, are very much
welcome.  Please clone the repository and make pull requests.

If you have questions or problems, please register an issue in the
issue tracker, or post to the [the mailing
list](http://groups.google.com/group/duke-dedup). If you don't want to
join the list you can always write to me at `larsga [a]
garshol.priv.no`, too.

## Using Duke with Maven

Duke is hosted in Maven Central, so if you want to use Duke it's as
easy as including the following in your pom file:

```
<dependency>
  <groupId>no.priv.garshol.duke</groupId>
  <artifactId>duke</artifactId>
  <version>1.2</version>
</dependency>
```

## Older documentation

[This blog post](http://www.garshol.priv.no/blog/217.html) describes
the basic approach taken to match records. It does not deal with the
Lucene-based lookup, but describes an early, slow O(n^2)
prototype. [This early
presentation](http://www.slideshare.net/larsga/deduplication)
describes the ideas behind the engine and the intended architecture