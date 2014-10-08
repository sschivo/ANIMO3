package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.model.Model;

public class EnableDisableEdgeViewContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {
	CyNetwork Network;

	CyNetworkView NetView;

	View<CyEdge> EdgeView;

	@Override
	public void actionPerformed(ActionEvent e) {
		CyNetworkView view = NetView;

		for (@SuppressWarnings("unchecked")
		Iterator<View<CyEdge>> i = view.getEdgeViews().iterator(); i.hasNext();) {
			View<CyEdge> eView = i.next();
			CyEdge edge = eView.getModel();
			CyRow row = Network.getRow(edge);
			boolean status;
			if (!row.isSet(Model.Properties.ENABLED)) {

				status = false;
			} else {
				status = !row.get(Model.Properties.ENABLED, Boolean.class);
			}
			row.set(Model.Properties.ENABLED, status);
		}
		// TODO: Ik denk dat dit overbodig is, maar misschien ook niet, (ZWET)

		// if (view.getSelectedEdges().isEmpty())
		// { //if the user wanted to change only one edge (i.e. right click on an edge without first selecting one), here we go
		// Edge edge = edgeView.getEdge();
		// boolean status;
		// if (!edgeAttr.hasAttribute(edge.getIdentifier(), Model.Properties.ENABLED))
		// {
		// status = false;
		// }
		// else
		// {
		// status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), Model.Properties.ENABLED);
		// }
		// edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
		// }

	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		EdgeView = edgeView;
		NetView = netView;
		Network = NetView.getModel();
		JMenuItem menuItem = new JMenuItem("Enable/disable");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);
		return cyMenuItem;
	}

}
