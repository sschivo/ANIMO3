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

public class PlotHideNodeMenu implements CyNodeViewContextMenuFactory, ActionListener
{
    CyNetwork network;

    CyNetworkView netView;
    View<CyNode> nodeView;

    @Override
    public void actionPerformed(ActionEvent e)
    {

        CyNetworkView view = netView;

        CyNetwork network = view.getModel();

        for (Iterator<View<CyNode>> i = view.getNodeViews().iterator(); i.hasNext();)
        {
            View<CyNode> nView = i.next();

            CyNode node = nView.getModel();
            CyRow row = network.getRow(node);
            boolean status;
            if (!row.isSet(Model.Properties.PLOTTED))
            {

                status = false;
            }
            else
            {
                status = !row.get(Model.Properties.PLOTTED, Boolean.class);
            }
            row.set(Model.Properties.PLOTTED, status);


        }
        // //TODO: Ik denk dat dit overbodig is, maar misschien ook niet, (ZWET)
        //        if (view.getSelectedNodes().isEmpty())
        //        { //if the user wanted to change only one node (i.e. right click on a node without first selecting one), here we go
        //            CyNode node = nodeView.getNode();
        //            boolean status;
        //            if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED))
        //            {
        //                status = false;
        //            }
        //            else
        //            {
        //                status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED);
        //            }
        //            nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.PLOTTED, status);
        //        }

    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView)
    {
        JMenuItem menuItem = new JMenuItem("Plot/hide");
        menuItem.addActionListener(this);
        CyMenuItem cyMenuItem = new CyMenuItem(menuItem, 0);

        this.netView = netView;
        this.network = this.netView.getModel();
        this.nodeView = nodeView;
        return cyMenuItem;

    }
}
