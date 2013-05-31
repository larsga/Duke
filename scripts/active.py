'''
Experimental attempt to port the genetic algorithm to active learning,
thus removing the need for a test file. This is a big deal, as it
means we only need the data sources set up in order to produce a full,
working configuration.

The basic idea is stolen from this paper
  http://svn.aksw.org/papers/2012/ESWC_EAGLE/public.pdf

The code is still fairly messy, and very inefficient, so more work is
needed. Plus, the F-measure is not computed the right way for our
purposes, so need to rework that, too.
'''

import random, sys, threading, time, os
from java.io import FileWriter
from java.util import ArrayList
from no.priv.garshol.duke import ConfigLoader, Processor, Property, DukeConfigException
from no.priv.garshol.duke.utils import ObjectUtils, LinkFileWriter, TestFileUtils
from no.priv.garshol.duke.matchers import TestFileListener, PrintMatchListener, AbstractMatchListener

SOUND = False # This works only on MacOS X, using the 'say' command
POPULATION_SIZE = 100
GENERATIONS = 100
EXAMPLES = 10

def score(count):
    return (POPULATION_SIZE - count) * (POPULATION_SIZE - (POPULATION_SIZE - count))

def score2(count):
    return score(count) + count

def score3(count):
    return count

class MostImportantExemplarsTracker(AbstractMatchListener):
    def __init__(self):
        self._idprops = config.getIdentityProperties()
        self._counts = {} # (id1, id2) -> count
        self._map = TestFileUtils.load(testfilename)

    def matches(self, r1, r2, conf):
        key = (self._getid(r1), self._getid(r2))
        self._counts[key] = self._counts.get(key, 0) + 1

    def _getid(self, r):
        for idprop in self._idprops:
            v = r.getValue(idprop.getName())
            if v:
                return v
        raise Exception("!!!")

    def get_examples(self):
        if highest == 0.0:
            func = score3
            print "PICK MOST COMMON"
        else:
            func = score2
            print "PICK MOST DIFFERENT"
        
        ex = [(func(count), key) for (key, count) in self._counts.items()]
        ex.sort()
        ex.reverse()
        # tmp = [(alldb.findRecordById(id1), alldb.findRecordById(id2), sc)
        #         for (sc, (id1, id2)) in ex
        #         if not self._map.containsKey(id1 + ',' + id2)]

        # for (r1, r2, sc) in tmp:
        #     PrintMatchListener.prettyCompare(r1, r2, float(sc), '=' * 75, properties)
        
        ex = ex[ : EXAMPLES]
        return [(alldb.findRecordById(id1), alldb.findRecordById(id2))
                for (sc, (id1, id2)) in ex
                if not self._map.containsKey(id1 + ',' + id2)]

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
    out = FileWriter(testfilename, True)
    writer = LinkFileWriter(out, config)
    
    for (r1, r2) in pick_examples(population):
        # print '=' * 75
        # PrintMatchListener.prettyPrint(r1, properties)
        # print
        # PrintMatchListener.prettyPrint(r2, properties)
        PrintMatchListener.prettyCompare(r1, r2, 0.0, '=' * 75, properties)
        print
        print 'SAME? (y/n)', 
        resp = (raw_input().strip().lower() == 'y')

        writer.write(r1, r2, resp)

    out.close()

def get_all(datasources):
    records = []
    for src in datasources:
        records += src.getRecords()
    return records

def round(num):
    return int(num * 100) / 100.0

def one_is_alive(threads):
    for thread in threads:
        if thread.isAlive():
            return True
    return False

def generate_random_configuration():
    c = GeneticConfiguration()
    c.set_threshold(round(random.uniform(lowlimit, 1.0)))
    for name in props:
        if name == "ID":
            prop = Property(name)
        else:
            low = round(random.uniform(0.0, 0.5))
            high = round(random.uniform(0.5, 1.0))
            prop = Property(name, random.choice(comparators), low, high)
        c.add_property(prop)
    return c

def show_best(best, show = True):
    if SOUND:
        os.system('say new best')
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
                c.add_property(Property(prop.getName()))
            else:
                c.add_property(Property(prop.getName(),
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

def evaluate(tstconf):
    if index.has_key(tstconf):
        return index[tstconf]

    testfile = TestFileListener(testfilename, config, False,
                                processor, False, False, True)
    testfile.setQuiet(True)

    try:
        run_with_config(tstconf, testfile)
    except DukeConfigException:
        # this means there's no way to get above the threshold in this config.
        # we consider that total failure, and just return.
        print "FAILED"
        index[tstconf] = 0.0
        return 0.0

    testfile.close()
    f = testfile.getFNumber()
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
        processor.linkRecords(config.getDataSources(2))

# (0) decode command-line
configfile = sys.argv[1]
testfilename = '/tmp/tst.file'
    
# (1) load configuration
config = ConfigLoader.load(configfile)
properties = config.getProperties()[:]
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
    processor = Processor(config)
    database = processor.getDatabase()
    if not linking:
        processor.index(config.getDataSources(), 40000)
    else:
        processor.index(config.getDataSources(1), 40000)
else:
    database = alldb
    
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
best = None
highest = 0.0

for generation in range(GENERATIONS):
    print "===== GENERATION %s ===================================" % generation

    # now, ask the user to give us some examples
    ask_the_user(population)
    
    # evaluate
    for ix in range(len(population)):
        c = population[ix]
        print c, "#", ix
        f = evaluate(c)
        print "  ", f, parent_info(c)

        if f > highest:
            best = c
            highest = f
            show_best(best, False)

            #if highest == 1.0:
            #    break
        
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

show_best(best)
