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

package visad.data;

import java.io.IOException;

import java.rmi.RemoteException;

import visad.Data;
import visad.DataImpl;
import visad.VisADException;

import visad.data.fits.FitsForm;
import visad.data.gif.GIFForm;
import visad.data.tiff.TiffForm;
import visad.data.hdfeos.HdfeosAdaptedForm;
import visad.data.netcdf.Plain;
import visad.data.vis5d.Vis5DForm;
import visad.data.visad.VisADForm;
import visad.data.mcidas.AreaForm;
import visad.data.mcidas.MapForm;
import visad.data.biorad.BioRadForm;
import visad.data.jai.JAIForm;
import visad.data.qt.QTForm;
import visad.data.text.TextForm;

/**
  * A container for all the officially supported VisAD datatypes.<br>
  * <br>
  * To read a <tt>Data</tt> object from a file or URL:<br>
  * <pre>
  *    Data data = new DefaultFamily("dflt").open(string);
  * </pre>
  * <br>
  * To save a Data object to a file:<br>
  * <pre>
  *    new DefaultFamily("dflt").save("file.nc", data, true);
  * </pre>
  * <br>
  * To add a Data object to an existing file:<br>
  * <pre>
  *    new DefaultFamily("dflt").add("file.nc", data, true);
  * </pre>
  */
public class DefaultFamily
	extends FunctionFormFamily
{
  /**
    * List of all supported VisAD datatype Forms.
    */
  /*
   *  note that I hardcoded the number of FormNodes (100)
   *  increase this if you add a new FormNode
   */
  private static FormNode[] list = new FormNode[100];
  private static boolean listInitialized = false;

  /**
   * 'adde' URL needs special handler loaded
   */
  private static boolean addeHandlerLoaded = false;

  /**
   * Build a list of all known file adapter Forms
   */
  private static void buildList()
  {
    int i = 0;

    try {
      list[i] = new FitsForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new GIFForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new TiffForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new HdfeosAdaptedForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new Plain();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new Vis5DForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new VisADForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new AreaForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new MapForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new BioRadForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new JAIForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new QTForm();
      i++;
    } catch (Throwable t) {
    }
    try {
      list[i] = new TextForm();
      i++;
    } catch (Throwable t) {
    }

    // added to support HDF5 adapter (visad.data.hdf5.HDF5Form)
    try {
      Object hdf5form = null;
      ClassLoader cl = ClassLoader.getSystemClassLoader();
      Class hdf5form_class = cl.loadClass("visad.data.hdf5.HDF5Form");
      hdf5form = hdf5form_class.newInstance();
      if (hdf5form != null) list[i++] = (Form)hdf5form;
    } catch (Throwable t) {
    }

    // throw an Exception if too many Forms for list
    FormNode junk = list[i];

    while (i < list.length) {
      list[i++] = null;
    }
    listInitialized = true; // WLH 24 Jan 2000
  }

  /**
    * Add to the family of the supported VisAD datatype Forms
    * @exception ArrayIndexOutOfBoundsException
    *			If there is no more room in the list.
    */
  public static void addFormToList(FormNode form)
    throws ArrayIndexOutOfBoundsException
  {
    synchronized (list) {
      if (!listInitialized) {
	buildList();
      }

      int i = 0;
      while (i < list.length) {
        if (list[i] == null) {
          list[i] = form;
          return;
        }
        i++;
      }
    }

    throw new ArrayIndexOutOfBoundsException("Only " + list.length +
                                             " entries allowed");
  }

  /**
    * Construct a family of the supported VisAD datatype Forms
    */
  public DefaultFamily(String name)
  {
    super(name);

    synchronized (list) {
      if (!listInitialized) {
	buildList();
      }
    }

    for (int i = 0; i < list.length && list[i] != null; i++) {
      forms.addElement(list[i]);
    }
  }

  /**
    * Open a local data object using the first appropriate Form.
    */
  public DataImpl open(String id)
	throws BadFormException, VisADException
  {
    // if this is an 'adde:' URL...
    if (id != null && id.length() >= 5 &&
        id.substring(0, 5).equalsIgnoreCase("adde:"))
    {
      // if 'adde:' handler isn't loaded, try to load it
      if (!addeHandlerLoaded) {
        addeHandlerLoaded =
	  edu.wisc.ssec.mcidas.AreaFile.isURLHandlerLoaded();
      }
    }

    return super.open(id);
  }

  /**
    * Test the DefaultFamily class
    */
  public static void main(String[] args)
	throws BadFormException, IOException, RemoteException, VisADException
  {
    if (args.length < 1) {
      System.err.println("Usage: DefaultFamily infile [infile ...]");
      System.exit(1);
      return;
    }

    DefaultFamily fr = new DefaultFamily("sample");

    for (int i = 0; i < args.length; i++) {
      Data data;
      System.out.println("Trying file " + args[i]);
      data = fr.open(args[i]);
      System.out.println(args[i] + ": " + data.getType().prettyString());
    }
  }
}