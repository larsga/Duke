
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.utils.StringUtils;

public class NorwegianCompanyNameCleaner extends AbstractRuleBasedCleaner {
  private LowerCaseNormalizeCleaner sub;

  public NorwegianCompanyNameCleaner() {
    super();
    this.sub = new LowerCaseNormalizeCleaner();

    add("\\s(a/s)(\\s|$)", "as");
    add("\\s(a\\\\s)(\\s|$)", "as");
    add("^(a/s)\\s", "as");
    add("^(a\\\\s)\\s", "as");
    add("\\s(a/l)(\\s|$)", "al");
    add("^(a/l)\\s", "al");
  }

  public String clean(String value) {
    // get rid of commas
    value = StringUtils.replaceAnyOf(value, ",().-_", ' ');
    
    // do basic cleaning
    value = sub.clean(value);
    if (value == null || value.equals(""))
      return "";

    // perform pre-registered transforms
    value = super.clean(value);

    // renormalize whitespace, since being able to replace tokens with spaces
    // makes writing transforms easier
    value = StringUtils.normalizeWS(value);

    // transforms:
    //   "as foo bar" -> "foo bar as"
    //   "al foo bar" -> "foo bar al"
    if (value.startsWith("as ") || value.startsWith("al "))
      value = value.substring(3) + ' ' + value.substring(0, 2);

    return value;
  }  
}