//
// TextAdapter.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad.data.text;

import java.io.IOException;
import java.io.*;
import java.util.*;
import visad.Set;

import java.net.URL;

import visad.*;
import visad.VisADException;


/** this is an VisAD file adapter for comma-, tab- and blank-separated
  * ASCII text file data.  It will attempt to create a FlatField from
  * the data and descriptions given in the file and/or the constructor.
  *
  * The text files contained delimited data.  The delimiter is 
  * determined as follows:  if the file has a well-known extension
  * (.csv, .tsv, .bsv) then the delimiter is implied by the extension.
  * In all other cases, the delimiter for the data (and for the
  * "column labels") is determined by reading the first line and
  * looking, in order, for a tab, comma, or blank.  Which ever one
  * is found first is taken as the delimiter.
  *
  * Two extra pieces of information are needed:  the VisAD "MathType"
  * which is specified as a string (e.g., (x,y)->(temperature))
  * and may either be the first line of the file or passed in through
  * one of the constructors.
  *
  * The second item are the "column labels" which contain the names
  * of each field in the data.  The names of all range components
  * specified in the "MathType" must appear.  The names of domain
  * components are optional.  The values in this string are separated
  * by the delimiter, as defined above.
  *
  * See visad.data.text.README.text for more details.
  * 
  * @author Tom Whittaker
  * 
  */
public class TextAdapter {

  private FlatField ff = null;
  private Field field = null;
  private boolean debug = false;
  private String DELIM;
  private final String COMMA = ",";
  private final String SEMICOLON = ";";
  private final String TAB = "\t";
  private final String BLANK = " ";


  String[] hdrNames;
  Unit[] hdrUnits;
  double[] hdrMissingValues;
  String[] hdrMissingStrings;
  int[] hdrIsInterval;
  double[] hdrErrorEstimates;
  double[] rangeErrorEstimates;
  Unit[] rangeUnits;
  double[] domainErrorEstimates;
  Unit[] domainUnits;
  double[] hdrScales;
  double[] hdrOffsets;
  int[][] hdrColumns;
  int[][] values_to_index;


  /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param filename name of local file.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public TextAdapter(String filename) throws IOException, VisADException {
    InputStream is = new FileInputStream(filename);
    DELIM = null;
    if (filename.trim().toLowerCase().endsWith(".csv")) DELIM=COMMA;
    if (filename.trim().toLowerCase().endsWith(".tsv")) DELIM=TAB;
    if (filename.trim().toLowerCase().endsWith(".bsv")) DELIM=BLANK;
    
    readit(is, null, null);
  }

  /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param filename name of local file.
    * @param map the VisAD "MathType" as a string defining the FlatField
    * @param params the list of parameters used to define what columns
    *  of the text file correspond to what MathType parameters.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public TextAdapter(String filename, String map, String params) 
                         throws IOException, VisADException {
    InputStream is = new FileInputStream(filename);
    DELIM = null;
    if (filename.trim().toLowerCase().endsWith(".csv")) DELIM=COMMA;
    if (filename.trim().toLowerCase().endsWith(".tsv")) DELIM=TAB;
    if (filename.trim().toLowerCase().endsWith(".bsv")) DELIM=BLANK;
    readit(is, map, params);
  }

