package edu.eecs.berkeley.glassremote;

import java.util.Comparator;

public class IntensityCompare implements Comparator<PhysicalTarget> {
  @Override
  public int compare(PhysicalTarget arg0, PhysicalTarget arg1) {
    return (arg1.getIntensity() - arg0.getIntensity());
  }
}