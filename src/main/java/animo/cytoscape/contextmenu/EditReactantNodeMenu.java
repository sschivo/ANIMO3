package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.cytoscape.Animo;
import animo.cytoscape.NodeDialog;

public class EditReactantNodeMenu implements CyNodeViewContextMenuFactory, ActionListener {
	CyNetwork network;
	CyNetworkView netView;
	View<CyNode> nodeView;

	@Override
	public void actionPerformed(ActionEvent e) {
		//List<CyNode> nodelist = CyTableUtil.getNodesInState(network, "selected", true);
		//CyNode node = nodelist.get(0);
		CyNode node = nodeView.getModel();
		NodeDialog dialog = new NodeDialog(network, node);
		dialog.pack();
		dialog.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
		dialog.setVisible(true);

	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
		JMenuItem menuItem = new JMenuItem("Edit reactant...");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);

		this.netView = netView;

		this.nodeView = nodeView;
		this.network = netView.getModel();
		return cyMenuItem;

	}
}
