package animo.inat.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import animo.inat.analyser.uppaal.ResultAverager;

/**
 * Represents a series of points (x,y) with a title.
 * It draws itself on the graph panel, with the given color.
 * If the series is a "slave" it means that it is "attached"
 * to another series. In particular, the slave series represents
 * the standard deviation of its master with (optional) vertical
 * bars and shading
 */
public class Series implements Comparable<Series>
{
    private enum BarsState
    {
        NOT_SHOWN, ONLY_BARS, ONLY_SHADING, SHADING_WITH_BARS, DOTS_WITH_BARS
    }

    private enum Symbol
    {
        CIRCLE, TRIANGLE_UP, SQUARE, TRIANGLE_DOWN, DIAMOND, X
    }

    protected static int seriesCounter = 0;
    private P[] data = null;
    private String name = "";
    private boolean enabled = true;
    private Scale scale = null;
    private Series master = null, slave = null; //ideally, the slave series should be used to represent confidence intervals for the corresponding master series
    //TODO: also here, we assume that ResultAverager.STD_DEV is all lowercase
    public static String SLAVE_SUFFIX = ResultAverager.STD_DEV, //for a series to be a representation of confidence intervals of series ABC, its name should be "ABC" + SLAVE_SUFFIX (suffix can have any capitalization).
            OVERLAY_SUFFIX = ResultAverager.OVERLAY_NAME; //if a series contains this string, it is part of an overlay graph, so we should make a group of all the series with the same identifier

    protected Color myColor = null;

    private boolean changeColor = false;

    private Symbol[] symbols = new Symbol[] { Symbol.CIRCLE, Symbol.TRIANGLE_UP, Symbol.SQUARE, Symbol.TRIANGLE_DOWN, Symbol.DIAMOND, Symbol.X };
    protected Symbol symbol = Symbol.CIRCLE;
    private BarsState barsState = BarsState.ONLY_BARS; //valid only if this Series is a slave. Tells to show the vertical error bars

    /**
     * x and y identify the center of the symbol
     * @param g
     * @param x
     * @param y
     * @param width
     * @param height
     * @return true if the symbol was drawn, false otherwise
     */
    private Polygon polygon = null;

    private int idxSymbol = 1;

    public Series(P[] data)
    {
        this(data, new Scale());
    }

    public Series(P[] data, Scale scale)
    {
        this(data, scale, "Series " + (++seriesCounter));
    }

    public Series(P[] data, Scale scale, String name)
    {
        this.data = data.clone();
        this.setScale(scale);
        this.name = name;
    }

    public Series(String name, Scale scale)
    {
        this.data = null;
        this.name = name;
        this.scale = scale;
    }

    public void changeErrorBars()
    {
        BarsState[] states = BarsState.values();
        int idx = 0;
        for (int i = 0; i < states.length; i++)
        {
            if (this.barsState.equals(states[i]))
            {
                idx = i;
            }
        }
        idx++;
        if (idx >= states.length)
            idx = 0;
        this.barsState = states[idx];
    }

    public void changeSymbol()
    {
        if (this.slave != null && this.slave.barsState == BarsState.DOTS_WITH_BARS)
        {
            this.symbol = symbols[idxSymbol];
            idxSymbol++;
            if (idxSymbol > symbols.length - 1)
            {
                idxSymbol = 0;
            }
            //System.err.println("Nuovo simbolo: " + this.symbol);
        }
    }

    @Override
    public int compareTo(Series s)
    {
        return this.name.toLowerCase().compareTo(s.name.toLowerCase());
    }

