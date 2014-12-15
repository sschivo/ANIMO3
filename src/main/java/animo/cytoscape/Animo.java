package animo.cytoscape;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

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
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.events.VisualStyleChangedListener;
import org.cytoscape.view.vizmap.events.VisualStyleSetListener;
import org.cytoscape.work.ServiceProperties;
import org.osgi.framework.BundleContext;

import animo.core.AnimoBackend;
import animo.cytoscape.contextmenu.EdgeDoubleClickTaskFactory;
import animo.cytoscape.contextmenu.EdgeOptimizeKContextMenu;
import animo.cytoscape.contextmenu.EdgeRescaleKContextMenu;
import animo.cytoscape.contextmenu.EditReactantNodeMenu;
import animo.cytoscape.contextmenu.EditReactionEdgeViewContextMenu;
import animo.cytoscape.contextmenu.EnableDisableEdgeViewContextMenu;
import animo.cytoscape.contextmenu.EnableDisableNodeMenu;
import animo.cytoscape.contextmenu.NodeDoubleClickTaskFactory;
import animo.cytoscape.contextmenu.PlotHideNodeMenu;
import animo.exceptions.AnimoException;
import animo.util.XmlConfiguration;

public class Animo extends AbstractCyActivator {
	public static final String APP_NAME = "ANIMO"; //We should absolutely AVOID to change this constant!
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

	public static boolean areWeTheDeveloper() {
		final XmlConfiguration configuration = AnimoBackend.get().configuration();
		String areWeTheDeveloperStr = configuration.get(XmlConfiguration.DEVELOPER_KEY);
		boolean areWeTheDeveloper = false;
		if (areWeTheDeveloperStr != null) {
			areWeTheDeveloper = Boolean.parseBoolean(areWeTheDeveloperStr);
		}
		return areWeTheDeveloper;
	}
	
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
			row.getTable().createColumn(columnName, type, false); //Simulazione da una rete ripristinata: Perche' mi dice che la colonna esiste gia'?? Ho appena controllato e non c'era!!

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
		//Listen to changes to the CyNetwork.NAME field for nodes and edges, to make sure that no duplicate names exist in the network. In case there are, reset to the old name and inform the user about the issue
		registerService(bc, eventListener, RowsSetListener.class, new Properties());

		registerService(bc, controlPanel, CytoPanelComponent.class, new Properties());

		EditReactantNodeMenu reactantmenu = new EditReactantNodeMenu();
		Properties props = new Properties();
		props.put(ServiceProperties.PREFERRED_MENU, APP_NAME); //"preferredMenu"
		props.put(ServiceProperties.MENU_GRAVITY, "3");
		registerAllServices(bc, reactantmenu, props);

		EnableDisableNodeMenu enabledisablenodemenu = new EnableDisableNodeMenu();
		Properties enabledisablenodeprops = new Properties();
		enabledisablenodeprops.put(ServiceProperties.PREFERRED_MENU, APP_NAME);
		enabledisablenodeprops.put(ServiceProperties.MENU_GRAVITY, "2");
		registerAllServices(bc, enabledisablenodemenu, enabledisablenodeprops);

		PlotHideNodeMenu plothidemenu = new PlotHideNodeMenu();
		Properties plothideprops = new Properties();
		plothideprops.put(ServiceProperties.PREFERRED_MENU, APP_NAME);
		plothideprops.put(ServiceProperties.MENU_GRAVITY, "1");
		registerAllServices(bc, plothidemenu, plothideprops);

		EditReactionEdgeViewContextMenu editreactionedge = new EditReactionEdgeViewContextMenu();
		Properties editreactionedgeprops = new Properties();
		editreactionedgeprops.put(ServiceProperties.PREFERRED_MENU, APP_NAME);
		editreactionedgeprops.put(ServiceProperties.MENU_GRAVITY, "3");
		registerAllServices(bc, editreactionedge, editreactionedgeprops);

