package animo.cytoscape;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;


public class ControlPanelContainer extends JPanel implements CytoPanelComponent
{

    /**
     * 
     */
    private static final long serialVersionUID = -255471556831642543L;

    ControlPanel cp = null;

    @Override
    public Component getComponent()
    {
        return this;
    }


    @Override
    public CytoPanelName getCytoPanelName()
    {
        return CytoPanelName.WEST;
    }


    @Override
    public Icon getIcon()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return "ANIMO";
    }

    public void setChangePerSeconds(double d)
    {
        if (cp != null)
        {
            cp.setChangePerSeconds(d);
        }


    }

    public void setControlPanel(ControlPanel p)
    {
        if (cp != null)
        {
            this.remove(cp);
        }

        cp = p;
        this.add(cp);
        cp.setVisible(true);
    }

}