package animo.cytoscape;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.Arrays;
import java.util.List;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import animo.core.model.Model;

public class VisualStyleAnimo {
	private VisualMappingManager visualMappingManager;
	private VisualStyleFactory visualStyleFactory;
	private VisualMappingFunctionFactory vmFactoryContinuous;
	private VisualMappingFunctionFactory vmFactoryDiscrete;
	private VisualMappingFunctionFactory vmFactoryPassthrough;
	private ColorsLegend colorsLegend;
	private ShapesLegend shapesLegend;
	private VisualStyle currentVisualStyle;
	private CyNetworkView currentNetworkView;
	public static final String ANIMO_NORMAL_VISUAL_STYLE = Animo.APP_NAME + "_VisualStyle",
			ANIMO_DIFF_VISUAL_STYLE = Animo.APP_NAME + "_difference_Visual_Style";

	VisualStyleAnimo(VisualMappingManager vmmServiceRef, VisualStyleFactory visualStyleFactoryServiceRef,
			VisualMappingFunctionFactory vmfFactoryC, VisualMappingFunctionFactory vmfFactoryD,
			VisualMappingFunctionFactory vmfFactoryP, ColorsLegend colorsLegend, ShapesLegend shapesLegend) {
		this.visualMappingManager = vmmServiceRef;
		this.visualStyleFactory = visualStyleFactoryServiceRef;
		this.vmFactoryContinuous = vmfFactoryC;
		this.vmFactoryDiscrete = vmfFactoryD;
		this.vmFactoryPassthrough = vmfFactoryP;
		this.colorsLegend = colorsLegend;
		this.shapesLegend = shapesLegend;
	}

	private void addVisMapEdgesEnabled() {
		DiscreteMapping<Boolean, Integer> dm = (DiscreteMapping<Boolean, Integer>) vmFactoryDiscrete
				.createVisualMappingFunction(Model.Properties.ENABLED, Boolean.class,
						BasicVisualLexicon.EDGE_TRANSPARENCY);
		dm.putMapValue(false, 50);
		dm.putMapValue(true, 255);
		currentVisualStyle.addVisualMappingFunction(dm);
	}

	private void addVisMapEdgesIncrement() {
		DiscreteMapping<Integer, ArrowShape> dm = (DiscreteMapping<Integer, ArrowShape>) vmFactoryDiscrete
				.createVisualMappingFunction(Model.Properties.INCREMENT, Integer.class,
						BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE);
		dm.putMapValue(1, ArrowShapeVisualProperty.ARROW);
		dm.putMapValue(-1, ArrowShapeVisualProperty.T);
		currentVisualStyle.addVisualMappingFunction(dm);
	}

	private void addVisMapEdgesShownLevel() {
		// VisualMappingFunction vmf = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.EDGE_WIDTH);
		// if (vmf == null)
		// {
		ContinuousMapping<Double, Double> cm = (ContinuousMapping<Double, Double>) vmFactoryContinuous
				.createVisualMappingFunction(Model.Properties.SHOWN_LEVEL, Double.class, BasicVisualLexicon.EDGE_WIDTH);
		BoundaryRangeValues<Double> lb = new BoundaryRangeValues<Double>(2.0, 2.0, 2.0);
		BoundaryRangeValues<Double> ub = new BoundaryRangeValues<Double>(10.0, 10.0, 10.0);
		cm.addPoint(0.0, lb);
		cm.addPoint(1.0, ub);
		// VisualMappingFunction<Double, Double> vmf = cm;
		currentVisualStyle.addVisualMappingFunction(cm);
		// }
	}

	private void addVisMapEdgeTooltip() {
		// Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.EDGE_TOOLTIP);
		// if (o == null)
		// {
		PassthroughMapping<String, String> pm = (PassthroughMapping<String, String>) vmFactoryPassthrough
				.createVisualMappingFunction(Model.Properties.DESCRIPTION, String.class,
						BasicVisualLexicon.EDGE_TOOLTIP);
		currentVisualStyle.addVisualMappingFunction(pm);
		// }
	}

