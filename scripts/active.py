'''
Experimental attempt to port the genetic algorithm to active learning,
thus removing the need for a test file. This is a big deal, as it
means we only need the data sources set up in order to produce a full,
working configuration.

The basic idea is stolen from this paper
  http://svn.aksw.org/papers/2012/ESWC_EAGLE/public.pdf

The code is still fairly messy, and very inefficient, so more work is
needed.
'''

# Fine-tune comparators in more detail

# TestFileListener and multi-threading?

import random, sys, threading, time, os
from java.io import FileWriter
from java.util import ArrayList
from no.priv.garshol.duke import ConfigLoader, Processor, PropertyImpl, DukeConfigException, InMemoryLinkDatabase, Link, LinkKind, LinkStatus
from no.priv.garshol.duke.utils import ObjectUtils, LinkFileWriter, TestFileUtils, LinkDatabaseUtils
from no.priv.garshol.duke.matchers import TestFileListener, PrintMatchListener, AbstractMatchListener

POPULATION_SIZE = 100
GENERATIONS = 100
EXAMPLES = 10

def score(count):
    '''Scoring function from original paper, slightly skewed towards wrong
    matches.'''
    return (POPULATION_SIZE - count) * (POPULATION_SIZE - (POPULATION_SIZE - count))

def score2(count):
    'Adjusted scoring function to balance it.'
    return score(count) + count

def score3(count):
    'Helper scoring function to emphasize correct matches.'
    return count

class MostImportantExemplarsTracker(AbstractMatchListener):
    def __init__(self):
        self._counts = {} # (id1, id2) -> count

    def matches(self, r1, r2, conf):
        key = makekey(getid(r1), getid(r2))
        self._counts[key] = self._counts.get(key, 0) + 1

    def get_examples(self):
        if generation == 0:
            func = score3
        else:
            func = score2
        ex = [(func(count), (id1, id2)) for ((id1, id2), count)
              in self._counts.items()
              if not linkdb.inferLink(id1, id2)]
        ex.sort()
        ex.reverse()
        ex = ex[ : EXAMPLES]
        return [(alldb.findRecordById(id1), alldb.findRecordById(id2))
                for (sc, (id1, id2)) in ex]

def makekey(id1, id2):
    if id1 < id2:
        return (id1, id2)
    else:
        return (id2, id1)
    
def getid(r):
    for idprop in idprops:
        v = r.getValue(idprop.getName())
        if v:
            return v
    raise Exception("!!!")
    
def pick_examples(population):
    tracker = MostImportantExemplarsTracker()
    for tstconf in population:
        run_with_config(tstconf, tracker)
    return tracker.get_examples()

# def pick_examples(population):
#     set1 = get_all(config.getDataSources(1))
#     set2 = get_all(config.getDataSources(2))

#     return [(random.choice(set1), random.choice(set2)) for ix
#             in range(EXAMPLES)]

def ask_the_user(population):
    for (r1, r2) in pick_examples(population):
        PrintMatchListener.prettyCompare(r1, r2, 0.0, '=' * 75, properties)
        print
        print 'SAME? (y/n)',
        if golddb:
            link = golddb.inferLink(getid(r1), getid(r2))
            if not link:
                print '  ASSUMING FALSE'
                resp = False
            else:
                resp = link.getKind() == LinkKind.SAME
                print '  ORACLE SAYS', resp
        else:
            resp = (raw_input().strip().lower() == 'y')

        if resp:
            kind = LinkKind.SAME
        else:
            kind = LinkKind.DIFFERENT
        linkdb.assertLink(Link(getid(r1), getid(r2), LinkStatus.ASSERTED, kind))

        outf = open('answers.txt', 'a')
        outf.write(str(Link(getid(r1), getid(r2), LinkStatus.ASSERTED, kind)) + '\n')
        outf.close()

def get_all(datasources):
    records = []
    for src in datasources:
        records += src.getRecords()
    return records

def round(num):
    return int(num * 100) / 100.0

def generate_random_configuration():
    c = GeneticConfiguration()
    c.set_threshold(round(random.uniform(lowlimit, 1.0)))
    for name in props:
        if name == "ID":
            prop = PropertyImpl(name)
        else:
            low = round(random.uniform(0.0, 0.5))
            high = round(random.uniform(0.5, 1.0))
            prop = PropertyImpl(name, random.choice(comparators), low, high)
        c.add_property(prop)
    return c

def show_best(best, show = True):
    print
    print "BEST SO FAR: %s" % index[best]
    if show:
        print best
    parent = best.get_parent()
    while parent:
        print "DERIVED FROM:", parent, index[parent]
        parent = parent.get_parent()
    print

