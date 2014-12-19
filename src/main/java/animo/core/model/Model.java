package animo.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.work.TaskMonitor;

import animo.core.AnimoBackend;
import animo.core.analyser.uppaal.VariablesModel;
import animo.cytoscape.Animo;
import animo.cytoscape.EdgeDialog;
import animo.exceptions.AnimoException;
import animo.fitting.ScenarioCfg;
import animo.util.Table;
import animo.util.XmlConfiguration;

/**
 * A model. This model keeps itself consistent, as long as both {@link Reactant} and {@link Reaction} implementations keep their {@link #equals(Object)} method based on identity
 * the model is automatically consistent.
 * 
 * @author B. Wanders
 */
public class Model implements Serializable {
	public static class Properties {
		/**
		 * Property that can belong to a node or to a network. If related to a single node, it represents the maximum number of levels for that single reactant. If related to a
		 * complete network, it is the maximum value of the NUMBER_OF_LEVELS property among all nodes in the network. Expressed as integer number in [0, 100] (chosen by the user).
		 */
		public static final String NUMBER_OF_LEVELS = "levels";
		/**
		 * Property belonging to a node. The initial activity level for a node. Expressed as an integer number in [0, NUMBER_OF_LEVELS for that node]
		 */
		public static final String INITIAL_LEVEL = "initialConcentration";
		/**
		 * Property belonging to a node. The current activity level of a node. Expressed as a relative number representing INITIAL_LEVEL / NUMBER_OF_LEVELS, so it is a double
		 * number in [0, 1]
		 */
		public static final String SHOWN_LEVEL = "activityRatio";
		/**
		 * Property belonging to a network. The number of real-life seconds represented by a single UPPAAL time unit.
		 */
		public static final String SECONDS_PER_POINT = "seconds per point";
		/**
		 * This value is multiplied to the time bounds as a counterbalance to any change in seconds per point. This allows us to avoid having to directly modify the parameters of
		 * scenarios.
		 */
		public static final String SECS_POINT_SCALE_FACTOR = "time scale factor";
		/**
		 * Also this value is multiplied to the time bounds, and it counterbalances the changes in the number of levels for the reactants. It is specific for every reaction.
		 */
		public static final String LEVELS_SCALE_FACTOR = "levels scale factor";
		/**
		 * Property belonging to an edge. The id of the scenario on which the reaction corresponding to the edge computes its time tables.
		 */
		public static final String SCENARIO = "scenario";
		/**
		 * The property used to indicate the user-chosen name of a node
		 */
		public static final String ALIAS = "alias";
		/**
		 * The same, but in the Cytoscape model instead of the Model
		 */
		public static final String CANONICAL_NAME = "canonicalName";
		/**
		 * The verbose description of a node/edge, possibly provided with references to papers and/or various IDs
		 */
		public static final String DESCRIPTION = "description";
		/**
		 * Tells us whether a node/edge is enabled
		 */
		public static final String ENABLED = "enabled";
		/**
		 * Tells us whether to plot a node or not
		 */
		public static final String PLOTTED = "plotted";
		/**
		 * A group of nodes identifies alternative phosphorylation sites (can be useless)
		 */
		public static final String GROUP = "group";
		/**
		 * Upper time bound
		 */
		public static final String TIMES_UPPER = "timesU";
		/**
		 * Time bound (no upper nor lower: it is possible that it is never be used in practice)
		 */
		public static final String TIMES = "times";
		/**
		 * Lower time bound
		 */
		public static final String TIMES_LOWER = "timesL";
		/**
		 * The minimum amount of time a reaction can take
		 */
		public static final String MINIMUM_DURATION = "minTime";
		/**
		 * The maximum amount of time a reaction can take (not infinite)
		 */
		public static final String MAXIMUM_DURATION = "maxTime";
		/**
		 * Increment in substrate as effect of the reaction (+1, -1, etc)
		 */
		public static final String INCREMENT = "increment";
		/**
		 * Reaction between two reactants (substrate/reactant and catalyst)
		 */
		public static final String BI_REACTION = "reaction2";
		/**
		 * Reaction with only one reactant
		 */
		public static final String MONO_REACTION = "reaction1";
		/**
		 * The percentage of uncertainty about the reaction parameter settings
		 */
		public static final String UNCERTAINTY = "uncertainty";
		/**
		 * The reactant for a mono-reaction or the substrate for a bi-reaction
		 */
		public static final String REACTANT = "reactant";
		/**
		 * (prefix for a parameter of a reaction) The Cytoscape ID for a reactant. The actual name of this property is for example _REACTANT_S, _REACTANT_E, _REACTANT_E1,
		 * _REACTANT_E2
		 */
		public static final String REACTANT_ID = "_REACTANT_";
		public static final String REACTANT_ID_E1 = REACTANT_ID + "E1",
								   REACTANT_ID_E2 = REACTANT_ID + "E2";
		/**
		 * (prefix for a parameter of a reaction) tells whether the input for a reaction considers the active or inactive part of the given reactant. The actual name of this
		 * property is for example _REACTANT_ACT_S, _REACTANT_ACT_E, _REACTANT_ACT_E1, _REACTANT_ACT_E2
		 */
		public static final String REACTANT_IS_ACTIVE_INPUT = "_REACTANT_ACT_";
		public static final String REACTANT_IS_ACTIVE_INPUT_E1 = REACTANT_IS_ACTIVE_INPUT + "E1",
								   REACTANT_IS_ACTIVE_INPUT_E2 = REACTANT_IS_ACTIVE_INPUT + "E2";
		/**
		 * whether the reactant known as r1 is a downstream reactant (i.e., its activity influences the reaction, but the reaction influences its activity too)
		 */
		public static final String R1_IS_DOWNSTREAM = "r1IsDownstream";
		/**
		 * see R1_IS_DOWNSTREAM
		 */
		public static final String R2_IS_DOWNSTREAM = "r2IsDownstream";
		/**
		 * The ID of the output reactant of a reaction
		 */
		public static final String OUTPUT_REACTANT = "output reactant";
		/**
		 * The ID assigned to the node/edge by Cytoscape
		 */
		public static final String CYTOSCAPE_ID = "cytoscape id";
		/**
		 * The type of the reactant (kinase, phosphatase, receptor, cytokine, ...)
		 */
		public static final String MOLECULE_TYPE = "moleculeType";
		/**
		 * The following TYPE_* keys are the possible values for the property MOLECULE_TYPE
		 */
		public static final String TYPE_CYTOKINE = "Cytokine";
		public static final String TYPE_RECEPTOR = "Receptor";
		public static final String TYPE_KINASE = "Kinase";
		public static final String TYPE_PHOSPHATASE = "Phosphatase";
		public static final String TYPE_TRANSCRIPTION_FACTOR = "Transcription factor";
		public static final String TYPE_OTHER = "Other";
		public static final String TYPE_MRNA = "mRNA";
		public static final String TYPE_GENE = "Gene";
		public static final String TYPE_DUMMY = "Dummy";
		/**
		 * The name of the reactant (possibly outdated property name)
		 */
		public static final String REACTANT_NAME = "name";
		/**
		 * Type of reaction (mono, bi)
		 */
		public static final String REACTION_TYPE = "type";
		/**
		 * The current scenario configuration (scenario index + parameter values) of the reaction
		 */
		public static final String SCENARIO_CFG = "SCENARIO_CFG";
		/**
		 * Catalyst in a bi-reaction
		 */
		public static final String CATALYST = "catalyst";
		/**
		 * The index of the reactant (sometimes we need to assign them an index)
		 */
		public static final String REACTANT_INDEX = "index";
		/**
		 * The following are all scenario parameters
		 */
		public static final String SCENARIO_PARAMETER_KM = "km";
		public static final String SCENARIO_PARAMETER_K2 = "k2";
		public static final String SCENARIO_PARAMETER_STOT = "Stot";
		public static final String SCENARIO_PARAMETER_K2_KM = "k2/km";
		public static final String SCENARIO_ONLY_PARAMETER = "parameter";
		public static final String SCENARIO_PARAMETER_K = "k";
		public static final String MODEL_CHECKING_TYPE = "model checking type";

		public static final int STATISTICAL_MODEL_CHECKING = 1;
		public static final int NORMAL_MODEL_CHECKING = 2;
	}

