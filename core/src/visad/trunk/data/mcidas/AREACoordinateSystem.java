//
// AREACoordinateSystem.java
//

/*
The software in this file is Copyright(C) 1998 by Tom Whittaker.
It is designed to be used with the VisAD system for interactive
analysis and visualization of numerical data.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package visad.data.mcidas;

import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.McIDASException;

import java.awt.geom.Rectangle2D;

import visad.CoordinateSystem;
import visad.CoordinateSystemException;
import visad.Gridded2DSet;
import visad.QuickSort;
import visad.RealTupleType;
import visad.Set;
import visad.Unit;
import visad.VisADException;

/**
 * AREACoordinateSystem is the VisAD CoordinateSystem class
 * for conversions to/from (Latitude, Longitude) and Cartesian (element,line),
 * and with Latitude and Longitude in degrees.
 */
public class AREACoordinateSystem
    extends visad.georef.MapProjection
{

  private AREAnav anav = null;
  private int lines;
  private int elements;

  private static Unit[] coordinate_system_units =
    {null, null};

  /** create a AREA coordinate system from the Area file's
    * directory and navigation blocks.
    *
    * This routine uses a flipped Y axis (first line of
    * the image file is number 0)
    *
    * @param reference is the CoordinateSystem reference
    * @param dir[] is the AREA file directory block
    * @param nav[] is the AREA file navigation block
    *
    */
  public AREACoordinateSystem(RealTupleType reference, int[] dir,
                                 int[] nav) throws VisADException {

    super(reference, coordinate_system_units);
    try {
      anav = AREAnav.makeAreaNav(nav);
    } catch (McIDASException excp) {
      throw new CoordinateSystemException(
          "AREACoordinateSystem: problem creating navigation" + excp);
    }
    anav.setImageStart(dir[5], dir[6]);
    anav.setRes(dir[11], dir[12]);
    anav.setStart(1,1);
    anav.setMag(1,1);
    lines = dir[8];
    elements = dir[9];
    anav.setFlipLineCoordinates(dir[8]); // invert Y axis coordinates
  }


  /** convert from image element,line to latitude,longitude
    *
    * @param tuples contains the element,line pairs to convert
    *
    */
  public double[][] toReference(double[][] tuples) throws VisADException {
    if (tuples == null || tuples.length != 2) {
      throw new CoordinateSystemException("AREACoordinateSystem." +
             "toReference: tuples wrong dimension");
    }

    if (anav == null) {
      throw new CoordinateSystemException("AREA O & A data not availble");
    }

    int[] nums = new int[2];
    double[] mins = new double[2];
    double[] maxs = new double[2];
    double[][] newval = makeSpline(tuples, mins, maxs, nums);
    if (newval != null) {
// System.out.println("new 1 " + tuples[0].length + " " + newval[0].length);
      double[][] newtrans = anav.toLatLon(newval);

      int len = tuples[0].length;
      double[][] misstrans = new double[2][len];
      int[][] miss_to_trans = new int[1][len];
      double[][] val = applySpline(tuples, mins, maxs, nums, newtrans,
                                   misstrans, miss_to_trans);
      if (miss_to_trans[0] != null) {
        double[][] newmiss = anav.toLatLon(misstrans);
        for (int i=0; i<miss_to_trans[0].length; i++) {
          val[0][miss_to_trans[0][i]] = newmiss[0][i];
          val[1][miss_to_trans[0][i]] = newmiss[1][i];
        }
      }
      return val;
    }
    else {
      return anav.toLatLon(tuples);
    }
  }

  /** convert from latitude,longitude to image element,line
    *
    * @param tuples contains the element,line pairs to convert
    *
    */
  public double[][] fromReference(double[][] tuples) throws VisADException {
    if (tuples == null || tuples.length != 2) {
      throw new CoordinateSystemException("AREACoordinateSystem." +
             "fromReference: tuples wrong dimension");
    }

    if (anav == null) {
      throw new CoordinateSystemException("AREA O & A data not availble");
    }

    int[] nums = new int[2];
    double[] mins = new double[2];
    double[] maxs = new double[2];
    double[][] newval = makeSpline(tuples, mins, maxs, nums);
    if (newval != null) {
// System.out.println("new 2 " + tuples[0].length + " " + newval[0].length);
      double[][] newtrans = anav.toLinEle(newval);

      int len = tuples[0].length;
      double[][] misstrans = new double[2][len];
      int[][] miss_to_trans = new int[1][len];
      double[][] val = applySpline(tuples, mins, maxs, nums, newtrans,
                                   misstrans, miss_to_trans);
      if (miss_to_trans[0] != null) {
        double[][] newmiss = anav.toLinEle(misstrans);
        for (int i=0; i<miss_to_trans[0].length; i++) {
          val[0][miss_to_trans[0][i]] = newmiss[0][i];
          val[1][miss_to_trans[0][i]] = newmiss[1][i];
        }
      }
      return val;
    }
    else {
      return anav.toLinEle(tuples);
    }

  }

  /** convert from image element,line to latitude,longitude
    *
    * @param tuples contains the element,line pairs to convert
    *
    */
  public float[][] toReference(float[][] tuples) throws VisADException {
    if (tuples == null || tuples.length != 2) {
      throw new CoordinateSystemException("AREACoordinateSystem." +
             "toReference: tuples wrong dimension");
    }

    if (anav == null) {
      throw new CoordinateSystemException("AREA O & A data not availble");
    }

    double[][] val = Set.floatToDouble(tuples);

    int[] nums = new int[2];
    double[] mins = new double[2];
    double[] maxs = new double[2];
    double[][] newval = makeSpline(val, mins, maxs, nums);
    if (newval != null) {
// System.out.println("new 3");
      double[][] newtrans = anav.toLatLon(newval);

      int len = tuples[0].length;
      double[][] misstrans = new double[2][len];
      int[][] miss_to_trans = new int[1][len];
      val = applySpline(val, mins, maxs, nums, newtrans,
                        misstrans, miss_to_trans);
      if (miss_to_trans[0] != null) {
        double[][] newmiss = anav.toLatLon(misstrans);
        for (int i=0; i<miss_to_trans[0].length; i++) {
          val[0][miss_to_trans[0][i]] = newmiss[0][i];
          val[1][miss_to_trans[0][i]] = newmiss[1][i];
        }
      }
    }
    else {
      val = anav.toLatLon(val);
    }
    return Set.doubleToFloat(val);
  }

  /** convert from latitude,longitude to image element,line
    *
    * @param tuples contains the element,line pairs to convert
    *
    */
  public float[][] fromReference(float[][] tuples) throws VisADException {
    if (tuples == null || tuples.length != 2) {
      throw new CoordinateSystemException("AREACoordinateSystem." +
             "fromReference: tuples wrong dimension");
    }

    if (anav == null) {
      throw new CoordinateSystemException("AREA O & A data not availble");
    }

    double[][] val = Set.floatToDouble(tuples);

    int[] nums = new int[2];
    double[] mins = new double[2];
    double[] maxs = new double[2];
    double[][] newval = makeSpline(val, mins, maxs, nums);
    if (newval != null) {
// System.out.println("new 4");
      double[][] newtrans = anav.toLinEle(newval);

      int len = tuples[0].length;
      double[][] misstrans = new double[2][len];
      int[][] miss_to_trans = new int[1][len];
      val = applySpline(val, mins, maxs, nums, newtrans,
                        misstrans, miss_to_trans);
      if (miss_to_trans[0] != null) {
        double[][] newmiss = anav.toLinEle(misstrans);
        for (int i=0; i<miss_to_trans[0].length; i++) {
          val[0][miss_to_trans[0][i]] = newmiss[0][i];
          val[1][miss_to_trans[0][i]] = newmiss[1][i];
        }
      }
    }
    else {
      val = anav.toLinEle(val);
    }
    return Set.doubleToFloat(val);

  }

  // return reduced array for approximatin by splines
  private double[][] makeSpline(double[][] val, double[] mins,
                                double[] maxs, int[] nums)
         throws VisADException {
    int len = val[0].length;
    if (len < 1000) return null;
    double reduction = 10.0;
    if (len < 10000) reduction = 2.0;
    else if (len < 100000) reduction = 5.0;

    // compute ranges
    mins[0] = Double.MAX_VALUE;
    maxs[0] = -Double.MAX_VALUE;
    mins[1] = Double.MAX_VALUE;
    maxs[1] = -Double.MAX_VALUE;
    for (int i=0; i<len; i++) {
      if (val[0][i] == val[0][i]) {
        if (val[0][i] < mins[0]) mins[0] = val[0][i];
        if (val[0][i] > maxs[0]) maxs[0] = val[0][i];
      }
      if (val[1][i] == val[1][i]) {
        if (val[1][i] < mins[1]) mins[1] = val[1][i];
        if (val[1][i] > maxs[1]) maxs[1] = val[1][i];
      }
    }

    // compute typical spacing btween points
    float[] norm = new float[len-1];
    int k = 0;
    // WLH 2 March 2000
    // for (int i=0; k<3 && i<len; i++) {
    for (int i=0; i<len-1; i++) {
      float n = (float) Math.sqrt(
        (val[0][i] - val[0][i+1]) * (val[0][i] - val[0][i+1]) +
        (val[1][i] - val[1][i+1]) * (val[1][i] - val[1][i+1]) );
      if (n == n) {
        norm[k++] = n;
      }
    }
    // WLH 2 March 2000
    if (k < 3) return null;
    float[] nnorm = new float[k];
    System.arraycopy(norm, 0, nnorm, 0, k);

    QuickSort.sort(nnorm);
    double spacing = reduction * nnorm[k / 4];

    // compute size of spline array
    nums[0] = (int) ((maxs[0] - mins[0]) / spacing) + 1;
    nums[1] = (int) ((maxs[1] - mins[1]) / spacing) + 1;

    // test if it will be too coarse
    if (nums[0] < 20 || nums[1] < 20) return null;
    // test if its worth it
    if ((nums[0] * nums[1]) > (len / 4)) return null;

    double spacing0 = (maxs[0] - mins[0]) / (nums[0] - 1);
    double spacing1 = (maxs[1] - mins[1]) / (nums[1] - 1);

    double[][] newval = new double[2][nums[0] * nums[1]];
    k = 0;
    for (int i=0; i<nums[0]; i++) {
      for (int j=0; j<nums[1]; j++) {
        newval[0][k] = mins[0] + i * spacing0;
        newval[1][k] = mins[1] + j * spacing1;
        k++;
      }
    }
    return newval;
  }

  // use splines to approximate transform
  private double[][] applySpline(double[][] val, double[] mins,
                     double[] maxs, int[] nums, double[][] newtrans,
                     double[][] misstrans, int[][] miss_to_trans)
         throws VisADException {
    int n0 = nums[0];
    int n1 = nums[1];
    double spacing0 = (maxs[0] - mins[0]) / (n0 - 1);
    double spacing1 = (maxs[1] - mins[1]) / (n1 - 1);
    int len = val[0].length;
    double[][] trans = new double[2][len];

    boolean[] lon_flags = new boolean[n0 * n1];
    double[] min_trans = {Double.MAX_VALUE, Double.MAX_VALUE};
    double[] max_trans = {-Double.MAX_VALUE, -Double.MAX_VALUE};
    for (int i=0; i<n0*n1; i++) {
      if (newtrans[0][i] == newtrans[0][i]) {
        if (newtrans[0][i] < min_trans[0]) min_trans[0] = newtrans[0][i];
        if (newtrans[0][i] > max_trans[0]) max_trans[0] = newtrans[0][i];
      }
      if (newtrans[1][i] == newtrans[1][i]) {
        if (newtrans[1][i] < min_trans[1]) min_trans[1] = newtrans[1][i];
        if (newtrans[1][i] > max_trans[1]) max_trans[1] = newtrans[1][i];
      }
      lon_flags[i] = false;
    }
    double minn0n1 = Math.min(n0, n1);
    double mean0 = (max_trans[0] - min_trans[0]) / minn0n1;
    double mean1 = (max_trans[1] - min_trans[1]) / minn0n1;

    for (int i0=0; i0<n0-1; i0++) {
      for (int i1=0; i1<n1-1; i1++) {
        int ii = i1 + i0 * n1;
        if (Math.abs(newtrans[0][ii + n1] - newtrans[0][ii]) > 3.0 * mean0 ||
            Math.abs(newtrans[0][ii + 1] - newtrans[0][ii]) > 3.0 * mean0 ||
            Math.abs(newtrans[0][ii + n1 + 1] - newtrans[0][ii + 1]) > 3.0 * mean0 ||
            Math.abs(newtrans[0][ii + n1 + 1] - newtrans[0][ii + n1]) > 3.0 * mean0 ||
            Math.abs(newtrans[1][ii + n1] - newtrans[1][ii]) > 3.0 * mean1 ||
            Math.abs(newtrans[1][ii + 1] - newtrans[1][ii]) > 3.0 * mean1 ||
            Math.abs(newtrans[1][ii + n1 + 1] - newtrans[1][ii + 1]) > 3.0 * mean1 ||
            Math.abs(newtrans[1][ii + n1 + 1] - newtrans[1][ii + n1]) > 3.0 * mean1) {
          lon_flags[ii] = true;
        }
      }
    }

    int nmiss = 0;

    for (int i=0; i<len; i++) {
      double a0 = (val[0][i] - mins[0]) / spacing0;
      int i0 = (int) a0;
      if (i0 < 0) i0 = 0;
      if (i0 > (n0 - 2)) i0 = n0 - 2;
      a0 -= i0;
      double a1 = (val[1][i] - mins[1]) / spacing1;
      int i1 = (int) a1;
      if (i1 < 0) i1 = 0;
      if (i1 > (n1 - 2)) i1 = n1 - 2;
      a1 -= i1;
      int ii = i1 + i0 * n1;
      if (lon_flags[ii]) {
        misstrans[0][nmiss] = val[0][i];
        misstrans[1][nmiss] = val[1][i];
        miss_to_trans[0][nmiss] = i;
        nmiss++;
      }
      else {
        trans[0][i] =
          (1.0 - a0) * ((1.0 - a1) * newtrans[0][ii] + a1 * newtrans[0][ii+1]) +
          a0 * ((1.0 - a1) * newtrans[0][ii+n1] + a1 * newtrans[0][ii+n1+1]);
        trans[1][i] =
          (1.0 - a0) * ((1.0 - a1) * newtrans[1][ii] + a1 * newtrans[1][ii+1]) +
          a0 * ((1.0 - a1) * newtrans[1][ii+n1] + a1 * newtrans[1][ii+n1+1]);

        if (trans[0][i] != trans[0][i] || trans[1][i] != trans[1][i]) {
          misstrans[0][nmiss] = val[0][i];
          misstrans[1][nmiss] = val[1][i];
          miss_to_trans[0][nmiss] = i;
          nmiss++;
        }
      }
    }
// System.out.println("len = " + len + " nmiss = " + nmiss);
    if (nmiss == 0) {
      miss_to_trans[0] = null;
      misstrans[0] = null;
      misstrans[1] = null;
    }
    else {
      double[] xmisstrans = new double[nmiss];
      System.arraycopy(misstrans[0], 0, xmisstrans, 0, nmiss);
      misstrans[0] = xmisstrans;
      xmisstrans = new double[nmiss];
      System.arraycopy(misstrans[1], 0, xmisstrans, 0, nmiss);
      misstrans[1] = xmisstrans;
      int[] xmiss_to_trans = new int[nmiss];
      System.arraycopy(miss_to_trans[0], 0, xmiss_to_trans, 0, nmiss);
      miss_to_trans[0] = xmiss_to_trans;
    }
    return trans;
  }

  /**
   * Get the bounds for this image
   */
  public Rectangle2D getDefaultMapArea() 
  { 
      return new Rectangle2D.Double(0.0, 0.0, elements-1, lines-1);
  }

  /**
   * Determines whether or not the <code>Object</code> in question is
   * the same as this <code>AREACoordinateSystem</code>.  The specified
   * <code>Object</code> is equal to this <CODE>AREACoordinateSystem</CODE>
   * if it is an instance of <CODE>AREACoordinateSystem</CODE> and it has
   * the same navigation module and default map area as this one.
   *
   * @param obj the Object in question
   */
  public boolean equals(Object obj)
  {
    if (!(obj instanceof AREACoordinateSystem))
        return false;
    AREACoordinateSystem that = (AREACoordinateSystem) obj;
    return this == that ||
           (anav.equals(that.anav) && 
           this.lines == that.lines &&
           this.elements == that.elements);
  }
}