	private VisualMappingFunction<Double, Paint> addVisMapNodeFillColor() {
		// VisualMappingFunction vmf = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
		// if (vmf == null)
		// {
		ContinuousMapping<Double, Paint> cm = (ContinuousMapping<Double, Paint>) vmFactoryContinuous
				.createVisualMappingFunction(Model.Properties.SHOWN_LEVEL, Double.class,
						BasicVisualLexicon.NODE_FILL_COLOR);
		Color clb = new Color(204, 0, 0);
		BoundaryRangeValues<Paint> lb = new BoundaryRangeValues<Paint>(clb, clb, clb);
		Color cmb = new Color(255, 204, 0);
		BoundaryRangeValues<Paint> mb = new BoundaryRangeValues<Paint>(cmb, cmb, cmb);
		Color cub = new Color(0, 204, 0);
		BoundaryRangeValues<Paint> ub = new BoundaryRangeValues<Paint>(cub, cub, cub);
		cm.addPoint(0d, lb);
		cm.addPoint(0.5, mb);
		cm.addPoint(1.0, ub);
		VisualMappingFunction<Double, Paint> vmf = cm;
		currentVisualStyle.addVisualMappingFunction(cm);
		// }
		return vmf;
	}

	private void addVisMapNodesCanonicalName() {
		PassthroughMapping<String, String> pMapping = (PassthroughMapping<String, String>) vmFactoryPassthrough
				.createVisualMappingFunction(Model.Properties.CANONICAL_NAME, String.class,
						BasicVisualLexicon.NODE_LABEL);
		currentVisualStyle.addVisualMappingFunction(pMapping);
	}

	private void addVisMapNodesEnabled() {
		// NODE_TRANSPARENCY
		DiscreteMapping<Boolean, Integer> dm = (DiscreteMapping<Boolean, Integer>) vmFactoryDiscrete
				.createVisualMappingFunction(Model.Properties.ENABLED, Boolean.class,
						BasicVisualLexicon.NODE_TRANSPARENCY);
		dm.putMapValue(false, 60);
		dm.putMapValue(true, 255);
		currentVisualStyle.addVisualMappingFunction(dm);

		// NODE_BORDER_TRANSPARENCY
		dm = (DiscreteMapping<Boolean, Integer>) vmFactoryDiscrete.createVisualMappingFunction(
				Model.Properties.ENABLED, Boolean.class, BasicVisualLexicon.NODE_BORDER_TRANSPARENCY);
		dm.putMapValue(false, 0);
		dm.putMapValue(true, 255);
		currentVisualStyle.addVisualMappingFunction(dm);

//		// NODE_BORDER_WIDTH //<<--- Apparently, having both this and node color causes problems. Whatever.
//		DiscreteMapping<Boolean, Double> dmd = 
//				(DiscreteMapping<Boolean, Double>)vmFactoryDiscrete.createVisualMappingFunction(Model.Properties.ENABLED, Boolean.class,
//																								BasicVisualLexicon.NODE_BORDER_WIDTH);
//		dmd.putMapValue(false, 3.0);
//		dmd.putMapValue(true, 6.0);
//		currentVisualStyle.addVisualMappingFunction(dmd);
	}