	private static final long serialVersionUID = 9078409933212069999L;
	/**
	 * This is NOT a random number: it is the maximum number that can currently be input to UPPAAL
	 */
	private static final int VERY_LARGE_TIME_VALUE = 1073741822;

	/**
	 * Check that all parameters are ok. If possible, ask the user to input parameters on the fly. If this is not possible, throw an exception specifying what parameters are
	 * missing.
	 */
	private static void checkParameters(Integer nMinutesToSimulate) throws AnimoException {

		/*
		 * get the property tables
		 */
		CyApplicationManager appmanager = Animo.getCytoscapeApp().getCyApplicationManager();
		CyNetwork currentNetwork = appmanager.getCurrentNetwork();
		//CyRootNetworkManager rootNetworkManager = Animo.getCytoscapeApp().getCyRootNetworkManager();
		//CyNetwork rootNetwork = rootNetworkManager.getRootNetwork(currentNetwork); //.getBaseNetwork();
		//network = currentNetwork; //Non dovrebbe aver senso controllare i parametri per la root network. E comunque, la default table e' quella condivisa, quindi quando modifico le proprieta' di nodi/edgi, si dovrebbe "vedere" la modifica anche dalla rete root
		
		// ============================== FIRST PART: CHECK THAT ALL PROPERTIES ARE SET =====================================
		// TODO: we could collect the list of all things that were set automatically and show them before continuing with the
		// generation of the model. Alternatively, we could throw exceptions like bullets for any slight misbehavior =)
		// Another alternative is to collect the list of what we want to change, and actually make the changes only after the
		// user has approved them. Otherwise, interrupt the analysis by throwing exception.

		//We specifically use the local table for the current network, because we want to keep those attributes separate from the other (sub)networks
		CyTable currentNetworkLocalTable = currentNetwork.getTable(CyNetwork.class, CyRootNetwork.LOCAL_ATTRS);
		
		
		/*
		This check has been shifted to the end, after we have checked the number of levels for all reactants, because the default number of levels of the network (in case we don't find it) is the MAXIMUM of the number of levels of all reactants, and should not be set to another number like 15 here 
		Integer nLvl = 15;
		if (currentNetworkLocalTable.getColumn(Model.Properties.NUMBER_OF_LEVELS) == null
			|| !currentNetworkLocalTable.getRow(currentNetwork.getSUID()).isSet(Model.Properties.NUMBER_OF_LEVELS)
			|| currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class) == null) {
			if (currentNetworkLocalTable.getColumn(Model.Properties.NUMBER_OF_LEVELS) == null) {
				currentNetworkLocalTable.createColumn(Model.Properties.NUMBER_OF_LEVELS, Integer.class, false);
			}
			currentNetworkLocalTable.getRow(currentNetwork.getSUID()).set(Model.Properties.NUMBER_OF_LEVELS, nLvl);
		} else { 
			nLvl = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
		}
		*/

		//It was useless to ask the user for a desired value for something that is very much not clear. Let's just assume that it is 1.0 and possibly change it automatically
		//If the user really knows what he is doing, he can set this parameter by hand without any problem
		Double nSecPerPoint = 1.0;
		if (currentNetworkLocalTable.getColumn(Model.Properties.SECONDS_PER_POINT) == null
			|| !currentNetworkLocalTable.getRow(currentNetwork.getSUID()).isSet(Model.Properties.SECONDS_PER_POINT)
			|| currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECONDS_PER_POINT, Double.class) == null) {
			if (currentNetworkLocalTable.getColumn(Model.Properties.SECONDS_PER_POINT) == null) {
				currentNetworkLocalTable.createColumn(Model.Properties.SECONDS_PER_POINT, Double.class, false);
			}
			currentNetworkLocalTable.getRow(currentNetwork.getSUID()).set(Model.Properties.SECONDS_PER_POINT, nSecPerPoint);
		} else {
			nSecPerPoint = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECONDS_PER_POINT, Double.class);
		}

		Double secStepFactor = 1.0 / nSecPerPoint;
		if (currentNetworkLocalTable.getColumn(Model.Properties.SECS_POINT_SCALE_FACTOR) == null
			|| !currentNetworkLocalTable.getRow(currentNetwork.getSUID()).isSet(Model.Properties.SECS_POINT_SCALE_FACTOR)
			|| currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECS_POINT_SCALE_FACTOR, Double.class) == null) {
			if (currentNetworkLocalTable.getColumn(Model.Properties.SECS_POINT_SCALE_FACTOR) == null) {
				currentNetworkLocalTable.createColumn(Model.Properties.SECS_POINT_SCALE_FACTOR, Double.class, false);
			}
			currentNetworkLocalTable.getRow(currentNetwork.getSUID()).set(Model.Properties.SECS_POINT_SCALE_FACTOR, secStepFactor);
		} else {
			secStepFactor = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECS_POINT_SCALE_FACTOR, Double.class);
		}


		boolean noReactantsPlotted = true;
		int maxNumberOfLevels = 0;
		final int defaultNumberOfLevels = 15; //This is where it makes sense to have a default value for the number of levels: a node, not the network 
		for (CyNode node : currentNetwork.getNodeList()) {

			Boolean enabled = currentNetwork.getRow(node).get(Model.Properties.ENABLED, Boolean.class);
			if (enabled == null) {
				enabled = true;
				Animo.setRowValue(currentNetwork.getRow(node), Model.Properties.ENABLED, Boolean.class, enabled);
			}

			if (!enabled)
				continue;

			Boolean plotted = currentNetwork.getRow(node).get(Model.Properties.PLOTTED, Boolean.class);
			if (plotted == null) {
				Animo.setRowValue(currentNetwork.getRow(node), Model.Properties.PLOTTED, Boolean.class, true);
				plotted = true;
				if (enabled) {
					noReactantsPlotted = false;
				}
			} else if (enabled && plotted) {
				noReactantsPlotted = false;
			}

			Integer nodeLvl = currentNetwork.getRow(node).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			if (nodeLvl == null) {
				Animo.setRowValue(currentNetwork.getRow(node), Model.Properties.NUMBER_OF_LEVELS, Integer.class, defaultNumberOfLevels);
			}
			if (nodeLvl > maxNumberOfLevels) {
				maxNumberOfLevels = nodeLvl;
			}

			Integer initialLevel = currentNetwork.getRow(node).get(Model.Properties.INITIAL_LEVEL, Integer.class);
			if (initialLevel == null) {
				Animo.setRowValue(currentNetwork.getRow(node), Model.Properties.INITIAL_LEVEL, Integer.class, 0);
			}

		}
		
		//Now we know the max number of levels, so we can have a default value for the network.
		Integer nLvl = maxNumberOfLevels;
		if (currentNetworkLocalTable.getColumn(Model.Properties.NUMBER_OF_LEVELS) == null
			|| !currentNetworkLocalTable.getRow(currentNetwork.getSUID()).isSet(Model.Properties.NUMBER_OF_LEVELS)
			|| currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class) == null) {
			if (currentNetworkLocalTable.getColumn(Model.Properties.NUMBER_OF_LEVELS) == null) {
				currentNetworkLocalTable.createColumn(Model.Properties.NUMBER_OF_LEVELS, Integer.class, false);
			}
			currentNetworkLocalTable.getRow(currentNetwork.getSUID()).set(Model.Properties.NUMBER_OF_LEVELS, nLvl);
		} else { 
			nLvl = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
		}

		/*
		 * if (noReactantsPlotted && !smcUppaal.isSelected()) { JOptionPane.showMessageDialog((JTask)this.monitor,
		 * "No reactants selected for plot: select at least one reactant to be plotted in the graph.", "Error", JOptionPane.ERROR_MESSAGE); throw new
		 * InatException("No reactants selected for plot: select at least one reactant to be plotted in the graph."); }
		 */
		if (noReactantsPlotted) {
//			if (JOptionPane.showConfirmDialog(Animo.getCytoscape().getJFrame(),/* (Component) monitor, */
//					"No graphs will be shown. Do you still want to continue?", "No reactants selected for plotting",
//					JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
//				throw new AnimoException("Model generation cancelled by the user");
//			}
		}

		Iterator<CyEdge> edges = currentNetwork.getEdgeList().iterator();
		for (CyEdge edge : currentNetwork.getEdgeList()) {
			Boolean enabled = currentNetwork.getRow(edge).get(Model.Properties.ENABLED, Boolean.class);
			if (enabled == null) {
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.ENABLED, Boolean.class, true);
				enabled = true;
			}

			if (!enabled)
				continue;

			Integer increment = currentNetwork.getRow(edge).get(Model.Properties.INCREMENT, Integer.class);
			if (increment == null) {
				if (edge.getSource().equals(edge.getTarget())) {
					increment = -1;
				} else {
					increment = 1;
				}
			}

