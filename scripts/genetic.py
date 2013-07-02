'''
Genetic algorithm for automatically creating a configuration. See
  http://code.google.com/p/duke/wiki/GeneticAlgorithm

for information on how to use it.
'''

import random, sys, threading, time, os
from java.util import ArrayList
from no.priv.garshol.duke import ConfigLoader, Processor, PropertyImpl, DukeConfigException
from no.priv.garshol.duke.utils import ObjectUtils
from no.priv.garshol.duke.matchers import TestFileListener

SOUND = False # This works only on MacOS X, using the 'say' command
POPULATION_SIZE = 100
POPULATIONS = 100
SHOW_CONFIGS = True

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
            prop = PropertyImpl(name)
        else:
            low = round(random.uniform(0.0, 0.5))
            high = round(random.uniform(0.5, 1.0))
            prop = PropertyImpl(name, random.choice(comparators), low, high)
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

def evaluate(tstconf):
    if index.has_key(tstconf):
        return index[tstconf]
    
    config.getProperties().clear()
    config.setThreshold(tstconf.get_threshold())
    try:
        config.setProperties(ArrayList(tstconf.get_properties()))
    except DukeConfigException:
        # this means there's no way to get above the threshold in this config.
        # we consider that total failure, and just return.
        index[tstconf] = 0.0
        return 0.0
        
    testfile = TestFileListener(testfilename, config, False,
                                processor, False, True)
    testfile.setQuiet(True)

    processor.getListeners().clear()
    processor.addMatchListener(testfile)

    if not linking:
        processor.linkRecords(config.getDataSources())
    else:
        processor.linkRecords(config.getDataSources(2))

    f = testfile.getFNumber()
    index[tstconf] = f
    return f

# (0) decode command-line
(configfile, testfilename) = sys.argv[1 : ]
    
# (1) load configuration
config = ConfigLoader.load(configfile)
linking = config.getDataSources().isEmpty()
if linking:
    lowlimit = 0.0
else:
    lowlimit = 0.4

# (2) index up all the data
processor = Processor(config)
database = processor.getDatabase()
if not linking:
    processor.index(config.getDataSources(), 40000)
else:
    processor.index(config.getDataSources(1), 40000)

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

for generation in range(POPULATIONS):
    print "===== GENERATION %s ===================================" % generation

    for ix in range(len(population)):
        c = population[ix]
        if SHOW_CONFIGS:
            print c, "#", ix
        f = evaluate(c)
        if SHOW_CONFIGS:
            print "  ", f, parent_info(c)

        if f > highest:
            best = c
            highest = f
            if SHOW_CONFIGS:
                show_best(best, False)

            if highest == 1.0:
                break

    # if we achieved a perfect score, just stop
    if highest == 1.0:
        break
        
    # make new generation
    population = sorted(population, key = lambda c: 1.0 - index[c])
    for ix in range(len(population)):
        population[ix].set_rank(ix + 1)
    if SHOW_CONFIGS:
        print "SUMMARY:", [index[c] for c in population], "avg:", (sum([index[c] for c in population]) / float(POPULATION_SIZE))
    else:
        print 'BEST: ', index[population[0]]
    
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