	@SuppressWarnings("unchecked")
	private DiscreteMapping<String, Double> addVisMapNodesHeight() {
		DiscreteMapping<String, Double> dm;
		Object o = visualMappingManager.getCurrentVisualStyle()
				.getVisualMappingFunction(BasicVisualLexicon.NODE_HEIGHT);
		if (o instanceof DiscreteMapping) {
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
		} else {
			dm = (DiscreteMapping<String, Double>) vmFactoryDiscrete.createVisualMappingFunction(
					Model.Properties.MOLECULE_TYPE, String.class, BasicVisualLexicon.NODE_HEIGHT);
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
		currentVisualStyle.addVisualMappingFunction(dm);
		return dm;
	}

	private void addVisMapNodesPlotted() {
		DiscreteMapping<Boolean, Paint> dm = (DiscreteMapping<Boolean, Paint>) vmFactoryDiscrete
				.createVisualMappingFunction(Model.Properties.PLOTTED, Boolean.class,
						BasicVisualLexicon.NODE_BORDER_PAINT);
		dm.putMapValue(false, Color.DARK_GRAY);
		dm.putMapValue(true, Color.BLUE);
		currentVisualStyle.addVisualMappingFunction(dm);
	}

	@SuppressWarnings("unchecked")
	private DiscreteMapping<String, NodeShape> addVisMapNodesType() {
		DiscreteMapping<String, NodeShape> dm;
		Object o = visualMappingManager.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_SHAPE);
		if (o instanceof DiscreteMapping) {
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
		} else {
			dm = (DiscreteMapping<String, NodeShape>) vmFactoryDiscrete.createVisualMappingFunction(
					Model.Properties.MOLECULE_TYPE, String.class, BasicVisualLexicon.NODE_SHAPE);
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
		currentVisualStyle.addVisualMappingFunction(dm);
		return dm;
	}

	@SuppressWarnings("unchecked")
	private DiscreteMapping<String, Double> addVisMapNodesWidth() {
		DiscreteMapping<String, Double> dm;
		Object o = visualMappingManager.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_WIDTH);
		if (o instanceof DiscreteMapping) {
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
		} else {
			dm = (DiscreteMapping<String, Double>) vmFactoryDiscrete.createVisualMappingFunction(
					Model.Properties.MOLECULE_TYPE, String.class, BasicVisualLexicon.NODE_WIDTH);
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
		currentVisualStyle.addVisualMappingFunction(dm);
		return dm;
	}
	
	@SuppressWarnings("unchecked")
	private DiscreteMapping<String, Double> addVisMapNodeLabelWidth() {
		DiscreteMapping<String, Double> dm;
		Object o = visualMappingManager.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_LABEL_WIDTH);
		if (o instanceof DiscreteMapping) {
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
		} else {
			dm = (DiscreteMapping<String, Double>) vmFactoryDiscrete.createVisualMappingFunction(
					Model.Properties.MOLECULE_TYPE, String.class, BasicVisualLexicon.NODE_LABEL_WIDTH);
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
		currentVisualStyle.addVisualMappingFunction(dm);
		return dm;
	}

	private void addVisMapNodeTooltip() {
		// Object o = vmmServiceRef.getCurrentVisualStyle().getVisualMappingFunction(BasicVisualLexicon.NODE_TOOLTIP);
		// if (o == null)
		// {
		PassthroughMapping<String, String> pm = (PassthroughMapping<String, String>) vmFactoryPassthrough
				.createVisualMappingFunction(Model.Properties.DESCRIPTION, String.class,
						BasicVisualLexicon.NODE_TOOLTIP);
		currentVisualStyle.addVisualMappingFunction(pm);
		// }
	}

	public void applyVisualStyle(String visualStyleName) {
		applyVisualStyleTo(visualStyleName, Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView());
	}

	public void applyVisualStyleTo(String visualStyleName, CyNetworkView networkview) {
		if (!visualStyleName.equals(ANIMO_NORMAL_VISUAL_STYLE) && // Only my styles are accepted
				!visualStyleName.equals(ANIMO_DIFF_VISUAL_STYLE)) {
			return;
		}
		if (networkview == null) { //Cannot apply any style to a null network view..
			return;
		}
		
		CyTable edgeLocalAttrs = networkview.getModel().getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS); //Make sure that the edge attribute "activityRatio" is present: in older versions of ANIMO we destroyed it when it was not needed, but it is better to keep it
		if (edgeLocalAttrs.getColumn(Model.Properties.SHOWN_LEVEL) == null) {
			edgeLocalAttrs.createColumn(Model.Properties.SHOWN_LEVEL, Double.class, false);
			for (CyEdge edge : networkview.getModel().getEdgeList()) {
				edgeLocalAttrs.getRow(edge.getSUID()).set(Model.Properties.SHOWN_LEVEL, 0.25);
			}
		}
		
		this.currentNetworkView = networkview;
		this.currentVisualStyle = visualMappingManager.getVisualStyle(currentNetworkView);
		if (!visualStyleName.equals(currentVisualStyle.getTitle())) {
			boolean found = false;
			for (VisualStyle style : visualMappingManager.getAllVisualStyles()) {
				if (style.getTitle().equals(visualStyleName)) {
					found = true;
					this.currentVisualStyle = style;
					break;
				}
			}
			if (!found) {
				if (visualStyleName.equals(ANIMO_NORMAL_VISUAL_STYLE)) {
					this.currentVisualStyle = this.visualStyleFactory.createVisualStyle(ANIMO_NORMAL_VISUAL_STYLE);
					addMappingsToVisualStyle();
				} else if (visualStyleName.equals(ANIMO_DIFF_VISUAL_STYLE)) {
					found = false; //For safety's sake, we don't assume that the "normal" style is already present, even if it should be so
					VisualStyle normalVisualStyle = null;
					for (VisualStyle style : visualMappingManager.getAllVisualStyles()) {
						if (style.getTitle().equals(ANIMO_NORMAL_VISUAL_STYLE)) {
							found = true;
							normalVisualStyle = style;
							break;
						}
					}
					if (!found) {
						normalVisualStyle = this.visualStyleFactory.createVisualStyle(ANIMO_NORMAL_VISUAL_STYLE);
						addMappingsToVisualStyle();
						visualMappingManager.addVisualStyle(normalVisualStyle);
					}
					this.currentVisualStyle = this.visualStyleFactory.createVisualStyle(normalVisualStyle);
					this.currentVisualStyle.setTitle(ANIMO_DIFF_VISUAL_STYLE);
					addDifferenceMappings();
				} else {
					// We cannot reach this point because at the beginning of the fucntion we explicitly test the value of visualStyleName to be one of the previous two
				}
				// Add the new style to the VisualMappingManager
				visualMappingManager.addVisualStyle(currentVisualStyle);
			}
		}

		// Apply the visual style to a NetwokView
		//currentVisualStyle.apply(currentNetworkView); TODO Ma se non la uso, serve ancora tenere la networkview??
		//currentNetworkView.updateView();
		visualMappingManager.setVisualStyle(currentVisualStyle, currentNetworkView);
		visualMappingManager.setCurrentVisualStyle(currentVisualStyle);
		updateLegends();
	}