//			String res = currentNetwork.getRow(edge.getSource()).get(Model.Properties.CANONICAL_NAME, String.class);
//			String edgeName;
//			StringBuilder reactionName = new StringBuilder();
//			if (res == null) {
//				reactionName.append(res);
//
//				if (increment >= 0) {
//					reactionName.append(" --> ");
//				} else {
//					reactionName.append(" --| ");
//				}
//				res = currentNetwork.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME, String.class);
//				if (res == null) {
//					edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
//				} else {
//					reactionName.append(res);
//					edgeName = reactionName.toString();
//				}
//			} else {
//				edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
//			}
			String edgeName = EdgeDialog.getEdgeName(currentNetwork, edge);

			// Check that the edge has a selected scenario
			Integer scenarioIdx = currentNetwork.getRow(edge).get(Model.Properties.SCENARIO, Integer.class);
			if (scenarioIdx == null) {
				scenarioIdx = 0;
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.SCENARIO, Integer.class, scenarioIdx);
			}
			// Check that the edge has the definition of all parameters requested by the selected scenario
			// otherwise set the parameters to their default values
			Scenario scenario;
			if (scenarioIdx >= 0 && scenarioIdx < Scenario.THREE_SCENARIOS.length) {
				scenario = Scenario.THREE_SCENARIOS[scenarioIdx];
			} else {
				scenarioIdx = 0;
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.SCENARIO, Integer.class, scenarioIdx);
				throw new AnimoException("The reaction " + edgeName + " has an invalid scenario setting ("
						+ scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
			}
			String[] paramNames = scenario.listVariableParameters();
			for (String param : paramNames) {
				Double d = currentNetwork.getRow(edge).get(param, Double.class);
				if (d == null) {
					Animo.setRowValue(currentNetwork.getRow(edge), param, Double.class,
							scenario.getDefaultParameterValue(param));
				} else if (d <= 0) { //We only accept strictly positive parameters!
					throw new AnimoException("Reaction " + edgeName + " with parameter " + param + " = " + animo.util.Utilities.roundToSignificantFigures(d, 4) + " <= 0.\n" + Animo.APP_NAME + " accepts only STRICTLY POSITIVE parameter values: please change it accordingly.");
				}
			}

			if (currentNetwork.getRow(edge).get(Model.Properties.UNCERTAINTY, Integer.class) == null) {
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.UNCERTAINTY, Integer.class, 0);
			}

			if (currentNetwork.getRow(edge).get(Model.Properties.INCREMENT, Integer.class) == null) {
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.INCREMENT, Integer.class, 1);
			}

			if (scenarioIdx == 2) {
				if (!currentNetwork.getRow(edge).isSet(Model.Properties.REACTANT_ID_E1)) {
					Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.REACTANT_ID_E1, Long.class,
							edge.getSource().getSUID());
				}
				if (!currentNetwork.getRow(edge).isSet(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1)) {
					Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1,
							Boolean.class, true);
				}
				if (!currentNetwork.getRow(edge).isSet(Model.Properties.REACTANT_ID_E2)) {
					Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.REACTANT_ID_E2, Long.class,
							edge.getTarget().getSUID());
				}
				if (!currentNetwork.getRow(edge).isSet(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2)) {
					Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2,
							Boolean.class, true);
				}
				
				StringBuilder nameBuilder = new StringBuilder();
				CyNode e1 = currentNetwork.getNode(currentNetwork.getRow(edge).get(Model.Properties.REACTANT_ID_E1, Long.class)),
					   e2 = currentNetwork.getNode(currentNetwork.getRow(edge).get(Model.Properties.REACTANT_ID_E2, Long.class));
				
				if (currentNetwork.getRow(e1).isSet(Model.Properties.CANONICAL_NAME)) {
					nameBuilder.append(currentNetwork.getRow(e1).get(Model.Properties.CANONICAL_NAME, String.class));
				} else {
					nameBuilder.append(currentNetwork.getRow(e1).get(CyNetwork.NAME, String.class));
				}
				
				nameBuilder.append(" AND ");
				if (currentNetwork.getRow(e2).isSet(Model.Properties.CANONICAL_NAME)) {
					nameBuilder.append(currentNetwork.getRow(e2).get(Model.Properties.CANONICAL_NAME, String.class));
				} else {
					nameBuilder.append(currentNetwork.getRow(e2).get(CyNetwork.NAME, String.class));
				}
				
				nameBuilder.append(increment >= 0 ? " --> " : " --| ");
				
				if (currentNetwork.getRow(edge.getTarget()).isSet(Model.Properties.CANONICAL_NAME)) {
					nameBuilder.append(currentNetwork.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME, String.class));
				} else {
					nameBuilder.append(currentNetwork.getRow(edge.getTarget()).get(CyNetwork.NAME, String.class));
				}
				edgeName = nameBuilder.toString();
			}

			switch (scenarioIdx) {
			case 0:
			case 1:
				CyRow source = currentNetwork.getRow(edge.getSource());
				CyRow target = currentNetwork.getRow(edge.getTarget());
				if (!source.get(Model.Properties.ENABLED, Boolean.class)
						&& target.get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that reactant \""
							+ source.get(Model.Properties.CANONICAL_NAME, String.class)
							+ "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
				} else if (!source.get(Model.Properties.ENABLED, Boolean.class)
						&& !target.get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that both reactants \""
							+ source.get(Model.Properties.CANONICAL_NAME, String.class) + "\" and \""
							+ target.get(Model.Properties.CANONICAL_NAME, String.class)
							+ "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
				} else if (source.get(Model.Properties.ENABLED, Boolean.class)
						&& !target.get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that reactant \""
							+ target.get(Model.Properties.CANONICAL_NAME, String.class)
							+ "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
				} else {
					// They are both enabled: all is good
				}
				break;
			case 2:
				CyRow e1 = currentNetwork.getRow(edge.getSource());
				CyRow e2 = currentNetwork.getRow(edge.getTarget());
				if (!e1.get(Model.Properties.ENABLED, Boolean.class) && e2.get(Model.Properties.ENABLED, Boolean.class)
						&& currentNetwork.getRow(edge.getTarget()).get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that reactant \""
							+ e1.get(Model.Properties.CANONICAL_NAME, String.class) + "\" is enabled, or reaction \""
							+ edgeName + "\" cannot stay enabled.");
				} else if (!e1.get(Model.Properties.ENABLED, Boolean.class)
						&& !e2.get(Model.Properties.ENABLED, Boolean.class)
						&& currentNetwork.getRow(edge.getTarget()).get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that both reactants \""
							+ e1.get(Model.Properties.CANONICAL_NAME, String.class) + "\" and \""
							+ e2.get(Model.Properties.CANONICAL_NAME, String.class) + "\" are enabled, or reaction \""
							+ edgeName + "\" cannot stay enabled.");
				} else if (e1.get(Model.Properties.ENABLED, Boolean.class)
						&& !e2.get(Model.Properties.ENABLED, Boolean.class)
						&& currentNetwork.getRow(edge.getTarget()).get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that reactant \""
							+ e2.get(Model.Properties.CANONICAL_NAME, String.class) + "\" is enabled, or reaction \""
							+ edgeName + "\" cannot stay enabled.");
				} else if (!e1.get(Model.Properties.ENABLED, Boolean.class)
						&& e2.get(Model.Properties.ENABLED, Boolean.class)
						&& !currentNetwork.getRow(edge.getTarget()).get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that both reactants \""
							+ e1.get(Model.Properties.CANONICAL_NAME, String.class) + "\" and \""
							+ currentNetwork.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME, String.class)
							+ "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
				} else if (!e1.get(Model.Properties.ENABLED, Boolean.class)
						&& !e2.get(Model.Properties.ENABLED, Boolean.class)
						&& !currentNetwork.getRow(edge.getTarget()).get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that reactants \""
							+ e1.get(Model.Properties.CANONICAL_NAME, String.class) + "\", \""
							+ e2.get(Model.Properties.CANONICAL_NAME, String.class) + "\" and \""
							+ currentNetwork.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME, String.class)
							+ "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
				} else if (e1.get(Model.Properties.ENABLED, Boolean.class)
						&& !e2.get(Model.Properties.ENABLED, Boolean.class)
						&& !currentNetwork.getRow(edge.getTarget()).get(Model.Properties.ENABLED, Boolean.class)) {
					throw new AnimoException("Please check that both reactants \""
							+ e2.get(Model.Properties.CANONICAL_NAME, String.class) + "\" and \""
							+ currentNetwork.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME, String.class)
							+ "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
				} else {
					// They are both enabled: all is good
				}
				break;
			default:
				break;
			}

			Integer tempScaleUpstream = currentNetwork.getRow(edge).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			if (tempScaleUpstream != null) {
				Double scaleUpstream = tempScaleUpstream.doubleValue();
				scaleUpstream /= 15.0;
				// Double scaleDownstream = network.getRow(edge.getTarget()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class).doubleValue() / 15;
				Animo.setRowValue(currentNetwork.getRow(edge.getSource()), Model.Properties.LEVELS_SCALE_FACTOR, Double.class,
						scaleUpstream);
				Animo.setRowValue(currentNetwork.getRow(edge.getTarget()), Model.Properties.LEVELS_SCALE_FACTOR, Double.class,
						scaleUpstream);
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.LEVELS_SCALE_FACTOR, Double.class, null);
			}
		}

		// ============ SECOND PART: MAKE SURE THAT REACTION PARAMETERS IN COMBINATION WITH TIME POINTS DENSITY (SECONDS/POINT) DON'T GENERATE BAD PARAMETERS FOR UPPAAL
		// =============

		double minSecStep = Double.NEGATIVE_INFINITY;
		double maxSecStep = Double.POSITIVE_INFINITY, // The lower bound of the "valid" interval for secs/step (minSecStep) is the maximum of the lower bounds we find for it, while
														// the upper bound (maxSecStep) is the minimum of all upper bounds. This is why we compute them in this apparently strange
														// way
		secPerStep = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECONDS_PER_POINT, Double.class);

		edges = currentNetwork.getEdgeList().iterator();
		while (edges.hasNext()) {
			CyEdge edge = edges.next();
			Boolean enabled = currentNetwork.getRow(edge).get(Model.Properties.ENABLED, Boolean.class);
			if (enabled == null || !enabled)
				continue;
			double levelsScaleFactor;
			double scaleFactorR1 = currentNetwork.getRow(edge.getSource())
					.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class).doubleValue();
			double scaleFactorR2 = currentNetwork.getRow(edge.getTarget())
					.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class).doubleValue();
			CyRow e1 = currentNetwork.getRow(edge.getSource());
			CyRow e2 = currentNetwork.getRow(edge.getTarget());

			Scenario[] scenarios = Scenario.THREE_SCENARIOS;
			Integer scenarioIdx = currentNetwork.getRow(edge).get(Model.Properties.SCENARIO, Integer.class);
			if (scenarioIdx == null) {
				scenarioIdx = 0;
			}
			switch (scenarioIdx) {
			case 0:
				levelsScaleFactor = 1 / scaleFactorR2 * scaleFactorR1;
				break;
			case 1:
				levelsScaleFactor = scaleFactorR1;
				break;
			case 2:
				e1 = currentNetwork.getRow(edge.getSource());
				e2 = currentNetwork.getRow(edge.getTarget());
				double scaleFactorE1 = e1.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
				double scaleFactorE2 = e2.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
				levelsScaleFactor = 1 / scaleFactorR2 * scaleFactorE1 * scaleFactorE2;
				break;
			default:
				levelsScaleFactor = 1.0;
				break;
			}

			int increment;
			increment = currentNetwork.getRow(edge).get(Model.Properties.INCREMENT, Integer.class);
			Scenario scenario;
			if (scenarioIdx >= 0 && scenarioIdx < scenarios.length) {
				scenario = scenarios[scenarioIdx];
			} else {
				// scenario = scenarios[0];
				CyRow myEdge = currentNetwork.getRow(edge);
				myEdge.set(Model.Properties.SCENARIO, 0);
				String edgeName;
				StringBuilder reactionName = new StringBuilder();

				if (e1.isSet(Model.Properties.CANONICAL_NAME)) {
					reactionName.append(e1.get(Model.Properties.CANONICAL_NAME, String.class));

					if (increment >= 0) {
						reactionName.append(" --> ");
					} else {
						reactionName.append(" --| ");
					}
					if (e2.isSet(Model.Properties.CANONICAL_NAME)) {
						reactionName.append(e2.get(Model.Properties.CANONICAL_NAME, String.class));
						edgeName = reactionName.toString();
					} else {
						edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
					}
				} else {
					edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
				}
				throw new AnimoException("The reaction " + edgeName + " has an invalid scenario setting ("
						+ scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
			}

			if (scenarioIdx == 2) {
				e1 = currentNetwork.getRow(edge.getSource());
				e2 = currentNetwork.getRow(edge.getTarget());
			}

			int nLevelsR1;
			int nLevelsR2;
			if (e1.isSet(Model.Properties.NUMBER_OF_LEVELS)) {
				nLevelsR1 = e1.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			} else {
				nLevelsR1 = currentNetwork.getRow(currentNetwork).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			}
			if (e2.isSet(Model.Properties.NUMBER_OF_LEVELS)) {
				nLevelsR2 = e2.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			} else {
				nLevelsR2 = currentNetwork.getRow(currentNetwork).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			}

			boolean activeR1 = true, activeR2 = false;
			if (scenarioIdx == 0) {
				activeR1 = activeR2 = true;
			} else if (scenarioIdx == 1) {
				activeR1 = true;
				if (increment >= 0) {
					activeR2 = false;
				} else {
					activeR2 = true;
				}
			} else if (scenarioIdx == 2) {
				activeR1 = currentNetwork.getRow(edge).get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1, Boolean.class);
				activeR2 = currentNetwork.getRow(edge).get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2, Boolean.class);
			}

			String[] parameters = scenario.listVariableParameters();
			for (int j = 0; j < parameters.length; j++) {
				Double parVal = currentNetwork.getRow(edge).get(parameters[j], Double.class);
				if (parVal != null) {
					scenario.setParameter(parameters[j], parVal);
				} else {
					// TODO: show the editing window
				}
			}

			Integer tempUncertainty = currentNetwork.getRow(edge).get(Model.Properties.UNCERTAINTY, Integer.class);
			Double uncertainty = null;
			if (tempUncertainty == null) {
				uncertainty = 0d;
			} else {
				uncertainty = tempUncertainty.doubleValue();
			}

			Double maxValueRate = 0.0;
			Double minValueRate;
			Double maxValueFormula = Double.POSITIVE_INFINITY;
			Double minValueFormula;

			int maxValueInTables, minValueInTables;
			int colMax, rowMax, incrementColMax, incrementRowMax, colMin, rowMin;
			if (activeR1 && !activeR2) {
				colMax = 0;
				rowMax = nLevelsR2; // The largest number should be in the lower-left corner (the first not to be considered INFINITE_TIME)
				incrementColMax = 1;
				incrementRowMax = -1;
				colMin = nLevelsR1;
				rowMin = 0; // The smallest number should be in the top-right corner
			} else if (activeR1 && activeR2) {
				colMax = 0;
				rowMax = 0; // The largest number should be in the top-left corner (the first != INF)
				incrementColMax = 1;
				incrementRowMax = 1;
				colMin = nLevelsR1;
				rowMin = nLevelsR2; // The smallest number should be in the lower right corner
			} else if (!activeR1 && !activeR2) {
				colMax = nLevelsR1;
				rowMax = nLevelsR2; // The largest number should be in the lower right corner (the first != INF)
				incrementColMax = -1;
				incrementRowMax = -1;
				colMin = 0;
				rowMin = 0; // The smallest number should be in the top-left corner
			} else if (!activeR1 && activeR2) {
				colMax = nLevelsR1;
				rowMax = 0; // The largest number should be in the top-right corner (the first != INF)
				incrementColMax = -1;
				incrementRowMax = 1;
				colMin = 0;
				rowMin = nLevelsR2; // The smallest number should be in the lower-left corner
			} else {
				// this should never happen, as we have already considered all 4 possibilities for activeR1 and activeR2
				colMax = rowMax = colMin = rowMin = incrementColMax = incrementRowMax = 1;
			}
			minValueRate = scenario.computeRate(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
			minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
			while (Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0
					&& rowMax <= nLevelsR2) {
				colMax = colMax + incrementColMax;
				rowMax = rowMax + incrementRowMax;
				maxValueRate = scenario.computeRate(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
				maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
			}
			
			if (Double.isInfinite(minValueFormula)) {
				minValueInTables = VariablesModel.INFINITE_TIME;
			} else {
				minValueInTables = Math.max(
						0,
						(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula
								* (1 - uncertainty / 100.0)));
			}
			if (Double.isInfinite(maxValueFormula)) {
				maxValueInTables = VariablesModel.INFINITE_TIME;
			} else {
				maxValueInTables = Math.max(
						0,
						(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula
								* (1 + uncertainty / 100.0)));
			}
			
			if (minValueInTables == 0) {
				double proposedSecStep = secPerStep
						/ (1.1 * minValueRate / (secStepFactor * levelsScaleFactor * (1 - uncertainty / 100.0)));
				if (proposedSecStep < maxSecStep) {
					maxSecStep = proposedSecStep;
				}
			}
			if (maxValueInTables > VERY_LARGE_TIME_VALUE) {
				double proposedSecStep = secPerStep
						/ (VERY_LARGE_TIME_VALUE * maxValueRate / (secStepFactor * levelsScaleFactor * (1 + uncertainty / 100.0)));
				if (proposedSecStep > minSecStep) {
					minSecStep = proposedSecStep;
				}
			}

		}

		if (nMinutesToSimulate != null && nMinutesToSimulate >= 0) { // If we were requested to perform a simulation (and not simply translate the model "out of curiosity" or for
																		// model checking), we were given also a time limit for the simulation. We need to check whether that time
																		// limit is a "large number"
			int myTimeTo = (int) (nMinutesToSimulate * 60.0 / secPerStep);
			if (myTimeTo > VERY_LARGE_TIME_VALUE) {
				double proposedSecStep = nMinutesToSimulate * 60.0 / VERY_LARGE_TIME_VALUE;
				if (proposedSecStep > minSecStep) {
					minSecStep = proposedSecStep;
				}
			}
		}

		if (!Double.isInfinite(minSecStep) || !Double.isInfinite(maxSecStep)) {
			System.err.println("As far as I see from the computations, a valid interval for secs/point is ["
					+ minSecStep + ", " + maxSecStep + "]");
		}
		if (!Double.isInfinite(maxSecStep) && secPerStep > maxSecStep) {
			System.err.println("\tThe current setting is over the top: " + secPerStep + " > " + maxSecStep
					+ ", so take " + maxSecStep);
			double oldSecPerStep = secPerStep;
			secPerStep = maxSecStep;
			Animo.setRowValue(currentNetworkLocalTable.getRow(currentNetwork.getSUID()), Model.Properties.SECONDS_PER_POINT, Double.class, secPerStep);
			double factor = oldSecPerStep / secPerStep;
			secStepFactor *= factor;
			Animo.setRowValue(currentNetworkLocalTable.getRow(currentNetwork.getSUID()), Model.Properties.SECS_POINT_SCALE_FACTOR, Double.class, secStepFactor);
		} else {
			// System.err.println("\tNon vado sopra il massimo: " + secPerStep + " <= " + maxSecStep);
		}
		if (!Double.isInfinite(minSecStep) && secPerStep < minSecStep) { // Notice that this check is made last because it is the most important: if we set seconds/point to a value
																			// less than the computed minimum, the time values will be so large that UPPAAL will not be able to
																			// understand them, thus producing no result
			System.err.println("\tThe current setting is under the bottom: " + secPerStep + " < " + minSecStep
					+ ", so take " + minSecStep);
			double oldSecPerStep = secPerStep;
			secPerStep = minSecStep;
			Animo.setRowValue(currentNetworkLocalTable.getRow(currentNetwork.getSUID()), Model.Properties.SECONDS_PER_POINT, Double.class, secPerStep);
			double factor = oldSecPerStep / secPerStep;
			secStepFactor *= factor;
			Animo.setRowValue(currentNetworkLocalTable.getRow(currentNetwork.getSUID()), Model.Properties.SECS_POINT_SCALE_FACTOR, Double.class, secStepFactor);
		}
	}

	/**
	 * Translate the Cytoscape network in the internal ANIMO model representation. This intermediate model will then translated as needed into the proper UPPAAL model by the
	 * analysers. All properties needed from the Cytoscape network are copied in the resulting model, checking that all are set ok.
	 * 
	 * @param monitor
	 *            The TaskMonitor with which to communicate the advancement of the model generation
	 * @param nMinutesToSimulate
	 *            If a simulation was requested, this parameter is >= 0 and when checking the presence of all necessary parameters (see checkParameters()) we will also check that
	 *            the upper bound is not "too large" for UPPAAL to understand it. The parameter can be < 0 or null to indicate that no simulation is requested.
	 * @param generateTables
	 *            Whether to generate the time tables, or just find minimum and maximum to set the reaction activityRatio when the slider is used
	 * @return The intermediate ANIMO model
	 * @throws AnimoException
	 */
	public static Model generateModelFromCurrentNetwork(TaskMonitor monitor, Integer nMinutesToSimulate,
			boolean generateTables) throws AnimoException {
		checkParameters(nMinutesToSimulate);

		Map<Long, String> nodeSUIDToModelId = new HashMap<Long, String>();
		Map<Long, String> edgeSUIDToModelId = new HashMap<Long, String>();

		Model model = new Model();

		final CyNetwork currentNetwork = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
		//CyRootNetworkManager rootNetworkManager = Animo.getCytoscapeApp().getCyRootNetworkManager();
		//CyNetwork rootNetwork = rootNetworkManager.getRootNetwork(currentNetwork); //.getBaseNetwork();
		//network = currentNetwork;
		
		//We specifically use the local table for the current network, because we want to keep those attributes separate from the other (sub)networks
		CyTable currentNetworkLocalTable = currentNetwork.getTable(CyNetwork.class, CyRootNetwork.LOCAL_ATTRS);
		
		
		final int totalWork = currentNetwork.getNodeCount() + currentNetwork.getEdgeCount();
		int doneWork = 0;

		if (currentNetworkLocalTable.getRow(currentNetwork.getSUID()).isSet("deltaAlternating")) {
			model.getProperties().let("deltaAlternating")
					.be(currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get("deltaAlternating", Boolean.class));
		}
		if (currentNetworkLocalTable.getRow(currentNetwork.getSUID()).isSet("useOldResetting")) {
			model.getProperties().let("useOldResetting")
					.be(currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get("useOldResetting", Boolean.class));
		}
//		model.getProperties().let(Model.Properties.NUMBER_OF_LEVELS)
//				.be(currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class));
//		model.getProperties().let(Model.Properties.SECONDS_PER_POINT)
//				.be(currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECONDS_PER_POINT, Double.class));
		
		double secStepFactor = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECS_POINT_SCALE_FACTOR, Double.class);
		final Integer maxNLevels = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
		final Double nSecondsPerPoint = currentNetworkLocalTable.getRow(currentNetwork.getSUID()).get(Model.Properties.SECONDS_PER_POINT, Double.class);

		model.getProperties().let(Model.Properties.SECS_POINT_SCALE_FACTOR).be(secStepFactor);
		model.getProperties().let(Model.Properties.NUMBER_OF_LEVELS).be(maxNLevels);
		model.getProperties().let(Model.Properties.SECONDS_PER_POINT).be(nSecondsPerPoint);

		// do nodes first

		List<CyNode> nodesList = currentNetwork.getNodeList();
		Collections.sort(nodesList, new Comparator<CyNode>() {
			@Override
			public int compare(CyNode n1, CyNode n2) {
				String name1, name2;
				name1 = currentNetwork.getRow(n1).get(Model.Properties.CANONICAL_NAME, String.class);
				if (name1 == null) {
					name1 = currentNetwork.getRow(n1).get(CyNetwork.NAME, String.class);
				}
				name2 = currentNetwork.getRow(n2).get(Model.Properties.CANONICAL_NAME, String.class);
				if (name2 == null) {
					name2 = currentNetwork.getRow(n2).get(CyNetwork.NAME, String.class);
				}
				return name1.compareTo(name2);
			}
		});
		final Iterator<CyNode> nodes = nodesList.iterator();
		for (int i = 0; nodes.hasNext(); i++) {
			if (monitor != null) {
				monitor.setProgress((100 * doneWork++) / totalWork);
			}
			CyNode node = nodes.next();

			final String reactantId = "R" + i;
			Reactant r = new Reactant(reactantId);
			nodeSUIDToModelId.put(node.getSUID(), reactantId);
			// TODO: types zijn nogal gegokt
			r.let(Model.Properties.CYTOSCAPE_ID).be(node.getSUID());
			r.let(Model.Properties.REACTANT_NAME).be(currentNetwork.getRow(node).get(CyNetwork.NAME, String.class));
			r.setName(currentNetwork.getRow(node).get(Model.Properties.CANONICAL_NAME, String.class));
			r.let(Model.Properties.NUMBER_OF_LEVELS).be(
					currentNetwork.getRow(node).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class));
			r.let(Model.Properties.LEVELS_SCALE_FACTOR).be(
					currentNetwork.getRow(node).get(Model.Properties.LEVELS_SCALE_FACTOR, Double.class));
			r.let(Model.Properties.GROUP).be(currentNetwork.getRow(node).get(Model.Properties.GROUP, String.class));
			r.let(Model.Properties.ENABLED).be(currentNetwork.getRow(node).get(Model.Properties.ENABLED, Boolean.class));
			r.let(Model.Properties.PLOTTED).be(currentNetwork.getRow(node).get(Model.Properties.PLOTTED, Boolean.class));
			r.let(Model.Properties.INITIAL_LEVEL).be(
					currentNetwork.getRow(node).get(Model.Properties.INITIAL_LEVEL, Integer.class));

			model.add(r);
		}

		// do edges next

		final Iterator<CyEdge> edges = currentNetwork.getEdgeList().iterator();
		int minTimeModel = Integer.MAX_VALUE;
		int maxTimeModel = Integer.MIN_VALUE;
		Integer unc = 0; // Uncertainty value is now ANIMO-wide (TODO: is that a bit excessive? One would expect the uncertainty to be connected to the model...)
		XmlConfiguration configuration = AnimoBackend.get().configuration();
		try {
			unc = Integer.valueOf(configuration.get(XmlConfiguration.UNCERTAINTY_KEY));
		} catch (NumberFormatException ex) {
			unc = 0;
		}
		for (int i = 0; edges.hasNext(); i++) {
			if (monitor != null)
				monitor.setProgress((100 * doneWork++) / totalWork);

			CyEdge edge = edges.next();
			if (!currentNetwork.getRow(edge).get(Model.Properties.ENABLED, Boolean.class))
				continue;

			Double levelsScaleFactor;
			String reactionId = "reaction" + i; // <<---- THIS ID IS NOT USED!! We set it later!
			Reaction r = new Reaction(reactionId);
			//edgeSUIDToModelId.put(edge.getSUID(), reactionId); <<-- So also this one is not done now.

			r.let(Model.Properties.ENABLED).be(currentNetwork.getRow(edge).get(Model.Properties.ENABLED, Boolean.class));
			r.let(Model.Properties.INCREMENT).be(currentNetwork.getRow(edge).get(Model.Properties.INCREMENT, Integer.class));
			r.let(Model.Properties.CYTOSCAPE_ID).be(edge.getSUID());

			r.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);

			final String reactant = nodeSUIDToModelId.get(edge.getTarget().getSUID());
			r.let(Model.Properties.REACTANT).be(reactant);

			final String catalyst = nodeSUIDToModelId.get(edge.getSource().getSUID());
			r.let(Model.Properties.CATALYST).be(catalyst);

			int nLevelsR1, nLevelsR2;

			if (!model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
				nLevelsR1 = model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR1 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			}
			if (!model.getReactant(reactant).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
				nLevelsR2 = model.getReactant(reactant).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR2 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			}

			Scenario[] scenarios = Scenario.THREE_SCENARIOS;
			Integer scenarioIdx;
			scenarioIdx = currentNetwork.getRow(edge).get(Model.Properties.SCENARIO, Integer.class);
			Scenario scenario;
			if (scenarioIdx >= 0 && scenarioIdx < scenarios.length) {
				scenario = scenarios[scenarioIdx];
				switch (scenarioIdx) {
				case 0:
					levelsScaleFactor = 1.0
							/ model.getReactant(reactant).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class)
							* model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
					break;
				case 1:
					levelsScaleFactor = 1.0 * model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS)
							.as(Integer.class);
					break;
				case 2:
					Long e1_id = currentNetwork.getRow(edge).get(Model.Properties.REACTANT_ID_E1, Long.class);
					Long e2_id = currentNetwork.getRow(edge).get(Model.Properties.REACTANT_ID_E2, Long.class);
					String e1 = nodeSUIDToModelId.get(e1_id);
					String e2 = nodeSUIDToModelId.get(e2_id);
					levelsScaleFactor = 1.0
							/ model.getReactant(reactant).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class)
							* model.getReactant(e1).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class)
							* model.getReactant(e2).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
					// If we wanted, we could also "simplify" the multiplication above by trying to identify the substrate (called "reactant") with one of the two upstream entities
					// ("e1" and "e2"), but the result would be the same
					break;
				default:
					levelsScaleFactor = 1.0;
					break;
				}
			} else {
				Animo.setRowValue(currentNetwork.getRow(edge), Model.Properties.SCENARIO, Integer.class, 0);
				int increment;
				if (currentNetwork.getRow(edge).isSet(Properties.INCREMENT)) {
					increment = currentNetwork.getRow(edge).get(Properties.INCREMENT, Integer.class);
				} else {
					if (edge.getSource().equals(edge.getTarget())) {
						increment = -1;
					} else {
						increment = 1;
					}
				}
				String res;
				String edgeName;
				StringBuilder reactionName = new StringBuilder();
				if (currentNetwork.getRow(edge.getSource()).isSet(Model.Properties.CANONICAL_NAME)) {
					res = currentNetwork.getRow(edge.getSource()).get(Model.Properties.CANONICAL_NAME, String.class);
					reactionName.append(res);

					if (increment >= 0) {
						reactionName.append(" --> ");
					} else {
						reactionName.append(" --| ");
					}
					if (currentNetwork.getRow(edge.getTarget()).isSet(Model.Properties.CANONICAL_NAME)) {
						res = currentNetwork.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME, String.class);
						reactionName.append(res);
						edgeName = reactionName.toString();
					} else {
						edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
					}
				} else {
					edgeName = "" + edge.getSource() + " --> " + edge.getTarget();
				}
				throw new AnimoException("The reaction " + edgeName + " has an invalid scenario setting ("
						+ scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
			}
			r.let(Model.Properties.SCENARIO).be(scenarioIdx);
			r.let(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").be(levelsScaleFactor);
			r.let(Model.Properties.SCENARIO_PARAMETER_K).be(
					currentNetwork.getRow(edge).get(Model.Properties.SCENARIO_PARAMETER_K, Double.class));

			String[] parameters = scenario.listVariableParameters();
			HashMap<String, Double> scenarioParameterValues = new HashMap<String, Double>();
			for (int j = 0; j < parameters.length; j++) {
				Double parVal = currentNetwork.getRow(edge).get(parameters[j], Double.class);
				if (parVal != null) {
					scenario.setParameter(parameters[j], parVal);
					scenarioParameterValues.put(parameters[j], parVal);
				} else {
					// checkParameters should make sure that each parameter is present, at least with its default value
				}
			}
			r.let(Model.Properties.SCENARIO_CFG).be(new ScenarioCfg(scenarioIdx, scenarioParameterValues));

			double uncertainty = unc;
			// Also: while we are here, we delete the Model.Properties.UNCERTAINTY attribute if we find it

			if (currentNetwork.getRow(edge).isSet(Model.Properties.UNCERTAINTY)) {
				currentNetwork.getRow(edge).set(Model.Properties.UNCERTAINTY, null);

			}

			if (scenarioIdx == 2) { // actually, they are both catalysts
				String cata, reac;
				cata = nodeSUIDToModelId.get(currentNetwork.getRow(edge).get(Model.Properties.REACTANT_ID_E1, Long.class));
				reac = nodeSUIDToModelId.get(currentNetwork.getRow(edge).get(Model.Properties.REACTANT_ID_E2, Long.class));
				r.let(Model.Properties.CATALYST).be(cata);
				r.let(Model.Properties.REACTANT).be(reac);
				if (!model.getReactant(cata).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
					nLevelsR1 = model.getReactant(cata).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
				} else {
					nLevelsR1 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
				}
				if (!model.getReactant(reac).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
					nLevelsR2 = model.getReactant(reac).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
				} else {
					nLevelsR2 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
				}
				String out = nodeSUIDToModelId.get(edge.getTarget().getSUID());
				r.let(Model.Properties.OUTPUT_REACTANT).be(out);
				// levelsScaleFactor /= 2*nodeAttributes.getDoubleAttribute(network.getRow(edge).get(Model.Properties.REACTANT_ID_E2,String.class)),
				// Model.Properties.LEVELS_SCALE_FACTOR);
			} else {
				r.let(Model.Properties.OUTPUT_REACTANT).be(reactant);
			}

			String reactionAlias;
			reactionAlias = model.getReactant(r.get(Model.Properties.CATALYST).as(String.class)).getName();
			if (scenarioIdx == 2) {
				reactionAlias += " AND "
						+ model.getReactant(r.get(Model.Properties.REACTANT).as(String.class)).getName();
			}
			if (r.get(Model.Properties.INCREMENT).as(Integer.class) >= 0) {
				reactionAlias += " --> ";
			} else {
				reactionAlias += " --| ";
			}
			if (scenarioIdx == 2) {
				reactionAlias += model.getReactant(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class)).getName();
			} else {
				reactionAlias += model.getReactant(r.get(Model.Properties.REACTANT).as(String.class)).getName();
			}
			r.setName(reactionAlias);

			boolean activeR1 = true, activeR2 = false;
			boolean reactant1IsDownstream = false, reactant2IsDownstream = true;

			if (scenarioIdx == 0 || scenarioIdx == 1) {
				activeR1 = true;
				if (currentNetwork.getRow(edge).get(Model.Properties.INCREMENT, Integer.class) >= 0) {
					activeR2 = false;
				} else {
					activeR2 = true;
				}
			} else if (scenarioIdx == 2) {
				activeR1 = currentNetwork.getRow(edge).get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1, Boolean.class);
				activeR2 = currentNetwork.getRow(edge).get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2, Boolean.class);
				r.let(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1).be(activeR1);
				r.let(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2).be(activeR2);
				reactant1IsDownstream = r.get(Model.Properties.CATALYST).as(String.class)
						.equals(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class));
				reactant2IsDownstream = r.get(Model.Properties.REACTANT).as(String.class)
						.equals(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class));
			} else {
				// This should never happen, because we have already made these checks
				activeR1 = activeR2 = true;
			}
			r.let(Model.Properties.R1_IS_DOWNSTREAM).be(reactant1IsDownstream);
			r.let(Model.Properties.R2_IS_DOWNSTREAM).be(reactant2IsDownstream);

			if (generateTables) {
				List<Double> times = scenario.generateTimes(1 + nLevelsR1, activeR1, reactant1IsDownstream,
						1 + nLevelsR2, activeR2, reactant2IsDownstream);
				Table timesLTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
				Table timesUTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);

				int minTime = Integer.MAX_VALUE, maxTime = Integer.MIN_VALUE;
				for (int j = 0; j < nLevelsR2 + 1; j++) {
					for (int k = 0; k < nLevelsR1 + 1; k++) {
						Double t = times.get(j * (nLevelsR1 + 1) + k);
						if (Double.isInfinite(t)) {
							timesLTable.set(j, k, VariablesModel.INFINITE_TIME);
							timesUTable.set(j, k, VariablesModel.INFINITE_TIME);
						} else if (uncertainty == 0) {
							timesLTable.set(j, k, (int) Math.round(secStepFactor * levelsScaleFactor * t));
							timesUTable.set(j, k, (int) Math.round(secStepFactor * levelsScaleFactor * t));
							if (timesLTable.get(j, k) < minTime) {
								minTime = timesLTable.get(j, k);
							}
							if (timesUTable.get(j, k) > maxTime) {
								maxTime = timesUTable.get(j, k);
							}
						} else {
							timesLTable.set(
									j,
									k,
									Math.max(
											0,
											(int) Math.round(secStepFactor * levelsScaleFactor * t
													* (1 - uncertainty / 100.0))));
							timesUTable.set(
									j,
									k,
									Math.max(
											0,
											(int) Math.round(secStepFactor * levelsScaleFactor * t
													* (1 + uncertainty / 100.0))));
							if (timesLTable.get(j, k) < minTime) {
								minTime = timesLTable.get(j, k);
							}
							if (timesUTable.get(j, k) > maxTime) {
								maxTime = timesUTable.get(j, k);
							}
						}
					}
				}
				if (minTime == Integer.MAX_VALUE) {
					minTime = VariablesModel.INFINITE_TIME;
				}
				if (maxTime == Integer.MIN_VALUE) {
					maxTime = VariablesModel.INFINITE_TIME;
				}
				r.let(Model.Properties.TIMES_LOWER).be(timesLTable);
				r.let(Model.Properties.TIMES_UPPER).be(timesUTable);
				r.let(Model.Properties.MINIMUM_DURATION).be(minTime);
				r.let(Model.Properties.MAXIMUM_DURATION).be(maxTime);
				if (minTime != VariablesModel.INFINITE_TIME
						&& (minTimeModel == Integer.MAX_VALUE || minTime < minTimeModel)) {
					minTimeModel = minTime;
				}
				if (maxTime != VariablesModel.INFINITE_TIME
						&& (maxTimeModel == Integer.MIN_VALUE || maxTime > maxTimeModel)) {
					maxTimeModel = maxTime;
				}
			} else { // No tabels were requested, so find only min and max time to be used in the computation of reaction activityRatio
				Double maxValueFormula = Double.POSITIVE_INFINITY, minValueFormula;
				int maxValueInTables, minValueInTables;
				int colMax, rowMax, incrementColMax, incrementRowMax, colMin, rowMin;
				if (activeR1 && !activeR2) {
					colMax = 0;
					rowMax = nLevelsR2; // The largest number should be in the lower-left corner (the first not to be considered INFINITE_TIME)
					incrementColMax = 1;
					incrementRowMax = -1;
					colMin = nLevelsR1;
					rowMin = 0; // The smallest number should be in the top-right corner
				} else if (activeR1 && activeR2) {
					colMax = 0;
					rowMax = 0; // The largest number should be in the top-left corner (the first != INF)
					incrementColMax = 1;
					incrementRowMax = 1;
					colMin = nLevelsR1;
					rowMin = nLevelsR2; // The smallest number should be in the lower right corner
				} else if (!activeR1 && !activeR2) {
					colMax = nLevelsR1;
					rowMax = nLevelsR2; // The largest number should be in the lower right corner (the first != INF)
					incrementColMax = -1;
					incrementRowMax = -1;
					colMin = 0;
					rowMin = 0; // The smallest number should be in the top-left corner
				} else if (!activeR1 && activeR2) {
					colMax = nLevelsR1;
					rowMax = 0; // The largest number should be in the top-right corner (the first != INF)
					incrementColMax = -1;
					incrementRowMax = 1;
					colMin = 0;
					rowMin = nLevelsR2; // The smallest number should be in the lower-left corner
				} else {
					// This should never happen, as we have already considered all 4 possibilities for activeR1 and activeR2
					colMax = rowMax = colMin = rowMin = incrementColMax = incrementRowMax = 1;
				}
				minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
				while (Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0
						&& rowMax <= nLevelsR2) {
					colMax = colMax + incrementColMax;
					rowMax = rowMax + incrementRowMax;
					maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
				}

				if (Double.isInfinite(minValueFormula)) {
					minValueInTables = VariablesModel.INFINITE_TIME;
				} else {
					if (uncertainty == 0) {
						minValueInTables = Math.max(0,
								(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula));
					} else {
						minValueInTables = Math.max(
								0,
								(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula
										* (1 - uncertainty / 100.0)));
					}
				}
				if (Double.isInfinite(maxValueFormula)) {
					maxValueInTables = VariablesModel.INFINITE_TIME;
				} else {
					if (uncertainty == 0) {
						maxValueInTables = Math.max(0,
								(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula));
					} else {
						maxValueInTables = Math.max(
								0,
								(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula
										* (1 + uncertainty / 100.0)));
					}
				}
				r.let(Model.Properties.MINIMUM_DURATION).be(minValueInTables);
				r.let(Model.Properties.MAXIMUM_DURATION).be(maxValueInTables);
				if (minValueInTables != VariablesModel.INFINITE_TIME
						&& (minTimeModel == Integer.MAX_VALUE || minValueInTables < minTimeModel)) {
					minTimeModel = minValueInTables;
				}
				if (maxValueInTables != VariablesModel.INFINITE_TIME
						&& (maxTimeModel == Integer.MIN_VALUE || maxValueInTables > maxTimeModel)) {
					maxTimeModel = maxValueInTables;
				}
			}

			String r1Id = r.get(Model.Properties.CATALYST).as(String.class);
			String r2Id = r.get(Model.Properties.REACTANT).as(String.class);
			String rOutput = r.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
			r.setId(r1Id + "_" + r2Id + ((rOutput.equals(r2Id)) ? "" : "_" + rOutput));
			
			edgeSUIDToModelId.put(edge.getSUID(), r.getId());

			model.add(r);
		}
		if (minTimeModel == Integer.MAX_VALUE) {
			minTimeModel = VariablesModel.INFINITE_TIME;
		}
		model.getProperties().let(Model.Properties.MINIMUM_DURATION).be(minTimeModel);
		if (maxTimeModel == Integer.MIN_VALUE) {
			maxTimeModel = VariablesModel.INFINITE_TIME;
		}
		model.getProperties().let(Model.Properties.MAXIMUM_DURATION).be(maxTimeModel);

		model.mapCytoscapeIDtoReactantID = nodeSUIDToModelId;
		model.mapCytoscapeIDtoReactionID = edgeSUIDToModelId;

		return model;
	}

	/**
	 * The vertices in the model.
	 */
	private Map<String, Reactant> reactants;

	/**
	 * The edges in the model.
	 */
	private Map<String, Reaction> reactions;

	private Map<Long, String> mapCytoscapeIDtoReactantID = null;
	private Map<Long, String> mapCytoscapeIDtoReactionID = null;

	/**
	 * The global properties on the model.
	 */
	private PropertyBag properties;

	/**
	 * Constructor.
	 */
	public Model() {
		this.reactants = new HashMap<String, Reactant>();
		this.reactions = new HashMap<String, Reaction>();
		this.properties = new PropertyBag();
	}

	/**
	 * Puts (adds or replaces) a vertex into the model.
	 * 
	 * @param v
	 *            the vertex to add
	 */
	public void add(Reactant v) {
		assert v.getModel() == null : "Can't add a reactant that is already part of a model.";

		this.reactants.put(v.getId(), v);
		v.setModel(this);
	}

	/**
	 * Puts (adds or replaces) an edge into the model.
	 * 
	 * @param e
	 *            the edge to remove
	 */
	public void add(Reaction e) {
		assert e.getModel() == null : "Can't add a reaction that is already part of a model.";

		this.reactions.put(e.getId(), e);
		e.setModel(this);
	}

	/**
	 * returns a (deep) copy of this model
	 */
	public Model copy() {
		Model model = new Model();
		model.properties = this.properties.copy();
		for (String k : reactants.keySet()) {
			model.reactants.put(k, reactants.get(k).copy());
		}
		for (String k : reactions.keySet()) {
			model.reactions.put(k, reactions.get(k).copy());
		}
		return model;
	}

	public Map<Long, String> getMapCytoscapeIDtoReactantID() {
		return this.mapCytoscapeIDtoReactantID;
	}

	/**
	 * Returns the properties for this model.
	 * 
	 * @return the properties of this model
	 */
	public PropertyBag getProperties() {
		return this.properties;
	}

	/**
	 * Returns the vertex with the given identifier, or {@code null}.
	 * 
	 * @param id
	 *            the identifier we are looking for
	 * @return the found {@link Reactant}, or {@code null}
	 */
	public Reactant getReactant(String id) {
		return this.reactants.get(id);
	}

	/**
	 * Given the Cytoscape node id, return the corresponding Reactant in the model
	 * 
	 * @param id
	 *            The Cytoscape node identificator (SUID)
	 * @return The Reactant as constructed in the current model
	 */
	public Reactant getReactantByCytoscapeID(Long id) {
		return this.reactants.get(this.mapCytoscapeIDtoReactantID.get(id));
	}
	
	public Reaction getReactionByCytoscapeID(Long id) {
		return this.reactions.get(this.mapCytoscapeIDtoReactionID.get(id));
	}

	/**
	 * Returns an unmodifiable view of all vertices in this model.
	 * 
	 * @return all vertices
	 */
	public Collection<Reactant> getReactantCollection() {
		return Collections.unmodifiableCollection(this.reactants.values());
	}

	public Map<String, Reactant> getReactants() {
		return this.reactants;
	}
	
	public List<Reactant> getSortedReactantList() {
		List<Reactant> result = new ArrayList<Reactant>();
		result.addAll(this.reactants.values());
		Collections.sort(result, new Comparator<Reactant>() {
			@Override
			public int compare(Reactant r1, Reactant r2) {
				return r1.getName().compareTo(r2.getName());
			}
		});
		return result;
	}

	/**
	 * Returns the edge with the given identifier, or {@code null}.
	 * 
	 * @param id
	 *            the identifier we are looking for
	 * @return the found {@link Reaction}, or {@code null}
	 */
	public Reaction getReaction(String id) {
		return this.reactions.get(id);
	}

	/**
	 * returns an unmodifiable view of all edges in this model.
	 * 
	 * @return all edges
	 */
	public Collection<Reaction> getReactionCollection() {
		return Collections.unmodifiableCollection(this.reactions.values());
	}

	public Map<String, Reaction> getReactions() {
		return this.reactions;
	}

	/**
	 * Removes a vertex, this method also cleans all edges connecting to this vertex.
	 * 
	 * @param v
	 *            the vertex to remove
	 */
	public void remove(Reactant v) {
		assert v.getModel() == this : "Can't remove a reactant that is not part of this model.";
		this.reactants.remove(v.getId());
		v.setModel(null);
	}

	/**
	 * Removes an edge.
	 * 
	 * @param e
	 *            the edge to remove
	 */
	public void remove(Reaction e) {
		assert e.getModel() == this : "Can't remove a reaction that is not part of this model.";
		this.reactions.remove(e.getId());
		e.setModel(null);
	}

	public void setMapCytoscapeIDtoReactantID(Map<Long, String> mapCytoscapeIDtoReactantID) {
		this.mapCytoscapeIDtoReactantID = mapCytoscapeIDtoReactantID;
	}

	public void setProperties(PropertyBag properties) {
		this.properties = properties;
	}

	public void setReactants(Map<String, Reactant> reactants) { // The next 7 methods are to keep compatibility with java beans and XML encoder/decoder
		this.reactants = reactants;
	}

	public void setReactions(Map<String, Reaction> reactions) {
		this.reactions = reactions;
	}

	/**
	 * Outputs a string listing all reactants and reactions of this model.
	 * 
	 * @return a string with reactants and reactions on seperate lines
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append("Model[\n");
		for (Reactant v : this.reactants.values()) {
			result.append("  " + v + "\n");
		}
		for (Reaction e : this.reactions.values()) {
			result.append("  " + e + "\n");
		}
		result.append("]");

		return result.toString();
	}
}
