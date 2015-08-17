package animo.cytoscape;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import animo.core.graph.Graph;
import animo.core.graph.P;
import animo.core.graph.Series;

public class ManageGraphSeriesDialog extends JDialog {
	private static final long serialVersionUID = 6630154220142970079L;
	private static final String //SAVE = "Save",
								CANCEL = "Close";
	private Graph graph = null;
	private Vector<Series> graphSeries = null;
	private Map<Component, Series> mapSeriesComponent = null;

	public ManageGraphSeriesDialog(Graph graph, Vector<Series> graphSeries) {
		this(Animo.getCytoscape().getJFrame(), graph, graphSeries);
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param owner The window to which this dialog belongs
	 * @param graphSeries All the series of the graph to be managed
	 */
	public ManageGraphSeriesDialog(final Window owner, final Graph graph, final Vector<Series> graphSeries) {
		super(owner, "Manage graph series", Dialog.ModalityType.APPLICATION_MODAL);
		this.graph = graph;
		this.graphSeries = graphSeries;
		this.mapSeriesComponent = new HashMap<Component, Series>();
		
		JPanel values = makeComponents(graphSeries); //new JPanel(new GridLayout(1, 2, 2, 2));
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		

//		controls.add(new JButton(new AbstractAction(SAVE) {
//			private static final long serialVersionUID = -6920908627164931058L;
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				
//				ManageGraphSeriesDialog.this.dispose();
//			}
//		}));

		
		JButton closeButton = new JButton(new AbstractAction(CANCEL) {
			private static final long serialVersionUID = 3103827646050457714L;

			@Override
			public void actionPerformed(ActionEvent e) {
				ManageGraphSeriesDialog.this.dispose();
			}
		});
		controls.add(closeButton);

		// Associate the "Cancel" button with the Esc key
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", closeButton.getAction());

		this.add(values, BorderLayout.CENTER);
		this.add(controls, BorderLayout.SOUTH);
		
		Dimension dim = this.getPreferredSize();
		dim.setSize(300, dim.getHeight());
		this.setMinimumSize(dim);
		this.pack();
	}

	
	public void showYourself() {
		this.setLocationRelativeTo(graph); //Animo.getCytoscape().getJFrame());
		this.setVisible(true);
	}
	
	
	
	
	
	
	public JPanel makeComponents(Vector<Series> graphSeries) {
		Box box = Box.createVerticalBox();
		DragMouseAdapter dh = new DragMouseAdapter();
		box.addMouseListener(dh);
		box.addMouseMotionListener(dh);

		int idx = 0;
		for (Series s : graphSeries) {
			if (s.isSlave()) continue; //Don't show the "slave" series
			JComponent component = createListItem(idx++, s);
			box.add(component);
			mapSeriesComponent.put(component, s);
		}
		JPanel p = new JPanel(new BorderLayout());
		p.add(box, BorderLayout.NORTH);
		return p;
	}

	protected ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	private static final int TOP_BORDER = 5,
							 LEFT_BORDER = 0,
							 BOTTOM_BORDER = 5,
							 RIGHT_BORDER = 0;
	private final Icon moveIcon = createImageIcon("/drag_reorder16x16.png");
	
	private JComponent createListItem(int i, final Series series) {
		final JLabel l = new JLabel(moveIcon);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
						BorderFactory.createEmptyBorder(TOP_BORDER, LEFT_BORDER, BOTTOM_BORDER, RIGHT_BORDER)
						) 
					);
		p.add(l, BorderLayout.WEST);
		
