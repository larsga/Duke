
package no.priv.garshol.duke.genetic;

/**
 * Common code for the float aspects which obey the drift range.
 */
public abstract class FloatAspect extends Aspect {
  protected double drift(double original, double max, double min,
                         double float_drift_range) {
    // double upper = original + (float_drift_range / 2.0);
    // if (original + (float_drift_range / 2.0) > max)
    //   upper = max;
    // else if (original - (float_drift_range / 2.0) < min)
    //   upper = float_drift_range + min;

    // double delta = float_drift_range * Math.random();
    // return upper - delta;

    return Math.random() * (max - min) + min;
  }
}
