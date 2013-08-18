
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.DukeException;

// The formula is taken from here:
// http://www.movable-type.co.uk/scripts/latlong.html

/**
 * Comparator which compares two geographic positions given by
 * coordinates by the distance between them along the earth's surface.
 * It assumes the parameters are of the form '59.917516,10.757933',
 * where the numbers are degrees latitude and longitude, respectively.
 *
 * <p>The computation simply assumes a sphere with a diameter of 6371
 * kilometers, so no particular geodetic model is assumed. WGS83
 * coordinates will work fine, while UTM coordinates will not work.
 *
 * @since 1.0
 */
public class GeopositionComparator implements Comparator {
  private static final double R = 6371000; // in meters
  private double maxdist; // we default to 100 meters as the max
  private boolean strict; // whether to fail on errors

  public GeopositionComparator() {
    this.maxdist = 100;
  }
  
  public boolean isTokenized() {
    return false;
  }
  
  public double compare(String v1, String v2) {
    try {
      double lat1 = getLatitude(v1);
      double lon1 = getLongitude(v1);
      double lat2 = getLatitude(v2);
      double lon2 = getLongitude(v2);

      if (!valid(lat1, lon1) || !valid(lat2, lon2))
        return 0.5;
        
      double dist = distance(lat1, lon1, lat2, lon2);
      if (dist > maxdist)
        return 0.0;

      return ((1.0 - (dist / maxdist)) * 0.5 ) + 0.5;
    } catch (NumberFormatException e) {
      if (strict)
        throw new DukeException("Invalid number: " + e, e);
      return 0.5;
    }
  }

  public void setStrict(boolean strict) {
    this.strict = strict;
  }

  public void setMaxDistance(double maxdist) {
    this.maxdist = maxdist;
  }

  public double getMaxDistance() {
    return maxdist;
  }

  /**
   * Returns the distance between the two points in meters.
   */
  public static double distance(double lat1, double lon1,
                                double lat2, double lon2) {
    double dLat = Math.toRadians(lat2-lat1);
    double dLon = Math.toRadians(lon2-lon1);
    lat1 = Math.toRadians(lat1);
    lat2 = Math.toRadians(lat2);

    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
    return R * c;
  }

  private static double getLongitude(String v) {
    int pos = v.indexOf(',');
    if (pos == -1)
      throw new NumberFormatException("No comma separating lat/long");

    return Double.parseDouble(v.substring(pos + 1));
  }

  private static double getLatitude(String v) {
    int pos = v.indexOf(',');
    if (pos == -1)
      throw new NumberFormatException("No comma separating lat/long");

    return Double.parseDouble(v.substring(0, pos));
  }

  private boolean valid(double lat, double lon) {
    if (lat > 90.0 || lat < -90.0 || // north pole: 90 deg north
        lon > 180.0 || lon < -180.0) { // date line at -180 and +180
      if (strict)
        throw new DukeException("Position outside legal range: " + lat + ", " +
                                lon);
      return false;
    } else
      return true;
  }
}