def parent_info(c):
    parent = c.get_parent()
    if not parent:
        return ""
    return "#%s, %s" % (parent.get_rank(), index[parent])

def shortname(comparator):
    comparator = str(comparator) #  no...comparators.NumericComparator@6c742397
    end = comparator.find("@")
    start = comparator.rfind(".")
    return comparator[start + 1 : end]

class Aspect:
    """Represents one specific aspect of a configuration that might be
    changed by genetic programming."""

class ThresholdAspect(Aspect):

    def modify(self, conf):
        conf.set_threshold(round(random.uniform(lowlimit, 1.0)))

    def get(self, conf):
        return conf.get_threshold()

    def set(self, conf, value):
        conf.set_threshold(value)

class PropertyPropertyAspect(Aspect):

    def __init__(self, name, method):
        self._name = name
        self._method = method

    def modify(self, conf):
        prop = self._get_prop(conf)
        if self._method == "setComparator":
            value = random.choice(comparators)
        elif self._method == "setLowProbability":
            value = round(random.uniform(0.0, 0.5))
        else:
            value = round(random.uniform(0.5, 1.0))
        getattr(prop, self._method)(value)

    def get(self, conf):
        prop = self._get_prop(conf)
        method = "g" + self._method[1 : ]
        return getattr(prop, method)() 

    def set(self, conf, value):
        prop = self._get_prop(conf)
        getattr(prop, self._method)(value)

    def _get_prop(self, conf):
        for prop in conf.get_properties():
            if prop.getName() == self._name:
                return prop            

class GeneticConfiguration:

    def __init__(self, parent = None):
        self._props = []
        self._threshold = 0.0
        self._parent = parent
        self._rank = None

    def set_threshold(self, threshold):
        self._threshold = threshold

    def add_property(self, prop):
        self._props.append(prop)

    def get_properties(self):
        return self._props

    def get_threshold(self):
        return self._threshold

    def get_parent(self):
        return self._parent

    def set_rank(self, rank):
        self._rank = rank

    def get_rank(self):
        return self._rank
    
    def make_new(self, population):
        # either we make a number or random modifications, or we mate.
        # draw a number, if 0 modifications, we mate.
        mods = random.randint(0, 3)
        if mods:
            return self._mutate(mods)
        else:
            return self._mate(random.choice(population))

    def _mutate(self, mods):
        c = self._copy()
        for ix in range(mods):
            aspect = random.choice(aspects)
            aspect.modify(c)
        return c

    def _mate(self, other):
        c = self._copy()
        for aspect in aspects:
            aspect.set(c, aspect.get(random.choice([self, other])))
        return c

    def _copy(self):
        c = GeneticConfiguration(self)
        c.set_threshold(self._threshold)
        for prop in self.get_properties():
            if prop.getName() == "ID":
                c.add_property(PropertyImpl(prop.getName()))
            else:
                c.add_property(PropertyImpl(prop.getName(),
                                            prop.getComparator(),
                                            prop.getLowProbability(),
                                            prop.getHighProbability()))
        return c
        
    def __str__(self):
        props = ["[Property %s %s %s %s" % (prop.getName(),
                                            shortname(prop.getComparator()),
                                            prop.getLowProbability(),
                                            prop.getHighProbability())
                 for prop in self._props]
        return "[GeneticConfiguration %s %s]" % \
            (self._threshold, " ".join(map(str, props)))

    def __eq__(self, other):
        if self._threshold != other.get_threshold():
            return False

        for myprop in self._props:
            for yourprop in other.get_properties():
                if myprop.getName() == yourprop.getName():
                    if myprop.getComparator() != yourprop.getComparator():
                        return False
                    if myprop.getLowProbability() != yourprop.getLowProbability():
                        return False
                    if myprop.getHighProbability() != yourprop.getHighProbability():
                        return False
                    break

        return True

    def __hash__(self):
        h = hash(self._threshold)
        for prop in self._props:
            h += hash(prop.getComparator())
            h += hash(prop.getLowProbability())
            h += hash(prop.getHighProbability())
        return h

def evaluate(tstconf, linkdb, pessimistic = False):
    # if index.has_key(tstconf):
    #     return index[tstconf]

    testfile = TestFileListener(linkdb, config, False,
                                processor, False, True)
    testfile.setQuiet(True)
    testfile.setPessimistic(pessimistic)

    try:
        run_with_config(tstconf, testfile)
    except DukeConfigException:
        # this means there's no way to get above the threshold in this config.
        # we consider that total failure, and just return.
        print "FAILED"
        index[tstconf] = 0.0
        return 0.0

    f = testfile.getFNumber()
    if f > 1.0:
        sys.exit(1)
    index[tstconf] = f
    return f

