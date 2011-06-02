
package no.priv.garshol.duke;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the result of a SPARQL query.
 */
public class SparqlResult {
  private List<String> variables;
  private List<String[]> rows;

  public SparqlResult() {
    this.variables = new ArrayList();
    this.rows = new ArrayList();
  }

  public List<String> getVariables() {
    return variables;
  }
  
  public List<String[]> getRows() {
    return rows;
  }

  // package internal
  void addVariable(String variable) {
    variables.add(variable);
  }

  void addRow(String[] row) {
    rows.add(row);
  }
}