	// If the style has been changed, we wan to reflect the change in the legend panel
	public void updateLegends() {
		colorsLegend.updateFromSettings();
		shapesLegend.updateFromSettings();
	}

	private void addMappingsToVisualStyle() {

		addVisMapNodesCanonicalName();
		addVisMapNodesEnabled();
		addVisMapNodesPlotted();
		addVisMapEdgesEnabled();
		addVisMapEdgesIncrement();
		addVisMapEdgesShownLevel();
		// DiscreteMapping<String, NodeShape> shapesMap =
		addVisMapNodesType();
		// DiscreteMapping<String, Double> heightMap =
		addVisMapNodesHeight();
		// DiscreteMapping<String, Double> widthMap =
		addVisMapNodesWidth();
		addVisMapNodeLabelWidth();
		setShapesLegendNameOrder();
		// VisualMappingFunction<Double, Paint> legendColors =
		addVisMapNodeFillColor();
		updateLegends();
		addVisMapNodeTooltip();
		addVisMapEdgeTooltip();

		setDefaults();

	}

	private void setDefaults() {
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.DARK_GRAY);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 6.0);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_FACE, new Font(Font.SANS_SERIF, Font.BOLD, 12));
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 14);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_WIDTH, 60.0);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.RECTANGLE);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 50.0);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_WIDTH, 60.0);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_HEIGHT, 35.0);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 4.0);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_PAINT, Color.BLACK);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, Color.BLACK);
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SELECTED_PAINT, new Color(102, 102, 255));
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_SELECTED_PAINT, new Color(102, 102, 255));
		currentVisualStyle.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.WHITE);
		// Disable "Lock node width and height", so we can set a custom width and height. (unfortunately, this seems to be the "proper" way to do it)
		for (@SuppressWarnings("rawtypes")
		VisualPropertyDependency visualPropertyDependency : currentVisualStyle.getAllVisualPropertyDependencies()) {
			if (visualPropertyDependency.getIdString().equals("nodeSizeLocked")) {
				visualPropertyDependency.setDependency(false);
				break;
			}
		}
	}
	
	//The ANIMO_DIFF_VISUAL_STYLE is based on the normal one, with the node fill color continuous mapping done differently
	private void addDifferenceMappings() {
		ContinuousMapping<Double, Paint> cm = (ContinuousMapping<Double, Paint>) vmFactoryContinuous
				.createVisualMappingFunction(Model.Properties.SHOWN_LEVEL, Double.class,
						BasicVisualLexicon.NODE_FILL_COLOR);
		Color clb = new Color(204, 0, 0);
		BoundaryRangeValues<Paint> lb = new BoundaryRangeValues<Paint>(clb, clb, clb);
		Color cmb = new Color(255, 255, 255);
		BoundaryRangeValues<Paint> mb = new BoundaryRangeValues<Paint>(cmb, cmb, cmb);
		Color cub = new Color(0, 204, 0);
		BoundaryRangeValues<Paint> ub = new BoundaryRangeValues<Paint>(cub, cub, cub);
		cm.addPoint(-1.0, lb);
		cm.addPoint(0.0, mb);
		cm.addPoint(1.0, ub);
		currentVisualStyle.removeVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
		currentVisualStyle.addVisualMappingFunction(cm);
		setDefaults(); //If this style was created as a COPY, why on hell (o Cytoscape of my boots) should I need to call this again??
	}

	private void setShapesLegendNameOrder() {
		List<String> orderedNames = Arrays.asList(new String[] { Model.Properties.TYPE_CYTOKINE,
				Model.Properties.TYPE_RECEPTOR, Model.Properties.TYPE_KINASE, Model.Properties.TYPE_PHOSPHATASE,
				Model.Properties.TYPE_TRANSCRIPTION_FACTOR, Model.Properties.TYPE_GENE, Model.Properties.TYPE_MRNA,
				Model.Properties.TYPE_DUMMY, Model.Properties.TYPE_OTHER });
		shapesLegend.setNameOrder(orderedNames);
	}
}