def run_with_config(tstconf, listener):
    config.getProperties().clear()
    config.setThreshold(tstconf.get_threshold())
    config.setProperties(ArrayList(tstconf.get_properties()))

    processor.getListeners().clear()
    processor.addMatchListener(listener)

    if not linking:
        processor.linkRecords(config.getDataSources())
    else:
        processor.linkRecords(config.getDataSources(2), False)

# (0) decode command-line
configfile = sys.argv[1]
linkdb = InMemoryLinkDatabase()
linkdb.setDoInference(True)
if len(sys.argv) == 3:
    golddb = InMemoryLinkDatabase()
    golddb.setDoInference(True)
    LinkDatabaseUtils.loadTestFile(sys.argv[2], golddb)
else:
    golddb = None
    
# (1) load configuration
config = ConfigLoader.load(configfile)
properties = config.getProperties()[:]
idprops = config.getIdentityProperties()
linking = not config.isDeduplicationMode()
if linking:
    lowlimit = 0.0
else:
    lowlimit = 0.4

# (2) index up all the data
processor = Processor(config)
alldb = processor.getDatabase()
if not linking:
    processor.index(config.getDataSources(), 40000)
else:
    processor.index(config.getDataSources(1), 40000)
    processor.index(config.getDataSources(2), 40000)

if linking:
    config.setPath(config.getPath() + '2') # AHEM...
    processor = Processor(config)
    database = processor.getDatabase()
    if not linking:
        processor.index(config.getDataSources(), 40000)
    else:
        processor.index(config.getDataSources(1), 40000)
else:
    database = alldb

try:
    import os
    os.unlink('answers.txt')
except OSError:
    pass
    
# (3) actual genetic stuff
pkg = "no.priv.garshol.duke.comparators."
comparators = ["DiceCoefficientComparator",
               "DifferentComparator",
               "ExactComparator",
               "JaroWinkler",
               "JaroWinklerTokenized",
               "Levenshtein",
               "NumericComparator",
               "PersonNameComparator",
               "SoundexComparator",
               "WeightedLevenshtein",
               "NorphoneComparator",
               "MetaphoneComparator",
               "QGramComparator",
               "GeopositionComparator"]
comparators = [ObjectUtils.instantiate(pkg + c) for c in comparators]

# (a) generate 100 random configurations
if linking:
    src = config.getDataSources(2).iterator().next()
else:
    src = config.getDataSources().iterator().next()
props = [col.getProperty() for col in src.getColumns()]

# preparation
aspects = [ThresholdAspect()]
for prop in props:
    if prop != "ID":
        aspects.append(PropertyPropertyAspect(prop, "setComparator"))
        aspects.append(PropertyPropertyAspect(prop, "setLowProbability"))
        aspects.append(PropertyPropertyAspect(prop, "setHighProbability"))

population = []
for ix in range(POPULATION_SIZE):
    c = generate_random_configuration()
    population.append(c)

# (b) evaluate each configuration by running through data
index = {}

for generation in range(GENERATIONS):
    print "===== GENERATION %s ===================================" % generation

    # now, ask the user to give us some examples
    if generation % 2 == 0 or generation == 1:
        ask_the_user(population)
        best = None
        highest = 0.0
    
    # evaluate
    for ix in range(len(population)):
        c = population[ix]
        print c, "#", ix
        f = evaluate(c, linkdb)
        print "  ", f, parent_info(c)

        if f > highest:
            best = c
            highest = f
            show_best(best, False)
        
    # make new generation
    population = sorted(population, key = lambda c: 1.0 - index[c])
    for ix in range(len(population)):
        population[ix].set_rank(ix + 1)
    print "SUMMARY:", [index[c] for c in population], "avg:", (sum([index[c] for c in population]) / float(POPULATION_SIZE))
    
    # ditch lower quartile ++
    population = population[ : int(POPULATION_SIZE * 0.7)]
    # double upper quartile
    population = (population[ : int(POPULATION_SIZE * 0.02)] +
                  population[ : int(POPULATION_SIZE * 0.03)] +
                  population[: int(POPULATION_SIZE * 0.25)] +
                  population[: int(POPULATION_SIZE * 0.25)] +
                  population[int(POPULATION_SIZE * 0.25) : ])

    population = [c.make_new(population) for c in population]

    if golddb:
        print "EVALUATING BEST:"
        print "  ", best, evaluate(best, golddb, True), parent_info(best)

show_best(best)
