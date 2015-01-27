package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.analyser.LevelResult;
import animo.core.graph.FileUtils;
import animo.core.graph.Graph;
import animo.core.graph.Series;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;
import animo.core.model.Scenario;
import animo.cytoscape.Animo;
import animo.fitting.ParameterFitterObserver;
import animo.fitting.levenbergmarquardt.LevenbergMarquardtFitter;

public class EdgeOptimizeKContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {

	CyNetwork network;

	CyNetworkView netView;

	View<CyEdge> edgeView;

	@Override
	public void actionPerformed(ActionEvent e) { //TODO This should actually be what the "generic parameter fitter" should do: choose the fitting algorithm/strategy, make the model (ANIMO Model), select the reactions to be optimized, the nodes to be compared etc
		final CyNetwork currentNetwork = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
		if (currentNetwork == null) {
			return;
		}
		int timeTo = -1;
		Model model = null;
		try {
			model = Model.generateModelFromCurrentNetwork(null, timeTo, false);
		} catch (Exception ex) { //TODO: THIS IS WRONG! We should show the exception to the user! The exceptions raised here are errors for the user!
			ex.printStackTrace(System.err);
			return;
		}
		
		//The reactions to be optimized are the ones affected by this context menu call (so the ones that are currently selected, or the one on which the context menu was originated if none is selected)
		List<Reaction> reactionsToBeOptimized = new Vector<Reaction>(); //model.getReactionCollection());
		List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network, "selected", true);
		if (selectedEdges.isEmpty()) { //We can rescale a set of edges or only the edge on which the right-click action was performed. We are in the second case here.
			CyEdge edge = edgeView.getModel();
			Reaction reaction = model.getReactionByCytoscapeID(edge.getSUID());
			if (reaction == null || !reaction.isEnabled()) {
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "The only selected reaction is disabled, so I cannot optimize anything.\nPlease enable the reactions you want to use:\nright-click --> " + Animo.APP_NAME + " --> Enable/disable", "Reaction disabled", JOptionPane.ERROR_MESSAGE);
				return; //That was the only one: if it was not even enabled, we are losing time here
			}
			reactionsToBeOptimized.add(reaction);
		} else {
			boolean onlyDisabled = true;
			for (CyEdge edge : selectedEdges) {
				Reaction reaction = model.getReactionByCytoscapeID(edge.getSUID());
				if (reaction == null || !reaction.isEnabled()) continue;
				onlyDisabled = false;
				reactionsToBeOptimized.add(reaction);
			}
			if (onlyDisabled) { //If all selected reactions were disabled, we can do nothing, so exit
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "All the selected reactions are disabled, so I cannot optimize anything.\nPlease enable the reactions you want to use:\nright-click --> " + Animo.APP_NAME + " --> Enable/disable", "Reactions disabled", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		double minParameter = Double.POSITIVE_INFINITY;
		for (Reaction r : reactionsToBeOptimized) {
			Scenario scenario = Scenario.THREE_SCENARIOS[r.get(Model.Properties.SCENARIO).as(Integer.class)];
			for (String paramName : scenario.listVariableParameters()) {
				double paramValue = r.get(paramName).as(Double.class);
				if (paramValue < minParameter) {
					minParameter = paramValue;
				}
			}
		}
		
		String referenceDataFile = FileUtils.open(Graph.CSV_FILE_EXTENSION, Graph.CSV_FILE_DESCRIPTION, "Open reference data file", Animo.getCytoscape().getJFrame());
		if (referenceDataFile == null) {
			return;
		}
		LevelResult result = null;
		try {
			result = Graph.readCSVtoLevelResult(referenceDataFile, null, -1);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			return;
		}
		timeTo = (int)Math.round(result.getTimeIndices().get(result.getTimeIndices().size() - 1));
		//We use SortedMap to ensure that we always keep the same ordering of the parameters
		SortedMap<Reactant, String> reactantToDataCorrespondence = new TreeMap<Reactant, String>();
		boolean onlyDisabled = true,
				onlyHidden = true;
		for (Reactant r : model.getReactantCollection()) {
			if (!r.isEnabled()) continue;
			onlyDisabled = false;
			if (!r.get(Model.Properties.PLOTTED).as(Boolean.class)) continue; //Assume that PLOTTED = selected for comparison
			onlyHidden = false;
			for (String dataSeriesName : result.getReactantIds()) {
				if (!dataSeriesName.toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX) && dataSeriesName.contains(r.getName())) { //If I can find the name of a "selected" reactant among the ones in the data file, I associate them for the comparison
					reactantToDataCorrespondence.put(r, dataSeriesName);
				}
			}
			if (!reactantToDataCorrespondence.containsKey(r)) { //If I didn't add this reactant, maybe I should be more flexible and try to make a comparison without considering capitalization
				for (String dataSeriesName : result.getReactantIds()) {
					if (!dataSeriesName.toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX) && dataSeriesName.toLowerCase().contains(r.getName().toLowerCase())) {
						reactantToDataCorrespondence.put(r, dataSeriesName);
					}
				}
			}
		}
		if (onlyHidden) { //If all the reactants are hidden (i.e. not plotted), I will consider them all
			for (Reactant r : model.getReactantCollection()) {
				if (!r.isEnabled()) continue;
				for (String dataSeriesName : result.getReactantIds()) {
					if (!r.getName().trim().equals("")  //This check in particular also helps avoiding that nodes with a name like " " are chosen for comparisons 
							&& !dataSeriesName.toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX)
							&& !reactantToDataCorrespondence.containsKey(r) //I also make sure that each reactant has no more than one association
							&& !reactantToDataCorrespondence.containsValue(dataSeriesName) //(and the same must hold for the data series)
							&& dataSeriesName.contains(r.getName())) { //If I can find the name of a "selected" reactant among the ones in the data file, I associate them for the comparison
						reactantToDataCorrespondence.put(r, dataSeriesName);
					}
				}
			}
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "All the reactants are \"hidden\", so all were seleted for comparison.\nNext time, please select some reactants for graph plots:\nright-click on a node --> " + Animo.APP_NAME + " --> Plot/hide", "No plotted reactants", JOptionPane.ERROR_MESSAGE);
		}
		if (onlyDisabled) {
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "All the reactants are disabled, I cannot do anything.\nPlease, enable some reactants for the simulations:\nright-click on a node --> " + Animo.APP_NAME + " --> Enable/disable", "No enabled reactants", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (reactantToDataCorrespondence.isEmpty() //If I have no associations between my reactants (possibly only the plotted ones, if at least one of the enabled reactants is also marked as plotted) and the data series in the csv file, I must ask the user which are the associations they want
			|| reactantToDataCorrespondence.keySet().size() != reactantToDataCorrespondence.values().size()) { //I also check that we have exactly as many series from the model as from the data file, otherwise the comparison between matrices with different size will not be feasible
			//TODO ask the user for the associations my reactants --> data series
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "At the moment, only automatic detection of node --> experimental data series\nis implemented. We recommend to rename the columns of the .csv\nfile so that the exp. data to be associated to node A\nis called \"A data\".", "No node --> data detected", JOptionPane.ERROR_MESSAGE);
			return;
		}
		//TODO it would be nice to show the automatically selected associations, in case the user wants to change them, for example by including (non-plotted) reactants or changing something that was incorrectly guessed
		

		Properties parameters = new Properties();
		//TODO: the parameters should be selected/changed by the user
		//TODO: And maybe find a better way to get a useful value for DELTA
		parameters.setProperty(LevenbergMarquardtFitter.MIN_COST_KEY, "0.5"); //This is actually not used anymore
		parameters.setProperty(LevenbergMarquardtFitter.DELTA_KEY, "" + (minParameter / 10)); //"0.0001");
		final LevenbergMarquardtFitter fitter = new LevenbergMarquardtFitter(model, reactionsToBeOptimized, referenceDataFile, reactantToDataCorrespondence, timeTo, parameters);
		ParameterFitterObserver observer = new ParameterFitterObserver() {
			@Override
			public void notifyDone() {
				if (fitter.getKeepResult()) { //If the user chose to keep the result, we set the parameters following the list we have in reactionsToBeOptimized
					Map<Reaction, Map<String, Double>> reactionParameters = fitter.getReactionParameters();
					for (Reaction r : reactionParameters.keySet()) {
						long edgeId = r.get(Model.Properties.CYTOSCAPE_ID).as(Long.class);
						CyEdge edge = currentNetwork.getEdge(edgeId);
						if (edge == null) {
							System.err.println("Edge with ID " + edgeId + " not existing?!?");
							continue;
						}
						CyRow edgeRow = currentNetwork.getRow(edge);
						Map<String, Double> params = reactionParameters.get(r);
						for (String param : params.keySet()) {
							edgeRow.set(param, params.get(param));
						}
					}
					JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "All parameters were correctly set.", "Parameter optimization complete", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "No parameter was changed", "Parameter optimization cancelled", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		};
		fitter.registerObserver(observer);
		//TODO to do things properly, this operation should be done with a Task
		fitter.performParameterFitting();
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		this.edgeView = edgeView;
		this.netView = netView;
		this.network = netView.getModel();
		JMenuItem menuItem = new JMenuItem("Optimize k value(s)");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 2.5f);
		return cyMenuItem;
	}
	
	
}
