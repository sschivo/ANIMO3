package animo.inat.cytoscape;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.osgi.framework.BundleContext;

import animo.inat.InatBackend;
import animo.inat.cytoscape.contextmenu.EditReactantNodeMenu;
import animo.inat.cytoscape.contextmenu.EditReactionEdgeViewContextMenu;
import animo.inat.cytoscape.contextmenu.EnableDisableEdgeViewContextMenu;
import animo.inat.cytoscape.contextmenu.EnableDisableNodeMenu;
import animo.inat.cytoscape.contextmenu.PlotHideNodeMenu;
import animo.inat.exceptions.InatException;

public class Animo extends AbstractCyActivator
{
    private static CySwingApplication cytoscape;
    private static CyAppAdapter cytoscapeapp;
    //  private static AbstractVisualMappingFunction mappingfunction;
    private static EventListener eventListener;
    private static ControlPanelContainer controlPanelContainer;
    private static ResultPanelContainer resultPanelContainer;
    private static VisualStyleAnimo vsa;

    public static ControlPanelContainer getControlPanelContainer()
    {
        return controlPanelContainer;

    }

    public static CySwingApplication getCytoscape()
    {
        return cytoscape;
    }

    public static CyAppAdapter getCytoscapeApp()
    {
        return cytoscapeapp;
    }

    public static EventListener getEventListener()
    {
        return eventListener;
    }

    public static ResultPanelContainer getResultPanelContainer()
    {
        return resultPanelContainer;
    }

    public static VisualStyleAnimo getVSA()
    {
        return vsa;
    }

    public static void setRowValue(CyRow row, String columnName, Class<?> type, Object value)
    {
        if (row.getTable().getColumn(columnName) == null)
        {
            row.getTable().createColumn(columnName, type, false);

        }
        row.set(columnName, value);

    }

    private void hookListeners(BundleContext bc)
    {
        eventListener = new EventListener();
        resultPanelContainer = new ResultPanelContainer();

        controlPanelContainer = new ControlPanelContainer();
        ControlPanel cp = new ControlPanel();
        controlPanelContainer.setControlPanel(cp);

        registerService(bc, eventListener, AddedEdgesListener.class, new Properties());
        registerService(bc, eventListener, AddedNodesListener.class, new Properties());
        registerService(bc, eventListener, SessionAboutToBeSavedListener.class, new Properties());
        registerService(bc, eventListener, SessionLoadedListener.class, new Properties());
        registerService(bc, eventListener, NetworkViewAddedListener.class, new Properties());

        registerService(bc, resultPanelContainer, CytoPanelComponent.class, new Properties());

        registerService(bc, controlPanelContainer, CytoPanelComponent.class, new Properties());

        EditReactantNodeMenu reactantmenu = new EditReactantNodeMenu();
        Properties props = new Properties();
        props.put("preferredMenu", "Animo");
        registerAllServices(bc, reactantmenu, props);

        EnableDisableNodeMenu enabledisablenodemenu = new EnableDisableNodeMenu();
        Properties enabledisablenodeprops = new Properties();
        enabledisablenodeprops.put("preferredMenu", "Animo");
        registerAllServices(bc, enabledisablenodemenu, enabledisablenodeprops);


        PlotHideNodeMenu plothidemenu = new PlotHideNodeMenu();
        Properties plothideprops = new Properties();
        plothideprops.put("preferredMenu", "Animo");
        registerAllServices(bc, plothidemenu, plothideprops);


        EditReactionEdgeViewContextMenu editreactionedge = new EditReactionEdgeViewContextMenu();
        Properties editreactionedgeprops = new Properties();
        editreactionedgeprops.put("preferredMenu", "Animo");
        registerAllServices(bc, editreactionedge, editreactionedgeprops);


        EnableDisableEdgeViewContextMenu enabledisableedge = new EnableDisableEdgeViewContextMenu();
        Properties enabledisableedgeprops = new Properties();
        enabledisableedgeprops.put("preferredMenu", "Animo");
        registerAllServices(bc, enabledisableedge, enabledisableedgeprops);

    }

    private void initVisuals(BundleContext bc, CyApplicationManager cyAppManager)
    {
        // To get references to services in CyActivator class
        VisualMappingManager vmmServiceRef = getService(bc, VisualMappingManager.class);

        VisualStyleFactory visualStyleFactoryServiceRef = getService(bc, VisualStyleFactory.class);

        VisualMappingFunctionFactory vmfFactoryC = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
        VisualMappingFunctionFactory vmfFactoryD = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        VisualMappingFunctionFactory vmfFactoryP = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

        vsa = new VisualStyleAnimo(vmmServiceRef, visualStyleFactoryServiceRef, vmfFactoryC, vmfFactoryD, vmfFactoryP, cyAppManager);

    }

    @Override
    public void start(BundleContext bc)
    {
        cytoscape = getService(bc, CySwingApplication.class);
        cytoscapeapp = getService(bc, CyAppAdapter.class);
        /* TODO: Wordt nergens gebruikt, gaat wel op zijn plaat
        mappingfunction = getService(bc, AbstractVisualMappingFunction.class);
        */
        String folder = System.getProperty("user.home") + "/.animo/";
        String file = "animo_configuration.xml";
        File configuration = new File(folder + file);

        try
        {
            if (!configuration.exists())
            {
                new File(folder).mkdirs();

                configuration.createNewFile();
            }
        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try
        {
            InatBackend.initialise(configuration);
        }
        catch (InatException e)
        {


        }
        hookListeners(bc);
        initVisuals(bc, cytoscapeapp.getCyApplicationManager());
    }

    //  public static AbstractVisualMappingFunction getMappingFunction()
    //   {
    //     return mappingfunction;

    //  }
}
