
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
    private String[] stopwords;
    private ArrayList<String> wordsList = new ArrayList<String>();


    public StopwordsCleaner() {
        this.sub = new LowerCaseNormalizeCleaner();

        try {
            this.stopwords = loadStopwords();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String clean(String value) {

        value = sub.clean(value);
        if (value == null || value.equals(""))
            return value;


        String[] words = value.split(" ");
        for (String word : words) {
            wordsList.add(word);
        }

        for (int j = 0; j < stopwords.length; j++) {
            if (wordsList.contains(stopwords[j])) {
                wordsList.remove(stopwords[j]);
            }
        }

        return String.join(" ",wordsList);

    }

    private String[] loadStopwords() throws IOException {
        String mapfile = "no/priv/garshol/duke/english-stopwords.txt";


        BufferedReader in = new BufferedReader(new FileReader(mapfile));
        String str;

        List<String> list = new ArrayList<String>();
        while((str = in.readLine()) != null){
            list.add(str);
        }

        String[] stopwords = list.toArray(new String[0]);

        in.close();
        return stopwords;
    }

}

