
package no.priv.garshol.duke.cleaners;

public class NorwegianAddressCleaner extends AbstractRuleBasedCleaner {
  private LowerCaseNormalizeCleaner sub;

  public NorwegianAddressCleaner() {
    super();
    this.sub = new LowerCaseNormalizeCleaner();

    add("^(co/ ?)", "c/o ");
    add("^(c\\\\o)", "c/o");
    add("[A-Za-z]+(g\\.) [0-9]+", "gata");
    add("[A-Za-z]+ (gt?\\.?) [0-9]+", "gate");
    add("[A-Za-z]+(v\\.) [0-9]+", "veien");
    add("[A-Za-z]+ (v\\.?) [0-9]+", "vei");
    add("[A-Za-z]+(vn\\.?)[0-9]+", "veien ");
    add("[A-Za-z]+(vn\\.?) [0-9]+", "veien");
    add("[A-Za-z]+(gt\\.?) [0-9]+", "gata");
    add("[A-Za-z]+(gaten) [0-9]+", "gata");
    add("(\\s|^)(pb\\.?) [0-9]+", "postboks", 2);
    add("(\\s|^)(boks) [0-9]+", "postboks", 2);
    add("[A-Za-z]+ [0-9]+(\\s+)[A-Za-z](\\s|$)", "");
    add("[A-Za-z]+(gata|veien)()[0-9]+[a-z]?(\\s|$)", " ");

    // FIXME: not sure about the following rules
    add("postboks\\s+[0-9]+(\\s*-\\s*)", " ");
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