/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.roommodel0;

import com.io7m.jfunctional.Pair;
import com.io7m.jmurmur.Murmur3;
import com.io7m.jregions.core.unparameterized.areas.AreaI;
import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jspatial.api.TreeVisitResult;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableIType;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors2I;
import com.io7m.roommodel0.mesh.Mesh;
import com.io7m.roommodel0.mesh.MeshEditing;
import com.io7m.roommodel0.mesh.MeshEditingPolygonCreatorType;
import com.io7m.roommodel0.mesh.MeshEditingType;
import com.io7m.roommodel0.mesh.MeshEditingVertexMoverType;
import com.io7m.roommodel0.mesh.MeshReadableType;
import com.io7m.roommodel0.mesh.MeshType;
import com.io7m.roommodel0.mesh.PolygonEdgeType;
import com.io7m.roommodel0.mesh.PolygonType;
import com.io7m.roommodel0.mesh.PolygonVertexType;
import com.io7m.roommodel0.undo.UndoController;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.jnull.NullCheck.notNull;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public final class RoomDemo
{
  private static final Logger LOG;
  private static int GRID_SNAP = 32;

  static {
    LOG = LoggerFactory.getLogger(RoomDemo.class);
  }

  private RoomDemo()
  {

  }

  private static Stroke dashedStroke()
  {
    final float[] dash = {1.0f};
    return new BasicStroke(
      1.0f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,
      10.0f,
      dash,
      0.0f);
  }

  private static int snap(
    final int v,
    final int r)
  {
    return (int) (Math.floor((double) v / (double) r) * (double) r);
  }

  public static void main(
    final String[] args)
  {
    SwingUtilities.invokeLater(() -> {
      final PolygonWindow window = new PolygonWindow();
      window.pack();
      window.setVisible(true);
      window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    });
  }

  private interface EditingOperationType
    extends MouseListener, MouseMotionListener
  {
    void paint(Graphics2D g);
  }

  private static final class PolygonCanvas extends JPanel
  {
    private final UndoController<MeshType> undo_controller;
    private final MeshEditingType mesh_editing;
    private final MeshType mesh;
    private RoomModelLiquidCells liquid_cells;
    private Vector2I mouse;
    private Vector2I mouse_snap;
    private EditingOperationType edit_op;

    PolygonCanvas(
      final PublishSubject<String> messages)
    {
      this.mouse = Vectors2I.zero();
      this.mouse_snap = Vectors2I.zero();

      this.mesh =
        Mesh.create(
          AreaI.of(32, 16 * 32, 32, 10 * 32));
      this.undo_controller =
        new UndoController<>(this.mesh, 32);
      this.mesh_editing =
        MeshEditing.create(this.undo_controller);
      this.liquid_cells =
        RoomModelLiquidCells.generate(this.mesh);

      this.addMouseMotionListener(new MouseAdapter()
      {
        @Override
        public void mouseMoved(final MouseEvent e)
        {
          PolygonCanvas.this.onMouseMoved(e.getX(), e.getY());
        }
      });

      this.undo_controller.observable().subscribe(
        e -> this.repaint());
      this.undo_controller.observable().subscribe(
        e -> this.liquid_cells = RoomModelLiquidCells.generate(this.mesh));
    }

    private static void paintQuadTree(
      final Graphics2D g,
      final QuadTreeReadableIType<PolygonType> q)
    {
      final Stroke s = g.getStroke();
      try {
        g.setColor(Color.PINK);
        g.setStroke(dashedStroke());
        q.iterateQuadrants(g, (gg, quadrant, depth) -> {
          final AreaI area = quadrant.area();
          gg.drawRect(
            area.minimumX(), area.minimumY(), area.sizeX(), area.sizeY());
          return TreeVisitResult.RESULT_CONTINUE;
        });
      } finally {
        g.setStroke(s);
      }
    }

    private static void paintPolygon(
      final Graphics2D g,
      final PolygonType p)
    {
      final Stroke s = g.getStroke();
      try {
        g.setColor(Color.GRAY);
        g.setStroke(dashedStroke());
        g.drawRect(
          p.bounds().minimumX(),
          p.bounds().minimumY(),
          p.bounds().sizeX(),
          p.bounds().sizeY());
      } finally {
        g.setStroke(s);
      }

      final List<PolygonVertexType> vs = p.vertices();
      final List<PolygonEdgeType> es = p.edges();

      g.setColor(new Color(0xf0, 0xf0, 0xf0));

      {
        final int[] xs = new int[vs.size()];
        final int[] ys = new int[vs.size()];
        for (int index = 0; index < vs.size(); ++index) {
          xs[index] = vs.get(index).position().x();
          ys[index] = vs.get(index).position().y();
        }
        g.fillPolygon(xs, ys, xs.length);
      }

      for (int index = 0; index < es.size(); ++index) {
        final PolygonEdgeType e = es.get(index);

        if (e.isExternal()) {
          g.setColor(Color.BLUE);
        } else {
          g.setColor(Color.CYAN);
        }

        final Vector2I p0 = e.vertex0().position();
        final Vector2I p1 = e.vertex1().position();
        g.drawLine(p0.x(), p0.y(), p1.x(), p1.y());

        if (e.isExternal()) {
          final Vector2I normal_p0 =
            Vectors2I.add(
              p0,
              Vectors2I.scale(Vectors2I.subtract(p1, p0), 0.5));

          final Vector2D extra =
            Vectors2D.scale(e.normal(), 10.0);

          final Vector2I normal_p1 =
            Vectors2I.add(
              normal_p0,
              Vector2I.of((int) extra.x(), (int) extra.y()));

          g.drawLine(
            normal_p0.x(),
            normal_p0.y(),
            normal_p1.x(),
            normal_p1.y());
        }
      }

      g.setColor(Color.BLUE);
      for (int index = 0; index < vs.size(); ++index) {
        final PolygonVertexType v = vs.get(index);
        final Vector2I pos = v.position();
        g.fillOval(pos.x() - 4, pos.y() - 4, 8, 8);
        g.drawString(
          "V" + Long.toString(v.id().value()),
          pos.x() - 8,
          pos.y() - 8);
      }

      final Vector2D center =
        RoomPolygons.barycenter(
          p.vertices()
            .stream()
            .map(PolygonVertexType::position)
            .collect(Collectors.toList()));

      g.drawString(
        "P" + Long.toString(p.id().value()),
        (int) center.x(),
        (int) center.y());
    }

    private void paintGrid(
      final Graphics2D g)
    {
      final Stroke s = g.getStroke();
      try {
        g.setColor(new Color(0xee, 0xee, 0xee));
        g.setStroke(dashedStroke());

        final int w = this.getWidth();
        final int h = this.getHeight();
        for (int x = 0; x < w; x += GRID_SNAP) {
          g.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += GRID_SNAP) {
          g.drawLine(0, y, w, y);
        }
      } finally {
        g.setStroke(s);
      }
    }

    private void onMouseMoved(
      final int x,
      final int y)
    {
      LOG.trace("onMouseMoved: {} {}", Integer.valueOf(x), Integer.valueOf(y));
      this.mouse = Vector2I.of(x, y);
      this.mouse_snap = Vector2I.of(
        snap(x + (GRID_SNAP / 2), GRID_SNAP),
        snap(y + (GRID_SNAP / 2), GRID_SNAP));

      this.repaint();
    }

    @Override
    public void paint(
      final Graphics g)
    {
      super.paint(g);

      final Graphics2D gg = (Graphics2D) g;
      gg.setPaint(Color.WHITE);
      gg.fillRect(0, 0, this.getWidth(), this.getHeight());

      this.paintGrid(gg);
      this.paintModel(gg);
      this.paintLiquid(gg);

      {
        final EditingOperationType op = this.edit_op;
        if (op != null) {
          op.paint(gg);
        }
      }

      gg.setPaint(Color.GREEN);
      gg.fillOval(this.mouse.x() - 4, this.mouse.y() - 4, 8, 8);

      gg.drawString(
        String.format(
          "%dx%d",
          Integer.valueOf(this.mouse.x()),
          Integer.valueOf(this.mouse.y())),
        this.mouse_snap.x() + 8,
        this.mouse_snap.y() + 8);

      gg.setPaint(Color.RED);
      gg.fillOval(this.mouse_snap.x() - 4, this.mouse_snap.y() - 4, 8, 8);

      gg.drawString(
        String.format(
          "%dx%d",
          Integer.valueOf(this.mouse_snap.x()),
          Integer.valueOf(this.mouse_snap.y())),
        this.mouse_snap.x() + 8,
        this.mouse_snap.y() - 8);
    }

    private void paintLiquid(
      final Graphics2D gg)
    {
      for (int p_index = 0; p_index < this.liquid_cells.polygons.size(); ++p_index) {
        final List<Vector2I> p = this.liquid_cells.polygons.get(p_index);

        gg.setPaint(new Color(Murmur3.hashInt(p_index)));

        for (int index = 0; index < p.size(); ++index) {
          final Vector2I v0 = p.get(index);
          final Vector2I v1;
          if (index + 1 < p.size()) {
            v1 = p.get(index + 1);
          } else {
            v1 = p.get(0);
          }
          gg.drawLine(v0.x(), v0.y(), v1.x(), v1.y());
        }

        {
          final Vector2D c = RoomPolygons.barycenter(p);
          gg.drawString(Integer.toString(p_index), (int) c.x(), (int) c.y());
        }
      }

      gg.setPaint(Color.RED);
      for (final Pair<Vector2I, Vector2I> p : this.liquid_cells.intersections) {
        final Vector2I p0 = p.getLeft();
        final Vector2I p1 = p.getRight();

        final int p0x = p0.x();
        final int p0y = p0.y();
        final int p1x = p1.x();
        final int p1y = p1.y();
        gg.drawLine(p0x, p0y, p1x, p1y);
        gg.fillOval(p0x - 2, p0y - 2, 4, 4);
        gg.fillOval(p1x - 2, p1y - 2, 4, 4);
      }
    }

    private void paintModel(final Graphics2D g)
    {
      final MeshReadableType m = this.undo_controller.state();

      paintQuadTree(g, m.polygonTree());

      for (final PolygonType p : m.polygons()) {
        paintPolygon(g, p);
      }
    }

    public void undo()
    {
      this.undo_controller.undo();
      final List<String> errors = this.mesh.check();
      errors.forEach(e -> LOG.error("bug: {}", e));
    }

    public void startEditingOperation(
      final EditingOperationType op)
    {
      if (this.edit_op != null) {
        this.stopEditingOperation(this.edit_op);
      }

      this.edit_op = notNull(op, "op");
      this.addMouseMotionListener(this.edit_op);
      this.addMouseListener(this.edit_op);
    }

    public void stopEditingOperation(
      final EditingOperationType op)
    {
      this.removeMouseMotionListener(op);
      this.removeMouseListener(op);
      this.edit_op = null;
    }
  }

  private static final class PolygonDeletionEditingOp
    extends MouseAdapter implements EditingOperationType
  {
    private final PublishSubject<String> messages;
    private final PolygonCanvas canvas;
    private final MeshEditingType editing;

    PolygonDeletionEditingOp(
      final PolygonCanvas in_canvas,
      final PublishSubject<String> in_messages,
      final MeshEditingType in_editing)
    {
      this.canvas = notNull(in_canvas, "Canvas");
      this.messages = notNull(in_messages, "Messages");
      this.editing = notNull(in_editing, "Editing");
    }

    @Override
    public void paint(
      final Graphics2D g)
    {

    }

    @Override
    public void mouseReleased(
      final MouseEvent e)
    {
      final int x = e.getX();
      final int y = e.getY();
      final int button = e.getButton();

      try {
        LOG.trace(
          "onMouseReleased: {} {} {}",
          Integer.valueOf(x),
          Integer.valueOf(y),
          Integer.valueOf(button));

        if (button == 1) {
          this.editing.polygonDelete(x, y);
        }
      } catch (final Exception ex) {
        this.messages.onNext(ex.getMessage());
        LOG.error("error: ", ex);
      }
    }
  }

  private static final class VertexMovingOp
    extends MouseAdapter implements EditingOperationType
  {
    private final PublishSubject<String> messages;
    private final PolygonCanvas canvas;
    private final MeshEditingVertexMoverType vertex_move;
    private Vector2I mouse_snap;

    VertexMovingOp(
      final PolygonCanvas in_canvas,
      final PublishSubject<String> in_messages,
      final MeshEditingType in_editing)
    {
      this.canvas = notNull(in_canvas, "Canvas");
      this.messages = notNull(in_messages, "Messages");
      this.vertex_move = notNull(in_editing, "Editing").vertexMove();
    }

    @Override
    public void mouseReleased(
      final MouseEvent e)
    {
      this.mouse_snap = Vector2I.of(
        snap(e.getX() + (GRID_SNAP / 2), GRID_SNAP),
        snap(e.getY() + (GRID_SNAP / 2), GRID_SNAP));

      if (e.getButton() == 1) {
        if (this.vertex_move.isVertexSelected()) {
          if (this.vertex_move.isVertexOK()) {
            this.vertex_move.commit();
            this.canvas.stopEditingOperation(this);
            this.messages.onNext("");
          } else {
            this.messages.onNext("Cannot move vertex here.");
          }
        } else {
          this.vertex_move.selectVertex(this.mouse_snap);
          this.messages.onNext("Move the cursor to the new vertex position.");
        }
      }
    }

    @Override
    public void mouseMoved(
      final MouseEvent e)
    {
      this.mouse_snap = Vector2I.of(
        snap(e.getX() + (GRID_SNAP / 2), GRID_SNAP),
        snap(e.getY() + (GRID_SNAP / 2), GRID_SNAP));

      if (this.vertex_move.isVertexSelected()) {
        this.vertex_move.setVertexPosition(this.mouse_snap);
      }
    }

    @Override
    public void paint(
      final Graphics2D g)
    {
      if (this.vertex_move.isVertexSelected()) {
        for (final MeshEditingVertexMoverType.TemporaryPolygonType p :
          this.vertex_move.temporaryPolygons()) {

          {
            final List<MeshEditingVertexMoverType.TemporaryVertexType> vs = p.vertices();
            final int[] xs = new int[vs.size()];
            final int[] ys = new int[vs.size()];
            for (int index = 0; index < vs.size(); ++index) {
              xs[index] = vs.get(index).position().x();
              ys[index] = vs.get(index).position().y();
            }

            if (p.isConvex()) {
              g.setPaint(new Color(0xe0, 0xe0, 0xe0, 0x90));
            } else {
              g.setPaint(new Color(0xff, 0x00, 0x00, 0x90));
            }

            g.fillPolygon(xs, ys, xs.length);

            if (p.isConvex()) {
              g.setPaint(new Color(0xa0, 0xa0, 0xa0, 0xff));
            } else {
              g.setPaint(new Color(0xff, 0x00, 0x00, 0xff));
            }

            g.drawPolygon(xs, ys, xs.length);

            for (int index = 0; index < vs.size(); ++index) {
              g.fillOval(xs[index] - 4, ys[index] - 4, 8, 8);
            }
          }
        }
      }
    }
  }

  private static final class PolygonCreatorEditingOp
    extends MouseAdapter implements EditingOperationType
  {
    private final PublishSubject<String> messages;
    private final PolygonCanvas canvas;
    private final MeshEditingPolygonCreatorType poly_create;
    private Vector2I mouse_snap;

    PolygonCreatorEditingOp(
      final PolygonCanvas in_canvas,
      final PublishSubject<String> in_messages,
      final MeshEditingType in_editing)
    {
      this.canvas = notNull(in_canvas, "Canvas");
      this.messages = notNull(in_messages, "Messages");
      this.poly_create = notNull(in_editing, "Editing").polygonCreate();
    }

    @Override
    public void mouseReleased(
      final MouseEvent e)
    {
      final int x = e.getX();
      final int y = e.getY();
      final int button = e.getButton();

      try {
        LOG.trace(
          "onMouseReleased: {} {} {}",
          Integer.valueOf(x),
          Integer.valueOf(y),
          Integer.valueOf(button));

        if (button == 1) {
          if (this.poly_create.addVertex(this.mouse_snap)) {
            final PolygonType pid = this.poly_create.create();
            this.messages.onNext("Created " + pid.id().value());
            this.canvas.stopEditingOperation(this);
          }
        }
      } catch (final Exception ex) {
        this.messages.onNext(ex.getMessage());
        LOG.error("error: ", ex);
      }
    }

    @Override
    public void mouseMoved(
      final MouseEvent e)
    {
      final int x = e.getX();
      final int y = e.getY();
      LOG.trace(
        "onMouseMoved: {} {}",
        Integer.valueOf(x),
        Integer.valueOf(y));
      this.mouse_snap = Vector2I.of(
        snap(x + (GRID_SNAP / 2), GRID_SNAP),
        snap(y + (GRID_SNAP / 2), GRID_SNAP));
    }

    @Override
    public void paint(final Graphics2D g)
    {
      final List<Vector2I> vs = this.poly_create.vertices();
      for (int index = 0; index < vs.size(); ++index) {
        final int index_next;
        if (index + 1 == vs.size()) {
          index_next = 0;
        } else {
          index_next = index + 1;
        }

        final Vector2I v0 = vs.get(index);
        final Vector2I v1 = vs.get(index_next);
        g.setColor(Color.RED);
        g.drawLine(v0.x(), v0.y(), v1.x(), v1.y());

        if (index == 0) {
          g.setColor(Color.GREEN);
        }
        g.fillOval(v0.x() - 4, v0.y() - 4, 8, 8);
      }
    }
  }

  private static final class StatusBar extends JPanel
  {
    private final JLabel text_field;

    StatusBar(final Observable<String> messages)
    {
      this.text_field = new JLabel(" ");
      this.setLayout(new FlowLayout(FlowLayout.LEFT));
      this.add(this.text_field);
      messages.subscribe(this.text_field::setText);
    }
  }

  private static final class ToolBar extends JPanel
  {
    private final JButton create_polygon;
    private final JButton delete_polygon;
    private final JButton move_vertex;

    ToolBar(
      final PolygonCanvas canvas,
      final PublishSubject<String> messages)
    {
      this.create_polygon =
        new JButton(new ImageIcon(
          RoomDemo.class.getResource("polygon_create.png")));
      this.create_polygon.setToolTipText("Create polygons");
      this.create_polygon.addActionListener(
        e -> {
          messages.onNext(
            "Create points by clicking. Click the starting point to create a polygon.");
          canvas.startEditingOperation(
            new PolygonCreatorEditingOp(
              canvas,
              messages,
              canvas.mesh_editing));
        });

      this.delete_polygon =
        new JButton(new ImageIcon(
          RoomDemo.class.getResource("polygon_delete.png")));
      this.delete_polygon.setToolTipText("Delete polygons");
      this.delete_polygon.addActionListener(
        e -> {
          messages.onNext(
            "Click a polygon to delete it.");
          canvas.startEditingOperation(
            new PolygonDeletionEditingOp(
              canvas,
              messages,
              canvas.mesh_editing));
        });

      this.move_vertex =
        new JButton(new ImageIcon(
          RoomDemo.class.getResource("vertex_move.png")));
      this.move_vertex.setToolTipText("Move vertices");
      this.move_vertex.addActionListener(
        e -> {
          messages.onNext(
            "Click a vertex to start moving it.");
          canvas.startEditingOperation(
            new VertexMovingOp(
              canvas,
              messages,
              canvas.mesh_editing));
        });

      this.setLayout(new FlowLayout(FlowLayout.LEFT));
      this.add(this.create_polygon);
      this.add(this.delete_polygon);
      this.add(this.move_vertex);
    }
  }

  private static final class PolygonWindow extends JFrame
  {
    private final PolygonCanvas canvas;
    private final ToolBar toolbar;
    private final StatusBar status;
    private final PublishSubject<String> messages;

    private PolygonWindow()
    {
      super("Polygons");

      this.messages = PublishSubject.create();
      this.canvas = new PolygonCanvas(this.messages);
      this.toolbar = new ToolBar(this.canvas, this.messages);
      this.status = new StatusBar(this.messages);
      this.canvas.setFocusable(true);
      this.canvas.requestFocusInWindow();

      final JMenuItem file_exit = new JMenuItem("Exit");
      file_exit.addActionListener(e -> this.dispose());
      file_exit.setMnemonic('x');
      final JMenu file = new JMenu("File");
      file.setMnemonic('F');
      file.add(file_exit);

      final JMenuItem edit_undo = new JMenuItem("Undo");
      edit_undo.setEnabled(false);
      edit_undo.setMnemonic('U');
      edit_undo.setAccelerator(
        KeyStroke.getKeyStroke((int) 'Z', CTRL_DOWN_MASK));
      edit_undo.addActionListener(e -> this.canvas.undo());
      this.canvas.undo_controller.observable().subscribe(
        state -> {
          final Optional<String> operation_opt = state.undoOperation();
          if (operation_opt.isPresent()) {
            final String operation = operation_opt.get();
            edit_undo.setText("Undo " + operation);
            edit_undo.setEnabled(true);
          } else {
            edit_undo.setText("Undo");
            edit_undo.setEnabled(false);
          }
        });

      final JMenu edit = new JMenu("Edit");
      edit.setMnemonic('E');
      edit.add(edit_undo);

      final JMenuBar menu = new JMenuBar();
      menu.add(file);
      menu.add(edit);
      this.setJMenuBar(menu);

      this.setPreferredSize(new Dimension(800, 600));
      final Container content = this.getContentPane();
      content.setLayout(new BorderLayout());
      content.add(this.toolbar, BorderLayout.PAGE_START);
      content.add(this.canvas, BorderLayout.CENTER);
      content.add(this.status, BorderLayout.PAGE_END);
      this.addWindowListener(new WindowAdapter()
      {
        @Override
        public void windowOpened(final WindowEvent e)
        {
          PolygonWindow.this.canvas.requestFocusInWindow();
        }
      });
    }
  }
}