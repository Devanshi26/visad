//
// TwoDDisplayRendererJ3D.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2001 Bill Hibbard, Curtis Rueden, Tom
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

package visad.java3d;

import visad.*;

import java.awt.event.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * <CODE>TwoDDisplayRendererJ3D</CODE> is the VisAD class for 2-D background
 * and metadata rendering under Java3D.<P>
 */
public class TwoDDisplayRendererJ3D extends DisplayRendererJ3D {

  /** color of box and cursor */
  private ColoringAttributes box_color = null;
  private ColoringAttributes cursor_color = null;

  private MouseBehaviorJ3D mouse = null; // Behavior for mouse interactions

  /**
   * This <CODE>DisplayRenderer</CODE> supports 2-D only rendering.
   * It is easiest to describe in terms of differences from
   * <CODE>DefaultDisplayRendererJ3D</CODE>.  The cursor and box
   * around the scene are 2-D, the scene cannot be rotated,
   * the cursor cannot be translated in and out, and the
   * scene can be translated sideways with the left mouse
   * button with or without pressing the Ctrl key.<P>
   * No RealType may be mapped to ZAxis or Latitude.
   */
  public TwoDDisplayRendererJ3D () {
    super();
  }

  public boolean getMode2D() {
    return true;
  }

  public boolean legalDisplayScalar(DisplayRealType type) {
    if (Display.ZAxis.equals(type) ||
        Display.Latitude.equals(type)) return false;
    else return super.legalDisplayScalar(type);
  }

  /**
   * Create scene graph root, if none exists, with Transform
   * and direct manipulation root;
   * create 3-D box, lights and MouseBehaviorJ3D for
   * embedded user interface.
   * @param v
   * @param vpt
   * @param c
   * @return Scene graph root.
   */
  public BranchGroup createSceneGraph(View v, TransformGroup vpt,
                                      VisADCanvasJ3D c) {
    BranchGroup root = getRoot();
    if (root != null) return root;

    // create MouseBehaviorJ3D for mouse interactions
    mouse = new MouseBehaviorJ3D(this);
    getDisplay().setMouseBehavior(mouse);
    box_color = new ColoringAttributes();
    cursor_color = new ColoringAttributes();
    root = createBasicSceneGraph(v, vpt, c, mouse, box_color, cursor_color);
    TransformGroup trans = getTrans();

    // create the box containing data depictions
    LineArray box_geometry = new LineArray(8, LineArray.COORDINATES);

    // WLH 24 Nov 2000
    box_geometry.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

    box_geometry.setCoordinates(0, box_verts);
    Appearance box_appearance = new Appearance();
    box_color.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
    box_color.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
    float[] ctlBox = getRendererControl().getBoxColor();
    box_color.setColor(ctlBox[0], ctlBox[1], ctlBox[2]);
    box_appearance.setColoringAttributes(box_color);
    Shape3D box = new Shape3D(box_geometry, box_appearance);
    box.setCapability(Shape3D.ALLOW_GEOMETRY_READ); // WLH 24 Nov 2000
    BranchGroup box_on = getBoxOnBranch();
    box_on.addChild(box);

    Appearance cursor_appearance = new Appearance();
    cursor_color.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
    cursor_color.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
    float[] ctlCursor = getRendererControl().getCursorColor();
    cursor_color.setColor(ctlCursor[0], ctlCursor[1], ctlCursor[2]);
    cursor_appearance.setColoringAttributes(cursor_color);

    BranchGroup cursor_on = getCursorOnBranch();
    LineArray cursor_geometry = new LineArray(4, LineArray.COORDINATES);
    cursor_geometry.setCoordinates(0, cursor_verts);
    Shape3D cursor = new Shape3D(cursor_geometry, cursor_appearance);
    cursor_on.addChild(cursor);

    // insert MouseBehaviorJ3D into scene graph
    BoundingSphere bounds =
      new BoundingSphere(new Point3d(0.0,0.0,0.0), 2000000.0);
    mouse.setSchedulingBounds(bounds);
    trans.addChild(mouse);

    // create ambient light, directly under root (not transformed)
/* WLH 27 Jan 98
    Color3f color = new Color3f(0.4f, 0.4f, 0.4f);
*/
    Color3f color = new Color3f(0.6f, 0.6f, 0.6f);
    AmbientLight light = new AmbientLight(color);
    light.setInfluencingBounds(bounds);
    root.addChild(light);

    // create directional lights, directly under root (not transformed)
    Color3f dcolor = new Color3f(0.9f, 0.9f, 0.9f);
    Vector3f direction1 = new Vector3f(0.0f, 0.0f, 1.0f);
    Vector3f direction2 = new Vector3f(0.0f, 0.0f, -1.0f);
    DirectionalLight light1 =
      new DirectionalLight(true, dcolor, direction1);
    light1.setInfluencingBounds(bounds);
    DirectionalLight light2 =
      new DirectionalLight(true, dcolor, direction2);
    light2.setInfluencingBounds(bounds);
    root.addChild(light1);
    root.addChild(light2);

    return root;
  }

  // WLH 24 Nov 2000
  public void setBoxAspect(double[] aspect) {
    float[] new_verts = new float[box_verts.length];
    for (int i=0; i<box_verts.length; i+=3) {
      new_verts[i] = (float) (box_verts[i] * aspect[0]);
      new_verts[i+1] = (float) (box_verts[i+1] * aspect[1]);
      new_verts[i+2] = (float) (box_verts[i+2] * aspect[2]);
    }
    BranchGroup box_on = getBoxOnBranch();
    Shape3D box = (Shape3D) box_on.getChild(0);
    LineArray box_geometry = (LineArray) box.getGeometry();
    box_geometry.setCoordinates(0, new_verts);
  }

/*
  private static final float[] box_verts = {
     // front face
         -1.0f, -1.0f,  0.3f,                       -1.0f,  1.0f,  0.3f,
         -1.0f,  1.0f,  0.3f,                        1.0f,  1.0f,  0.3f,
          1.0f,  1.0f,  0.3f,                        1.0f, -1.0f,  0.3f,
          1.0f, -1.0f,  0.3f,                       -1.0f, -1.0f,  0.3f
  };

  private static final float[] cursor_verts = {
          0.0f,  0.1f,  0.3f,                        0.0f, -0.1f,  0.3f,
          0.1f,  0.0f,  0.3f,                       -0.1f,  0.0f,  0.3f
  };
*/

  private static final float[] box_verts = {
     // front face
         -1.0f, -1.0f,  0.0f,                       -1.0f,  1.0f,  0.0f,
         -1.0f,  1.0f,  0.0f,                        1.0f,  1.0f,  0.0f,
          1.0f,  1.0f,  0.0f,                        1.0f, -1.0f,  0.0f,
          1.0f, -1.0f,  0.0f,                       -1.0f, -1.0f,  0.0f
  };

  private static final float[] cursor_verts = {
          0.0f,  0.1f,  0.0f,                        0.0f, -0.1f,  0.0f,
          0.1f,  0.0f,  0.0f,                       -0.1f,  0.0f,  0.0f
  };
}

