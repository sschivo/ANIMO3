package animo.inat.cytoscape;

import java.awt.Color;
import java.awt.Paint;
import java.util.Arrays;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import animo.inat.model.Model;

public class VisualStyleAnimo
{
    private VisualMappingManager vmmServiceRef;
    private VisualStyleFactory visualStyleFactoryServiceRef;
    private VisualMappingFunctionFactory vmfFactoryC;
    private VisualMappingFunctionFactory vmfFactoryD;
    private VisualMappingFunctionFactory vmfFactoryP;
    private CyApplicationManager cyAppManager;
    private VisualStyle vs;
    private CyNetworkView cnv;
    private String name;

    VisualStyleAnimo(VisualMappingManager vmmServiceRef, VisualStyleFactory visualStyleFactoryServiceRef, VisualMappingFunctionFactory vmfFactoryC,
            VisualMappingFunctionFactory vmfFactoryD, VisualMappingFunctionFactory vmfFactoryP, CyApplicationManager cyAppManager)
    {
        this.vmmServiceRef = vmmServiceRef;
        this.visualStyleFactoryServiceRef = visualStyleFactoryServiceRef;
        this.vmfFactoryC = vmfFactoryC;
        this.vmfFactoryD = vmfFactoryD;
        this.vmfFactoryP = vmfFactoryP;
        this.cyAppManager = cyAppManager;

    }


    private void addVisMapEdgesEnabled()
    {
        DiscreteMapping<Boolean, Integer> dm = (DiscreteMapping<Boolean, Integer>) vmfFactoryD.createVisualMappingFunction(Model.Properties.ENABLED,
                Boolean.class, BasicVisualLexicon.EDGE_TRANSPARENCY);
        dm.putMapValue(false, 50);
        dm.putMapValue(true, 255);
        vs.addVisualMappingFunction(dm);
    }

