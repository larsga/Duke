package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

public class GaussianComparator implements Comparator {

    private Double sigma = 1.0;
    private Double sigmaSq = 1.0;
    private Double powerCoef = -0.5;
    private final Double sqrtTwoPi = 2.50662827;
    private Double linearCoef = 1.0 / sqrtTwoPi;

    public boolean isTokenized() {
        return false;
    }

    public void setSigma(Double sigma) {
        this.sigma = sigma;
        updateValues();
    }

    // We return the probability (almost) of drawing v1 from a gaussian with mean v2 and standard deviation sigma.
    // It's not the honest probability, because we don't multiply by the normalizing factor (linearCoef).
    // This makes values somewhat easier to think about, because you don't have to know the maximum value of the
    // normal distribution for your chosen sigma.
    public double compare(String v1, String v2) {
        // begin like NumericComparator
        double d1 = 0.0;
        double d2 = 0.0;
        try {
            d1 = Double.parseDouble(v1);
            d2 = Double.parseDouble(v2);
        } catch (NumberFormatException e) {
            return 0.5; // we just ignore this. whether it's wise I'm not sure
        }

        double diff = Math.abs(d1 - d2);
        return Math.exp(powerCoef * diff * diff);
    }

    private void updateValues() {
        sigmaSq = sigma * sigma;
        powerCoef = -1.0 / (2.0 * sigmaSq);
        linearCoef = 1.0 / (sigma * sqrtTwoPi);
    }
}
