package animo.cytoscape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

public class ShapesLegend extends JPanel {
	private static final long serialVersionUID = 8963894565747542198L;
	private DiscreteMapping<String, NodeShape> shapesMap;
	private DiscreteMapping<String, Double> widthsMap, heightsMap;
	private List<String> nameOrder = null;

	public ShapesLegend() {

	}

	public List<String> getNameOrder() {
		return this.nameOrder;
	}

	@Override
	public void paint(Graphics g1) {
		if (shapesMap == null || widthsMap == null || heightsMap == null) {
			super.paint(g1);
			return;
		}
		Graphics2D g = (Graphics2D) g1;
		g.setPaint(Color.WHITE);
		g.fill(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight()));
		float rWidth = Math.min(this.getWidth(), 400);
		Rectangle2D.Float rectangle = new Rectangle2D.Float((this.getWidth() - rWidth / 2.5f) / 2,
				0.1f * this.getHeight(), rWidth / 3, 0.9f * this.getHeight() - 1); // this.getWidth() / 2 - (this.getWidth() / 1.5f), 0, this.getWidth() / 3, this.getHeight());
		FontMetrics fm = g.getFontMetrics();
		rectangle.y += 10 + fm.getHeight();
		rectangle.height -= 10 + fm.getHeight();
		// rectangle.x += 1; rectangle.width -= 2; rectangle.y += 1; rectangle.height -= 2; //Otherwise we can't properly see the contours because they would be drawn on a limit
		// that isn't actually there
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font oldFont = g.getFont();
		g.setFont(oldFont.deriveFont(Font.BOLD));
		g.setPaint(Color.BLACK);
		g.drawString("Protein category", this.getWidth() / 2 - fm.stringWidth("Protein category") / 2,
				0.1f * this.getHeight() + fm.getHeight());
		g.setFont(oldFont);

		Map<String, NodeShape> shapes = shapesMap.getAll();
		Map<String, Double> widths = widthsMap.getAll(),
							heights = heightsMap.getAll();

		float nodeSpace = rectangle.height / shapes.size();

		float maxHeight = -1, maxWidth = -1;
		int maxStrLength = -1;
		for (String moleculeType : heights.keySet()) {
			Object o = heights.get(moleculeType);
			float h = 50.0f;
			if (o instanceof Float) {
				h = (Float) o;
			} else if (o instanceof Double) {
				h = new Float((Double) o);
			}
			if (h > maxHeight) {
				maxHeight = h;
			}
			int strLen = fm.stringWidth(moleculeType);
			if (strLen > maxStrLength) {
				maxStrLength = strLen;
			}
		}
		for (String moleculeType : widths.keySet()) {
			Object o = widths.get(moleculeType);
			float w = 50.0f;
			if (o instanceof Float) {
				w = (Float) o;
			} else if (o instanceof Double) {
				w = new Float((Double) o);
			}
			if (w > maxWidth) {
				maxWidth = w;
			}
		}

		float x = rectangle.x + rectangle.width / 2 - maxStrLength / 2.0f, y = rectangle.y;

		if (nameOrder == null) {
			nameOrder = new ArrayList<String>();
			nameOrder.addAll(shapes.keySet());
		}
		for (String moleculeType : nameOrder) {
			NodeShape shape = shapes.get(moleculeType);

			if (shape == null)
				continue;
			float width = 50.0f, height = 50.0f;
			Object o1 = widths.get(moleculeType);
			if (o1 instanceof Float) {
				width = (Float) o1;
			} else if (o1 instanceof Double) {
				width = new Float((Double) o1);
			}
			Object o2 = heights.get(moleculeType);
			if (o2 instanceof Float) {
				height = (Float) o2;
			} else if (o2 instanceof Double) {
				height = new Float((Double) o2);
			}
			float rate = 0.85f * nodeSpace / maxHeight;
			width *= rate;
			height *= rate;
			g.setStroke(new BasicStroke(2.0f));
			
			int xI = Math.round(x - width / 2),
				yI = Math.round(y + nodeSpace / 2 - height / 2);
			Icon icona = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentRenderingEngine().createIcon(BasicVisualLexicon.NODE_SHAPE, shape, (int)Math.round(width), (int)Math.round(height));
			icona.paintIcon(this, g, xI, yI);
			
			g.drawString(moleculeType, x + (maxWidth + 5) * rate, y + nodeSpace / 2 + fm.getAscent() / 2.0f);// moleculeType, x + rectangle.width /2, y + nodeSpace / 2 +
																												// fm.getAscent() / 2.0f);
			y += nodeSpace;
		}
	}

	public void setNameOrder(List<String> nameOrder) {
		this.nameOrder = nameOrder;
	}

	public void setParameters(DiscreteMapping<String, NodeShape> shapesMap, DiscreteMapping<String, Double> widthsMap, DiscreteMapping<String, Double> heightsMap) {
		this.shapesMap = shapesMap;
		this.widthsMap = widthsMap;
		this.heightsMap = heightsMap;
		this.repaint();
	}
	
	@SuppressWarnings("unchecked")
	public void updateFromSettings() {
		VisualMappingManager vizMap = Animo.getCytoscapeApp().getVisualMappingManager();
		VisualStyle visualStyle = vizMap.getCurrentVisualStyle();
		@SuppressWarnings("rawtypes")
		VisualMappingFunction shapesMap = visualStyle.getVisualMappingFunction(BasicVisualLexicon.NODE_SHAPE),
							  widthsMap = visualStyle.getVisualMappingFunction(BasicVisualLexicon.NODE_WIDTH),
							  heightsMap = visualStyle.getVisualMappingFunction(BasicVisualLexicon.NODE_HEIGHT);
		if (shapesMap != null && shapesMap instanceof DiscreteMapping
			&& widthsMap != null && widthsMap instanceof DiscreteMapping
			&& heightsMap != null && heightsMap instanceof DiscreteMapping) {
			this.setParameters((DiscreteMapping<String, NodeShape>)shapesMap, (DiscreteMapping<String, Double>)widthsMap, (DiscreteMapping<String, Double>)heightsMap);
		}
	}
}
