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

import animo.cytoscape.Animo;
import animo.cytoscape.EdgeDialog;

public class EditReactionEdgeViewContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {
	CyNetwork network;
	View<CyEdge> edgeView;
	
	@Override
	public void actionPerformed(ActionEvent e) {
		//CyEdge edge = CyTableUtil.getEdgesInState(network, "selected", true).get(0);
		CyEdge edge = edgeView.getModel();
		EdgeDialog dialog = new EdgeDialog(edge, network);
		dialog.pack();

		dialog.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
		dialog.setVisible(true);
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		JMenuItem menuItem = new JMenuItem("Edit reaction...");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);
		network = netView.getModel();
		return cyMenuItem;

	}

}
