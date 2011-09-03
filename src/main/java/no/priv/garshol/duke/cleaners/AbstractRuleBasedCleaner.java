
package no.priv.garshol.duke.cleaners;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import no.priv.garshol.duke.Cleaner;

public abstract class AbstractRuleBasedCleaner implements Cleaner {
  private List<Transform> transforms;

  public AbstractRuleBasedCleaner() {
    this.transforms = new ArrayList();
  }

  public String clean(String value) {
    // perform pre-registered transforms
    for (Transform t : transforms)
      value = t.transform(value);
    
    return value;
  }

  public void add(String regex, String replacement) {
    add(regex, replacement, 1);
  }

  public void add(String regex, String replacement, int groupno) {
    transforms.add(new Transform(regex, replacement, groupno));
  }
}