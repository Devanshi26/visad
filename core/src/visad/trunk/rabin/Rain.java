
//
// Rain.java
//

package visad.rabin;

// import needed classes
import visad.*;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.TwoDDisplayRendererJ3D;
import visad.java3d.DirectManipulationRendererJ3D;
import visad.util.VisADSlider;
import visad.util.LabeledRGBWidget;
import visad.data.vis5d.Vis5DForm;
import java.rmi.RemoteException;
import java.io.IOException;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


public class Rain implements ControlListener {

  static final int N_COLUMNS = 3;
  static final int N_ROWS = 4;
  static final JPanel[] row_panels =
    new JPanel[N_ROWS];
  static final JPanel[][] cell_panels =
    new JPanel[N_ROWS][N_COLUMNS];
  static final DataReferenceImpl[][] cell_refs =
    new DataReferenceImpl[N_ROWS][N_COLUMNS];
  static final CellImpl[][] cells =
    new CellImpl[N_ROWS][N_COLUMNS];
  static final DisplayImpl[][] displays =
    new DisplayImpl[N_ROWS][N_COLUMNS];
  static final boolean[][] display_done =
    new boolean[N_ROWS][N_COLUMNS];

  static String[][] cell_names =
    {{"A1", "B1", "C1"}, {"A2", "B2", "C2"},
     {"A3", "B3", "C3"}, {"A4", "B4", "C4"}};

  static JTextField[][] cell_fields =
    new JTextField[N_ROWS][N_COLUMNS];

  static final Real ten = new Real(10.0);
  static final Real one = new Real(1.0);
  static final Real three = new Real(3.0);
  static final Real fifty_three = new Real(53.0);

  /** the width and height of the UI frame */
  static final int WIDTH = 1100;
  static final int HEIGHT = 900;

  static final double MIN = 0.0;
  static final double MAX = 300.0;
  static final double MAXC4 = 10.0;

  static DataReference ref300 = null;
  static DataReference ref1_4 = null;
  static DataReference refMAX = null;
  static DataReference ref_cursor = null;


  static LabeledRGBWidget color_widgetC1 = null;
  static LabeledRGBWidget color_widgetC4 = null;
  static ColorControl color_control = null;
  static ColorControl[][] color_controls = new ColorControl[N_ROWS][N_COLUMNS];
  static ProjectionControl[][] projection_controls =
    new ProjectionControl[N_ROWS][N_COLUMNS];
  static ScalarMap color_map = null;
  static ScalarMap[][] color_maps = new ScalarMap[N_ROWS][N_COLUMNS];

  static boolean in_proj = false;

  static RealTupleType cursor_type = null;

  static final int DELAY = 300;

  static Rain rain = null;

  // type 'java Rain' to run this application
  public static void main(String args[])
         throws VisADException, RemoteException, IOException {
    rain = new Rain();
    rain.makeRain(args);
  }

  private Rain()
          throws VisADException, RemoteException {
    ref300 = new DataReferenceImpl("num300");
    ref1_4 = new DataReferenceImpl("num1_4");
    refMAX = new DataReferenceImpl("colorMAX");
    ref_cursor = new DataReferenceImpl("cursor");
  }

