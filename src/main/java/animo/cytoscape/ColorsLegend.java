package animo.cytoscape;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JPanel;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.ContinuousMappingPoint;

public class ColorsLegend extends JPanel {
	private static final long serialVersionUID = 9182942528018588493L;
	private float[] fractions = null;
	private Color[] colors = null;

	public ColorsLegend() {

	}

	@Override
	public void paint(Graphics g1) {
		if (fractions == null || colors == null) {
			super.paint(g1);
			return;
		}
		Graphics2D g = (Graphics2D) g1;
		g.setPaint(Color.WHITE);
		g.fill(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight()));
		float width = Math.min(this.getWidth(), 400);
		Rectangle2D.Float rectangle = new Rectangle2D.Float(this.getWidth() / 2 - width / 3, 0, width / 4,
				0.9f * this.getHeight());
		FontMetrics fm = g.getFontMetrics();
		rectangle.y += 10 + fm.getHeight();
		rectangle.height -= 10 + fm.getHeight();
		rectangle.x += 1;
		rectangle.width -= 2;
		rectangle.y += 1;
		rectangle.height -= 2; // Otherwise we can't properly see the contours because they would be drawn on a limit that isn't actually there
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font oldFont = g.getFont();
		g.setFont(oldFont.deriveFont(Font.BOLD));
		g.setPaint(Color.BLACK);
		g.drawString("Activity", this.getWidth() / 2 - fm.stringWidth("Activity") / 2, fm.getHeight());
		g.setFont(oldFont);
		g.setPaint(new LinearGradientPaint(rectangle.x + rectangle.width / 2.0f, rectangle.y, rectangle.x
				+ rectangle.width / 2.0f, rectangle.y + rectangle.height, fractions, colors));
		g.fill(rectangle);
		g.setPaint(Color.BLACK);
		g.draw(rectangle);

		int[] xT = { (int) (rectangle.x + rectangle.width + 10),
				(int) (rectangle.x + rectangle.width + 10 + rectangle.width / 2),
				(int) (rectangle.x + rectangle.width + 10) + 4 };
		int[] yT = { (int) rectangle.y, (int) rectangle.y, (int) (rectangle.y + rectangle.height) };
		g.fill(new Polygon(xT, yT, xT.length));

		g.drawString("Max", xT[1] + 5, yT[0] + fm.getAscent());
		g.drawString("Min", xT[1] + 5, yT[2]);
	}

	public void setParameters(float[] fractions, Color[] colors) {
		this.fractions = fractions.clone();
		this.colors = colors.clone();
		this.repaint();
	}

	public void updateFromSettings() {
		VisualMappingManager vizMap = Animo.getCytoscapeApp().getVisualMappingManager();
		VisualStyle visualStyle = vizMap.getCurrentVisualStyle();
		@SuppressWarnings("rawtypes")
		VisualMappingFunction mappingFunction = visualStyle.getVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
		if (mappingFunction instanceof ContinuousMapping) {
			@SuppressWarnings("unchecked")
			ContinuousMapping<Double, Paint> mapping = (ContinuousMapping<Double, Paint>) mappingFunction;
			List<ContinuousMappingPoint<Double, Paint>> points = mapping.getAllPoints();
			float[] newFractions = new float[points.size()];
			Color[] newColor = new Color[points.size()];

			int i = 0;
			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, intervalSize = 0.0f;
			for (ContinuousMappingPoint<Double, Paint> point : points) {
				float v = (float) point.getValue().doubleValue();
				if (v < min) {
					min = v;
				}
				if (v > max) {
					max = v;
				}
			}
			intervalSize = max - min;
			for (ContinuousMappingPoint<Double, Paint> point : points) {
				// System.err.println("Leggo un punto dal valore di " + (float)point.getValue().doubleValue());
				// fractions[fractions.length - 1 - i] = 1 - (float)point.getValue().doubleValue();
				newFractions[newFractions.length - 1 - i] = 1 - ((float) point.getValue().doubleValue() - min) / intervalSize;
				if (point.getRange().equalValue instanceof Color) {
					newColor[newColor.length - 1 - i] = (Color) point.getRange().equalValue; // Color.getHSBColor(point.getValue().floatValue(), 0, 0);
				} else {
					newColor[newColor.length - 1 - i] = Color.getHSBColor(point.getValue().floatValue(), 0, 0);
				}
				i++;
			}
			this.setParameters(newFractions, newColor);
		}
	}

	/**
	 * This function is probably broken
	 */
	/*public void updateFromSettingsOld() {
		// TODO Most classes used here were deprecated. Hotfixed, to be able to compile.
		VisualMappingManager vizMap = Animo.getCytoscapeApp().getVisualMappingManager();
		VisualStyle visualStyle = vizMap.getDefaultVisualStyle();
		VisualMappingFunction nac = visualStyle.getVisualMappingFunction(null);
		// TODO : Hij wordt nooit gebruikt, maar gaat wel op zijn plaat
		// AbstractVisualMappingFunction avmf = Animo.getMappingFunction();
		Vector<VisualStyle> vector = new Vector<VisualStyle>();

		vector.addAll(vizMap.getAllVisualStyles());

		for (VisualStyle om : vector) {
			if (!(om instanceof ContinuousMapping))
				continue;
			ContinuousMapping<Double, Double> mapping = (ContinuousMapping<Double, Double>) om;
			List<ContinuousMappingPoint<Double, Double>> points = mapping.getAllPoints();
			float[] newFractions = new float[points.size()];
			Color[] newColor = new Color[points.size()];

			int i = 0;
			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, intervalSize = 0.0f;
			for (ContinuousMappingPoint<Double, Double> point : points) {
				float v = (float) point.getValue().doubleValue();
				if (v < min) {
					min = v;
				}
				if (v > max) {
					max = v;
				}
			}
			intervalSize = max - min;
			for (ContinuousMappingPoint<Double, Double> point : points) {
				// System.err.println("Leggo un punto dal valore di " + (float)point.getValue().doubleValue());
				// fractions[fractions.length - 1 - i] = 1 - (float)point.getValue().doubleValue();
				newFractions[newFractions.length - 1 - i] = 1 - ((float) point.getValue().doubleValue() - min)
						/ intervalSize;
				// Wrong color
				// TODO Change to right function
				newColor[newColor.length - 1 - i] = Color.getHSBColor(point.getValue().floatValue(), 0, 0);
				i++;
			}
			this.setParameters(newFractions, newColor);
		}
		this.repaint();
	}*/
}
