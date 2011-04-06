
package no.priv.garshol.duke;

public class AssociationNoCleaner implements Cleaner {

  public String clean(String value) {
    char[] tmp = new char[value.length()];
    int pos = 0;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt();
      if (ch >= '0' && ch <= '9')
        tmp[pos++] = ch;
    }
    return new String(tmp, 0, pos);
  }
  
}