  private void makeRain(String args[])
         throws VisADException, RemoteException, IOException {
    if (args == null || args.length < 1) {
      System.out.println("run 'java visad.rabin.Rain file.v5d'");
    }
    Vis5DForm form = new Vis5DForm();
    FieldImpl vis5d = null;
    try {
      vis5d = (FieldImpl) form.open(args[0]);
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }
    if (vis5d == null) {
      System.out.println("bad Vis5D file read");
      return;
    }

    FunctionType vis5d_type = (FunctionType) vis5d.getType();
    // System.out.println(vis5d_type);
    RealType time = (RealType) vis5d_type.getDomain().getComponent(0);
    FunctionType grid_type = (FunctionType) vis5d_type.getRange();
    RealTupleType domain = grid_type.getDomain();
    RealType x_domain = (RealType) domain.getComponent(0);
    RealType y_domain = (RealType) domain.getComponent(1);
    RealTupleType range = (RealTupleType) grid_type.getRange();
    RealType rangeC1 = (RealType) range.getComponent(0);
    RealType rangeC4 = (RealType) range.getComponent(8);
    // System.out.println("rangeC1 = " + rangeC1 + " rangeC4 = " + rangeC4);
    int dim = range.getDimension();
    RealType[] range_types = new RealType[dim];
    for (int i=0; i<dim; i++) {
      range_types[i] = (RealType) range.getComponent(i);
    }

    // create cursor
    RealType shape = new RealType("shape");
    RealTupleType cursor_type = new RealTupleType(x_domain, y_domain, shape);
    SampledSet grid_set =
      (SampledSet) ((FlatField) vis5d.getSample(0)).getDomainSet();
    float[] lows = grid_set.getLow();
    float[] his = grid_set.getHi();
    double cursorx = 0.5 * (lows[0] + his[0]);
    double cursory = 0.5 * (lows[1] + his[1]);
    RealTuple cursor =
      new RealTuple(cursor_type, new double[] {cursorx, cursory, 0.0});
    ref_cursor.setData(cursor);
    Gridded1DSet shape_count_set =
      new Gridded1DSet(shape, new float[][] {{0.0f}}, 1);
    VisADLineArray cross = new VisADLineArray();
    cross.coordinates = new float[]
      {0.1f,  0.0f, 0.0f,    -0.1f,  0.0f, 0.0f,
       0.0f, -0.1f, 0.0f,     0.0f,  0.1f, 0.0f};
    cross.colors = new byte[]
      {-1,  -1, -1,     -1,  -1, -1,
       -1,  -1, -1,     -1,  -1, -1};
    cross.vertexCount = cross.coordinates.length / 3;
    VisADGeometryArray[] shapes = {cross};

    //
    // construct JFC user interface
    //
 
    // create a JFrame
    JFrame frame = new JFrame("Vis5D");
    WindowListener l = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    };
    frame.addWindowListener(l);
    frame.setSize(WIDTH, HEIGHT);
    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation(screenSize.width/2 - WIDTH/2,
                      screenSize.height/2 - HEIGHT/2);
 
    // create big_panel JPanel in frame
    JPanel big_panel = new JPanel();
    big_panel.setLayout(new BoxLayout(big_panel, BoxLayout.X_AXIS));
    big_panel.setAlignmentY(JPanel.TOP_ALIGNMENT);
    big_panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    frame.getContentPane().add(big_panel);
 
    final JPanel left_panel = new JPanel();
    left_panel.setLayout(new BoxLayout(left_panel, BoxLayout.Y_AXIS));
    left_panel.setAlignmentY(JPanel.TOP_ALIGNMENT);
    left_panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    big_panel.add(left_panel);

    JPanel display_panel = new JPanel();
    display_panel.setLayout(new BoxLayout(display_panel, BoxLayout.Y_AXIS));
    display_panel.setAlignmentY(JPanel.TOP_ALIGNMENT);
    display_panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    big_panel.add(display_panel);