		Box content = new Box(BoxLayout.X_AXIS);
		final JCheckBox enabled = new JCheckBox();
		enabled.setSelected(series.getEnabled());
		enabled.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				series.setEnabled(enabled.isSelected());
				graph.ensureRedraw();
				graph.repaint();
			}
		});
		final JTextField name = new JTextField(series.getName());
		name.setColumns(name.getText().length());
		name.getDocument().addDocumentListener(new DocumentListener() {
			private void nameChanged() {
				series.setName(name.getText());
				graph.ensureRedraw();
				graph.repaint();
			}
			
			public void changedUpdate(DocumentEvent e) {
			}
			
			public void removeUpdate(DocumentEvent e) {
				nameChanged();
			}
			
			public void insertUpdate(DocumentEvent e) {
				nameChanged();
			}
		});
		final JComboBox<Color> color = new JComboBox<Color>(graph.getAvailableColors());
		color.setRenderer(new ColorCellRenderer());
		color.setSelectedItem(series.getColor());
		color.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color selectedColor = (Color)color.getSelectedItem();
				series.setColor(selectedColor);
				graph.ensureRedraw();
				graph.repaint();
			}
		});
		final JComboBox<Series.BarsState> stdDevType = new JComboBox<Series.BarsState>(Series.BarsState.values());
		final JComboBox<Series.Symbol> stdDevSymbol = new JComboBox<Series.Symbol>(Series.Symbol.values());
		if (series.isMaster()) {
			final Series slave = series.getSlave();
			stdDevType.setSelectedItem(slave.getErrorBars());
			if (slave.getErrorBars() == Series.BarsState.DOTS_WITH_BARS) {
				stdDevSymbol.setEnabled(true);
				stdDevSymbol.setVisible(true);
				ManageGraphSeriesDialog.this.pack();
				stdDevSymbol.setSelectedItem(series.getSymbol()); //Tricky: it's the master series that knows the symbol
			} else {
				stdDevSymbol.setEnabled(false);
				stdDevSymbol.setVisible(false);
				ManageGraphSeriesDialog.this.pack();
			}
			stdDevType.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Series.BarsState selectedState = (Series.BarsState)stdDevType.getSelectedItem();
					slave.setErrorBars(selectedState);
					if (selectedState == Series.BarsState.DOTS_WITH_BARS) {
						stdDevSymbol.setEnabled(true);
						stdDevSymbol.setVisible(true);
						stdDevSymbol.setSelectedItem(series.getSymbol());
						ManageGraphSeriesDialog.this.pack();
					} else {
						stdDevSymbol.setEnabled(false);
						stdDevSymbol.setVisible(false);
						ManageGraphSeriesDialog.this.pack();
					}
					graph.ensureRedraw();
					graph.repaint();
				}
			});
			stdDevSymbol.setForeground(series.getColor());
			color.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Color selectedColor = (Color)color.getSelectedItem();
					stdDevSymbol.setForeground(selectedColor);
					stdDevSymbol.validate();
				}
			});
			stdDevSymbol.setRenderer(new StdDevSymbolCellRenderer(stdDevSymbol));
			stdDevSymbol.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Series.Symbol selectedSymbol = (Series.Symbol)stdDevSymbol.getSelectedItem();
					series.setSymbol(selectedSymbol); //The symbols are actually associated to the master series, not the slave: they are drawn where the data points are
					graph.ensureRedraw();
					graph.repaint();
				}
			});
		}
		content.add(Box.createHorizontalStrut(5));
		content.add(enabled);
		content.add(new LabelledField("Title", name));
		content.add(new LabelledField("Color", color));
		if (series.isMaster()) {
			Box errorBars = new Box(BoxLayout.X_AXIS);
			errorBars.add(stdDevType);
			errorBars.add(stdDevSymbol);
			content.add(new LabelledField("Error bars", errorBars));
		}
		p.add(content, BorderLayout.CENTER);
		p.setOpaque(false);
		return p;
	}
	
	
	
	private class ColorCellRenderer extends JButton implements ListCellRenderer<Color> {
		private static final long serialVersionUID = 1742730085321443168L;
		private boolean blocker=false;
		
		public ColorCellRenderer() {
			setOpaque(true);
		}
		
		@Override
		public void setBackground(Color bg) {
			if(!blocker) {
				return;
			}
			super.setBackground(bg);
		}
		
		@Override
		public Component getListCellRendererComponent(JList<? extends Color> list, Color value, int index, boolean isSelected, boolean cellHasFocus) {
			blocker = true;
			setText("");
			if (isSelected) {
				setBackground((Color)value.brighter());
			} else {
				setBackground((Color)value);
			}
//			this.setBackground((Color)value.brighter());
			blocker = false;
			return this;
		}
	}
	
	private class StdDevSymbolCellRenderer extends JPanel implements ListCellRenderer<Series.Symbol> {
		private static final long serialVersionUID = 1463214102358674317L;
		private Series proxyDrawer = new Series(new P[]{new P(0,0), new P(1,1)}, graph.getScale(), "Fake series") {
			{
				this.setSlave(this); //We do these things to ensure that the series sees itself as a good candidate for actually drawing the symbol
				this.setErrorBars(BarsState.DOTS_WITH_BARS);
			}
		};
		private JComboBox<Series.Symbol> comboBox = null;
		
		public StdDevSymbolCellRenderer(JComboBox<Series.Symbol> comboBox) {
			this.comboBox = comboBox;
			setOpaque(true);
			setPreferredSize(new Dimension(40, 20));
		}
		
		@Override
		public void paint(Graphics g_) {
			Graphics2D g = (Graphics2D)g_;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setPaint(this.getBackground());
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
			g.setPaint(this.getForeground()); //Graph.FOREGROUND_COLOR);
			g.setStroke(new BasicStroke(2.0f));
			proxyDrawer.drawSymbol(g, getWidth()/2, getHeight()/2, getHeight()/2, getHeight()/2); //Draw the symbol centered and as large as half the height of this component (both for width and height)
		}
		
		@Override
		public Component getListCellRendererComponent(JList<? extends Series.Symbol> list, Series.Symbol value, int index, boolean isSelected, boolean cellHasFocus) {
			proxyDrawer.setSymbol(value);
			if (isSelected) {
				setBackground(list.getSelectionBackground());
			} else {
				setBackground(list.getBackground());
			}
			setForeground(comboBox.getForeground()); //I would have guessed that list was actually this comboBox, but it is not (??)
			return this;
		}
	}
	
	
	/**
	 * Manage the reordering of the data series with drag-drop
	 *
	 */
	private class DragMouseAdapter extends MouseAdapter {
//		private final int xoffset = 16;
		private final Rectangle R1 = new Rectangle();
		private final Rectangle R2 = new Rectangle();
		private Rectangle prevRect;
		private final JWindow window = new JWindow();
		private Component draggingComonent;
		private int index = -1;
		private JComponent gap;
		private Point startPt;
		private Point dragOffset;
		private final int gestureMotionThreshold = DragSource.getDragThreshold();

		public DragMouseAdapter() {
			super();
			window.setBackground(new Color(0, true));
		}


		private Cursor oldCursor = null;
		@Override
		public void mouseEntered(MouseEvent ev) {
			Component src = ev.getComponent();
			//if (!(tmpSrc instanceof JLabel)) return;
			//JLabel src = (JLabel)tmpSrc;
			oldCursor = src.getCursor();
			src.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		public void mouseExited(MouseEvent ev) {
			Component src = ev.getComponent();
			//if (!(tmpSrc instanceof JLabel)) return;
			//JLabel src = (JLabel)tmpSrc;
			if (oldCursor != null) {
				src.setCursor(oldCursor);
				oldCursor = null;
			}
		}


		@Override 
		public void mousePressed(MouseEvent e) {
			JComponent parent = (JComponent) e.getComponent();
			if (parent.getComponentCount() <= 1) {
				startPt = null;
				return;
			}
			startPt = e.getPoint();
		}

		private void startDragging(JComponent parent, Point pt) {
			//get a dragging panel
			Component c = parent.getComponentAt(pt);
			index = parent.getComponentZOrder(c);
			if (Objects.equals(c, parent) || index < 0) {
				return;
			}
			draggingComonent = c;
			Dimension d = draggingComonent.getSize();
			d.setSize(d.getWidth() - (LEFT_BORDER + RIGHT_BORDER), d.getHeight() - (TOP_BORDER + BOTTOM_BORDER));

			Point dp = draggingComonent.getLocation();
			dragOffset = new Point(pt.x - dp.x, pt.y - dp.y);

			//make a dummy filler
			gap = new JPanel();
			gap.add(Box.createRigidArea(d));
			gap.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			swapComponentLocation(parent, c, gap, index);

			//make a cursor window
			window.add(draggingComonent);
			window.pack();

			updateWindowLocation(pt, parent);
			window.setVisible(true);
		}

		private void updateWindowLocation(Point pt, JComponent parent) {
			Point p = new Point(pt.x - dragOffset.x, pt.y - dragOffset.y);
			SwingUtilities.convertPointToScreen(p, parent);
			window.setLocation(p);
		}

		private int getTargetIndex(Rectangle r, Point pt, int i) {
			int ht2 = (int)(.5 + r.height * .5);
			R1.setBounds(r.x, r.y,       r.width, ht2);
			R2.setBounds(r.x, r.y + ht2, r.width, ht2);
			if (R1.contains(pt)) {
				prevRect = R1;
				return i - 1 > 0 ? i : 0;
			} else if (R2.contains(pt)) {
				prevRect = R2;
				return i;
			}
			return -1;
		}
		private void swapComponentLocation(
				Container parent, Component remove, Component add, int idx) {
			parent.remove(remove);
			parent.add(add, idx);
			parent.revalidate();
			parent.repaint();
			int indiceSerie = graphSeries.indexOf(mapSeriesComponent.get(add));
			int contaSlave = 0;
			for (int i=0; i<=idx; i++) { //All the "slave" series before idx were not counted in the user interface, as they are not shown. But they are there in the vector, so we should count them.
				if (graphSeries.elementAt(i).isSlave()) {
					contaSlave++;
				}
			}
			idx += contaSlave;
			if (indiceSerie >= 0 && indiceSerie < graphSeries.size() && idx >=0 && idx < graphSeries.size()) {
				//System.err.println("Metto " + graphSeries.elementAt(indiceSerie).getName() + " al posto " + idx + " (dove adesso c'e' " + graphSeries.elementAt(idx).getName() + ")");
				Series serieSmossa = graphSeries.remove(indiceSerie);
				graphSeries.add(idx, serieSmossa);
				graph.ensureRedraw();
				graph.repaint();
			}
		}

		@Override 
		public void mouseDragged(MouseEvent e) {
			Point pt = e.getPoint();
			JComponent parent = (JComponent) e.getComponent();

			//MotionThreshold
			double a = Math.pow(pt.x - startPt.x, 2);
			double b = Math.pow(pt.y - startPt.y, 2);
			if (draggingComonent == null &&
					Math.sqrt(a + b) > gestureMotionThreshold) {
				startDragging(parent, pt);
				return;
			}
			if (!window.isVisible() || draggingComonent == null) {
				return;
			}

			//update the cursor window location
			updateWindowLocation(pt, parent);
			if (prevRect != null && prevRect.contains(pt)) {
				return;
			}

			//change the dummy filler location
			for (int i = 0; i < parent.getComponentCount(); i++) {
				Component c = parent.getComponent(i);
				Rectangle r = c.getBounds();
				if (Objects.equals(c, gap) && r.contains(pt)) {
					return;
				}
				int tgt = getTargetIndex(r, pt, i);
				if (tgt >= 0) {
					swapComponentLocation(parent, gap, gap, tgt);
					return;
				}
			}
			parent.remove(gap);
			parent.revalidate();
		}

		@Override 
		public void mouseReleased(MouseEvent e) {
			startPt = null;
			if (!window.isVisible() || draggingComonent == null) {
				return;
			}
			Point pt = e.getPoint();
			JComponent parent = (JComponent) e.getComponent();

			//close the cursor window
			Component cmp = draggingComonent;
			draggingComonent = null;
			prevRect = null;
			startPt = null;
			dragOffset = null;
			window.setVisible(false);

			//swap the dragging panel and the dummy filler
			for (int i = 0; i < parent.getComponentCount(); i++) {
				Component c = parent.getComponent(i);
				if (Objects.equals(c, gap)) {
					swapComponentLocation(parent, gap, cmp, i);
					return;
				}
				int tgt = getTargetIndex(c.getBounds(), pt, i);
				if (tgt >= 0) {
					swapComponentLocation(parent, gap, cmp, tgt);
					return;
				}
			}
			if (parent.getParent().getBounds().contains(pt)) {
				swapComponentLocation(parent, gap, cmp, parent.getComponentCount());
			} else {
				swapComponentLocation(parent, gap, cmp, index);
			}
		}
	}

}
