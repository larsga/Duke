
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.utils.StringUtils;

public class NorwegianCompanyNameCleaner extends AbstractRuleBasedCleaner {
  private LowerCaseNormalizeCleaner sub;

  public NorwegianCompanyNameCleaner() {
    super();
    this.sub = new LowerCaseNormalizeCleaner();

    add("\\s(a/s)(\\s|$)", "as");
  }

  public String clean(String value) {
    // get rid of commas
    value = StringUtils.replaceAnyOf(value, ",().", ' ');
    
    // do basic cleaning
    value = sub.clean(value);
    if (value == null || value.equals(""))
      return "";

    // perform pre-registered transforms
    value = super.clean(value);

    // renormalize whitespace, since being able to replace tokens with spaces
    // makes writing transforms easier
    return StringUtils.normalizeWS(value);
  }  
}