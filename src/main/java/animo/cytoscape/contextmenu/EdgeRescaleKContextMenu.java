package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

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

import animo.core.model.Model;
import animo.core.model.Scenario;
import animo.cytoscape.Animo;
import animo.cytoscape.EdgeDialog;

public class EdgeRescaleKContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {

	CyNetwork network;

	CyNetworkView netView;

	View<CyEdge> edgeView;

	private double rescaleEdge(CyRow edgeRow, double scaleFactor) {
		double k;
		if (edgeRow.isSet(Model.Properties.SCENARIO_PARAMETER_K)) {
			k = edgeRow.get(Model.Properties.SCENARIO_PARAMETER_K, Double.class);
		} else {
			int scenarioIdx = 0;
			if (edgeRow.isSet(Model.Properties.SCENARIO)) {
				scenarioIdx = edgeRow.get(Model.Properties.SCENARIO, Integer.class);
				if (scenarioIdx < 0 && scenarioIdx >= Scenario.THREE_SCENARIOS.length) {
					scenarioIdx = 0;
					edgeRow.set(Model.Properties.SCENARIO, scenarioIdx);
				}
			} else {
				edgeRow.set(Model.Properties.SCENARIO, scenarioIdx);
			}
			Scenario scenario = Scenario.THREE_SCENARIOS[scenarioIdx];
			k = scenario.getDefaultParameterValue(Model.Properties.SCENARIO_PARAMETER_K);
			edgeRow.set(Model.Properties.SCENARIO_PARAMETER_K, k);
		}
		k *= scaleFactor;
		edgeRow.set(Model.Properties.SCENARIO_PARAMETER_K, k);
		return k;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network, "selected", true);
		if (selectedEdges.isEmpty()) { //We can enable/disable a set of edges or only the edge on which the right-click action was performed. We are in the second case here.
			try {
				CyEdge edge = edgeView.getModel();
				CyRow edgeRow = network.getRow(edge);
				
				String edgeName = EdgeDialog.getEdgeName(network, edge);
				String resultingScaleS = null;
				Double resultingScale = null;
				do {
					resultingScaleS = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Rescale " + edgeName + " by a factor.\nE.g., with factor = 0.5 the reaction will be half as fast.", 1.0);
					if (resultingScaleS == null) return; //Cancel = cancel
					try {
						resultingScale = Double.parseDouble(resultingScaleS);

					} catch (Exception ex) {
						resultingScale = null;
					}
				} while (resultingScale == null);
				double k = rescaleEdge(edgeRow, resultingScale);
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "k value for " + edgeName + " rescaled by " + resultingScale + "\nNow k = " + k, "Rescale successful", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Could not rescale the k value.\nError: " + ex, "Error while rescaling", JOptionPane.ERROR_MESSAGE);
			}
		} else { //Enable/disable all selected edges
			try {
				String resultingScaleS = null;
				Double resultingScale = null;
				do {
					resultingScaleS = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Rescale all selected edges by a factor.\nE.g., with factor = 0.5 the reactions will be half as fast.", 1.0);
					if (resultingScaleS == null) return; //Cancel = cancel
					try {
						resultingScale = Double.parseDouble(resultingScaleS);
					} catch (Exception ex) {
						resultingScale = null;
					}
				} while (resultingScale == null);
				for (CyEdge edge : selectedEdges) {
					CyRow edgeRow = network.getRow(edge);
					try {
						rescaleEdge(edgeRow, resultingScale);
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
						JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Could not rescale the k values for " + EdgeDialog.getEdgeName(network, edge) + ".\nError: " + ex, "Error while rescaling", JOptionPane.ERROR_MESSAGE);
					}
				}
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "All k values rescaled by " + resultingScale + ".", "Rescale successful", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Could not rescale the k values for the selected edges.\nError: " + ex, "Error while rescaling", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		this.edgeView = edgeView;
		this.netView = netView;
		this.network = netView.getModel();
		JMenuItem menuItem = new JMenuItem("Rescale k value(s)");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 2);
		return cyMenuItem;
	}

}
