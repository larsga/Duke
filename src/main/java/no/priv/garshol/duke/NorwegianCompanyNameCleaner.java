
package no.priv.garshol.duke;

public class NorwegianCompanyNameCleaner extends AbstractRuleBasedCleaner {
  private LowerCaseNormalizeCleaner sub;

  public NorwegianCompanyNameCleaner() {
    super();
    this.sub = new LowerCaseNormalizeCleaner();

    add("\\s(a/s)(\\s|$)", "as");
  }

  public String clean(String value) {
    // get rid of commas
    value = value.replace(',', ' ');
    
    // do basic cleaning
    value = sub.clean(value);
    if (value == null || value.equals(""))
      return value;

    // perform pre-registered transforms
    value = super.clean(value);
    
    return value;
  }  
}