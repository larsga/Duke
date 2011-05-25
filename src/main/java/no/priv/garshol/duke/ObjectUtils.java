
package no.priv.garshol.duke;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class ObjectUtils {

  // uses Lisp convention: foo-bar, not dromedaryCase fooBar
  public static void setBeanProperty(Object object, String prop, String value) {
    prop = makePropertyName(prop);
    try {
      Method[] methods = object.getClass().getMethods();
      for (int ix = 0; ix < methods.length; ix++) {
        if (!methods[ix].getName().equals(prop))
          continue;
        if (methods[ix].getParameterTypes().length != 1)
          continue;

        Class type = methods[ix].getParameterTypes()[0];
        methods[ix].invoke(object, convertToType(value, type));
        break; // ok, we found it
      }
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

  private static Object convertToType(String value, Class type) {
    if (type == Integer.TYPE)
      return Integer.parseInt(value);
    else if (type == Boolean.TYPE)
      return Boolean.parseBoolean(value);
    else
      return value;
  }
  
}