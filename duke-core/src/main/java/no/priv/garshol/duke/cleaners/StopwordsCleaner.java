
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * A cleaner which removes english stopwords from a string.
 */

public class StopwordsCleaner implements Cleaner {
    private LowerCaseNormalizeCleaner sub;
    HashSet<String> stopwords = new HashSet<String>();
    private ArrayList<String> wordsList = new ArrayList<String>();


    public StopwordsCleaner() {
        this.sub = new LowerCaseNormalizeCleaner();

        try {
            this.stopwords = loadStopwords();
        } catch (DukeException e) {
            throw new RuntimeException(e);
        }
    }


    public String clean(String value) {

        value = sub.clean(value);
        if (value == null || value.equals(""))
            return value;


        for (String word : words) {
          if (!stopwords.contains(word))
            wordsList.add(word);
        }

        return String.join(" ",wordsList);

    }

    private HashSet<String> loadStopwords() throws IOException {
        String mapfile = "no/priv/garshol/duke/english-stopwords.txt";

        BufferedReader in = new BufferedReader(new FileReader(mapfile));
        String str;

        HashSet<String> stopwords = new HashSet<String>();
        while((str = in.readLine()) != null){
            stopwords.add(str);
        }

        in.close();
        return stopwords;
    }

}

