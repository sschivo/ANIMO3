package animo.cytoscape;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.events.VisualStyleChangedListener;
import org.cytoscape.view.vizmap.events.VisualStyleSetListener;
import org.osgi.framework.BundleContext;

import animo.core.AnimoBackend;
import animo.cytoscape.contextmenu.EditReactantNodeMenu;
import animo.cytoscape.contextmenu.EditReactionEdgeViewContextMenu;
import animo.cytoscape.contextmenu.EnableDisableEdgeViewContextMenu;
import animo.cytoscape.contextmenu.EnableDisableNodeMenu;
import animo.cytoscape.contextmenu.PlotHideNodeMenu;
import animo.exceptions.AnimoException;

public class Animo extends AbstractCyActivator {
	private static CySwingApplication cytoscape;
	private static CyAppAdapter cytoscapeapp;
	private static CyServiceRegistrar cyServiceRegistrar;
	// private static AbstractVisualMappingFunction mappingfunction;
	private static EventListener eventListener;
//	private static ControlPanelContainer controlPanelContainer;
	private static ControlPanel controlPanel;
	private static ColorsLegend colorsLegend;
	private static ShapesLegend shapesLegend;
//	private static ResultPanelContainer resultPanelContainer;
	private static VisualStyleAnimo vsa;

//	public static ControlPanelContainer getControlPanelContainer() {
//		return controlPanelContainer;
//	}
//	public static ControlPanel getControlPanel() {
//		return controlPanel;
//	}

	public static CySwingApplication getCytoscape() {
		return cytoscape;
	}

	public static CyAppAdapter getCytoscapeApp() {
		return cytoscapeapp;
	}

	public static CyServiceRegistrar getCyServiceRegistrar() {
		return cyServiceRegistrar;
	}

	public static EventListener getEventListener() {
		return eventListener;
	}

//	public static ResultPanelContainer getResultPanelContainer() {
//		return resultPanelContainer;
//	}

	public static void addResultPanel(CytoPanelComponent resultPanel) {
		cyServiceRegistrar.registerService(resultPanel, CytoPanelComponent.class, new Properties());
	}

	public static void removeResultPanel(CytoPanelComponent resultPanel) {
		cyServiceRegistrar.unregisterService(resultPanel, CytoPanelComponent.class);
	}

	public static VisualStyleAnimo getVSA() {
		return vsa;
	}
	
	public static void selectAnimoControlPanel() {
		CytoPanel cyControlPanel = Animo.getCytoscape().getCytoPanel(CytoPanelName.WEST);
		cyControlPanel.setSelectedIndex(cyControlPanel.indexOfComponent(controlPanel));
	}

	public static void setRowValue(CyRow row, String columnName, Class<?> type, Object value) {
		if (row.getTable().getColumn(columnName) == null) {
			row.getTable().createColumn(columnName, type, false);

		}
		row.set(columnName, value);

	}

	private void hookListeners(BundleContext bc) {
		eventListener = new EventListener();
//		resultPanelContainer = new ResultPanelContainer();

		//controlPanelContainer = new ControlPanelContainer();
		controlPanel = new ControlPanel();
		colorsLegend = controlPanel.getColorsLegend();
		shapesLegend = controlPanel.getShapesLegend();
		//controlPanelContainer.setControlPanel(controlPanel);
		
		//When an edge has been added, show the edge editor dialog
		registerService(bc, eventListener, AddedEdgesListener.class, new Properties());
		//When a node has been added, show the node editor dialog
		registerService(bc, eventListener, AddedNodesListener.class, new Properties());
		//Save the simulation files
		registerService(bc, eventListener, SessionAboutToBeSavedListener.class, new Properties());
		//Load the needed simulation files from the newly-loaded session, and show the ANIMO panel by default
		registerService(bc, eventListener, SessionLoadedListener.class, new Properties());
		//Apply our visual style to any network view
		registerService(bc, eventListener, NetworkAddedListener.class, new Properties());
		registerService(bc, eventListener, NetworkViewAddedListener.class, new Properties());
		//Update the colors and shapes legends if the visual style has changed
		registerService(bc, eventListener, VisualStyleChangedListener.class, new Properties());
		registerService(bc, eventListener, VisualStyleSetListener.class, new Properties());
		//Make sure that the Results Panel always has the proper width
		registerService(bc, eventListener, CytoPanelComponentSelectedListener.class, new Properties());

		registerService(bc, controlPanel, CytoPanelComponent.class, new Properties());

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

	//Create the personalized visual mappings for ANIMO to be used for all network views
	private void initVisuals(BundleContext bc, CyApplicationManager cyAppManager) {
	
		VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
		VisualStyleFactory visualStyleFactory = getService(bc, VisualStyleFactory.class);

		VisualMappingFunctionFactory vmFactoryContinuous = getService(bc, VisualMappingFunctionFactory.class,
				"(mapping.type=continuous)");
		VisualMappingFunctionFactory vmFactoryDiscrete = getService(bc, VisualMappingFunctionFactory.class,
				"(mapping.type=discrete)");
		VisualMappingFunctionFactory vmFactoryPassthrough = getService(bc, VisualMappingFunctionFactory.class,
				"(mapping.type=passthrough)");

		vsa = new VisualStyleAnimo(visualMappingManager, visualStyleFactory, vmFactoryContinuous, vmFactoryDiscrete, vmFactoryPassthrough, colorsLegend, shapesLegend, cyAppManager);

	}

	@Override
	public void start(BundleContext bc) {
		cytoscape = getService(bc, CySwingApplication.class);
		cytoscapeapp = getService(bc, CyAppAdapter.class);
		cyServiceRegistrar = getService(bc, CyServiceRegistrar.class);
		/*
		 * TODO: Wordt nergens gebruikt, gaat wel op zijn plaat
		mappingfunction = getService(bc, AbstractVisualMappingFunction.class);
		 */
		String folder = System.getProperty("user.home") + File.separatorChar + ".animo" + File.separatorChar;
		String file = "animo_configuration.xml";
		File configuration = new File(folder + file);

		try {
			if (!configuration.exists()) {
				new File(folder).mkdirs();

				configuration.createNewFile();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			AnimoBackend.initialise(configuration);
		} catch (AnimoException e) {

		}
		hookListeners(bc);
		initVisuals(bc, cytoscapeapp.getCyApplicationManager());
	}

	// public static AbstractVisualMappingFunction getMappingFunction()
	// {
	// return mappingfunction;

	// }
}