		EnableDisableEdgeViewContextMenu enabledisableedge = new EnableDisableEdgeViewContextMenu();
		Properties enabledisableedgeprops = new Properties();
		enabledisableedgeprops.put(ServiceProperties.PREFERRED_MENU, APP_NAME);
		enabledisableedgeprops.put(ServiceProperties.MENU_GRAVITY, "1");
		registerAllServices(bc, enabledisableedge, enabledisableedgeprops);
		
		EdgeRescaleKContextMenu rescaleEdges = new EdgeRescaleKContextMenu();
		Properties rescaleEdgesProps = new Properties();
		rescaleEdgesProps.put(ServiceProperties.PREFERRED_MENU, APP_NAME);
		rescaleEdgesProps.put(ServiceProperties.MENU_GRAVITY, "2");
		registerAllServices(bc, rescaleEdges, rescaleEdgesProps);
		
		if (areWeTheDeveloper()) { //TODO Add this normally when it is well tested
			EdgeOptimizeKContextMenu optimizeEdges = new EdgeOptimizeKContextMenu();
			Properties optimizeEdgesProps = new Properties();
			optimizeEdgesProps.put(ServiceProperties.PREFERRED_MENU, APP_NAME);
			optimizeEdgesProps.put(ServiceProperties.MENU_GRAVITY, "2.5");
			registerAllServices(bc, optimizeEdges, optimizeEdgesProps);
		}
		
		//Register node and edge double click listeners to open the edit dialogs
		NodeViewTaskFactory nodeDblClkListener = new NodeDoubleClickTaskFactory();
		Properties doubleClickProperties = new Properties();
	    doubleClickProperties.setProperty(ServiceProperties.PREFERRED_ACTION, "OPEN"); //The "OPEN" action, for not very clearly specified reasons, corresponds to the double click
	    doubleClickProperties.setProperty(ServiceProperties.TITLE, "Edit reactant...");
	    registerService(bc,nodeDblClkListener,NodeViewTaskFactory.class, doubleClickProperties);
	    EdgeViewTaskFactory edgeDblClkListener = new EdgeDoubleClickTaskFactory();
	    registerService(bc,edgeDblClkListener,EdgeViewTaskFactory.class, doubleClickProperties);
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

		vsa = new VisualStyleAnimo(visualMappingManager, visualStyleFactory, vmFactoryContinuous, vmFactoryDiscrete, vmFactoryPassthrough, colorsLegend, shapesLegend);

	}

	@Override
	public void start(BundleContext bc) {
		cytoscape = getService(bc, CySwingApplication.class);
		cytoscapeapp = getService(bc, CyAppAdapter.class);
		cyServiceRegistrar = getService(bc, CyServiceRegistrar.class);
		//String folder = System.getProperty("user.home") + File.separatorChar + ".animo" + File.separatorChar;
		File animoConfigFolder = Animo.getCytoscapeApp().getCyApplicationConfiguration().getAppConfigurationDirectoryLocation(Animo.class);
		String file = Animo.APP_NAME + "_configuration.xml"; //"animo_configuration.xml";
		File configuration = new File(animoConfigFolder, file); //new File(folder + file);
		
		try {
			if (!animoConfigFolder.exists()) {
				animoConfigFolder.mkdirs();
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(getCytoscape().getJFrame(), "Couldn't create the configuration directory " + animoConfigFolder.getAbsolutePath(), "Cannot create directory", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace(System.err);
		}
		try {
			if (!configuration.exists()) {
				configuration.createNewFile();
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(getCytoscape().getJFrame(), "Couldn't write the file " + configuration.getAbsolutePath(), "Cannot create file", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace(System.err);
		}
		try {
			AnimoBackend.initialise(configuration);
		} catch (AnimoException e) {
			JOptionPane.showMessageDialog(getCytoscape().getJFrame(), "Couldn't initialize ANIMO: " + e.getMessage(), "Cannot start ANIMO", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace(System.err);
		}
		hookListeners(bc);
		initVisuals(bc, cytoscapeapp.getCyApplicationManager());
	}

}
