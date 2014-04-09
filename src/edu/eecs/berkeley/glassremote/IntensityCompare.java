package edu.eecs.berkeley.glassremote;

import java.util.Comparator;

public class IntensityCompare implements Comparator<PhysicalTarget> {
  @Override
  public int compare(PhysicalTarget arg0, PhysicalTarget arg1) {
    return (arg0.getIntensity() - arg1.getIntensity());
  }
}