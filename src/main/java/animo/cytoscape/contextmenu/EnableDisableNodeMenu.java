package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.model.Model;

public class EnableDisableNodeMenu implements CyNodeViewContextMenuFactory, ActionListener {
	CyNetwork network;
	View<CyNode> nodeView;

	@Override
	public void actionPerformed(ActionEvent e) {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, "selected", true);
		if (selectedNodes.isEmpty()) { //We can enable/disable a set of nodes or only the node on which the right-click action was performed. We are in the second case here.
			CyNode node = nodeView.getModel();
			CyRow row = network.getRow(node);
			boolean status;
			if (!row.isSet(Model.Properties.ENABLED)) {
				status = false;
			} else {
				status = !row.get(Model.Properties.ENABLED, Boolean.class);
			}
			row.set(Model.Properties.ENABLED, status);
//			for (CyEdge edge : network.getAdjacentEdgeList(node, CyEdge.Type.ANY)) { //Any incoming or outgoing edges get the same enabled state as the current node
//				CyRow edgeRow = network.getRow(edge);
//				edgeRow.set(Model.Properties.ENABLED, status);
//			}
		} else { //Enable/disable all selected nodes
			for (CyNode node : selectedNodes) {
				CyRow row = network.getRow(node);
				boolean status;
				if (!row.isSet(Model.Properties.ENABLED)) {
					status = false;
				} else {
					status = !row.get(Model.Properties.ENABLED, Boolean.class);
				}
				row.set(Model.Properties.ENABLED, status);
				for (CyEdge edge : network.getAdjacentEdgeList(node, CyEdge.Type.ANY)) { //Any incoming or outgoing edges get the same enabled state as the current node
					CyRow edgeRow = network.getRow(edge);
					edgeRow.set(Model.Properties.ENABLED, status);
				}
			}
		}
		//If one of the two extremities of an edge is disabled, also the edge gets disabled
		for (CyEdge edge : network.getEdgeList()) {
			 CyNode source = (CyNode)edge.getSource(),
					target = (CyNode)edge.getTarget();
			 CyRow rowSource = network.getRow(source),
				   rowTarget = network.getRow(target);
			 if ((rowSource.isSet(Model.Properties.ENABLED) && !rowSource.get(Model.Properties.ENABLED, Boolean.class))
				|| (rowTarget.isSet(Model.Properties.ENABLED) && !rowTarget.get(Model.Properties.ENABLED, Boolean.class))) {
				 
				 CyRow edgeRow = network.getRow(edge);
				 edgeRow.set(Model.Properties.ENABLED, false);
			 }
		}
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
		JMenuItem menuItem = new JMenuItem("Enable/Disable");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 2);

		this.network = netView.getModel();
		this.nodeView = nodeView;
		return cyMenuItem;

	}
}
