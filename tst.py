# coding: utf-8

import csv, sys
from java.util import HashMap
from java.util import ArrayList
from no.priv.garshol.duke import RecordImpl, Deduplicator, Database, Property
from no.priv.garshol.duke import MinimalCleaner, MatchListener, Cleaner

class Listener(MatchListener):

    def __init__(self):
        self._count = 0
    
    def matches(self, r1, r2, conf):
        self._count += 1
        print "MATCHES %s %s" % (conf, self._count)
        print [r1.getValue(p.getName()) for p in props]
        print [r2.getValue(p.getName()) for p in props]

class AssociationNoCleaner(Cleaner):

    def clean(self, v):
        keep = []
        for ch in v:
            if ch in "0123456789":
                keep.append(ch)
        v = "".join(keep)

        if not v:
            return None
        return v

class NameCleaner(Cleaner):

    def clean(self, v):
        v = " ".join(v.split())
        if not v:
            return None
        v = v.lower()

        v = v.replace("a/s", "as")
        
        return v

class BasicCleaner(Cleaner):

    def clean(self, v):
        v = " ".join(v.split())
        if not v:
            return None
        v = v.lower()
        return v

class AddressCleaner(Cleaner):

    def clean(self, v):
        v = " ".join(v.split())
        if not v:
            return None
        v = v.lower()

        v = v.replace("postboks", "boks")

        return v
    
# name cleaner:
#   as a/s
#   case
#   whitespace
#   %UTGÅR% -> støy
#   UTGÅR / UTFÅR
#   MVA
#   DIV. DIVISJON
#   * % ( ) -> ut
#   bruk
    
def load_file(file, relevant): # FIXME: must rewrite
    reader = csv.reader(open(file))
    
    headings = reader.next()
    columns_to_keep = [headings.index(col) for col in relevant]

    objects = []
    for row in reader:
        #print row[1]
        data = []
        for ix in columns_to_keep:
            try:
                data.append(row[ix])
            except IndexError:
                data.append(None) # we don't have a value
        objects.append(data)
        
    return objects

# set up database with properties etc

# read in data
print "Loading"
relevant = ["CUSTOMER_ID","NAME","ASSOCIATION_NO","COUNTRY_DB",
            "ADDRESS1","ADDRESS2","ZIP_CODE"]
rows = load_file("customers-merged.csv", relevant)

relevant = ["SUPPLIER_ID","NAME","ASSOCIATION_NO","COUNTRY_DB",
            "ADDRESS1","ADDRESS2","ZIP_CODE"]
rows2 = load_file("suppliers-merged.csv", relevant)

rows += rows2

#sys.exit()

# hack hack hack
import os
os.system("rm -rf test")

# set up
minimal = BasicCleaner()
props = [
    Property("ID", True, False, minimal, 0.0, 0.0),
    Property("NAME", False, True, NameCleaner(), 0.5, 0.9),
    Property("ASSOCIATION_NO", False, False, AssociationNoCleaner(), 0.1, 0.88),
    Property("COUNTRY_DB", False, False, None, 0.1, 0.5),
    Property("ADDRESS1", False, True, AddressCleaner(), 0.49, 0.7),
    Property("ADDRESS2", False, True, AddressCleaner(), 0.49, 0.7),
    Property("ZIP_CODE", False, False, minimal, 0.35, 0.55)
    ]
db = Database("test", props, 0.85, None) #Listener())
dedup = Deduplicator(db)

# building records
print "Building records (%s)" % len(rows)
records = ArrayList()
for row in rows:
    data = HashMap()
    for ix in range(len(props)):
        value = row[ix]
        if value:
            data.put(props[ix].getName(), ArrayList([value]))

    records.add(RecordImpl(data))

# process
print "Processing"
dedup.process(records)
    
# ask database for clusters?

# clean up
db.close()
