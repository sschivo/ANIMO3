package animo.inat.cytoscape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

public class ShapesLegend extends JPanel
{
    private static final long serialVersionUID = 8963894565747542198L;
    private DiscreteMapping shapesMap, widthsMap, heightsMap;
    private List<String> nameOrder = null;

    public ShapesLegend()
    {

    }

    public List<String> getNameOrder()
    {
        return this.nameOrder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void paint(Graphics g1)
    {
        if (shapesMap == null || widthsMap == null || heightsMap == null)
        {
            super.paint(g1);
            return;
        }
        Graphics2D g = (Graphics2D) g1;
        g.setPaint(Color.WHITE);
        g.fill(new Rectangle2D.Float(0, 0, this.getWidth(), this.getHeight()));
        float rWidth = Math.min(this.getWidth(), 400);
        Rectangle2D.Float rectangle = new Rectangle2D.Float((this.getWidth() - rWidth / 2.5f) / 2, 0.1f * this.getHeight(), rWidth / 3,
                0.9f * this.getHeight() - 1); //this.getWidth() / 2 - (this.getWidth() / 1.5f), 0, this.getWidth() / 3, this.getHeight());
        FontMetrics fm = g.getFontMetrics();
        rectangle.y += 10 + fm.getHeight();
        rectangle.height -= 10 + fm.getHeight();
        //rectangle.x += 1; rectangle.width -= 2; rectangle.y += 1; rectangle.height -= 2; //Otherwise we can't properly see the contours because they would be drawn on a limit that isn't actually there
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont(Font.BOLD));
        g.setPaint(Color.BLACK);
        g.drawString("Protein category", this.getWidth() / 2 - fm.stringWidth("Protein category") / 2, 0.1f * this.getHeight() + fm.getHeight());
        g.setFont(oldFont);

        Map<String, NodeShape> shapes = shapesMap.getAll();
        Map<String, Object> widths = widthsMap.getAll(), //The maps SHOULD be to Float, but the smart Cytoscape reads the mapping from file as Double, so we must make sure that we work in both cases.
        heights = heightsMap.getAll();

        float nodeSpace = rectangle.height / shapes.size();

        float maxHeight = -1, maxWidth = -1;
        int maxStrLength = -1;
        for (String moleculeType : heights.keySet())
        {
            Object o = heights.get(moleculeType);
            float h = 50.0f;
            if (o instanceof Float)
            {
                h = (Float) o;
            }
            else if (o instanceof Double)
            {
                h = new Float((Double) o);
            }
            if (h > maxHeight)
            {
                maxHeight = h;
            }
            int strLen = fm.stringWidth(moleculeType);
            if (strLen > maxStrLength)
            {
                maxStrLength = strLen;
            }
        }
        for (String moleculeType : widths.keySet())
        {
            Object o = widths.get(moleculeType);
            float w = 50.0f;
            if (o instanceof Float)
            {
                w = (Float) o;
            }
            else if (o instanceof Double)
            {
                w = new Float((Double) o);
            }
            if (w > maxWidth)
            {
                maxWidth = w;
            }
        }

        float x = rectangle.x + rectangle.width / 2 - maxStrLength / 2.0f, y = rectangle.y;

        if (nameOrder == null)
        {
            nameOrder = new ArrayList<String>();
            nameOrder.addAll(shapes.keySet());
        }
        for (String moleculeType : nameOrder)
        {
            NodeShape shape = shapes.get(moleculeType);

            if (shape == null)
                continue;
            float width = 50.0f, height = 50.0f;
            Object o1 = widths.get(moleculeType);
            if (o1 instanceof Float)
            {
                width = (Float) o1;
            }
            else if (o1 instanceof Double)
            {
                width = new Float((Double) o1);
            }
            Object o2 = heights.get(moleculeType);
            if (o2 instanceof Float)
            {
                height = (Float) o2;
            }
            else if (o2 instanceof Double)
            {
                height = new Float((Double) o2);
            }
            float rate = 0.85f * nodeSpace / maxHeight;
            width *= rate;
            height *= rate;
            g.setStroke(new BasicStroke(2.0f));
            if (shape == NodeShapeVisualProperty.DIAMOND)
            {
                int[] xD = new int[] { Math.round(x - width / 2), Math.round(x), Math.round(x + width / 2), Math.round(x) }, yD = new int[] {
                        Math.round(y + nodeSpace / 2), Math.round(y + nodeSpace / 2 - height / 2), Math.round(y + nodeSpace / 2),
                        Math.round(y + nodeSpace / 2 + height / 2) };
                Polygon polygon = new Polygon(xD, yD, xD.length);
                g.setPaint(Color.DARK_GRAY);
                g.fillPolygon(polygon);
                g.setPaint(Color.BLACK);
                g.drawPolygon(polygon);
            }
            else if (shape == NodeShapeVisualProperty.ELLIPSE)
            {

                int xE = Math.round(x - width / 2), yE = Math.round(y + nodeSpace / 2 - height / 2);
                g.setPaint(Color.DARK_GRAY);
                g.fillOval(xE, yE, (int) width, (int) height);
                g.setPaint(Color.BLACK);
                g.drawOval(xE, yE, (int) width, (int) height);
            }
            else if (shape == NodeShapeVisualProperty.PARALLELOGRAM)
            {


                int[] xP = new int[] { Math.round(x - width / 2), (int) Math.round(x + width / 4.0), Math.round(x + width / 2),
                        (int) Math.round(x - width / 4.0) };
                int[] yP = new int[] { Math.round(y + nodeSpace / 2 - height / 2), Math.round(y + nodeSpace / 2 - height / 2),
                        Math.round(y + nodeSpace / 2 + height / 2), Math.round(y + nodeSpace / 2 + height / 2) };
                Polygon parallelogram = new Polygon(xP, yP, xP.length);
                g.setPaint(Color.DARK_GRAY);
                g.fillPolygon(parallelogram);
                g.setPaint(Color.BLACK);
                g.drawPolygon(parallelogram);
            }

            else if (shape == NodeShapeVisualProperty.RECTANGLE)
            {

                int xR = Math.round(x - width / 2);
                int yR = Math.round(y + nodeSpace / 2 - height / 2);
                Rectangle2D.Float rect = new Rectangle2D.Float(xR, yR, width, height);
                g.setPaint(Color.DARK_GRAY);
                g.fill(rect);
                g.setPaint(Color.BLACK);
                g.draw(rect);
            }

            else if (shape == NodeShapeVisualProperty.ROUND_RECTANGLE)
            {
                int xRR = Math.round(x - width / 2);
                int yRR = Math.round(y + nodeSpace / 2 - height / 2);
                RoundRectangle2D.Float rectRound = new RoundRectangle2D.Float(xRR, yRR, width, height, width / 5, height / 5);
                g.setPaint(Color.DARK_GRAY);
                g.fill(rectRound);
                g.setPaint(Color.BLACK);
                g.draw(rectRound);
            }

            if (shape == NodeShapeVisualProperty.TRIANGLE)
            {
                int[] xT = new int[] { Math.round(x), Math.round(x + width / 2), Math.round(x - width / 2) };
                int[] yT = new int[] { Math.round(y + nodeSpace / 2 - height / 2), Math.round(y + nodeSpace / 2 + height / 2),
                        Math.round(y + nodeSpace / 2 + height / 2) };
                Polygon triangle = new Polygon(xT, yT, xT.length);
                g.setPaint(Color.DARK_GRAY);
                g.fill(triangle);
                g.setPaint(Color.BLACK);
                g.draw(triangle);


            }
            g.drawString(moleculeType, x + (maxWidth + 5) * rate, y + nodeSpace / 2 + fm.getAscent() / 2.0f);//moleculeType, x + rectangle.width /2, y + nodeSpace / 2 + fm.getAscent() / 2.0f);
            y += nodeSpace;
        }
    }