    private void addVisMapEdgesIncrement()
    {
        DiscreteMapping<Integer, ArrowShape> dm = (DiscreteMapping<Integer, ArrowShape>) vmfFactoryD.createVisualMappingFunction(Model.Properties.INCREMENT,
                Integer.class, BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
        dm.putMapValue(1, ArrowShapeVisualProperty.ARROW);
        dm.putMapValue(-1, ArrowShapeVisualProperty.T);
        vs.addVisualMappingFunction(dm);
    }

    private void addVisMapEdgesShownLevel()
    {
        //        VisualMappingFunction vmf = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.EDGE_WIDTH);
        //        if (vmf == null)
        //        {
        ContinuousMapping<Double, Double> cm = (ContinuousMapping<Double, Double>) vmfFactoryC.createVisualMappingFunction(Model.Properties.SHOWN_LEVEL,
                Double.class, BasicVisualLexicon.EDGE_WIDTH);
        BoundaryRangeValues<Double> lb = new BoundaryRangeValues<Double>(2.0, 2.0, 2.0);
        BoundaryRangeValues<Double> ub = new BoundaryRangeValues<Double>(10.0, 10.0, 10.0);
        cm.addPoint(0.0, lb);
        cm.addPoint(1.0, ub);
        VisualMappingFunction vmf = cm;
        vs.addVisualMappingFunction(cm);
        //        }
    }

    private void addVisMapEdgeTooltip()
    {
        //        Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.EDGE_TOOLTIP);
        //        if (o == null)
        //        {
        PassthroughMapping<String, String> pm = (PassthroughMapping<String, String>) vmfFactoryP.createVisualMappingFunction(Model.Properties.DESCRIPTION,
                String.class, BasicVisualLexicon.EDGE_TOOLTIP);
        vs.addVisualMappingFunction(pm);
        //        }
    }

    private VisualMappingFunction addVisMapNodeFillColor()
    {
        //        VisualMappingFunction vmf = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
        //        if (vmf == null)
        //        {
        ContinuousMapping<Double, Paint> cm = (ContinuousMapping<Double, Paint>) vmfFactoryC.createVisualMappingFunction(Model.Properties.SHOWN_LEVEL,
                Double.class, BasicVisualLexicon.NODE_FILL_COLOR);
        Color clb = new Color(204, 0, 0);
        BoundaryRangeValues<Paint> lb = new BoundaryRangeValues<Paint>(clb, clb, clb);
        Color cmb = new Color(255, 204, 0);
        BoundaryRangeValues<Paint> mb = new BoundaryRangeValues<Paint>(cmb, cmb, cmb);
        Color cub = new Color(0, 204, 0);
        BoundaryRangeValues<Paint> ub = new BoundaryRangeValues<Paint>(cub, cub, cub);
        cm.addPoint(0.0, lb);
        cm.addPoint(0.5, mb);
        cm.addPoint(1.0, ub);
        VisualMappingFunction vmf = cm;
        vs.addVisualMappingFunction(cm);
        //        }
        return vmf;
    }

    private void addVisMapNodesCanonicalName()
    {
        PassthroughMapping<String, String> pMapping = (PassthroughMapping<String, String>) vmfFactoryP.createVisualMappingFunction(
                Model.Properties.CANONICAL_NAME, String.class, BasicVisualLexicon.NODE_LABEL);
        vs.addVisualMappingFunction(pMapping);
    }

    private void addVisMapNodesEnabled()
    {
        // NODE_TRANSPARENCY
        DiscreteMapping<Boolean, Integer> dm = (DiscreteMapping<Boolean, Integer>) vmfFactoryD.createVisualMappingFunction(Model.Properties.ENABLED,
                Boolean.class, BasicVisualLexicon.NODE_TRANSPARENCY);
        dm.putMapValue(false, 60);
        dm.putMapValue(true, 255);
        vs.addVisualMappingFunction(dm);

        // NODE_BORDER_TRANSPARENCY
        dm = (DiscreteMapping<Boolean, Integer>) vmfFactoryD.createVisualMappingFunction(Model.Properties.ENABLED, Boolean.class,
                BasicVisualLexicon.NODE_BORDER_TRANSPARENCY);
        dm.putMapValue(false, 60);
        dm.putMapValue(true, 255);
        vs.addVisualMappingFunction(dm);

        // NODE_BORDER_WIDTH
        DiscreteMapping<Boolean, Double> dmd = (DiscreteMapping<Boolean, Double>) vmfFactoryD.createVisualMappingFunction(Model.Properties.ENABLED,
                Boolean.class, BasicVisualLexicon.NODE_BORDER_WIDTH);
        dmd.putMapValue(false, 3.0);
        dmd.putMapValue(true, 6.0);
        vs.addVisualMappingFunction(dmd);
    }

    private DiscreteMapping<String, Double> addVisMapNodesHeight()
    {
        DiscreteMapping<String, Double> dm;
        Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_HEIGHT);
        if (o instanceof DiscreteMapping)
        {
            dm = (DiscreteMapping<String, Double>) o;
            if (dm.getMapValue(Model.Properties.TYPE_CYTOKINE) == null)
                dm.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0);
            if (dm.getMapValue(Model.Properties.TYPE_RECEPTOR) == null)
                dm.putMapValue(Model.Properties.TYPE_RECEPTOR, 65.0);
            if (dm.getMapValue(Model.Properties.TYPE_KINASE) == null)
                dm.putMapValue(Model.Properties.TYPE_KINASE, 55.0);
            if (dm.getMapValue(Model.Properties.TYPE_PHOSPHATASE) == null)
                dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0);
            if (dm.getMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR) == null)
                dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 40.0);
            if (dm.getMapValue(Model.Properties.TYPE_GENE) == null)
                dm.putMapValue(Model.Properties.TYPE_GENE, 50.0);
            if (dm.getMapValue(Model.Properties.TYPE_MRNA) == null)
                dm.putMapValue(Model.Properties.TYPE_MRNA, 50.0);
            if (dm.getMapValue(Model.Properties.TYPE_DUMMY) == null)
                dm.putMapValue(Model.Properties.TYPE_DUMMY, 40.0);
            if (dm.getMapValue(Model.Properties.TYPE_OTHER) == null)
                dm.putMapValue(Model.Properties.TYPE_OTHER, 35.0);
        }
        else
        {
            dm = (DiscreteMapping<String, Double>) vmfFactoryD.createVisualMappingFunction(Model.Properties.MOLECULE_TYPE, String.class,
                    BasicVisualLexicon.NODE_HEIGHT);
            dm.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0);
            dm.putMapValue(Model.Properties.TYPE_RECEPTOR, 65.0);
            dm.putMapValue(Model.Properties.TYPE_KINASE, 55.0);
            dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0);
            dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 40.0);
            dm.putMapValue(Model.Properties.TYPE_GENE, 50.0);
            dm.putMapValue(Model.Properties.TYPE_MRNA, 50.0);
            dm.putMapValue(Model.Properties.TYPE_DUMMY, 40.0);
            dm.putMapValue(Model.Properties.TYPE_OTHER, 35.0);
        }
        vs.addVisualMappingFunction(dm);
        return dm;
    }

    private void addVisMapNodesPlotted()
    {
        DiscreteMapping<Boolean, Paint> dm = (DiscreteMapping<Boolean, Paint>) vmfFactoryD.createVisualMappingFunction(Model.Properties.ENABLED, Boolean.class,
                BasicVisualLexicon.NODE_BORDER_PAINT);
        dm.putMapValue(false, Color.DARK_GRAY);
        dm.putMapValue(true, Color.BLUE);
        vs.addVisualMappingFunction(dm);
    }

    private DiscreteMapping<String, NodeShape> addVisMapNodesType()
    {
        DiscreteMapping<String, NodeShape> dm;
        Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_SHAPE);
        if (o instanceof DiscreteMapping)
        {
            dm = (DiscreteMapping<String, NodeShape>) o;
            if (dm.getMapValue(Model.Properties.TYPE_CYTOKINE) == null)
                dm.putMapValue(Model.Properties.TYPE_CYTOKINE, NodeShapeVisualProperty.RECTANGLE);
            if (dm.getMapValue(Model.Properties.TYPE_RECEPTOR) == null)
                dm.putMapValue(Model.Properties.TYPE_RECEPTOR, NodeShapeVisualProperty.ELLIPSE);
            if (dm.getMapValue(Model.Properties.TYPE_KINASE) == null)
                dm.putMapValue(Model.Properties.TYPE_KINASE, NodeShapeVisualProperty.ELLIPSE);
            if (dm.getMapValue(Model.Properties.TYPE_PHOSPHATASE) == null)
                dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, NodeShapeVisualProperty.DIAMOND);
            if (dm.getMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR) == null)
                dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, NodeShapeVisualProperty.ELLIPSE);
            if (dm.getMapValue(Model.Properties.TYPE_GENE) == null)
                dm.putMapValue(Model.Properties.TYPE_GENE, NodeShapeVisualProperty.TRIANGLE);
            if (dm.getMapValue(Model.Properties.TYPE_MRNA) == null)
                dm.putMapValue(Model.Properties.TYPE_MRNA, NodeShapeVisualProperty.PARALLELOGRAM);
            if (dm.getMapValue(Model.Properties.TYPE_DUMMY) == null)
                dm.putMapValue(Model.Properties.TYPE_DUMMY, NodeShapeVisualProperty.ROUND_RECTANGLE);
            if (dm.getMapValue(Model.Properties.TYPE_OTHER) == null)
                dm.putMapValue(Model.Properties.TYPE_OTHER, NodeShapeVisualProperty.RECTANGLE);
        }
        else
        {
            dm = (DiscreteMapping<String, NodeShape>) vmfFactoryD.createVisualMappingFunction(Model.Properties.MOLECULE_TYPE, String.class,
                    BasicVisualLexicon.NODE_SHAPE);
            dm.putMapValue(Model.Properties.TYPE_CYTOKINE, NodeShapeVisualProperty.RECTANGLE);
            dm.putMapValue(Model.Properties.TYPE_RECEPTOR, NodeShapeVisualProperty.ELLIPSE);
            dm.putMapValue(Model.Properties.TYPE_KINASE, NodeShapeVisualProperty.ELLIPSE);
            dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, NodeShapeVisualProperty.DIAMOND);
            dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, NodeShapeVisualProperty.ELLIPSE);
            dm.putMapValue(Model.Properties.TYPE_GENE, NodeShapeVisualProperty.TRIANGLE);
            dm.putMapValue(Model.Properties.TYPE_MRNA, NodeShapeVisualProperty.PARALLELOGRAM);
            dm.putMapValue(Model.Properties.TYPE_DUMMY, NodeShapeVisualProperty.ROUND_RECTANGLE);
            dm.putMapValue(Model.Properties.TYPE_OTHER, NodeShapeVisualProperty.RECTANGLE);
        }
        vs.addVisualMappingFunction(dm);
        return dm;
    }

    private DiscreteMapping<String, Double> addVisMapNodesWidth()
    {
        DiscreteMapping<String, Double> dm;
        Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_WIDTH);
        if (o instanceof DiscreteMapping)
        {
            dm = (DiscreteMapping<String, Double>) o;
            if (dm.getMapValue(Model.Properties.TYPE_CYTOKINE) == null)
                dm.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0);
            if (dm.getMapValue(Model.Properties.TYPE_RECEPTOR) == null)
                dm.putMapValue(Model.Properties.TYPE_RECEPTOR, 45.0);
            if (dm.getMapValue(Model.Properties.TYPE_KINASE) == null)
                dm.putMapValue(Model.Properties.TYPE_KINASE, 55.0);
            if (dm.getMapValue(Model.Properties.TYPE_PHOSPHATASE) == null)
                dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0);
            if (dm.getMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR) == null)
                dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 60.0);
            if (dm.getMapValue(Model.Properties.TYPE_GENE) == null)
                dm.putMapValue(Model.Properties.TYPE_GENE, 50.0);
            if (dm.getMapValue(Model.Properties.TYPE_MRNA) == null)
                dm.putMapValue(Model.Properties.TYPE_MRNA, 50.0);
            if (dm.getMapValue(Model.Properties.TYPE_DUMMY) == null)
                dm.putMapValue(Model.Properties.TYPE_DUMMY, 60.0);
            if (dm.getMapValue(Model.Properties.TYPE_OTHER) == null)
                dm.putMapValue(Model.Properties.TYPE_OTHER, 60.0);
        }
        else
        {
            dm = (DiscreteMapping<String, Double>) vmfFactoryD.createVisualMappingFunction(Model.Properties.MOLECULE_TYPE, String.class,
                    BasicVisualLexicon.NODE_WIDTH);
            dm.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0);
            dm.putMapValue(Model.Properties.TYPE_RECEPTOR, 45.0);
            dm.putMapValue(Model.Properties.TYPE_KINASE, 55.0);
            dm.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0);
            dm.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 60.0);
            dm.putMapValue(Model.Properties.TYPE_GENE, 50.0);
            dm.putMapValue(Model.Properties.TYPE_MRNA, 50.0);
            dm.putMapValue(Model.Properties.TYPE_DUMMY, 60.0);
            dm.putMapValue(Model.Properties.TYPE_OTHER, 60.0);
        }
        vs.addVisualMappingFunction(dm);
        return dm;
    }

    private void addVisMapNodeTooltip()
    {
        //        Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_TOOLTIP);
        //        if (o == null)
        //        {
        PassthroughMapping<String, String> pm = (PassthroughMapping<String, String>) vmfFactoryP.createVisualMappingFunction(Model.Properties.DESCRIPTION,
                String.class, BasicVisualLexicon.NODE_TOOLTIP);
        vs.addVisualMappingFunction(pm);
        //        }
    }

    public void applyTo(CyNetworkView networkview)
    {
        cnv = networkview;
        name = "VisualStyleANIMO_ " + cnv.getSUID();
        this.vs = vmmServiceRef.getVisualStyle(cnv);
        if (!name.equals(vs.getTitle()))
        {
            this.vs = this.visualStyleFactoryServiceRef.createVisualStyle(name);
        }

        init();
        // Add the new style to the VisualMappingManager
        vmmServiceRef.addVisualStyle(vs);

        // Apply the visual style to a NetwokView
        vs.apply(cnv);
        cnv.updateView();
    }

    private void init()
    {

        addVisMapNodesCanonicalName();
        addVisMapNodesEnabled();
        addVisMapNodesPlotted();
        addVisMapEdgesEnabled();
        addVisMapEdgesIncrement();
        addVisMapEdgesShownLevel();
        DiscreteMapping<String, NodeShape> shapesMap = addVisMapNodesType();
        DiscreteMapping<String, Double> heightMap = addVisMapNodesHeight();
        DiscreteMapping<String, Double> widthMap = addVisMapNodesWidth();
        VisualMappingFunction legendColors = addVisMapNodeFillColor();
        // TODO: update legend colors to be able to handle any vmf instead of the specific ANIMO CM
        // TODO: update legend shapes to be able to handle any vmf instead of the specific ANIMO DM
        addVisMapNodeTooltip();
        addVisMapEdgeTooltip();

        setDefaults();

        // TODO: set dependency node size locked


    }

    private void setDefaults()
    {
        vs.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.BLUE);
        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 14);
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_WIDTH, 6.0);
        vs.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE);
        vs.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 50.0);
        vs.setDefaultValue(BasicVisualLexicon.NODE_WIDTH, 60.0);
        vs.setDefaultValue(BasicVisualLexicon.NODE_HEIGHT, 35.0);
        vs.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 4.0);
        vs.setDefaultValue(BasicVisualLexicon.EDGE_PAINT, Color.BLACK);
        vs.setDefaultValue(BasicVisualLexicon.NODE_SELECTED_PAINT, new Color(102, 102, 255));
        vs.setDefaultValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, new Color(102, 102, 255));
        vs.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.white);

    }

    private void setShapesLegendNameOrder(ShapesLegend sl)
    {
        List<String> orderedNames = Arrays.asList(new String[] { Model.Properties.TYPE_CYTOKINE, Model.Properties.TYPE_RECEPTOR, Model.Properties.TYPE_KINASE,
                Model.Properties.TYPE_PHOSPHATASE, Model.Properties.TYPE_TRANSCRIPTION_FACTOR, Model.Properties.TYPE_GENE, Model.Properties.TYPE_MRNA,
                Model.Properties.TYPE_DUMMY, Model.Properties.TYPE_OTHER });
        sl.setNameOrder(orderedNames);
    }
}
