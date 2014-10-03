package animo.core.cytoscape;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

public class ResultPanelContainer extends JPanel implements CytoPanelComponent
{

    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * 
     */
    private static final long serialVersionUID = -255471556831642543L;

    public ResultPanelContainer()
    {
        this.setLayout(new BorderLayout());
        this.add(tabbedPane);
    }

    public void addTab(JPanel panel)
    {
        tabbedPane.add(panel);
    }

    @Override
    public Component getComponent()
    {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName()
    {
        // TODO Auto-generated method stub

        return CytoPanelName.EAST;
    }

    @Override
    public Icon getIcon()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return "ANIMO results";
    }

    public void removeTab(JPanel panel)
    {
        tabbedPane.remove(panel);
    }

}