    // create row JPanels
    for (int i=0; i<N_ROWS; i++) {
      row_panels[i] = new JPanel();
      row_panels[i].setLayout(new BoxLayout(row_panels[i],
                                            BoxLayout.X_AXIS));
      row_panels[i].setAlignmentY(JPanel.TOP_ALIGNMENT);
      row_panels[i].setAlignmentX(JPanel.LEFT_ALIGNMENT);
      display_panel.add(row_panels[i]);

      // create cell JPanels
      for (int j=0; j<N_COLUMNS; j++) {
        cell_panels[i][j] = new JPanel();
        cell_panels[i][j].setLayout(new BoxLayout(cell_panels[i][j],
                                                 BoxLayout.Y_AXIS));
        cell_panels[i][j].setAlignmentY(JPanel.TOP_ALIGNMENT);
        cell_panels[i][j].setAlignmentX(JPanel.LEFT_ALIGNMENT);
        row_panels[i].add(cell_panels[i][j]);
        cell_refs[i][j] = new DataReferenceImpl("cell_" + i + "_" + j);
        displays[i][j] = new DisplayImplJ3D("display_" + i + "_" + j,
                                            new TwoDDisplayRendererJ3D());
        displays[i][j].addMap(new ScalarMap(y_domain, Display.XAxis));
        displays[i][j].addMap(new ScalarMap(x_domain, Display.YAxis));

        ScalarMap shape_map = new ScalarMap(shape, Display.Shape);
        displays[i][j].addMap(shape_map);
        ShapeControl shape_control = (ShapeControl) shape_map.getControl();
        shape_control.setShapeSet(shape_count_set);
        shape_control.setShapes(shapes);

        projection_controls[i][j] = displays[i][j].getProjectionControl();
        projection_controls[i][j].addControlListener(this);

        display_done[i][j] = false;
        JPanel d_panel = (JPanel) displays[i][j].getComponent();
        Border etchedBorder5 =
          new CompoundBorder(new EtchedBorder(),
                             new EmptyBorder(5, 5, 5, 5));
        d_panel.setBorder(etchedBorder5);
        cell_panels[i][j].add(d_panel);
        JPanel wpanel = new JPanel();
        wpanel.setLayout(new BoxLayout(wpanel, BoxLayout.X_AXIS));
        JLabel cell_label = new JLabel(cell_names[i][j]);
        cell_fields[i][j] = new JTextField("---");
        Dimension msize = cell_fields[i][j].getMaximumSize();
        Dimension psize = cell_fields[i][j].getPreferredSize();
        msize.height = psize.height;
        cell_fields[i][j].setMaximumSize(msize);
        wpanel.add(cell_label);
        wpanel.add(cell_fields[i][j]);
        // wpanel.add(Box.createRigidArea(new Dimension(10, 0)));
        cell_panels[i][j].add(wpanel);

      } // end for (int j=0; j<N_ROWS; j++)
    } // end for (int i=0; i<N_COLUMNS; i++)

    DisplayImpl.delay(DELAY);

    VisADSlider slider300 = new VisADSlider("num300", 0, 600, 300, 1.0,
                                            ref300, RealType.Generic);
    VisADSlider slider1_4 = new VisADSlider("num1_4", 0, 280, 140, 0.01,
                                            ref1_4, RealType.Generic);
    VisADSlider sliderMAX = new VisADSlider("colorMAX", 0, 1000, ((int) MAX),
                                            1.0, refMAX, RealType.Generic);

    left_panel.add(slider300);
    left_panel.add(new JLabel("  "));
    left_panel.add(slider1_4);
    left_panel.add(new JLabel("  "));
    left_panel.add(sliderMAX);
    left_panel.add(new JLabel("  "));

    // cell A1
    displays[0][0].addMap(new ScalarMap(range_types[0], Display.Red));
    displays[0][0].addMap(new ScalarMap(range_types[1], Display.Green));
    displays[0][0].addMap(new ScalarMap(range_types[2], Display.Blue));
    displays[0][0].addMap(new ScalarMap(time, Display.Animation));
    cell_refs[0][0].setData(vis5d);
    displays[0][0].addReference(cell_refs[0][0]);
    display_done[0][0] = true;

    DisplayImpl.delay(DELAY);

