
package no.priv.garshol.duke;

public class DummyLogger implements Logger {

  public void trace(String msg) {
  }
  
  public void debug(String msg) {
  }
  
  public void info(String msg) {
  }
  
  public boolean isTraceEnabled() {
    return false;
  }
  
  public boolean isDebugEnabled() {
    return false;
  }
  
  public boolean isInfoEnabled() {
    return false;
  }
}