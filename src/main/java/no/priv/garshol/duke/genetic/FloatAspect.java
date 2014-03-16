
package no.priv.garshol.duke.genetic;

/**
 * Common code for the float aspects.
 */
public abstract class FloatAspect extends Aspect {
  protected double drift(double original, double max, double min) {

    // FIXME: the following is a chunk of experimental code that
    // hasn't been fully evaluated yet. leaving it in since it *may*
    // be reactivated, after more evaluation

    // <EXPERIMENT>
    // double upper = original + (float_drift_range / 2.0);
    // if (original + (float_drift_range / 2.0) > max)
    //   upper = max;
    // else if (original - (float_drift_range / 2.0) < min)
    //   upper = float_drift_range + min;

    // double delta = float_drift_range * Math.random();
    // return upper - delta;
    // </EXPERIMENT>

    return Math.random() * (max - min) + min;
  }
}