    // cell B1
    cells[0][1] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        Field field = (Field) cell_refs[0][0].getData();
        if (field != null) {
          cell_refs[0][1].setData(field.getSample(0));
        }
      }
    };
    cells[0][1].addReference(cell_refs[0][0]);

    displays[0][1].addMap(new ScalarMap(range_types[0], Display.Red));
    displays[0][1].addMap(new ScalarMap(range_types[1], Display.Green));
    displays[0][1].addMap(new ScalarMap(range_types[2], Display.Blue));
    displays[0][1].addReference(cell_refs[0][1]);
    display_done[0][1] = true;

    DisplayImpl.delay(DELAY);

    // cell C1
    cells[0][2] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = baseCell(cell_refs[0][1], 0);
        if (field != null) {
          cell_refs[0][2].setData(field);
        }
      }
    };
    cells[0][2].addReference(cell_refs[0][1]);
    cells[0][2].addReference(ref300);
    cells[0][2].addReference(ref1_4);

    color_map = new ScalarMap(rangeC1, Display.RGB);
    displays[0][2].addMap(color_map);
    color_widgetC1 = new LabeledRGBWidget(color_map, (float) MIN, (float) MAX);
    Dimension d = new Dimension(500, 170);
    color_widgetC1.setMaximumSize(d);
    color_map.setRange(MIN, MAX);

    left_panel.add(color_widgetC1);
    left_panel.add(new JLabel("  "));

    color_control = (ColorControl) color_map.getControl();
    // listener sets all non-null color_controls[i][j]
    // for ControlEvents from color_control
    color_control.addControlListener(this);
    // controlChanged(null);

    displays[0][2].addReference(cell_refs[0][2]);
    displays[0][2].addReferences(new DirectManipulationRendererJ3D(), ref_cursor);
    display_done[0][2] = true;

    DisplayImpl.delay(DELAY);

    // cell A2
    cells[1][0] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = baseCell(cell_refs[0][1], 1);
        if (field != null) {
          cell_refs[1][0].setData(field);
          // finishDisplay(field, 1, 0);
        }
      }
    };
    cells[1][0].addReference(cell_refs[0][1]);
    cells[1][0].addReference(ref300);
    cells[1][0].addReference(ref1_4);
    finishDisplay((RealType) range.getComponent(1), 1, 0);

    DisplayImpl.delay(DELAY);

    // cell B2
    cells[1][1] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = baseCell(cell_refs[0][1], 2);
        if (field != null) {
          cell_refs[1][1].setData(field);
          // finishDisplay(field, 1, 1);
        }
      }
    };
    cells[1][1].addReference(cell_refs[0][1]);
    cells[1][1].addReference(ref300);
    cells[1][1].addReference(ref1_4);
    finishDisplay((RealType) range.getComponent(2), 1, 1);

    DisplayImpl.delay(DELAY);

    // cell C2
    cells[1][2] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = baseCell(cell_refs[0][1], 3);
        if (field != null) {
          cell_refs[1][2].setData(field);
          // finishDisplay(field, 1, 2);
        }
      }
    };
    cells[1][2].addReference(cell_refs[0][1]);
    cells[1][2].addReference(ref300);
    cells[1][2].addReference(ref1_4);
    finishDisplay((RealType) range.getComponent(3), 1, 2);

    DisplayImpl.delay(DELAY);

    // cell A3
    cells[2][0] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = baseCell(cell_refs[0][1], 4);
        if (field != null) {
          cell_refs[2][0].setData(field);
          // finishDisplay(field, 2, 0);
        }
      }
    };
    cells[2][0].addReference(cell_refs[0][1]);
    cells[2][0].addReference(ref300);
    cells[2][0].addReference(ref1_4);
    finishDisplay((RealType) range.getComponent(4), 2, 0);

    DisplayImpl.delay(DELAY);

    // cell B3
    cells[2][1] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = baseCell(cell_refs[0][1], 5);
        if (field != null) {
          cell_refs[2][1].setData(field);
          // finishDisplay(field, 2, 1);
        }
      }
    };
    cells[2][1].addReference(cell_refs[0][1]);
    cells[2][1].addReference(ref300);
    cells[2][1].addReference(ref1_4);
    finishDisplay((RealType) range.getComponent(5), 2, 1);

    DisplayImpl.delay(DELAY);

    // cell C3
    cells[2][2] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField fieldC1 = (FlatField) cell_refs[0][2].getData();
        FlatField fieldA2 = (FlatField) cell_refs[1][0].getData();
        FlatField fieldB2 = (FlatField) cell_refs[1][1].getData();
        FlatField fieldC2 = (FlatField) cell_refs[1][2].getData();
        FlatField fieldA3 = (FlatField) cell_refs[2][0].getData();
        FlatField fieldB3 = (FlatField) cell_refs[2][1].getData();
        if (fieldC1 != null && fieldA2 != null && fieldB2 != null &&
            fieldC2 != null && fieldA3 != null && fieldB3 != null) {
          FlatField field = (FlatField) fieldC1.add(fieldA2);
          field = (FlatField) field.add(fieldB2);
          field = (FlatField) field.add(fieldC2);
          field = (FlatField) field.add(fieldA3);
          field = (FlatField) field.multiply(ten);
          fieldB3 = (FlatField) fieldB3.multiply(three);
          field = (FlatField) field.add(fieldB3);
          field = (FlatField) field.divide(fifty_three);

          cell_refs[2][2].setData(field);
          // finishDisplay(field, 2, 2);
        }
      }
    };
    cells[2][2].addReference(cell_refs[0][2]);
    cells[2][2].addReference(cell_refs[1][0]);
    cells[2][2].addReference(cell_refs[1][1]);
    cells[2][2].addReference(cell_refs[1][2]);
    cells[2][2].addReference(cell_refs[2][0]);
    cells[2][2].addReference(cell_refs[2][1]);
    finishDisplay(rangeC1, 2, 2);

    DisplayImpl.delay(DELAY);

    // cell A4
    cells[3][0] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = (FlatField) cell_refs[0][1].getData();
        if (field != null) {
          field = (FlatField) field.extract(6);
          cell_refs[3][0].setData(field);
          // finishDisplay(field, 3, 0);
        }
      }
    };
    cells[3][0].addReference(cell_refs[0][1]);
    finishDisplay((RealType) range.getComponent(6), 3, 0);

    DisplayImpl.delay(DELAY);

    // cell B4
    cells[3][1] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = (FlatField) cell_refs[0][1].getData();
        if (field != null) {
          field = (FlatField) field.extract(7);
          cell_refs[3][1].setData(field);
          // finishDisplay(field, 3, 1);
        }
      }
    };
    cells[3][1].addReference(cell_refs[0][1]);
    finishDisplay((RealType) range.getComponent(7), 3, 1);

    GraphicsModeControl mode = displays[3][1].getGraphicsModeControl();
    mode.setTextureEnable(false);
    mode.setPointMode(true);
    mode.setPointSize(5.0f);

    DisplayImpl.delay(DELAY);

    // cell C4
    cells[3][2] = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        FlatField field = (FlatField) cell_refs[0][1].getData();
        if (field != null) {
          field = (FlatField) field.extract(8);
          cell_refs[3][2].setData(field);
        }
      }
    };
    cells[3][2].addReference(cell_refs[0][1]);

    ScalarMap color_mapC4 = new ScalarMap(rangeC4, Display.RGB);
    displays[3][2].addMap(color_mapC4);
    color_widgetC4 = new LabeledRGBWidget(color_mapC4, (float) MIN, (float) MAXC4);
    Dimension dC4 = new Dimension(500, 170);
    color_widgetC4.setMaximumSize(dC4);
    color_mapC4.setRange(MIN, MAXC4);

    left_panel.add(color_widgetC4);
    left_panel.add(new JLabel("  "));

    displays[3][2].addReference(cell_refs[3][2]);
    displays[3][2].addReferences(new DirectManipulationRendererJ3D(), ref_cursor);
    display_done[3][2] = true;

    DisplayImpl.delay(DELAY);

    Cell cellMAX = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        double max = ((Real) refMAX.getData()).getValue();
        color_map.setRange(MIN, max);
        for (int i=0; i<N_ROWS; i++) {
          for (int j=0; j<N_COLUMNS; j++) {
            if (color_maps[i][j] != null) {
              color_maps[i][j].setRange(MIN, max);
            }
          }
        }
      }
    };
    cellMAX.addReference(refMAX);

    DisplayImpl.delay(DELAY);

    Cell cell_cursor = new CellImpl() {
      public void doAction() throws VisADException, RemoteException {
        RealTuple c = (RealTuple) ref_cursor.getData();
        RealTuple dom = new RealTuple(new Real[]
                        {(Real) c.getComponent(0), (Real) c.getComponent(1)});
        for (int i=0; i<N_ROWS; i++) {
          for (int j=0; j<N_COLUMNS; j++) {
            try {
              FlatField field = (FlatField) cell_refs[i][j].getData();
              double val = ((Real) field.evaluate(dom)).getValue();
              cell_fields[i][j].setText("" + val);
            }
            catch (Exception e) {}
          }
        }
      }
    }; 
    cell_cursor.addReference(ref_cursor);

    DisplayImpl.delay(DELAY);

    // make the JFrame visible
    frame.setVisible(true);
  }

  public static FlatField baseCell(DataReferenceImpl ref, int component)
         throws VisADException, RemoteException {
    FlatField field = (FlatField) ref.getData();
    if (field != null) {
      field = (FlatField) field.extract(component);
      field = (FlatField) field.divide(ten);
      field = (FlatField) ten.pow(field);
      field = (FlatField) field.divide(ref300.getData());
      field = (FlatField) field.pow(one.divide(ref1_4.getData()));
    }
    return field;
  }

