
package no.priv.garshol.duke.utils;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.DukeConfigException;

public class ObjectUtils {

  /**
   * Returns the enum constant from the given enum class representing
   * the constant with the given identifier/name.
   */
  public static Object getEnumConstantByName(Class klass, String name) {
    name = name.toUpperCase();
    Object c[] = klass.getEnumConstants();
    for (int ix = 0; ix < c.length; ix++)
      if (c[ix].toString().equals(name))
        return c[ix];
    throw new DukeConfigException("No such " + klass + ": '" + name + "'");
  }
  
  /**
   * Calls the named bean setter property on the object, converting
   * the given value to the correct type. Note that parameter 'prop'
   * is converted to a method name according to Lisp convention:
   * (foo-bar), and not the usual Java dromedaryCase (fooBar). So
   * "foo-bar" will become "setFooBar".
   *
   * <p>The value conversion is mostly straightforward, except that if
   * the type of the method's first parameter is not a
   * java.lang.Something, then the method will assume that the value
   * is the name of an object in the 'objects' map, and pass that.
   *
   * <p>Further, if the type is a Collection, the method will assume
   * the value is a list of object names separated by whitespace.
   */
  public static void setBeanProperty(Object object, String prop, String value,
                                     Map<String, Object> objects) {
    prop = makePropertyName(prop);
    try {
      boolean found = false;
      Method[] methods = object.getClass().getMethods();
      for (int ix = 0; ix < methods.length && !found; ix++) {
        if (!methods[ix].getName().equals(prop))
          continue;
        if (methods[ix].getParameterTypes().length != 1)
          continue;

        Class type = methods[ix].getParameterTypes()[0];
        methods[ix].invoke(object, convertToType(value, type, objects));
        found = true;
      }

      if (!found)
        throw new DukeConfigException("Couldn't find method '" + prop + "' in " +
                                      "class " + object.getClass());
    } catch (IllegalArgumentException e) {
      throw new DukeConfigException("Couldn't set bean property " + prop +
                                    " on object of class " + object.getClass() +
                                    ": " + e);
    } catch (IllegalAccessException e) {
      throw new DukeException(e);
    } catch (InvocationTargetException e) {
      throw new DukeConfigException("Couldn't set bean property " + prop +
                                    " on object of class " + object.getClass() +
                                    ": " + e);
    }
  }

  // public because it's used by other packages that use Duke
  public static String makePropertyName(String name) {
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

  private static Object convertToType(String value, Class type,
                                      Map<String, Object> objects) {
    if (type == String.class)
      return value;
    else if (type == Integer.TYPE)
      return Integer.parseInt(value);
    else if (type == Boolean.TYPE)
      return Boolean.parseBoolean(value);
    else if (type == Double.TYPE)
      return Double.parseDouble(value);
    else if (type == Float.TYPE)
      return Float.parseFloat(value);
    else if (type == Character.TYPE) {
      if (value.length() != 1)
        throw new DukeConfigException("String '" + value + "' is not a character");
      return new Character(value.charAt(0));
    } else if (type.isEnum())
      return getEnumConstantByName(type, value);
    else if (type.equals(Collection.class)) {
      Collection coll = new ArrayList();
      String[] values = StringUtils.split(value);
      for (int ix = 0; ix < values.length; ix++) {
        Object object = objects.get(values[ix]);
        if (object == null)
          object = values[ix];
        coll.add(object);
      }
      return coll;
    } else {
      // now we check if there's an object by this name. if there is
      // we return that, otherwise we return the value itself.
      Object object = objects.get(value);
      if (object != null)
        return object;
      else
        return value;
    }
  }

  public static Object instantiate(String klassname) {
    try {
      Class klass = Class.forName(klassname);
      return klass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
  
}