package animo.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.model.Model;

public class EnableDisableEdgeViewContextMenu implements CyEdgeViewContextMenuFactory, ActionListener {
	CyNetwork network;

	CyNetworkView netView;

	View<CyEdge> edgeView;

	@Override
	public void actionPerformed(ActionEvent e) {
		/*CyNetworkView view = netView;
		for (Iterator<View<CyEdge>> i = view.getEdgeViews().iterator(); i.hasNext();) {
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
		}*/
		List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network, "selected", true);
		if (selectedEdges.isEmpty()) { //We can enable/disable a set of edges or only the edge on which the right-click action was performed. We are in the second case here.
			CyEdge edge = edgeView.getModel();
			CyRow row = network.getRow(edge);
			boolean status;
			if (!row.isSet(Model.Properties.ENABLED)) {
				status = false;
			} else {
				status = !row.get(Model.Properties.ENABLED, Boolean.class);
			}
			row.set(Model.Properties.ENABLED, status);
		} else { //Enable/disable all selected edges
			for (CyEdge edge : selectedEdges) {
				CyRow row = network.getRow(edge);
				boolean status;
				if (!row.isSet(Model.Properties.ENABLED)) {
					status = false;
				} else {
					status = !row.get(Model.Properties.ENABLED, Boolean.class);
				}
				row.set(Model.Properties.ENABLED, status);
			}
		}
	}

	@Override
	public CyMenuItem createMenuItem(CyNetworkView netView, View<CyEdge> edgeView) {
		this.edgeView = edgeView;
		this.netView = netView;
		this.network = netView.getModel();
		JMenuItem menuItem = new JMenuItem("Enable/disable");
		menuItem.addActionListener(this);
		CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);
		return cyMenuItem;
	}

}
