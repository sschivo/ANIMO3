package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.model.Model;
import animo.cytoscape.Animo;
import animo.cytoscape.NodeDialog;
import animo.cytoscape.OptionPaneSlider;

public class NodeRescaleNLevelsContextMenu implements CyNodeViewContextMenuFactory, ActionListener {

	private View<CyNode> nodeView;
	private CyNetwork network;
	
	private void rescaleNLevels(CyRow nodeRow, int newNLevels) {
		int currentActivity = nodeRow.get(Model.Properties.INITIAL_LEVEL, Integer.class),
			currentNLevels = nodeRow.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
		currentActivity = (int)Math.round(1.0 * currentActivity / currentNLevels * newNLevels);
		nodeRow.set(Model.Properties.NUMBER_OF_LEVELS, newNLevels);
		nodeRow.set(Model.Properties.INITIAL_LEVEL, currentActivity);
		double activityLevel = 1.0 * currentActivity / newNLevels;
		nodeRow.set(Model.Properties.SHOWN_LEVEL, activityLevel);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, "selected", true);
		if (selectedNodes.isEmpty()) { //We can rescale a set of nodes or only the node on which the right-click action was performed. We are in the second case here.
			try {
				CyNode node = nodeView.getModel();
				CyRow nodeRow = network.getRow(node);
				
				String nodeName = nodeRow.get(Model.Properties.CANONICAL_NAME, String.class);
				int init = nodeRow.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
				Integer nLevels = OptionPaneSlider.showInputSliderDialog(Animo.getCytoscape().getJFrame(), "Change the number of levels of " + nodeName + ".", init, 1, 100, "Number of levels");
				if (nLevels == null) {
					return;
				}
				
				rescaleNLevels(nodeRow, nLevels);
				NodeDialog.tryNetworkViewUpdate();
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Number of levels of " + nodeName + " changed to " + nLevels + ".\n", "Update successful", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Could not change the number of levels.\nError: " + ex, "Error while updating", JOptionPane.ERROR_MESSAGE);
			}
		} else { //Rescale all selected nodes
			try {
				int currentMaxNLevels = 1;
				for (CyNode n : selectedNodes) {
					int nLevels = network.getRow(n).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
					if (nLevels > currentMaxNLevels) {
						currentMaxNLevels = nLevels;
					}
				}
				Integer nLevels = OptionPaneSlider.showInputSliderDialog(Animo.getCytoscape().getJFrame(), "Change the number of levels of all selected nodes.", currentMaxNLevels, 1, 100, "Number of levels");
				if (nLevels == null) {
					return; //Cancel = cancel
				}
				for (CyNode node : selectedNodes) {
					CyRow nodeRow = network.getRow(node);
					try {
						rescaleNLevels(nodeRow, nLevels); 
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
						JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Could not change the number of levels for node " + nodeRow.get(Model.Properties.CANONICAL_NAME, String.class) + ".\nError: " + ex, "Error while updating", JOptionPane.ERROR_MESSAGE);
					}
				}
				
				NodeDialog.tryNetworkViewUpdate();
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "All levels changed successfully to " + nLevels + ".", "Update successful", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Could not change the number of levels for the selected nodes.\nError: " + ex, "Error while updating", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
		this.nodeView = nodeView;
		this.network = netView.getModel();
		JMenuItem menuItem = new JMenuItem("Change number of levels");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 2.6f);
		return cyMenuItem;
	}

}
