
package no.priv.garshol.duke;

public class DummyLogger implements Logger {

  public void trace(String msg) {
  }
  
  public void debug(String msg) {
  }
  
  public void info(String msg) {
  }

  public void warn(String msg) {
  }

  public void warn(String msg, Throwable e) {
  }

  public void error(String msg) {
  }

  public void error(String msg, Throwable e) {
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
  
  public boolean isWarnEnabled() {
    return false;
  }
  
  public boolean isErrorEnabled() {
    return false;
  }
}