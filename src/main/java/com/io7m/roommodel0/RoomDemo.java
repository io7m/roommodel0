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

import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jspatial.api.TreeVisitResult;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors2I;
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
import java.util.List;
import java.util.stream.Collectors;

import static com.io7m.jnull.NullCheck.notNull;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public final class RoomDemo
{
  private static final Logger LOG;

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

  private static void drawPoints(
    final Graphics2D gg,
    final List<Vector2I> pl,
    final Color color_first,
    final Color color_rest)
  {
    for (int index = 0; index < pl.size(); ++index) {
      final Vector2I pc = pl.get(index);

      gg.setPaint(color_rest);
      if (index + 1 < pl.size()) {
        final Vector2I pn = pl.get(index + 1);
        gg.drawLine(pc.x(), pc.y(), pn.x(), pn.y());
      }

      gg.setPaint(index == 0 ? color_first : color_rest);
      gg.fillOval(pc.x() - 4, pc.y() - 4, 8, 8);
    }

    if (pl.size() > 1) {
      final Vector2I first = pl.get(0);
      final Vector2I last = pl.get(pl.size() - 1);
      gg.drawLine(first.x(), first.y(), last.x(), last.y());
    }
  }

  private static int GRID_SNAP = 32;

  private static int snap(
    final int v,
    final int r)
  {
    return (int) (Math.floor((double) v / (double) r) * (double) r);
  }

  private static final class PolygonCanvas extends JPanel
  {
    private final RoomModelOpExecutorType model_executor;
    private final RoomEditingModelType model_editing;
    private Vector2I mouse;
    private Vector2I mouse_snap;
    private EditingOperationType edit_op;

    PolygonCanvas(
      final PublishSubject<String> messages)
    {
      this.mouse = Vectors2I.zero();
      this.mouse_snap = Vectors2I.zero();

      final RoomModelType m = RoomModel.create(
        AreaL.of(-2048L, 2048L, -2048L, 2048L));

      {
        final RoomPolyVertexType v0 =
          m.vertexCreate(Vector2I.of(14 * 32, 3 * 32));
        final RoomPolyVertexType v1 =
          m.vertexCreate(Vector2I.of(5 * 32, 5 * 32));
        final RoomPolyVertexType v2 =
          m.vertexCreate(Vector2I.of(12 * 32, 12 * 32));
        final RoomPolyVertexType v3 =
          m.vertexCreate(Vector2I.of(18 * 32, 7 * 32));
        final RoomPolyVertexType v4 =
          m.vertexCreate(Vector2I.of(20 * 32, 4 * 32));

        final RoomPolygonType p0 = m.polygonCreateV(v0, v1, v2);
        final RoomPolygonType p1 = m.polygonCreateV(v0, v2, v3);
        final RoomPolygonType p2 = m.polygonCreateV(v0, v3, v4);
      }

      {
        final RoomPolyVertexType v0 =
          m.vertexCreate(Vector2I.of(2 * 32, 2 * 32));
        final RoomPolyVertexType v1 =
          m.vertexCreate(Vector2I.of(2 * 32, 4 * 32));
        final RoomPolyVertexType v2 =
          m.vertexCreate(Vector2I.of(4 * 32, 4 * 32));

        final RoomPolygonType p3 = m.polygonCreateV(v0, v1, v2);
      }

      {
        m.check().forEach(e -> LOG.error("{}", e));
      }

      this.model_executor = new RoomModelOpExecutor(m, 32);
      this.model_editing = RoomEditingModel.create(this.model_executor);

      this.addMouseMotionListener(new MouseAdapter()
      {
        @Override
        public void mouseMoved(final MouseEvent e)
        {
          PolygonCanvas.this.onMouseMoved(e.getX(), e.getY());
        }
      });

      this.model_executor.observable().subscribe(e -> this.repaint());
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

      this.paintModel(gg);

      {
        final EditingOperationType op = this.edit_op;
        if (op != null) {
          op.paint(gg);
        }
      }

      gg.setPaint(Color.GREEN);
      gg.fillOval(this.mouse.x() - 4, this.mouse.y() - 4, 8, 8);

      gg.setPaint(Color.RED);
      gg.fillOval(this.mouse_snap.x() - 4, this.mouse_snap.y() - 4, 8, 8);
    }

    private void paintModel(final Graphics2D g)
    {
      final RoomModelReadableType m = this.model_executor.model();

      this.paintQuadTree(g, m.polygonTree());

      for (final RoomPolygonType p : m.polygons()) {
        this.paintPolygon(g, p);
      }
    }

    private void paintQuadTree(
      final Graphics2D g,
      final QuadTreeReadableLType<RoomPolygonType> q)
    {
      final Stroke s = g.getStroke();
      try {
        g.setColor(Color.PINK);
        g.setStroke(dashedStroke());
        q.iterateQuadrants(g, (gg, quadrant, depth) -> {
          final AreaL area = quadrant.area();
          gg.drawRect(
            (int) area.minimumX(),
            (int) area.minimumY(),
            (int) area.sizeX(),
            (int) area.sizeY());
          return TreeVisitResult.RESULT_CONTINUE;
        });
      } finally {
        g.setStroke(s);
      }
    }

    private void paintPolygon(
      final Graphics2D g,
      final RoomPolygonType p)
    {
      final Stroke s = g.getStroke();
      try {
        g.setColor(Color.GRAY);
        g.setStroke(dashedStroke());
        g.drawRect(
          (int) p.bounds().minimumX(),
          (int) p.bounds().minimumY(),
          (int) p.bounds().sizeX(),
          (int) p.bounds().sizeY());
      } finally {
        g.setStroke(s);
      }

      final List<RoomPolyVertexType> vs = p.vertices();
      final List<RoomPolyEdgeType> es = p.edges();

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
        final RoomPolyEdgeType e = es.get(index);

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
        final RoomPolyVertexType v = vs.get(index);
        final Vector2I pos = v.position();
        g.fillOval(pos.x() - 4, pos.y() - 4, 8, 8);
        g.drawString("V" + Long.toString(v.id()), pos.x() - 8, pos.y() - 8);
      }

      final Vector2D center =
        RoomPolygons.barycenter(
          p.vertices()
            .stream()
            .map(RoomPolyVertexType::position)
            .collect(Collectors.toList()));

      g.drawString(
        "P" + Long.toString(p.id()),
        (int) center.x(),
        (int) center.y());
    }

    public void undo()
    {
      this.model_executor.undo();
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

  private interface EditingOperationType extends MouseListener,
    MouseMotionListener
  {
    void paint(Graphics2D g);
  }

  private static final class PolygonCreatorListener extends MouseAdapter implements
    EditingOperationType
  {
    private final PublishSubject<String> messages;
    private final PolygonCanvas canvas;
    private final RoomEditingPolygonCreatorType poly_create;
    private Vector2I mouse_snap;

    PolygonCreatorListener(
      final PolygonCanvas in_canvas,
      final PublishSubject<String> in_messages,
      final RoomEditingModelType in_editing)
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
            final RoomPolygonType pid = this.poly_create.create();
            this.messages.onNext("Created " + pid.id());
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
      LOG.trace("onMouseMoved: {} {}", Integer.valueOf(x), Integer.valueOf(y));
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
            new PolygonCreatorListener(canvas, messages, canvas.model_editing));
        });

      this.setLayout(new FlowLayout(FlowLayout.LEFT));
      this.add(this.create_polygon);
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
      file.add(file_exit);

      final JMenuItem edit_undo = new JMenuItem("Undo");
      edit_undo.setEnabled(false);
      edit_undo.setMnemonic('U');
      edit_undo.setAccelerator(KeyStroke.getKeyStroke(
        (int) 'Z',
        CTRL_DOWN_MASK));
      edit_undo.addActionListener(e -> this.canvas.undo());
      this.canvas.model_executor.observable().subscribe(
        u -> edit_undo.setEnabled(u.isUndoAvailable()));

      final JMenu edit = new JMenu("Edit");
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
          RoomDemo.PolygonWindow.this.canvas.requestFocusInWindow();
        }
      });
    }
  }

  public static void main(final String[] args)
  {
    SwingUtilities.invokeLater(() -> {
      final PolygonWindow window = new PolygonWindow();
      window.pack();
      window.setVisible(true);
      window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    });
  }
}
