
package no.priv.garshol.duke;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class ObjectUtils {

  // uses Lisp convention: foo-bar, not dromedaryCase fooBar
  public static void setBeanProperty(Object object, String prop, String value) {
    prop = makePropertyName(prop);
    try {
      Method method = object.getClass().getMethod(prop, "".getClass());
      method.invoke(object, value);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static String makePropertyName(String name) {
    char[] buf = new char[name.length() + 3];
    int pos = 0;
    buf[pos++] = 's';
    buf[pos++] = 'e';
    buf[pos++] = 't';
    
    for (int ix = 0; ix < name.length(); ix++) {
      char ch = name.charAt(ix);
      if (ix == 0)
        ch = Character.toUpperCase(ch);
      else if (ch == '-') {
        ix++;
        if (ix == name.length())
          break;
        ch = Character.toUpperCase(name.charAt(ix));
      }

      buf[pos++] = ch;
    }

    return new String(buf, 0, pos);
  }
  
}