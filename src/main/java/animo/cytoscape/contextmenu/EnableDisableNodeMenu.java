package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.model.Model;

public class EnableDisableNodeMenu implements CyNodeViewContextMenuFactory, ActionListener {
	CyNetwork network;

	CyNetworkView netView;
	View<CyNode> nodeView;

	@Override
	public void actionPerformed(ActionEvent e) {
		// CyNetwork network = Cytoscape.getCurrentNetwork();
		CyNetworkView view = netView;

		// CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
		for (@SuppressWarnings("unchecked")
		Iterator<View<CyNode>> i = view.getNodeViews().iterator(); i.hasNext();) {

			View<CyNode> nView = i.next();
			CyNode node = nView.getModel();
			CyRow row = network.getRow(node);
			boolean status;
			if (!row.isSet(Model.Properties.ENABLED)) {

				status = false;
			} else {
				status = !row.get(Model.Properties.ENABLED, Boolean.class);
			}
			row.set(Model.Properties.ENABLED, status);

		}
		// TODO: Ik denk dat dit overbodig is, maar misschien ook niet, (ZWET)

		// if (view. getSelectedNodes().isEmpty())
		// { //if the user wanted to change only one node (i.e. right click on a node without first selecting one), here we go
		// Node node = nodeView.getNode();
		// boolean status;
		// if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED))
		// {
		// status = false;
		// }
		// else
		// {
		// status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED);
		// }
		// nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.ENABLED, status);
		// /*int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
		// for (int edgeIdx : adjacentEdges) {
		// CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
		// edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
		// }*/
		// }
		// /*//In order to keep the model consistent, we disable all edges coming from (or going into) disabled nodes
		// for (int i : network.getEdgeIndicesArray()) {
		// CyEdge edge = (CyEdge)view.getEdgeView(i).getEdge();
		// CyNode source = (CyNode)edge.getSource(),
		// target = (CyNode)edge.getTarget();
		// if ((nodeAttr.hasAttribute(source.getIdentifier(), Model.Properties.ENABLED) && !nodeAttr.getBooleanAttribute(source.getIdentifier(), Model.Properties.ENABLED))
		// || (nodeAttr.hasAttribute(target.getIdentifier(), Model.Properties.ENABLED) && !nodeAttr.getBooleanAttribute(target.getIdentifier(), Model.Properties.ENABLED))) {
		// edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, false);
		// }
		// }*/
		//

	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
		JMenuItem menuItem = new JMenuItem("Enable/Disable");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);

		this.netView = netView;
		this.network = this.netView.getModel();
		this.nodeView = nodeView;
		return cyMenuItem;

	}
}
