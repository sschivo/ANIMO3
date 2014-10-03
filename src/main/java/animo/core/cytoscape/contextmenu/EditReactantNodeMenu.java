package animo.core.cytoscape.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.cytoscape.Animo;
import animo.core.cytoscape.NodeDialog;

public class EditReactantNodeMenu implements CyNodeViewContextMenuFactory, ActionListener
{
    CyNetwork network;
    CyNetworkView netView;
    View<CyNode> nodeView;

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // TODO Auto-generated method stub
        List<CyNode> nodelist = CyTableUtil.getNodesInState(network, "selected", true);
        CyNode node = nodelist.get(0);
        NodeDialog dialog = new NodeDialog(network, node);
        dialog.pack();
        dialog.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
        dialog.setVisible(true);

    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView)
    {
        JMenuItem menuItem = new JMenuItem("Edit reactant...");
        menuItem.addActionListener(this);
        CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);

        this.netView = netView;

        this.nodeView = nodeView;
        this.network = netView.getModel();
        return cyMenuItem;

    }
}
