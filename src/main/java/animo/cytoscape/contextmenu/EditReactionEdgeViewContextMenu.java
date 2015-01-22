package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.cytoscape.EdgeDialog;

public class EditReactionEdgeViewContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {
	CyNetwork network;
	View<CyEdge> edgeView;
	
	@Override
	public void actionPerformed(ActionEvent e) {
		CyEdge edge = edgeView.getModel();
		EdgeDialog dialog = new EdgeDialog(network, edge);
		dialog.showYourself();
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		JMenuItem menuItem = new JMenuItem("Edit interaction...");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 3);
		this.edgeView = edgeView;
		this.network = netView.getModel();
		return cyMenuItem;

	}

}