  /** Create a VisAD FlatField from a remote Text (comma-, tab- or 
    * blank-separated values) ASCII file
    *
    * @param url File URL.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public TextAdapter(URL url) throws IOException, VisADException {
    DELIM = null;
    String filename = url.getFile();
    if (filename.trim().toLowerCase().endsWith(".csv")) DELIM=COMMA;
    if (filename.trim().toLowerCase().endsWith(".tsv")) DELIM=TAB;
    if (filename.trim().toLowerCase().endsWith(".bsv")) DELIM=BLANK;
    InputStream is = url.openStream();
    readit(is, null, null);
  }

  /** Create a VisAD FlatField from a local Text (comma-, tab- or 
    * blank-separated values) ASCII file
    * @param url File URL.
    * @param map the VisAD "MathType" as a string defining the FlatField
    * @param params the list of parameters used to define what columns
    *  of the text file correspond to what MathType parameters.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public TextAdapter(URL url, String map, String params) 
                        throws IOException, VisADException {
    DELIM = null;
    String filename = url.getFile();
    if (filename.trim().toLowerCase().endsWith(".csv")) DELIM=COMMA;
    if (filename.trim().toLowerCase().endsWith(".tsv")) DELIM=TAB;
    if (filename.trim().toLowerCase().endsWith(".bsv")) DELIM=BLANK;
    InputStream is = url.openStream();
    readit(is, map, null);
  }

  void readit(InputStream is, String map, String params) 
                              throws IOException, VisADException {
    // read the ASCII file, using commas as field separators
    // first line is a header line

    ff = null;
    field = null;

    BufferedReader bis = new BufferedReader(new InputStreamReader(is));

    // mapping defines how the names are mapped
    // for example:   (x,y) => (one, two, three)

    String maps = null;
    if (map == null) {
      String t;
      while (true) {
        t = bis.readLine();
        if (t == null) return;
        if (!isText(t)) return;
        if (t.startsWith("#") ||
            t.startsWith("!") || 
            t.startsWith("%") || 
            t.length() < 1) continue;
        break;
      }
      maps = t.trim();
    } else {
      maps = map;
    }

    maps = makeMT(maps);
    System.out.println("Specified MathType = "+maps);

    // but first, we need to get the column headers because they
    // may have [units] associated with them.  The column headers
    // primarily define where the data are.

    String hdr = null;
    if (params == null) {

      while (true) {
        hdr = bis.readLine();
        if (hdr == null) return;
        if (!isText(hdr)) return;
        if (hdr.startsWith("#") || 
           hdr.startsWith("!") || 
           hdr.startsWith("%") || 
           hdr.length() < 1) continue;
        break;
      }
    } else {
      hdr = params;
    }

    String hdrDelim = DELIM;
    if (DELIM == null) {
      if (hdr.indexOf(BLANK) != -1) hdrDelim = BLANK; 
      if (hdr.indexOf(COMMA) != -1) hdrDelim = COMMA; 
      if (hdr.indexOf(SEMICOLON) != -1) hdrDelim = SEMICOLON; 
      if (hdr.indexOf(TAB) != -1) hdrDelim = TAB; 

      if (debug) System.out.println("Using header delimiter = "+
                                     (hdrDelim.getBytes())[0]);
    }

    StringTokenizer sthdr = new StringTokenizer(hdr,hdrDelim);
    int nhdr = sthdr.countTokens();
    hdrNames = new String[nhdr];
    hdrUnits = new Unit[nhdr];
    hdrMissingValues = new double[nhdr];
    hdrMissingStrings = new String[nhdr];
    hdrIsInterval = new int[nhdr];
    hdrErrorEstimates = new double[nhdr];
    hdrScales = new double[nhdr];
    hdrOffsets = new double[nhdr];
    hdrColumns = new int[2][nhdr];


    // pre-scan of the header names to seek out Units
    // since we cannot change a RealType once it's defined!!

    for (int i=0; i<nhdr; i++) {
      String name = sthdr.nextToken().trim();
      String hdrUnitString = null;
      hdrMissingValues[i] = Double.NaN;
      hdrMissingStrings[i] = null;
      hdrIsInterval[i] = 0;
      hdrScales[i] = 1.0;
      hdrOffsets[i] = 0.0;
      hdrErrorEstimates[i] = 0.0;
      hdrColumns[0][i] = -1; // indicating no fixed columns
      
      int m = name.indexOf("[");

      if (m == -1) {
        hdrNames[i] = name;
        hdrUnitString = null;

      } else {
        int m2 = name.indexOf("]");
        if (m2 == -1) {
          throw new VisADException("TextAdapter: Bad [descriptor] named in:"+name);
        }

        // now parse items: unit=xxx miss=xxx interval=xxx error=xxx

        // 1. tokenize on " "
        // 2. scan each token, retokenizing on "="
        // 3. if (has no "=") && (is first one) then treat as Unit
        // 4. otherwise, look for keys "unit" "miss" "inter" "err" "scale" "offset" "pos"
      
        //    and fill in the values in array[i]

        if (m2 >= name.length()) {
          hdrNames[i] = name.substring(0,m).trim();
        } else {
          hdrNames[i] = (name.substring(0,m)+name.substring(m2+1)).trim();
        }

        String cl = name.substring(m+1,m2).trim();
        StringTokenizer stcl = new StringTokenizer(cl," ");
        int ncl = stcl.countTokens();

        if (ncl == 1 && cl.indexOf("=") == -1) {
          hdrUnitString = cl;  // backward compatible...

        } else {
          for (int j=0; j<ncl; j++) {
            String s = stcl.nextToken().trim();
            StringTokenizer sts = new StringTokenizer(s,"=");
            if (sts.countTokens() != 2) {
              throw new VisADException("TextAdapter: Invalid clause in: "+s);
            }
            String tok = sts.nextToken().trim();
            String val = sts.nextToken().trim();
            if (tok.toLowerCase().startsWith("unit")) {
              hdrUnitString = val;

            } else if (tok.toLowerCase().startsWith("mis")) {
              hdrMissingStrings[i] = val.trim();
              try {
                hdrMissingValues[i] = Double.parseDouble(val);
              } catch (java.lang.NumberFormatException me) {
                hdrMissingValues[i] = Double.NaN;
              }
              
            } else if (tok.toLowerCase().startsWith("int")) {

              hdrIsInterval[i] = -1;
              if (val.toLowerCase().startsWith("t")) hdrIsInterval[i] = 1;
              if (val.toLowerCase().startsWith("f")) hdrIsInterval[i] = 0;
              if (hdrIsInterval[i] == -1) {
                throw new VisADException("TextAdapter: Value of \'interval\' must be \'true\' or \'false\'");
              }

            } else if (tok.toLowerCase().startsWith("err")) {
              hdrErrorEstimates[i] = Double.parseDouble(val);

            } else if (tok.toLowerCase().startsWith("sca")) {
              hdrScales[i] = Double.parseDouble(val);

            } else if (tok.toLowerCase().startsWith("off")) {
              hdrOffsets[i] = Double.parseDouble(val);

            } else if (tok.toLowerCase().startsWith("pos")) {
              StringTokenizer stp = new StringTokenizer(val,":");
              if (stp.countTokens() != 2) {
                throw new VisADException("TextAdapter: invalid Position parameter in:"+s);
              }
              hdrColumns[0][i] = Integer.parseInt(stp.nextToken().trim());
              hdrColumns[1][i] = Integer.parseInt(stp.nextToken().trim());

            } else {
              throw new VisADException("Invalid token name: "+s);
            }

          }
        }

      }

      if (debug) 
            System.out.println("hdr name = "+hdrNames[i]+" units="+
             hdrUnitString+
             " miss="+hdrMissingValues[i]+" interval="+hdrIsInterval[i]+ 
             " errorest="+hdrErrorEstimates[i]+" scale="+hdrScales[i]+
             " offset="+hdrOffsets[i]+" pos="+hdrColumns[0][i]+":"+
             hdrColumns[1][i]);

      Unit u = null;
      if (hdrUnitString != null) {
        try {
          u = visad.data.units.Parser.parse(hdrUnitString);
        } catch (Exception ue) {
          System.out.println("Unit name problem:"+ue+" with: "+hdrUnitString);
          u = null;
        }
      }

      RealType rt = RealType.getRealType(hdrNames[i], u, null, 
                                                  hdrIsInterval[i]);
      hdrUnits[i] = u;
    }

    // get the MathType of the function

    MathType mt = null;
    try {
      mt = MathType.stringToType(maps);
      System.out.println("from: "+maps+" made MathType: "+mt);
    } catch (Exception mte) {
      throw new VisADException("TextAdapter: MathType badly formed or missing: "+maps);
    }

    if (debug) {
      System.out.println(mt);
      new visad.jmet.DumpType().dumpMathType(mt,System.out);
    }

    // now get the names of the domain variables and range variables.
    String[] domainNames = null;
    String[] rangeNames = null;
    int numDom = 0;
    int numRng = 0;
    RealTupleType domType;
    TupleType rngType;

    if (mt instanceof FunctionType) {
      domType = ((FunctionType)mt).getDomain();
      numDom = domType.getDimension();
      domainNames = new String[numDom];

      for (int i=0; i<numDom; i++) {
        MathType comp = domType.getComponent(i);
        domainNames[i] = ((RealType)comp).toString().trim();
        if (debug) System.out.println("dom "+i+" = "+domainNames[i]);
      }
      rngType = (TupleType) ((FunctionType)mt).getRange();
      numRng = rngType.getDimension();
      rangeNames = new String[numRng];
      for (int i=0; i<numRng; i++) {
        MathType comp = rngType.getComponent(i);
        rangeNames[i] = (comp).toString().trim();
        if (debug) System.out.println("range "+i+" = "+rangeNames[i]);
      }

    } else { 
      throw new visad.VisADException("TextAdapter: Math Type is not a simple FunctionType");
    }


// now for each header label, determine if it's a domain or
// range component -- and if so, which one.

// also, if it's a domain component, allow for name(first:last[:number])
//
// and if none of the domain components appear in the list, then
// they are computed as name(0:N-1)

    int[] domainPointer = new int[numDom];
    double[][] domainRanges = new double[3][numDom]; // min, max, numb
    boolean[] gotDomainRanges = new boolean[numDom];
    domainErrorEstimates = new double[numDom];
    domainUnits = new Unit[numDom];
    rangeErrorEstimates = new double[numRng];
    rangeUnits = new Unit[numRng];
    int countDomain = 0;

    for (int i=0; i<numDom; i++) {
      domainPointer[i] = -1;
      gotDomainRanges[i] = false;
      domainErrorEstimates[i] = Double.NaN;
      domainUnits[i] = null;
    }

    int[] rangePointer = new int[numRng];
    int countRange = 0;

    for (int i=0; i<numRng; i++) {
      rangePointer[i] = -1;
      rangeErrorEstimates[i] = Double.NaN;
      rangeUnits[i] = null;
    }

    int countValues = -1;
    values_to_index = new int[3][nhdr];

    for (int i=0; i<nhdr; i++) {
      values_to_index[0][i] = -1;  // points to domains
      values_to_index[1][i] = -1;  // points to ranges
      values_to_index[2][i] = -1;  // points to names/units/etc
      countValues ++;

      String name = hdrNames[i];

      // see if it's a domain name
      boolean gotName = false;

      // is there a "min:max" clause?
      String test_name = name;
      int n = test_name.indexOf("(");
      if (n != -1) {
        // but allow for "(Text)" - jk
        if ((test_name.indexOf("(Text)")) == -1) {
          test_name = name.substring(0,n).trim();
          countValues --;  // this value wont appear in data!
          countDomain --; // and is a pre-defined, linear set
        }
      }

      // try to find the column header name in the domain name list
      for (int k=0; k<numDom; k++) {

        if (test_name.equals(domainNames[k]) ) { 
          domainPointer[k] = countValues;
          domainErrorEstimates[k] = hdrErrorEstimates[i];
          domainUnits[k] = hdrUnits[i];
          gotName = true;
          countDomain ++;
          // now see if a list is given...
          if (n != -1) {

            try {

              String ss = name.substring(n+1,name.length()-1);
              StringTokenizer sct = new StringTokenizer(ss,":");
              String first = sct.nextToken().trim();
              String second = sct.nextToken().trim();
              String third = "1";
              if (sct.hasMoreTokens()) third = sct.nextToken().trim();
              domainRanges[0][k] = Double.parseDouble(first);
              domainRanges[1][k] = Double.parseDouble(second);
              domainRanges[2][k] = Double.parseDouble(third);
              gotDomainRanges[k] = true;

            } catch (Exception ef) {
              throw new VisADException(
       "TextAdapter: Error while interpreting min:max values for domain "+name);
            }

          } else if (countValues > -1) { // if no list, get from file
            values_to_index[0][countValues] = k;
            values_to_index[2][countValues] = i;
          }

          break;
       }

    } 

    if (gotName) continue;

    // or see if its a range name...

    for (int k=0; k<numRng; k++) {
      if (name.equals(rangeNames[k]) ) {
        rangePointer[k] = countValues;
        rangeErrorEstimates[k] = hdrErrorEstimates[i];
        rangeUnits[k] = hdrUnits[i];
        countRange ++;
        values_to_index[1][countValues] = k;
        values_to_index[2][countValues] = i;
        gotName = true;
      }
    }
  }


// huge debug printout...
// *****************************************************************

  if (debug) {
    System.out.println("countDom/numDom="+countDomain+" "+numDom);

    System.out.println("countRange/numRng="+countRange+" "+numRng);

    System.out.println("Domain info:");
    for (int i=0; i<numDom; i++) {
      System.out.println("Dom name / index = "+domainNames[i]+"  "+
             domainPointer[i]);

      if (gotDomainRanges[i]) {
        System.out.println("    ..."+domainRanges[0][i]+"  "+
            domainRanges[1][i]+"    "+domainRanges[2][i]);
      }
    }

    System.out.println("Range info:");
    for (int i=0; i<numRng; i++) {
      System.out.println("Rng name / index / error est = "+rangeNames[i]+"  "+
             rangePointer[i]+ "  " + rangeErrorEstimates[i] +" "+
             rangeUnits[i]);
    }

    System.out.println("values_to_index pointers = ");
    for (int i=0; i<nhdr; i++) {
      System.out.println(" inx / value = "+i+ 
              " "+values_to_index[0][i]+"    "+values_to_index[1][i]+
              " "+values_to_index[2][i]);
    }
  }

// ***************************************************************


    // for each line of text, put the values into the ArrayList
    ArrayList domainValues = new ArrayList();
    ArrayList rangeValues = new ArrayList();
    ArrayList tupleValues = new ArrayList(); // jk
    Tuple tuple = null;
    
    String dataDelim = DELIM;
    boolean isRaster = false;
    int numElements = 1;

    // in the 'raster array' case, the numRng value will be 1,
    // along with the countRange.  numDomain must be 2.

    // if the domain is 2D, then get values from the first
    // matching column to the end...
    if (countRange == 1 && numRng == 1 && 
                numDom == 2 && countDomain < 2) isRaster = true;

    while (true) {
      String s = bis.readLine();
      if (debug) System.out.println("read:"+s);
      if (s == null) break;
      if (!isText(s)) return;
      if (s.startsWith("#") || 
         s.startsWith("!") || 
         s.startsWith("%") || 
         s.length() < 1) continue;

      if (dataDelim == null) {
        if (s.indexOf(BLANK) != -1) dataDelim = BLANK; 
        if (s.indexOf(COMMA) != -1) dataDelim = COMMA; 
        if (s.indexOf(SEMICOLON) != -1) dataDelim = SEMICOLON; 
        if (s.indexOf(TAB) != -1) dataDelim = TAB; 

        if (debug) System.out.println("Using data delimiter = "+
                                       (dataDelim.getBytes())[0]);
      }

      StringTokenizer st = new StringTokenizer(s,dataDelim);
      int n = st.countTokens();
      if (n < 1) continue; // something is wrong if this happens!

      double [] dValues = new double[numDom];
      double [] rValues = null;
      
      //jk

      Data [] tValues = null;

      if (isRaster) {

        if (debug) System.out.println("probably a raster...");
        boolean gotFirst = false;
        int rvaluePointer = 0;
        int irange = 0;
        for (int i=0; i<n; i++) {

          String sa = st.nextToken().trim();
          
          if (i >= nhdr) {  // are we past where domain would be found?

            if (!gotFirst) {
              throw new VisADException(
                        "TextAdapter: Cannot find first raster value");
            }

            rvaluePointer ++;
            rValues[rvaluePointer] = getVal(sa, irange);

          } else {  // or are we still looking for domain?
          
            if (values_to_index[0][i] != -1) {
              dValues[values_to_index[0][i]] = getVal(sa, i);
            }

            if (gotFirst) {  // already gathering data
              rvaluePointer ++;
              rValues[rvaluePointer] = getVal(sa, irange);

            } else {
               if (values_to_index[1][i] != -1) {
                 // cannot dimension the array until we have found
                 // the first set of range values!!
                 rValues = new double[n - i];
                 irange = i;
                 rValues[rvaluePointer] = getVal(sa, irange);
                 gotFirst = true;
               }
            }

          }
        }
         
      } else {  // is probably NOT a raster

        tValues = new Data[numRng];
      
        if (debug) System.out.println("probably not a raster...");
        rValues = new double[numRng];
        double thisDouble; // jk
        MathType thisMT;
        if (n > nhdr) n = nhdr; // in case the # tokens > # parameters

        for (int i=0; i<n; i++) {

          String sa = st.nextToken().trim();
          String sThisText;

          if (values_to_index[0][i] != -1) {
            dValues[values_to_index[0][i]] = getVal(sa, i);

          } else if (values_to_index[1][i] != -1) {
            thisMT = rngType.getComponent(values_to_index[1][i]);
            if (sa.startsWith("\"")) {
              StringTokenizer stText = new StringTokenizer(sa, "\"");
              if (stText.countTokens() == 0) {;
                // allow for empty string
                sThisText = "";
              } else {
                sThisText = stText.nextToken().trim();
              }
              try {
                tValues[values_to_index[1][i]] = 
                        new Text((TextType) thisMT, sThisText);
                if (debug) System.out.println("tValues[" + 
                          values_to_index[1][i] + "] = " + 
                          tValues[values_to_index[1][i]]);
              } catch (Exception e) {
                System.out.println(" Exception converting " + 
                                       thisMT + " to TextType " + e);
              }
              
            } else {
              rValues[values_to_index[1][i]] = getVal(sa,i);
              try {
                tValues[values_to_index[1][i]] = 
                          new Real((RealType) thisMT, getVal(sa,i));
                if (debug) System.out.println("tValues[" + 
                         values_to_index[1][i] + "] = " + 
                                  tValues[values_to_index[1][i]]);

              } catch (Exception e) {
                System.out.println(" Exception converting " + thisMT + " " + e);
              }
            }
          }
        }
      }

      try {

        if (tValues != null) tuple = new Tuple(tValues);
      } catch (visad.TypeException te) {
        // do nothing: it means they are all reals
        // tuple = new RealTuple(tValues);
        tuple = null;
      }

      domainValues.add(dValues);
      rangeValues.add(rValues);
      if (tuple != null) tupleValues.add(tuple); // jk
      if (isRaster) numElements = rValues.length;
    }

    int numSamples = rangeValues.size(); // # lines of data


// ***********************************************************
    if (debug) {
      try {
        System.out.println("domain size = "+domainValues.size());
        double[] dt = (double[]) domainValues.get(1);
        System.out.println("domain.array[0] = "+dt[0]);
        // jk
        // System.out.println("domain.array[1] = "+dt[1]);
      
        System.out.println("range size = "+rangeValues.size());
        System.out.println("# samples = "+numSamples);
      } catch (Exception er) {System.out.println("out range");}
    }
// ***********************************************************


    // make Linear1DSets for each possible domain component

    Linear1DSet[] lset = new Linear1DSet[numDom];
    int numVal = numRng; 
    if (numDom == 1) numVal = numSamples;
    if (numDom == 2 && numRng == 1 && numElements > 1) numVal = numElements;

    for (int i=0; i<numDom; i++) {

      if (gotDomainRanges[i]) {
        lset[i] = new Linear1DSet(domainRanges[0][i], 
                            domainRanges[1][i], numVal);

        if (debug) System.out.println("lset from domain = "+lset[i]);

      } else if (domainPointer[i] == -1 ) {
        lset[i] = new Linear1DSet(0., (double)(numVal-1), numVal);

        if (debug) System.out.println("lset from range = "+lset[i]);

      } else {
        lset[i] = null;
      }

      numVal = numSamples; 
    }


    // now make up the actual domain sets for the function
    Set domain = null;

    if (numDom == 1) {  // for 1-D domains

      if (lset[0] == null) {
        float[][] a = getDomSamples(0, numSamples, domainValues);
        domain = (Set) new Irregular1DSet(domType, a);

      } else {
        domain = lset[0];
      }

    } else if (numDom == 2) {  // for 2-D domains

      if (lset[0] != null && lset[1] != null) {
        domain = new Linear2DSet(domType, lset);

      } else {
        float[][] samples = new float[2][numSamples];

        if (lset[0] == null) {
          samples[0] = (getDomSamples(0, numSamples, domainValues))[0];
        } else {
          samples[0] = (lset[0].getSamples())[0];
        }

        if (lset[1] == null) {
          samples[1] = (getDomSamples(1, numSamples, domainValues))[0];
        } else {
          samples[1] = (lset[1].getSamples())[0];
        }

        domain = (Set) new Irregular2DSet(domType, samples);
      }
        
    } else if (numDom == 3) {  // for 3-D domains
    
      if (lset[0] != null && lset[1] != null && lset[2] != null) {
        domain = new Linear3DSet(domType, lset);

      } else {
        float[][] samples = new float[3][numSamples];

        if (lset[0] == null) {
          samples[0] = (getDomSamples(0, numSamples, domainValues))[0];
        } else {
          samples[0] = (lset[0].getSamples())[0];
        }

        if (lset[1] == null) {
          samples[1] = (getDomSamples(1, numSamples, domainValues))[0];
        } else {
          samples[1] = (lset[1].getSamples())[0];
        }

        if (lset[2] == null) {
          samples[2] = (getDomSamples(2, numSamples, domainValues))[0];
        } else {
          samples[2] = (lset[2].getSamples())[0];
        }

        domain = (Set) new Irregular3DSet(domType, samples);
      }

    } else {  // N-D domains (can only use LinearSets!!

      domain = new LinearNDSet(domType, lset);
    }


    try {
      ff = new FlatField((FunctionType) mt, domain);
    } catch (FieldException fe) {
      field = new FieldImpl((FunctionType) mt, domain);
    }
//*************************************************
    if (debug) {
    // jk
      if (ff != null) {
        System.out.println("ff.Length "+ff.getLength());
        System.out.println("ff.getType "+ff.getType());
      }
      if (field != null) {
        System.out.println("field.Length "+field.getLength());
        System.out.println("field.getType "+field.getType());
      }
      System.out.println("domain = "+domain);
      System.out.println("size of a = "+numRng+" x "+(numSamples*numElements));
    }
//*************************************************

    float[][]a = new float[numRng][numSamples * numElements];
    Tuple[] at = new Tuple[numSamples];
    
    // if this is a raster then the samples are in a slightly
    // difielderent form ...

    if (isRaster) {
      int samPointer = 0;
      for (int i=0; i<numSamples; i++) {
        double[] rs = (double[])(rangeValues.get(i));
        for (int j=0; j<numElements; j++) {
          a[0][samPointer] = (float)rs[j];
          samPointer ++;
        }
      }
    } else {
      for (int i=0; i<numSamples; i++) {
        double[] rs = (double[])(rangeValues.get(i));
        for (int j=0; j<numRng; j++) {
          a[j][i] = (float)rs[j];
        }
        if (!tupleValues.isEmpty()) {
          at[i] = (Tuple) tupleValues.get(i); 
        }
      }
    }

// set samples
    if (debug) System.out.println("about to field.setSamples");
    if (ff != null) {
      ff.setSamples(a);
      field = (Field) ff;
    } else {
      field.setSamples(at, false);
    }
      

    // make up error estimates and set them
    ErrorEstimate[] es = new ErrorEstimate[numRng];
    for (int i=0; i<numRng; i++) {
      es[i] = new ErrorEstimate(a[i], rangeErrorEstimates[i], rangeUnits[i]);
    }
    try {
        ((FlatField) field).setRangeErrors(es); // jk
    } catch (FieldException fe) {
        if (debug) System.out.println("caught "+fe);
        // not a flatfield
        // don't setRangeErrors
    } catch (ClassCastException cce) {
        if (debug) System.out.println("caught "+cce);
        // not a flatfield
        // don't setRangeErrors
    }

    if (debug) {
      new visad.jmet.DumpType().dumpDataType(field,System.out);
      System.out.println("field = "+field);
    }

    bis.close();

  }

  // munges a pseudo MathType string into something legal

  private String makeMT(String s) {
    String t = "";
    for (int i=0; i<s.length(); i++) {
      String r = s.substring(i,i+1);
      if (!r.equals(" ") && !r.equals("\t") && !r.equals("\n")) {
              t = t + r;
      }
    }
    int k = t.indexOf("->");
    if (k < 0) {
      System.out.println("Invalid MathType form: -> required:"+k);
      return null;
    }

    if (t.charAt(k-1) != ')' ) {
      String t2 = "("+t.substring(0,k) + ")->("+t.substring(k+2)+")";
      t = t2;
    } else if (t.charAt(k+2) != '(' ) {
      String t2 = t.substring(0,k+2)+"("+t.substring(k+2)+")";
      t = t2;
    }

    if (!t.startsWith("((") ) {
      String t2= "("+t+")";
      t = t2;
    }

    return t;
  }

  private static final boolean isText(String s)
  {
    final int len = (s == null ? -1 : s.length());

    if (len <= 0) {
      // well, it's not really *binary*, so pretend it's text
      return true;
    }

    for (int i = 0; i < len; i++) {
      final char ch = s.charAt(i);
      if (Character.isISOControl(ch) && !Character.isWhitespace(ch)) {
        // we might want to special-case formfeed/linefeed/newline here...
        return false;
      }
    }

    return true;
  }

  double getVal(String s, int k) {
    int i = values_to_index[2][k];
    if (i < 0 || s == null || s.length()<1 || s.equals(hdrMissingStrings[i])) {
      return Double.NaN;
    }
    try {

      double v = Double.parseDouble(s);
      if (v == hdrMissingValues[i]) {
        return Double.NaN;
      }
      v = v * hdrScales[i] + hdrOffsets[i];
      return v;

    } catch (java.lang.NumberFormatException ne) {
      System.out.println("Invalid number format for "+s);
      return Double.NaN;
    }
  }

  // get the samples from the ArrayList.
  float[][] getDomSamples(int comp, int numDomValues, ArrayList domValues) {
    float [][] a = new float[1][numDomValues];
    for (int i=0; i<numDomValues; i++) {
      double[] d = (double[])(domValues.get(i));
      a[0][i] = (float)d[comp];
    }
    return a;
  }

  /** get the data
  * @return a Field of the data read from the file
  *
  * Changed by jk
  *
  */
  public Field getData() {
    return field;
  }
}