    public void setNameOrder(List<String> nameOrder)
    {
        this.nameOrder = nameOrder;
    }

    public void setParameters(DiscreteMapping shapesMap, DiscreteMapping widthsMap, DiscreteMapping heightsMap)
    {
        this.shapesMap = shapesMap;
        this.widthsMap = widthsMap;
        this.heightsMap = heightsMap;
        this.repaint();
    }

    public void updateFromSettings()
    {
        //Again changed to be able to compile, this will probably be broken.
        VisualMappingManager vizMap = Animo.getCytoscapeApp().getVisualMappingManager();
        VisualStyle visualStyle = vizMap.getDefaultVisualStyle();

        DiscreteMapping newShapesMap = null, newWidthsMap = null, newHeightsMap = null;
        List<VisualStyle> mappings = new ArrayList<VisualStyle>();

        mappings.addAll(vizMap.getAllVisualStyles());

        for (VisualStyle om : mappings)
        {
            if (om instanceof DiscreteMapping)
            {
                newShapesMap = (DiscreteMapping) om;
                break;
            }
        }


        for (VisualStyle om : mappings)
        {
            if (om instanceof DiscreteMapping)
            {
                newWidthsMap = (DiscreteMapping) om;
                break;
            }
        }


        for (VisualStyle om : mappings)
        {
            if (om instanceof DiscreteMapping)
            {
                newHeightsMap = (DiscreteMapping) om;
                break;
            }
        }

        if (newShapesMap != null && newWidthsMap != null && newHeightsMap != null)
        {
            this.setParameters(newShapesMap, newWidthsMap, newHeightsMap);
        }

    }
}
