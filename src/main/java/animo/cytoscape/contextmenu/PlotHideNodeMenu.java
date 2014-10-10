package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.model.Model;

public class PlotHideNodeMenu implements CyNodeViewContextMenuFactory, ActionListener {
	CyNetwork network;
	View<CyNode> nodeView;

	@Override
	public void actionPerformed(ActionEvent e) {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, "selected", true);
		if (selectedNodes.isEmpty()) { //We can mark as plotted/hidden a set of nodes or only the node on which the right-click action was performed. We are in the second case here.
			CyNode node = nodeView.getModel();
			CyRow row = network.getRow(node);
			boolean status;
			if (!row.isSet(Model.Properties.PLOTTED)) {
				status = false;
			} else {
				status = !row.get(Model.Properties.PLOTTED, Boolean.class);
			}
			row.set(Model.Properties.PLOTTED, status);
		} else { //Mark as plotted/hidden all selected nodes
			for (CyNode node : selectedNodes) {
				CyRow row = network.getRow(node);
				boolean status;
				if (!row.isSet(Model.Properties.PLOTTED)) {
					status = false;
				} else {
					status = !row.get(Model.Properties.PLOTTED, Boolean.class);
				}
				row.set(Model.Properties.PLOTTED, status);
			}
		}
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
		JMenuItem menuItem = new JMenuItem("Plot/hide");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);

		this.network = netView.getModel();
		this.nodeView = nodeView;
		return cyMenuItem;

	}
}