/*
  public static void finishDisplay(FlatField field, int i, int j)
         throws VisADException, RemoteException {
    FunctionType type = (FunctionType) field.getType();
    if (!display_done[i][j] && type != null) {
      RealType rt = (RealType) type.getRange();
      // System.out.println("i, j = " + i + " " + j + " rt = " + rt);
      color_maps[i][j] = new ScalarMap(rt, Display.RGB);
      displays[i][j].addMap(color_maps[i][j]);
      color_maps[i][j].setRange(MIN, MAX);
      color_controls[i][j] = (ColorControl) color_maps[i][j].getControl();
      if (color_control != null) { 
        float[][] table = color_control.getTable();
        if (table != null) color_controls[i][j].setTable(table);
      }
      displays[i][j].addReference(cell_refs[i][j]);
      displays[i][j].addReferences(new DirectManipulationRendererJ3D(), ref_cursor);
      display_done[i][j] = true;
    }
  }
*/
  public static void finishDisplay(RealType rt, int i, int j)
         throws VisADException, RemoteException {
    color_maps[i][j] = new ScalarMap(rt, Display.RGB);
    displays[i][j].addMap(color_maps[i][j]);
    color_maps[i][j].setRange(MIN, MAX);
    color_controls[i][j] = (ColorControl) color_maps[i][j].getControl();
    if (color_control != null) { 
      float[][] table = color_control.getTable();
      if (table != null) color_controls[i][j].setTable(table);
    }
    displays[i][j].addReference(cell_refs[i][j]);
    displays[i][j].addReferences(new DirectManipulationRendererJ3D(), ref_cursor);
    display_done[i][j] = true;
  }

  public void controlChanged(ControlEvent e)
         throws VisADException, RemoteException {
    Control control = e.getControl();
    if (control != null && control instanceof ColorControl) {
      float[][] table = ((ColorControl) control).getTable();
      if (table != null) {
        for (int i=0; i<N_ROWS; i++) {
          for (int j=0; j<N_COLUMNS; j++) {
            if (color_controls[i][j] != null) {
              color_controls[i][j].setTable(table);
            }
          }
        }
      }
    }
    else if (!in_proj && control != null &&
             control instanceof ProjectionControl) {
      in_proj = true; // don't allow setMatrix below to re-trigger
      double[] matrix = ((ProjectionControl) control).getMatrix();
      for (int i=0; i<N_ROWS; i++) {
        for (int j=0; j<N_COLUMNS; j++) {
          if (control != projection_controls[i][j]) {
            projection_controls[i][j].setMatrix(matrix);
          }
        }
      }
      in_proj = false;
    }
  }

}