    public boolean drawSymbol(Graphics2D g, int x, int y, int width, int height)
    {
        if (this.slave != null && this.slave.barsState == BarsState.DOTS_WITH_BARS)
        {
            switch (symbol)
            {
            case CIRCLE:
                g.fillOval((int) (x - width / 2.0), (int) (y - height / 2.0), width, height);
                return true;
            case TRIANGLE_UP:
                width = (int) (width * 1.2);
                height = (int) (height * 1.2);
                int ydown = y + (int) Math.round(Math.tan(Math.PI / 6) * Math.tan(Math.PI / 6) * height),
                yup = ydown - height;
                int[] xTU = new int[] { (int) (x - width / 2.0), x, (int) (x + width / 2.0) };
                int[] yTU = new int[] { ydown, yup, ydown }; //(int)(y + height / 2.0), (int)(y - height / 2.0), (int)(y + height / 2.0)};
                polygon = new Polygon(xTU, yTU, xTU.length);
                g.fillPolygon(polygon);
                return true;
            case SQUARE:
                g.fillRect((int) (x - width / 2.0), (int) (y - height / 2.0), width, height);
                return true;
            case TRIANGLE_DOWN:
                width = (int) (width * 1.2);
                height = (int) (height * 1.2);
                int yu = y - (int) Math.round(Math.tan(Math.PI / 6) * Math.tan(Math.PI / 6) * height),
                yd = yu + height;
                int[] xTD = new int[] { (int) (x - width / 2.0), (int) (x + width / 2.0), x };
                int[] yTD = new int[] { yu, yu, yd }; //(int)(y - height / 2.0), (int)(y - height / 2.0), (int)(y + height / 2.0)};
                polygon = new Polygon(xTD, yTD, xTD.length);
                g.fillPolygon(polygon);
                return true;
            case DIAMOND:
                width = (int) (width * 1.5);
                height = (int) (height * 1.5);
                int[] xD = new int[] { (int) (x - width / 2.0), x, (int) (x + width / 2.0), x };
                int[] yD = new int[] { y, (int) (y - height / 2.0), y, (int) (y + height / 2.0) };
                polygon = new Polygon(xD, yD, xD.length);
                g.fillPolygon(polygon);
                return true;
            case X:
                width = (int) (width * 0.8);
                height = (int) (height * 0.8);
                g.drawLine((int) (x - width / 2.0), (int) (y - height / 2.0), (int) (x + width / 2.0), (int) (y + height / 2.0));
                g.drawLine((int) (x + width / 2.0), (int) (y - height / 2.0), (int) (x - width / 2.0), (int) (y + height / 2.0));
                return true;
            default:
                break;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Series s = (Series) obj;
        return name.equals(s.name);
    }

    public boolean getChangeColor()
    {
        return this.changeColor;
    }

    public Color getColor()
    {
        return this.myColor;
    }

    public P[] getData()
    {
        return this.data;
    }

    public boolean getEnabled()
    {
        return this.enabled;
    }

    public BarsState getErrorBars()
    {
        return this.barsState;
    }

    public Series getMaster()
    {
        return this.master;
    }

    public String getName()
    {
        return this.name;
    }

    public Scale getScale()
    {
        return this.scale;
    }

    public Series getSlave()
    {
        return this.slave;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    public boolean isMaster()
    {
        return this.slave != null;
    }

    public boolean isSlave()
    {
        return this.master != null;
    }

    public boolean isStdDev()
    {
        return isSlave() && !(this.master instanceof OverlaySeries);
    }

    public void plot(Graphics2D graphic, Rectangle bounds, boolean stepShaped, int scala)
    {
        scale.computeScale(bounds);
        double scaleX = scale.getXScale(), scaleY = scale.getYScale(), minX = scale.getMinX(), minY = scale.getMinY();
        if (!enabled)
            return;

        if (master != null)
        {
            myColor = master.myColor;
        }
        else
        {
            myColor = graphic.getColor();
            if (slave != null)
            {
                slave.myColor = myColor;
            }

        }
        if (isStdDev())
        {
            P[] masterData = master.getData();
            P vecchio = null;
            int index = 0;
            Color color = graphic.getColor();
            if (barsState.equals(BarsState.ONLY_SHADING) || barsState.equals(BarsState.SHADING_WITH_BARS))
            { //Draw standard deviation as shading
                for (P punto : data)
                {
                    if (punto.y < 1e-7 || punto.x < minX)
                    {
                        vecchio = punto;
                        continue;
                    }
                    if (index < masterData.length)
                    {
                        if (index > 0)
                        {
                            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                            Color c1 = Color.getHSBColor(hsb[0], hsb[1] / 3, hsb[2]);
                            //c2 = Color.getHSBColor(hsb[0], hsb[1]*3/2, hsb[2]);
                            float[] rgb = c1.getRGBComponents(null);
                            Color c3 = new Color(rgb[0], rgb[1], rgb[2], 0.5f);
                            rgb = color.getRGBColorComponents(null);
                            Color c4 = new Color(rgb[0], rgb[1], rgb[2], 0.6f);
                            /*g.setColor(Color.getHSBColor(hsb[0], hsb[1]/4, hsb[2]));
                            g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)),
                                       (int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
                            g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + vecchio.y - minY)),
                                       (int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)));*/
                            /*Polygon grayedError = new Polygon();
                            grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)));
                            grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
                            grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY)));
                            grayedError.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + vecchio.y - minY)));
                            g.setPaint(new GradientPaint(new Point2D.Float((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY))), c3, new Point2D.Float((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y + punto.y - minY))), c4));
                            g.fill(grayedError);*/
                            /*Polygon error1 = new Polygon();
                            error1.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - minY)));
                            error1.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - minY)));
                            error1.addPoint((int)(bounds.x + scaleX * (masterData[i].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i].y - punto.y - minY)));
                            error1.addPoint((int)(bounds.x + scaleX * (masterData[i-1].x - minX)), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y - minY)));
                            g.drawLine((int)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + masterData[i].y) / 2.0 - minY), (int)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (int)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y + masterData[i].y - punto.y) / 2.0 - minY));
                            g.setPaint(new GradientPaint((float)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y + masterData[i].y) / 2.0 - minY), c, (float)(bounds.x + scaleX * (masterData[i-1].x + masterData[i].x) / 2.0 - minX), (float)(bounds.y + bounds.height - scaleY * (masterData[i-1].y - vecchio.y + masterData[i].y - punto.y) / 2.0 - minY), c4));
                            g.fill(error1);*/

                            //I would like to make it simpler, so that it does not negatively influence performances, but I have no time..
                            double maxY = Math.max(vecchio.y, punto.y);
                            Point2D.Float a = new Point2D.Float((float) (bounds.x + scaleX * (masterData[index - 1].x - minX)), (float) (bounds.y
                                    + bounds.height - scaleY * (masterData[index - 1].y + maxY - minY))), b = new Point2D.Float((float) (bounds.x + scaleX
                                    * (masterData[index].x - minX)), (float) (bounds.y + bounds.height - scaleY * (masterData[index].y + maxY - minY))), c = new Point2D.Float(
                                    (float) (bounds.x + scaleX * (masterData[index].x - minX)), (float) (bounds.y + bounds.height - scaleY
                                            * (masterData[index].y - maxY - minY))), d = new Point2D.Float((float) (bounds.x + scaleX
                                    * (masterData[index - 1].x - minX)), (float) (bounds.y + bounds.height - scaleY * (masterData[index - 1].y - maxY - minY))), e = new Point2D.Float(
                                    (float) (bounds.x + scaleX * (masterData[index - 1].x - minX)), (float) (bounds.y + bounds.height - scaleY
                                            * (masterData[index - 1].y - minY))), f = new Point2D.Float((float) (bounds.x + scaleX
                                    * (masterData[index].x - minX)), (float) (bounds.y + bounds.height - scaleY * (masterData[index].y - minY))), i = new Point2D.Float(
                                    (e.x + f.x) / 2.0f, (e.y + f.y) / 2.0f);
                            float gx, gy;
                            if (a.y != b.y)
                            {
                                gx = (a.x * (b.y - a.y) / (b.x - a.x) - a.y + i.x * (b.x - a.x) / (b.y - a.y) + i.y)
                                        / ((b.y - a.y) / (b.x - a.x) + (b.x - a.x) / (b.y - a.y));
                                gy = (b.y - a.y) / (b.x - a.x) * (gx - a.x) + a.y;
                            }
                            else
                            {
                                gx = (a.x + b.x) / 2.0f;
                                gy = a.y;
                            }
                            Point2D.Float g = new Point2D.Float(gx, gy);
                            float hx, hy;
                            if (c.y != d.y)
                            {
                                hx = (d.x * (c.y - d.y) / (c.x - d.x) - d.y + i.x * (c.x - d.x) / (c.y - d.y) + i.y)
                                        / ((c.y - d.y) / (c.x - d.x) + (c.x - d.x) / (c.y - d.y));
                                hy = (c.y - d.y) / (c.x - d.x) * (gx - d.x) + d.y;
                            }
                            else
                            {
                                hx = (c.x + d.x) / 2.0f;
                                hy = c.y;
                            }
                            Point2D.Float h = new Point2D.Float(hx, hy);
                            Polygon error1 = new Polygon();
                            error1.addPoint((int) e.x, (int) e.y);
                            error1.addPoint((int) f.x, (int) f.y);
                            error1.addPoint((int) b.x, (int) (bounds.y + bounds.height - scaleY * (masterData[index].y + punto.y - minY)));
                            error1.addPoint((int) a.x, (int) (bounds.y + bounds.height - scaleY * (masterData[index - 1].y + vecchio.y - minY)));
                            //g.drawLine((int)I.x, (int)I.y, (int)G.x, (int)G.y);
                            //g.drawLine((int)A.x, (int)A.y, (int)B.x, (int)B.y);
                            graphic.setPaint(new GradientPaint(i, c4, g, c3));
                            graphic.fill(error1);
                            Polygon error2 = new Polygon();
                            error2.addPoint((int) e.x, (int) e.y);
                            error2.addPoint((int) f.x, (int) f.y);
                            error2.addPoint((int) c.x, (int) (bounds.y + bounds.height - scaleY * (masterData[index].y - punto.y - minY)));
                            error2.addPoint((int) d.x, (int) (bounds.y + bounds.height - scaleY * (masterData[index - 1].y - vecchio.y - minY)));
                            graphic.setPaint(new GradientPaint(i, c4, h, c3));
                            graphic.fill(error2);
                        }
                    }
                    vecchio = punto;
                }
            }
            graphic.setColor(color);
            index = 0;
            P lastBar = null;
            for (P punto : data)
            {
                if (punto.x < minX)
                {
                    vecchio = punto;
                    continue;
                }
                if (index < masterData.length)
                {
                    if (barsState.equals(BarsState.ONLY_BARS) || barsState.equals(BarsState.SHADING_WITH_BARS) || barsState.equals(BarsState.DOTS_WITH_BARS))
                    { //Draw standard deviation as error bars
                      //these lines draw the vertical error bars, but if we have a lot of points the thing becomes extremely clumsy
                        if (lastBar == null || scaleX * (punto.x - lastBar.x) > 10)
                        {
                            graphic.drawLine((int) (bounds.x + scaleX * (masterData[index].x - minX)), (int) (bounds.y + bounds.height - scaleY
                                    * (masterData[index].y - punto.y - minY)), (int) (bounds.x + scaleX * (masterData[index].x - minX)), (int) (bounds.y
                                    + bounds.height - scaleY * (masterData[index].y + punto.y - minY)));
                            graphic.drawLine((int) (bounds.x + scaleX * (masterData[index].x - minX)) - 3 * scala, (int) (bounds.y + bounds.height - scaleY
                                    * (masterData[index].y - punto.y - minY)), (int) (bounds.x + scaleX * (masterData[index].x - minX)) + 3 * scala,
                                    (int) (bounds.y + bounds.height - scaleY * (masterData[index].y - punto.y - minY)));
                            graphic.drawLine((int) (bounds.x + scaleX * (masterData[index].x - minX)) - 3 * scala, (int) (bounds.y + bounds.height - scaleY
                                    * (masterData[index].y + punto.y - minY)), (int) (bounds.x + scaleX * (masterData[index].x - minX)) + 3 * scala,
                                    (int) (bounds.y + bounds.height - scaleY * (masterData[index].y + punto.y - minY)));
                            lastBar = punto;
                        }
                    }
                    if (index > 0 && (barsState.equals(BarsState.ONLY_SHADING) || barsState.equals(BarsState.SHADING_WITH_BARS)))
                    { //If there was shading, we need to redraw the "master" line, which can have been partially overdrawn
                        graphic.drawLine((int) (bounds.x + scaleX * (masterData[index - 1].x - minX)), (int) (bounds.y + bounds.height - scaleY
                                * (masterData[index - 1].y - minY)), (int) (bounds.x + scaleX * (masterData[index].x - minX)),
                                (int) (bounds.y + bounds.height - scaleY * (masterData[index].y - minY)));
                    }
                }
                vecchio = punto;
            }
        }
        else
        {
            P vecchio = data[0];
            for (int j = 1; j < data.length; j++)
            {
                P punto = data[j];
                if (punto.x < minX)
                {
                    vecchio = punto;
                    continue;
                }
                if (slave != null && slave.barsState == BarsState.DOTS_WITH_BARS)
                {
                    int w, h;
                    w = h = 4 * (int) (((BasicStroke) graphic.getStroke()).getLineWidth());
                    drawSymbol(graphic, (int) (bounds.x + scaleX * (punto.x - minX)), (int) (bounds.y + bounds.height - scaleY * (punto.y - minY)), w, h);
                }
                else
                {
                    if (stepShaped)
                    {
                        graphic.drawLine((int) (bounds.x + scaleX * (vecchio.x - minX)), (int) (bounds.y + bounds.height - scaleY * (vecchio.y - minY)),
                                (int) (bounds.x + scaleX * (punto.x - minX)), (int) (bounds.y + bounds.height - scaleY * (vecchio.y - minY)));
                        graphic.drawLine((int) (bounds.x + scaleX * (punto.x - minX)), (int) (bounds.y + bounds.height - scaleY * (vecchio.y - minY)),
                                (int) (bounds.x + scaleX * (punto.x - minX)), (int) (bounds.y + bounds.height - scaleY * (punto.y - minY)));
                    }
                    else
                    {
                        graphic.drawLine((int) (bounds.x + scaleX * (vecchio.x - minX)), (int) (bounds.y + bounds.height - scaleY * (vecchio.y - minY)),
                                (int) (bounds.x + scaleX * (punto.x - minX)), (int) (bounds.y + bounds.height - scaleY * (punto.y - minY)));
                    }
                }
                vecchio = punto;
            }
        }
    }

    public void setChangeColor(boolean changeColor)
    {
        this.changeColor = changeColor;
    }

    //wether to show this series or not
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (slave != null)
        {
            this.slave.setEnabled(enabled);
        }
    }

    public void setMaster(Series s)
    {
        this.setMaster(s, true);
    }

    private void setMaster(Series s, boolean propagate)
    {
        this.master = s;
        this.setScale(this.master.getScale());
        if (propagate)
        {
            s.setSlave(this, false);
        }
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setScale(Scale scale)
    {
        this.scale = scale;
        if (!isStdDev())
        {
            this.scale.addData(data);
        }
        else
        {
            P[] dataLow = new P[data.length];
            P[] dataHigh = new P[data.length];
            for (int i = 0; i < data.length; i++)
            {
                dataLow[i] = new P(data[i].x, master.data[i].y - data[i].y);
                dataHigh[i] = new P(data[i].x, master.data[i].y + data[i].y);
            }
            this.scale.addData(dataLow);
            this.scale.addData(dataHigh);
        }
    }

    public void setSlave(Series s)
    {
        this.setSlave(s, true);
    }

    private void setSlave(Series s, boolean propagate)
    {
        this.slave = s;
        if (propagate)
        {
            s.setMaster(this, false);
        }
    }
}