package animo.cytoscape;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyNetworkViewDesktopMgr;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.task.write.ExportNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import animo.core.analyser.uppaal.ResultAverager;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.graph.FileUtils;
import animo.core.graph.Graph;
import animo.core.graph.GraphScaleListener;
import animo.core.graph.Scale;
import animo.core.model.Model;
import animo.core.model.Reaction;
import animo.util.Heptuple;
import animo.util.Pair;

/**
 * The Inat result panel.
 * 
 * @author Brend Wanders
 */
public class AnimoResultPanel extends JPanel implements ChangeListener, GraphScaleListener {

	private static final long serialVersionUID = -163756255393221954L;

	private static String TAB_NAME = "animo";

	/**
	 * Do exactly the opposite of encodeObjectToBase64
	 * 
	 * @param encodedObject
	 *            The encoded object as a CDATA text
	 * @return The decoded object
	 * @throws Exception
	 */
	private static Object decodeObjectFromBase64(String encodedObject) throws Exception {
		byte[] decodedModel = DatatypeConverter.parseBase64Binary(encodedObject);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decodedModel));
		Object o = ois.readObject();
		ois.close();
		return o;
	}

	/**
	 * The model from which the results were obtained
	 */
	private final Model model;
	/**
	 * Contains the results to be shown in this panel
	 */
	private final SimpleLevelResult result;
	/**
	 * The panel on which all the components of this resultPanel are layed out (thus, not only the panel itself, but also the slider, buttons etc)
	 */
	private ResultPanelContainer container;
	/**
	 * The slider to allow the user to choose a moment in the simulation time, which will be reflected on the network window as node colors, indicating the corresponding reactant
	 * activity level.
	 */
	private JSlider slider;
	/**
	 * The time scale
	 */
	private double scale;
	/**
	 * the scale to translate a value of the slider (in the interval [0,1]) to the corresponding position in the graph
	 */
	private double minValueOnGraph;
	/**
	 * the scale to translate a value of the slider (in the interval [0,1]) to the corresponding position in the graph
	 */
	private double maxValueOnGraph;
	/**
	 * the scale to translate a value of the slider (in the interval [0,1]) to the corresponding value resulting from the UPPAAL trace
	 */
	private double scaleForConcentration;
	/**
	 * The network which generated the data we are displaying in this panel: we can use it to replace the network at the user's command
	 */
	private CyNetwork savedNetwork;
	/**
	 * The image of the saved network: we can use it to display as a tooltip in the "reset to these settings" button
	 */
	private BufferedImage savedNetworkImage;
	/**
	 * The list of nodes of the network from which this result came
	 */
	private List<CyNode> savedNodesList;
	/**
	 * The list of edges of the network from whcih this result came
	 */
	private List<CyEdge> savedEdgesList;
	/**
	 * The attributes of the nodes in the saved network
	 */
	private HashMap<CyNode, Map<String, Object>> savedNodeAttributes;
	/**
	 * The attributes of the edges in the saved network
	 */
	private HashMap<CyEdge, Map<String, Object>> savedEdgeAttributes;
	private static final String PROP_POSITION_X = "Position.X";
	private static final String PROP_POSITION_Y = "Position.Y";
	private static final String DEFAULT_TITLE = "ANIMO Results";
	/**
	 * The three strings here are for one button. Everybody normally shows START_DIFFERENCE. When the user presses on the button (differenceWith takes the value of this for the
	 * InatResultPanel where the button was pressed),
	 */
	private static final String START_DIFFERENCE = "Difference with...";
	/**
	 * every other panel shows the END_DIFFERENCE. If the user presses a button with END_DIFFERENCE, a new InatResultPanel is created with the difference between the data in
	 * differenceWith and the panel where END_DIFFERENCE was pressed. Then every panel goes back to START_DIFFERENCE.
	 */
	private static final String END_DIFFERENCE = "Difference with this";
	/**
	 * CANCEL_DIFFERENCE is shown as text of the button only on the panel whare START_DIFFERENCE was pressed. If CANCEL_DIFFERENCE is pressed, then no difference is computed and
	 * every panel simply goes back to START_DIFFERENCE
	 */
	private static final String CANCEL_DIFFERENCE = "Cancel difference";

	private HashMap<Long, Pair<Boolean, List<Long>>> convergingEdges = null;

	public static AnimoResultPanel loadFromSessionSimFile(File simulationDataFile) {
		Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<CyNode, Map<String, Object>>, HashMap<CyEdge, Map<String, Object>>> simulationData = loadSimulationData(
				simulationDataFile, false);
		AnimoResultPanel panel = new AnimoResultPanel(simulationData.first, simulationData.second,
				simulationData.third, simulationData.fourth, null);
		panel.savedNetwork = Animo.getCytoscapeApp().getCyNetworkManager()
				.getNetwork(Long.parseLong(simulationData.fifth));
		if (panel.savedNetwork != null) {
			// TODO Currently only tries the first element of the Collection
			CyNetworkView savedNetworkView = Animo.getCytoscapeApp().getCyNetworkViewManager()
					.getNetworkViews(panel.savedNetwork).iterator().next();
			if (savedNetworkView != null) { // Keep all saved networks hidden
				Animo.getCytoscapeApp().getCyNetworkViewManager().destroyNetworkView(savedNetworkView);
				CyNetworkView otherView = null;
				Collection<CyNetworkView> c = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViewSet();
				if (!c.isEmpty()) {
					otherView = c.iterator().next();
				}
				if (otherView != null) {
					Animo.getCytoscapeApp().getCyApplicationManager().setCurrentNetworkView(otherView);
				}
			}
		}
		panel.savedNetworkImage = null; // We cannot save BufferedImages to file as objects, as they are not serializable, so we will show no image of the network
		panel.savedNodeAttributes = simulationData.sixth;
		panel.savedEdgeAttributes = simulationData.seventh;
		allExistingPanels.add(panel);
		return panel;
	}

	private String title; // The title to show to the user
	private Graph g;
	private JButton differenceButton = null;
	private static AnimoResultPanel differenceWith = null;
	private static List<AnimoResultPanel> allExistingPanels = new ArrayList<AnimoResultPanel>();

	/**
	 * Encode the given object to Base64, so that it can be included in a CDATA section in an xml file
	 * 
	 * @param o
	 *            The object to be encoded
	 * @return The encoded object
	 * @throws Exception
	 *             Any exception that may be thrown by ByteArrayInputStream, ObjectOutputStream and Base64
	 */
	private static String encodeObjectToBase64(Object o) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.flush();
		String encodedObject = DatatypeConverter.printBase64Binary(baos.toByteArray());
		baos.close();
		return encodedObject;
	}

	/**
	 * Load the data needed for creating a InatResultPanel from a .sim file.
	 * 
	 * @param inputFile
	 *            The file name
	 * @param normalFile
	 *            True if we are loading from an user-chosen file and not a cytoscape-associated file. In the latter case, we need also to load the saved network id, image and
	 *            properties
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<CyNode, Map<String, Object>>, HashMap<CyEdge, Map<String, Object>>> loadSimulationData(
			File inputFile, boolean normalFile) {
		try {
			Model model = null;
			SimpleLevelResult result = null;
			Double scale = null;
			String title = null;
			String networkId = null;
			HashMap<CyNode, Map<String, Object>> nodeProperties = null;
			HashMap<CyEdge, Map<String, Object>> edgeProperties = null;
			if (normalFile) {
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
				model = (Model) in.readObject();
				result = (SimpleLevelResult) in.readObject();
				scale = in.readDouble();
				title = in.readObject().toString();
				in.close();
			} else {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
				doc.getDocumentElement().normalize();
				NodeList children = doc.getFirstChild().getChildNodes(); // The first child is unique and is the "root" node
				for (int i = 0; i < children.getLength(); i++) {
					Node n = children.item(i);
					String name = n.getNodeName();
					if (name.equals("model")) {
						model = (Model) decodeObjectFromBase64(n.getFirstChild().getTextContent());
					} else if (name.equals("result")) {
						result = (SimpleLevelResult) decodeObjectFromBase64(n.getFirstChild().getTextContent());
					} else if (name.equals("scale")) {
						scale = Double.parseDouble(n.getFirstChild().getTextContent());
					} else if (name.equals("title")) {
						title = n.getFirstChild().getTextContent();
					} else if (name.equals("networkId")) {
						networkId = n.getFirstChild().getTextContent();
					} else if (name.equals("nodeProperties")) {
						nodeProperties = (HashMap<CyNode, Map<String, Object>>) decodeObjectFromBase64(n
								.getFirstChild().getTextContent());
					} else if (name.equals("edgeProperties")) {
						edgeProperties = (HashMap<CyEdge, Map<String, Object>>) decodeObjectFromBase64(n
								.getFirstChild().getTextContent());
					}
				}
			}
			return new Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<CyNode, Map<String, Object>>, HashMap<CyEdge, Map<String, Object>>>(
					model, result, scale, title, networkId, nodeProperties, edgeProperties);
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), baos.toString(), "Error: " + ex,
					JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	private boolean isDifference = false;

	private int countSessionChanges = 0;

	private JButton resetToThisNetwork = null;

	private int lastWidth;

	private CytoPanel fCytoPanel;

	/**
	 * Load the simulation data from a file instead than getting it from a simulation we have just made
	 * 
	 * @param model
	 *            The representation of the current Cytoscape network, to which the simulation data will be coupled (it is REQUIRED that the simulation was made on the same
	 *            network, or else the node IDs will not be the same, and the slider will not work properly)
	 * @param simulationDataFile
	 *            The file from which to load the data. For the format, see saveSimulationData()
	 */
	public AnimoResultPanel(File simulationDataFile) {
		this(loadSimulationData(simulationDataFile, true));
	}

	public AnimoResultPanel(
			Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<CyNode, Map<String, Object>>, HashMap<CyEdge, Map<String, Object>>> simulationData) {
		this(simulationData.first, simulationData.second, simulationData.third, simulationData.fourth, null);
	}

	public AnimoResultPanel(Model model, SimpleLevelResult result, double scale, CyNetwork originalNetwork) {
		this(model, result, scale, DEFAULT_TITLE, originalNetwork);
	}

	/**
	 * The panel constructor.
	 * 
	 * @param model
	 *            the model this panel uses
	 * @param result
	 *            the results object this panel uses
	 */
	public AnimoResultPanel(final Model model, final SimpleLevelResult result, double scale, String title,
			CyNetwork originalNetwork) {
		super(new BorderLayout(), true);
		allExistingPanels.add(this);
		this.model = model;
		this.result = result;
		this.scale = scale;
		this.title = title;
		if (originalNetwork != null) {
			try {
				// TODO Currently only the first CyNetworkView is tested
				CyNetworkView view = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViews(originalNetwork)
						.iterator().next();
				List<View<CyNode>> hiddenNodes = new ArrayList<View<CyNode>>();
				for (View<CyNode> o : view.getNodeViews()) {
					o.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
					if (o.getVisualProperty(BasicVisualLexicon.NODE_WIDTH) < 0) {
						hiddenNodes.add(o);
					}
				}
				// at this point, all nodes are visible and nodesToBeReHiddenAfterDoneHere contains the indexes of those nodes which were hidden before we started. They will be
				// re-hidden after we have finished copying the needed things

				CyNetworkView currentNetworkView = Animo.getCytoscapeApp().getCyApplicationManager()
						.getCurrentNetworkView();

				try {
					Component componentToBeSaved = findInnerCanvas(Animo.getCytoscape().getJFrame()); // Animo.getCytoscape().getJFrame();// TODO: Vies lelijk enzo, ipv netwerk saved ie nu volledige window, moet eleganter kunnen, was:
																					// currentNetworkView.getComponent();
					savedNetworkImage = new BufferedImage(currentNetworkView.getVisualProperty(
							BasicVisualLexicon.NETWORK_WIDTH).intValue() / 2, currentNetworkView.getVisualProperty(
							BasicVisualLexicon.NETWORK_HEIGHT).intValue() / 2, BufferedImage.TYPE_INT_ARGB);
					Graphics2D graphic = savedNetworkImage.createGraphics();
					graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphic.scale(0.5, 0.5);
					//graphic.setPaint(Animo.getCytoscape().getJFrame().getBackground()); // Cytoscape.getVisualMappingManager().getVisualStyle().getGlobalAppearanceCalculator().getDefaultBackgroundColor());
					graphic.setPaint(Animo.getCytoscapeApp().getVisualMappingManager().getCurrentVisualStyle().getDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT));
					graphic.fillRect(0, 0, componentToBeSaved.getWidth(), componentToBeSaved.getHeight());
					componentToBeSaved.paint(graphic);
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
				}
				
				copyNetwork(originalNetwork);
				
				CyNetworkView originalView = Animo.getCytoscapeApp().getCyNetworkViewManager()
						.getNetworkViews(originalNetwork).iterator().next(); // TODO confirm there's only one network view
				Animo.getCytoscapeApp().getCyApplicationManager().setCurrentNetworkView(originalView);
				CytoPanel controlPanel = Animo.getCytoscape().getCytoPanel(CytoPanelName.WEST);
				controlPanel.setSelectedIndex(controlPanel.indexOfComponent(TAB_NAME));
				for (View<CyNode> n : hiddenNodes) { // re-hide the hidden nodes after finishing
					n.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
				}
				
				// TODO make focus
				// Animo.getCytoscape().getJFrame().getFrame(originalView).setSelected(true);
				((JInternalFrame)findComponentByName(Animo.getCytoscape().getJFrame(), "JInternalFrame")).setSelected(true);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Error while saving fall-back network:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace(System.err);
			}
		} else {
			this.savedNetwork = null;
			this.savedNetworkImage = null;
			this.savedNodeAttributes = null;
			this.savedEdgeAttributes = null;
		}

		JPanel sliderPanel = new JPanel(new BorderLayout());
		this.slider = new JSlider();
		this.slider.setOrientation(SwingConstants.HORIZONTAL);
		this.slider.setMinimum(0);
		if (result.isEmpty()) {
			this.scaleForConcentration = 1;
		} else {
			this.scaleForConcentration = result.getTimeIndices().get(result.getTimeIndices().size() - 1);
		}
		this.minValueOnGraph = 0;
		this.maxValueOnGraph = this.scaleForConcentration * this.scale;
		this.slider.setMaximum(200);
		this.slider.setPaintTicks(true);
		this.slider.setPaintLabels(true);
		slider.setMajorTickSpacing(slider.getMaximum() / 10);
		slider.setMinorTickSpacing(slider.getMaximum() / 100);

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		int nLabels = 10;
		for (int i = 0; i <= nLabels; i++) {
			double val = 1.0 * i / nLabels * this.maxValueOnGraph;
			String valStr = formatter.format(val);
			labels.put((int) Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
		}

		this.slider.setLabelTable(labels);
		this.slider.setValue(0);
		this.slider.getModel().addChangeListener(this);

		JButton setParameters;
		try {
			URL url = getClass().getResource("/copy20x20.png");
			setParameters = new JButton(new ImageIcon(url));
		} catch (Exception ex) {
			setParameters = new JButton("Copy");
		}

		setParameters
				.setToolTipText("Copy the currently shown activity levels as initial activity levels in the model");
		setParameters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// set the initial activity levels of the reactants in the network as they are in this point of the simulation ("this point" = where the slider is currently)
				double t = getSliderTime();
				CyNetwork network = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
				int nLevels = result.getNumberOfLevels();
				for (String r : result.getReactantIds()) {
					if (model.getReactant(r) == null)
						continue;
					final Long id = model.getReactant(r).get(Model.Properties.CYTOSCAPE_ID).as(Long.class);
					final double level = result.getConcentration(r, t) / nLevels * network.getRow(network.getNode(id)).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class); // model.getReactant(r).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
																						//We also
																						// rescale the value to the correct number of levels of each node. Attention: we need to use
																						// the CURRENT number of levels of the node, or we will get inconsistent results!
					Animo.setRowValue(network.getRow(network.getNode(id)),
							Model.Properties.INITIAL_LEVEL, Integer.class, (int)Math.round(level));
				}
			}
		});
		sliderPanel.add(setParameters, BorderLayout.WEST);
		
		if (Animo.areWeTheDeveloper()) {
			JButton animate;
			animate = new JButton("Animation");
			animate.setToolTipText("Make an animation of this time series");
			animate.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int nSteps = 10;
					int percentage = 100;
					String nStepsString = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Number of frames", nSteps);
					if (nStepsString == null) return;
					try {
						nSteps = Integer.parseInt(nStepsString);
					} catch (NumberFormatException ex) {
						nSteps = 10;
					}
					String percentageString = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Percentage dimension of image", percentage);
					if (percentageString == null) return;
					try {
						percentage = Integer.parseInt(percentageString);
					} catch (NumberFormatException ex) {
						percentage = 100;
					}
					
					String directory = ".";
					File currentDirectory = new File(directory);
					String currentSessionFileName = Animo.getCytoscapeApp().getCySessionManager().getCurrentSessionFileName();
					if (currentSessionFileName != null) {
						File curSession = new File(currentSessionFileName);
						if (curSession != null && curSession.exists()) {
							currentDirectory = curSession.getParentFile();
						}
					}
					JFileChooser chooser = new JFileChooser(currentDirectory);
					chooser.setFileFilter(new FileFilter() {
						public boolean accept(File pathName) {
							if (pathName.isDirectory()) {
								return true;
							}
							return false;
						}
	
						public String getDescription() {
							return "Directory";
						}
					});
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int result = chooser.showSaveDialog(Animo.getCytoscape().getJFrame());
					if (result == JFileChooser.APPROVE_OPTION) {
						currentDirectory = chooser.getCurrentDirectory();
						String fileName = chooser.getSelectedFile().getAbsolutePath();
						directory = fileName;
					}
					
					slider.setValue(slider.getMinimum());
					int delta = (int)Math.round(1.0 * (slider.getMaximum() - slider.getMinimum() + 1) / nSteps);
					int idx = 0;
					CyNetworkView currentNetworkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
	//				BitmapExporter be = new BitmapExporter("png", percentage / 100.0);
					ExportNetworkViewTaskFactory taskFactory = Animo.getCyServiceRegistrar().getService(ExportNetworkViewTaskFactory.class);
					@SuppressWarnings("rawtypes")
					SynchronousTaskManager tm = Animo.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
					for (int i = slider.getMinimum(); i <= slider.getMaximum(); i += delta, idx++) {
						slider.setValue(i);
	//					CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
	//					edgeAttrs.deleteAttribute(Model.Properties.SHOWN_LEVEL);
						currentNetworkView.updateView();
						try {
							//be.export(currentNetworkView, new FileOutputStream(directory + File.separator + "Frame" + String.format("%03d", idx) + ".png"));
							TaskIterator ti = taskFactory.createTaskIterator(currentNetworkView, new File(directory + File.separator + "Frame" + String.format("%03d", idx) + ".png"));
							tm.execute(ti);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});
			sliderPanel.add(animate, BorderLayout.EAST);
		}


		sliderPanel.add(this.slider, BorderLayout.CENTER);

		this.add(sliderPanel, BorderLayout.SOUTH);

		g = new Graph();
		// We map reactant IDs to their corresponding aliases (canonical names, i.e., the names displayed to the user in the network window), so that
		// we will be able to use graph series names consistent with what the user has chosen.
		Map<String, String> seriesNameMapping = new HashMap<String, String>();
		List<String> filteredSeriesNames = new ArrayList<String>(); // profit from the cycle for the series mapping to create a filter for the series to be actually plotted
		for (String r : result.getReactantIds()) {
			if (r.charAt(0) == 'E')
				continue; // If the identifier corresponds to an edge (e.g. "Enode123 (DefaultEdge) node456") we don't consider it, as we are looking only at series to be plotted
							// in the graph, and those data instead are used for the slider (edge highlighting corresponding to reaction "strength")
			String name = null;
			String stdDevReactantName = null;
			if (model.getReactant(r) != null) { // we can also refer to a name not present in the reactant collection
				name = model.getReactant(r).getName(); // if an alias (canonical name) is set, we prefer it
				if (name == null) {
					name = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class); //Otherwise we use the name field in Cytoscape, which could also make sense
				}
			} else if (r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase())) {
				stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.STD_DEV));
				if (model.getReactant(stdDevReactantName).getName() != null) {
					name = model.getReactant(stdDevReactantName).getName() + ResultAverager.STD_DEV;
				} else {
					name = r; // in this case, I simply don't know what we are talking about =)
				}
			} else if (r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())) {
				stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.OVERLAY_NAME));
				if (model.getReactant(stdDevReactantName).getName() != null) {
					name = model.getReactant(stdDevReactantName).getName();
					seriesNameMapping.put(stdDevReactantName, name);
				} else {
					name = r; // boh
				}
			}
			if ((!r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase())
					&& !r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())
					&& model.getReactant(r) != null && model.getReactant(r).get(Model.Properties.PLOTTED)
					.as(Boolean.class))
					|| ((r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) || r.toLowerCase().contains(
							ResultAverager.OVERLAY_NAME.toLowerCase()))
							&& model.getReactant(stdDevReactantName) != null && model.getReactant(stdDevReactantName)
							.get(Model.Properties.PLOTTED).as(Boolean.class))) {

				filteredSeriesNames.add(r);
			}
			seriesNameMapping.put(r, name);
		}
		g.parseLevelResult(result.filter(filteredSeriesNames), seriesNameMapping, scale); // Add all series to the graph, using the mapping we built here to "translate" the names
																							// into the user-defined ones.
		g.setXSeriesName("Time (min)");
		g.setYLabel("Protein activity (a. u.)");

		if (!model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).isNull()) { // if we find a maximum value for activity levels, we declare it to the graph, so that other
																						// added graphs (such as experimental data) will be automatically rescaled to match us
			int nLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			g.declareMaxYValue(nLevels);
			double maxTime = scale * result.getTimeIndices().get(result.getTimeIndices().size() - 1);
			g.setDrawArea(0, maxTime, 0, nLevels); // This is done because the graph automatically computes the area to be shown based on minimum and maximum values for X and Y,
													// including StdDev. So, if the StdDev of a particular series (which represents an average) in a particular point is larger that
													// the value of that series in that point, the minimum y value would be negative. As this is not very nice to see, I decided
													// that we will recenter the graph to more strict bounds instead.
													// Also, if the maximum value reached during the simulation is not the maximum activity level, the graph does not loook nice
		}
		this.add(g, BorderLayout.CENTER);
		g.addGraphScaleListener(this);
	}

	/**
	 * Add the InatResultPanel to the given Cytoscape panel
	 * 
	 * @param cytoPanel
	 */
	public void addToPanel(final CytoPanel cytoPanel) {
		container = new ResultPanelContainer(this);
		container.setLayout(new BorderLayout(2, 2));
		container.add(this, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout()); // new GridLayout(2, 2, 2, 2));

		resetToThisNetwork = null;
		if (savedNetwork != null) {
			resetToThisNetwork = new JButton(new AbstractAction("Reset to here") {
				private static final long serialVersionUID = 7265495749842776073L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (savedNetwork == null) {
						JOptionPane
								.showMessageDialog(Animo.getCytoscape().getJFrame(),
										"No network was actually saved, so no network will be restored.\n(Please report this as a bug)");
						return; // The button does nothing when it discovers that the session was changed and the network is not available anymore
					}

					if (savedNetwork.getNodeCount() < 1 || countSessionChanges > 1) { // If there are no nodes, I am not deleting the whole network to replace it with something
																						// useless
						resetToThisNetwork.setEnabled(false);
						return;
					}
					
					CyApplicationManager cyApplicationManager = Animo.getCytoscapeApp().getCyApplicationManager();
					CyNetwork net = cyApplicationManager.getCurrentNetwork();
					
					CyEventHelper eventHelper = Animo.getCyServiceRegistrar().getService(CyEventHelper.class);
					//eventHelper.silenceEventSource(net); //Just silence the event source so we don't open the dialogs when new nodes/edges are added
					EventListener.setListenerStatus(false);
					
					CyRootNetworkManager rootNetworkManager = Animo.getCyServiceRegistrar().getService(CyRootNetworkManager.class);
					CyNetworkViewManager netViewManager = Animo.getCyServiceRegistrar().getService(CyNetworkViewManager.class);
					CyNetworkView currentNetworkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
					CyNetworkViewDesktopMgr networkViewDesktopManager = Animo.getCyServiceRegistrar().getService(CyNetworkViewDesktopMgr.class);
					Rectangle windowBounds = networkViewDesktopManager.getBounds(currentNetworkView);
					Double scaleFactor = currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR),
						   netCenterX = currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION),
						   netCenterY = currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION),
						   netCenterZ = currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION);
					netViewManager.destroyNetworkView(currentNetworkView);
					if (net instanceof CySubNetwork && !net.equals(rootNetworkManager.getRootNetwork(savedNetwork).getBaseNetwork())) {
						if (Animo.getCytoscapeApp().getCyNetworkManager().networkExists(net.getSUID())) {
							//rootNetworkManager.getRootNetwork(savedNetwork).removeSubNetwork((CySubNetwork)net);
							Animo.getCyServiceRegistrar().getService(CyNetworkManager.class).destroyNetwork(net);
						}
					}
					CyNetworkViewFactory netViewFactory = Animo.getCyServiceRegistrar().getService(CyNetworkViewFactory.class);
					CyNetwork recoveredNetwork = rootNetworkManager.getRootNetwork(savedNetwork).addSubNetwork(savedNodesList, savedEdgesList);
					//Recover the network attributes
					try {
						Map<String, Object> savedRow = savedNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(savedNetwork.getSUID()).getAllValues();
						CyRow recoveredRow = recoveredNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(recoveredNetwork.getSUID());
						for (String k : savedRow.keySet()) {
							Object value = savedRow.get(k);
							Animo.setRowValue(recoveredRow, k, value.getClass(), value);
						}
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
					}
					recoveredNetwork.getRow(recoveredNetwork).set(CyNetwork.NAME, "Based on " + getTitle()); //The name is based on the current result panel title, so we set this property after copying all, otherwise it is overwritten
					Animo.getCyServiceRegistrar().getService(CyNetworkManager.class).addNetwork(recoveredNetwork);
					CyNetworkView recoveredNetView = netViewFactory.createNetworkView(recoveredNetwork);
					netViewManager.addNetworkView(recoveredNetView);
					networkViewDesktopManager.setBounds(recoveredNetView, windowBounds);
					recoveredNetView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scaleFactor);
					recoveredNetView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, netCenterX);
					recoveredNetView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, netCenterY);
					recoveredNetView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Z_LOCATION, netCenterZ);
					eventHelper.flushPayloadEvents();
					for (CyNode node : savedNodeAttributes.keySet()) {
						for (String k : savedNodeAttributes.get(node).keySet()) {
							Object val = savedNodeAttributes.get(node).get(k);
							if (val != null) {
								Animo.setRowValue(recoveredNetwork.getRow(node), k, val.getClass(), val);
							}
						}
					}
					for (View<CyNode> node : recoveredNetView.getNodeViews()) {
						node.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, (Double)savedNodeAttributes.get(node.getModel()).get(PROP_POSITION_X));
						node.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, (Double)savedNodeAttributes.get(node.getModel()).get(PROP_POSITION_Y));
					}				
					for (CyEdge edge : savedEdgeAttributes.keySet()) {
						for (String k : savedEdgeAttributes.get(edge).keySet()) {
							Object val = savedEdgeAttributes.get(edge).get(k);
							if (val != null) {
								Animo.setRowValue(recoveredNetwork.getRow(edge), k, val.getClass(), val);
							}
						}
					}
					EventListener.setListenerStatus(true);
					NodeDialog.tryNetworkViewUpdate();
				}
			});
			try {
				if (savedNetworkImage != null) {
					File tmpFile = File.createTempFile("ANIMOimg", ".png");
					tmpFile.deleteOnExit();
					ImageIO.write(savedNetworkImage, "png", tmpFile);
					resetToThisNetwork
							.setToolTipText("<html>Reset the network to the input that gave this result, i.e. this network:<br/><img src=\"file:"
									+ tmpFile.getCanonicalPath() + "\"/></html>");
				} else {
					throw new IOException(); // Just to do the same when we have no image as when the image has problems
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				resetToThisNetwork.setToolTipText("Reset the network to the input that gave this result");
			}
		}

		JButton close = new JButton(new AbstractAction("Close") {
			private static final long serialVersionUID = 4327349309742276633L;

			@Override
			public void actionPerformed(ActionEvent e) {
				closeResultsPanel(cytoPanel);
			}
		});

		JButton save = new JButton(new AbstractAction("Save simulation data...") {
			private static final long serialVersionUID = -2492923184151760584L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String fileName = FileUtils.save(".sim", "ANIMO simulation data", Animo.getCytoscape().getJFrame());
				if (fileName != null)
					saveSimulationData(new File(fileName), true);
			}
		});

		JButton changeTitle = new JButton(new AbstractAction("Change title") {
			private static final long serialVersionUID = 7093059357198172376L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String newTitle = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(),
						"Please give a new title", title);
				if (newTitle == null) {
					return;
				}
				setTitle(newTitle);
			}
		});

		differenceButton = new JButton(START_DIFFERENCE);
		differenceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				differenceButtonPressed(differenceButton.getText());
			}
		});

		buttons.add(changeTitle);
		if (resetToThisNetwork != null) {
			buttons.add(resetToThisNetwork);
		}
		buttons.add(differenceButton);
		if (!isDifference) { // The differences are not saved (for the moment)
			buttons.add(save);
		}
		buttons.add(close);
		container.add(buttons, BorderLayout.NORTH);

		if (cytoPanel.getState().equals(CytoPanelState.HIDE)) {
			cytoPanel.setState(CytoPanelState.DOCK); // We show the Results panel if it was hidden.
		}
		fCytoPanel = cytoPanel;

		container.setName(this.getTitle());
		// Animo.getResultPanelContainer().addTab(container);
		Animo.addResultPanel(container);
		resetDivider();
		ensureCorrectVisualStyle();
		//Not-so-nice way to tell the panel that I want the latest addition to be selected
		try {
			if (fCytoPanel instanceof Container) {
				Container c = (Container)fCytoPanel;
				for (Component comp : c.getComponents()) {
					if (comp instanceof JTabbedPane) {
						JTabbedPane pane = (JTabbedPane)comp;
						pane.setSelectedIndex(pane.getTabCount() - 1);
						break;
					}
				}
			}
		} catch (Exception ex) {
			
		}
	}

	public void closeResultsPanel(CytoPanel cytoPanel) {
		if (savedNetwork != null && countSessionChanges <= 1) { // We destroy network & vizmap only if we are still in "our" session. Otherwise, we would risk destroying a network
																// from another session
			try {
				if (Animo.getCytoscapeApp().getCyNetworkManager().networkExists(savedNetwork.getSUID()))
					Animo.getCytoscapeApp().getCyNetworkManager().destroyNetwork(savedNetwork);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		// Animo.getResultPanelContainer().removeTab(container);
		Animo.removeResultPanel(container);
		allExistingPanels.remove(this);
	}
	
	private Component findInnerCanvas(Container c) {
		return findComponentByName(c, "InnerCanvas");
	}

	private Component findComponentByName(Container c, String className) {
		for (Component child : c.getComponents()) {
			if (child.getClass().getName().endsWith(className)) {
				return child;
			} else if (child instanceof Container) {
				Component result = findComponentByName((Container)child, className);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	protected void copyNetwork(CyNetwork originalNetwork) {
		savedNodesList = originalNetwork.getNodeList();
		savedEdgesList = originalNetwork.getEdgeList();
		savedNodeAttributes = new HashMap<CyNode, Map<String, Object>>();
		savedEdgeAttributes = new HashMap<CyEdge, Map<String, Object>>();
		CyNetworkView netView = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViews(originalNetwork).iterator().next();
		for (CyNode node : savedNodesList) {
			CyRow nodeRow = originalNetwork.getRow(node);
			Map<String, Object> attributes = new HashMap<String, Object>();
			for (String k : nodeRow.getAllValues().keySet()) {
				Object val = nodeRow.getRaw(k);
				attributes.put(k, val);
			}
			attributes.put(PROP_POSITION_X, netView.getNodeView(node).getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
			attributes.put(PROP_POSITION_Y, netView.getNodeView(node).getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));
			savedNodeAttributes.put(node, attributes);
		}
		for (CyEdge edge : savedEdgesList) {
			CyRow edgeRow = originalNetwork.getRow(edge);
			Map<String, Object> attributes = new HashMap<String, Object>();
			for (String k : edgeRow.getAllValues().keySet()) {
				Object val = edgeRow.getRaw(k);
				attributes.put(k, val);
			}
			savedEdgeAttributes.put(edge, attributes);
		}
		savedNetwork = Animo.getCyServiceRegistrar().getService(CyRootNetworkManager.class).getRootNetwork(originalNetwork).addSubNetwork(savedNodesList, savedEdgesList);
		savedNetwork.getRow(savedNetwork).set(CyNetwork.NAME, this.getTitle() + " - Network");
		Animo.getCyServiceRegistrar().getService(CyNetworkManager.class).addNetwork(savedNetwork);
		CyTable originalTable = originalNetwork.getTable(CyNetwork.class, CyRootNetwork.LOCAL_ATTRS);
		CyRow originalRow = originalTable.getRow(originalNetwork.getSUID());
		Map<String, Object> originalRowValues = originalRow.getAllValues(); //Copy also the current network properties
		CyRow savedRow = savedNetwork.getTable(CyNetwork.class, CyRootNetwork.LOCAL_ATTRS).getRow(savedNetwork.getSUID());
		for (String k : originalRowValues.keySet()) {
			Object value = originalRowValues.get(k);
			Class<?> listType = originalTable.getColumn(k).getListElementType();
			if (listType != null/* && (originalRow.getList(k, listType)).isEmpty()*/) { //Incredibile: usa anche la usa implementazione interna della lista, per cui dovrei probabilmente chiedere alla column se e' di tipo lista o no...
				continue;
			}
//			System.err.println("Copio la proprieta' " + k + ", che vale " + value);
			try {
				Animo.setRowValue(savedRow, k, value.getClass(), value); //If I can't copy a property, it's not too bad: I will simply go on.
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}

	private void differenceButtonPressed(String caption) {
		if (caption.equals(START_DIFFERENCE)) {
			differenceButton.setText(CANCEL_DIFFERENCE);
			differenceWith = this;
			for (AnimoResultPanel others : allExistingPanels) {
				if (others == this)
					continue;
				others.differenceButton.setText(END_DIFFERENCE);
			}
		} else if (caption.equals(END_DIFFERENCE)) {
			if (differenceWith != null) {
				Map<String, String> hisMapCytoscapeIDtoModelID = differenceWith.model.getMapCytoscapeIDtoReactantID();
				Map<String, String> myMapCytoscapeIDtoModelID = this.model.getMapCytoscapeIDtoReactantID();
				Map<String, String> myMapModelIDtoCytoscapeID = new HashMap<String, String>();

				for (String k : myMapCytoscapeIDtoModelID.keySet()) {
					myMapModelIDtoCytoscapeID.put(myMapCytoscapeIDtoModelID.get(k), k);
				}

				SimpleLevelResult diff = (SimpleLevelResult)this.result.difference(differenceWith.result, myMapModelIDtoCytoscapeID,
						hisMapCytoscapeIDtoModelID);
				if (diff.isEmpty()) {
					JOptionPane
							.showMessageDialog(
									Animo.getCytoscape().getJFrame(),
									"Error: empty difference. Please contact the developers and send them the current model,\nwith a reference to which simulations were used for the difference.");
					return;
				}
				AnimoResultPanel newPanel = new AnimoResultPanel(differenceWith.model, diff, differenceWith.scale,
						differenceWith.title + " - " + this.title, null);
				newPanel.isDifference = true;
				double maxY = Math.max(this.g.getScale().getMaxY(), differenceWith.g.getScale().getMaxY());
				Scale s = newPanel.g.getScale();
				newPanel.g.setDrawArea((int) s.getMinX(), (int) s.getMaxX(), (int) -maxY, (int) maxY); // (int)scale.getMaxY());
				if (fCytoPanel != null) {
					newPanel.addToPanel(fCytoPanel);
				}
				for (AnimoResultPanel panel : allExistingPanels) {
					panel.differenceButton.setText(START_DIFFERENCE);
				}
			}
		} else if (caption.equals(CANCEL_DIFFERENCE)) {
			differenceWith = null;
			for (AnimoResultPanel panel : allExistingPanels) {
				panel.differenceButton.setText(START_DIFFERENCE);
			}
		}
	}

	public CyNetwork getSavedNetwork() {
		return this.savedNetwork;
	}

	public BufferedImage getSavedNetworkImage() {
		return this.savedNetworkImage;
	}

	private double getSliderTime() {
		return (1.0 * this.slider.getValue() / this.slider.getMaximum() * (this.maxValueOnGraph - this.minValueOnGraph) + this.minValueOnGraph) / this.scale; // 1.0 * this.slider.getValue() / this.slider.getMaximum() * this.scaleForConcentration; //this.slider.getValue() / scale;
	}

	public String getTitle() {
		return this.title;
	}
	
	public static void adjustDivider() {
		if (!allExistingPanels.isEmpty()) {
			AnimoResultPanel first = allExistingPanels.get(0);
			first.resetDivider();
		}
	}

	public void resetDivider() {
		JSplitPane par = (JSplitPane)(fCytoPanel.getThisComponent().getParent());
		CyNetworkView currentNetworkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
		int width = 0;
		if (currentNetworkView != null) {
			try {
				Rectangle windowBounds = Animo.getCyServiceRegistrar().getService(CyNetworkViewDesktopMgr.class).getBounds(currentNetworkView);
				width += windowBounds.width; //currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH); The bounds of the window are more precise because they also include the window border
			} catch (Exception ex) {
				width += 0;
			}
		}
		if (width == 0) {
			width = lastWidth;
		}
		par.setDividerLocation(width);
		lastWidth = width;
	}
	
	public void ensureCorrectVisualStyle() {
		if (isDifference) {
			Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_DIFF_VISUAL_STYLE);
		} else {
			Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE);
		}
	}

	/**
	 * Save the simulation data to a file
	 * 
	 * @param outputFile
	 * @param normalFile
	 *            True if the file is user-chosen. Otherwise, we need to save also all saved network-related data (network id, image, properties)
	 */
	public void saveSimulationData(File outputFile, boolean normalFile) {
		if (isDifference)
			return; // We don't save the differences!
		try {
			if (normalFile) {
				FileOutputStream fOut = new FileOutputStream(outputFile);
				ObjectOutputStream out = new ObjectOutputStream(fOut);
				out.writeObject(model);
				out.writeObject(result);
				out.writeDouble(new Double(scale));
				out.writeObject(title);
				out.close();
			} else {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				org.w3c.dom.Element rootNode = doc.createElement("root"), modelNode = doc.createElement("model"), resultNode = doc
						.createElement("result"), scaleNode = doc.createElement("scale"), titleNode = doc
						.createElement("title"), networkIdNode = doc.createElement("networkId"), nodePropertiesNode = doc
						.createElement("nodeProperties"), edgePropertiesNode = doc.createElement("edgeProperties");
				CDATASection modelValue = doc.createCDATASection(encodeObjectToBase64(model)), resultValue = doc
						.createCDATASection(encodeObjectToBase64(result)), nodePropsValue = doc
						.createCDATASection(encodeObjectToBase64(savedNodeAttributes)), edgePropsValue = doc
						.createCDATASection(encodeObjectToBase64(savedEdgeAttributes));
				modelNode.appendChild(modelValue);
				rootNode.appendChild(modelNode);
				resultNode.appendChild(resultValue);
				rootNode.appendChild(resultNode);
				scaleNode.setTextContent(Double.toString(scale));
				rootNode.appendChild(scaleNode);
				titleNode.setTextContent(title);
				rootNode.appendChild(titleNode);
				networkIdNode.setTextContent(savedNetwork.getSUID().toString());
				rootNode.appendChild(networkIdNode);
				nodePropertiesNode.appendChild(nodePropsValue);
				rootNode.appendChild(nodePropertiesNode);
				edgePropertiesNode.appendChild(edgePropsValue);
				rootNode.appendChild(edgePropertiesNode);
				doc.appendChild(rootNode);
				Transformer tra = TransformerFactory.newInstance().newTransformer();
				tra.setOutputProperty(OutputKeys.INDENT, "yes");
				tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				FileOutputStream fos = new FileOutputStream(outputFile);
				tra.transform(new DOMSource(doc), new StreamResult(fos));
				fos.flush();
				fos.close();
			}
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), baos.toString(), "Error: " + ex,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * When the graph scale changes (due to a zoom or axis range change), we change the min and max of the JSlider to adapt it to the graph area.
	 */
	@Override
	public void scaleChanged(Scale newScale) {
		this.minValueOnGraph = newScale.getMinX();
		this.maxValueOnGraph = newScale.getMaxX();
		this.scaleForConcentration = this.maxValueOnGraph / this.scale;

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		int nLabels = 10;
		double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
		for (int i = 0; i <= nLabels; i++) {
			double val = this.minValueOnGraph + 1.0 * i / nLabels * graphWidth;
			String valStr = formatter.format(val);
			labels.put((int) Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
		}
		this.slider.setLabelTable(labels);
		stateChanged(null); // Note that we don't use that parameter, so I can also call the function with null
	}

	public void setSavedNetworkImage(BufferedImage image) {
		this.savedNetworkImage = image;
	}

	public void setTitle(String newTitle) {
		title = newTitle;
		JTabbedPane pane = (JTabbedPane) this.getParent().getParent();
		pane.setTitleAt(pane.getSelectedIndex(), title);
		resetDivider();
	}
	
	
	/**
	 * When the user moves the time slider, we update the activity ratio (SHOWN_LEVEL) of all nodes in the network window, so that, thanks to the continuous Visual Mapping defined
	 * when the interface is augmented (see AugmentAction), different colors will show different activity levels.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (this.slider.getValueIsAdjusting()) {
			//System.err.println("Evito l'aggiornamento perche' stai ancora slidando");
			NodeDialog.dontUpdateNetworkView();
		}
		CyApplicationManager cyApplicationManager = Animo.getCytoscapeApp().getCyApplicationManager();
		CyNetwork net = cyApplicationManager.getCurrentNetwork();
		if (net == null) {
			return;
		}
		
		final double t = getSliderTime();
		double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
		g.setRedLinePosition(1.0 * this.slider.getValue() / this.slider.getMaximum() * graphWidth);
		
		if (convergingEdges == null) {
			convergingEdges = new HashMap<Long, Pair<Boolean, List<Long>>>();
			
			for (String r : this.result.getReactantIds()) {
				if (this.model.getReactant(r) == null) continue;
				final Long id = this.model.getReactant(r).get(Model.Properties.CYTOSCAPE_ID).as(Long.class);
				List<CyEdge> incomingEdges = net.getAdjacentEdgeList(net.getNode(id), CyEdge.Type.UNDIRECTED);
				incomingEdges.addAll(net.getAdjacentEdgeList(net.getNode(id), CyEdge.Type.INCOMING));
				if (incomingEdges.size() > 1) {
					Pair<Boolean, List<Long>> edgesGroup = new Pair<Boolean, List<Long>>(true, new Vector<Long>());
					for (CyEdge edge : incomingEdges) {
						edgesGroup.second.add(edge.getSUID());
					}
					for (CyEdge edge : incomingEdges) {
						convergingEdges.put(edge.getSUID(), edgesGroup);
					}
				}
			}
		} else {
			for (Pair<Boolean, List<Long>> group : convergingEdges.values()) {
				group.first = true; //Each edge in each group is initially assumed to be a candidate for having 0 activityRatio
			}
		}

		final int levels = this.model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); // at this point, all levels have already been rescaled to the
																												// maximum (= the number of levels of the model), so we use it as a
																												// reference for the number of levels to show on the network nodes
		//The reactantIds in the result are R0, R1 like in the UPPAAL model for the reactants, and Exxx for reactions where xxx is the ID of the reaction in the UPPAAL model (so R0_R1, R3_R4_R0 etc)
		//The reactant IDs in the model are the IDs we use in the UPPAAL model, like R0, R1, R2, ...
		//The CYTOSCAPE_IDs are the SUIDs used by Cytoscape
		for (String r : this.result.getReactantIds()) {
			if (this.model.getReactant(r) == null)
				continue;
			Long id = -1L;
			try {  //There is first the chance that we have an ANIMO 2.x simulation: let's check if that "cytoscape id" isn't what we now call node name
				id = this.model.getReactant(r).get(Model.Properties.CYTOSCAPE_ID).as(Long.class);
			} catch (Exception ex) {
				id = -1L;
			}
			CyNode node = net.getNode(id);
			if (node == null) {
				Collection<CyRow> possibleNodes = net.getDefaultNodeTable().getMatchingRows(CyNetwork.NAME, this.model.getReactant(r).get(Model.Properties.CYTOSCAPE_ID).as(String.class));
				if (possibleNodes.size() != 1) {
					continue;
				}
				node = net.getNode(possibleNodes.iterator().next().get(CyNode.SUID, Long.class));
			}
			if (node == null) continue; //The node may be null if we are looking at a network where the node does not exist and playing a simulation from another network (where the node existed)
			final double level = this.result.getConcentration(r, t);
			CyRow nodeRow = net.getRow(node);
			Animo.setRowValue(nodeRow, Model.Properties.SHOWN_LEVEL, Double.class, level / levels);
		}
		
		try {
			for (String r : this.result.getReactantIds()) {
				if (!(r.charAt(0) == 'E')) continue; //We added an "E" in front of the reaction IDs to make them easily distinguished
				CyEdge edge = null;
				Reaction reaction = this.model.getReaction(r.substring(1));
				if (reaction == null) { //This means that we have an ANIMO 2.x simulation: the reaction id there is "E" + what here is called the reaction "name"
					Collection<CyRow> possibleEdges = net.getDefaultEdgeTable().getMatchingRows(CyNetwork.NAME, r.substring(1));
					if (possibleEdges.size() != 1) {
						continue;
					}
					edge = net.getEdge(possibleEdges.iterator().next().get(CyEdge.SUID, Long.class));
				} else {
					Long edgeId = reaction.get(Model.Properties.CYTOSCAPE_ID).as(Long.class);
					edge = net.getEdge(edgeId);
				}
				if (edge == null) continue;
				CyRow edgeRow = net.getRow(edge),
					  sourceRow = net.getRow(edge.getSource()),
					  targetRow = net.getRow(edge.getTarget());
				if (t == 0) {
					Animo.setRowValue(edgeRow, Model.Properties.SHOWN_LEVEL, Double.class, 0.25);
				} else {
					int scenario = edgeRow.get(Model.Properties.SCENARIO, Integer.class);
					double concentration = this.result.getConcentration(r, t);
					boolean candidate = false;
					switch (scenario) {
						case 0:
							if (sourceRow.get(Model.Properties.SHOWN_LEVEL, Double.class) == 0) {
								concentration = 0;
								candidate = true;
							}
							break;
						case 1:
							if (sourceRow.get(Model.Properties.SHOWN_LEVEL, Double.class) == 0) {
								concentration = 0;
								candidate = true;
								
							} else if ((edgeRow.get(Model.Properties.INCREMENT, Integer.class) >= 0
										&& targetRow.get(Model.Properties.SHOWN_LEVEL, Double.class) == 1)
									   || (edgeRow.get(Model.Properties.INCREMENT, Integer.class) < 0
										&& targetRow.get(Model.Properties.SHOWN_LEVEL, Double.class) == 0)) {
								//concentration = 0;
								candidate = true;
							}
							break;
						case 2:
							//We have saved the E1 and E2 with their cytoscape names (CyNetwork.NAME for Cytoscape 3), so we recover them by looking in the proper table saved by the model
							Long e1 = model.getReactantByCytoscapeName(edgeRow.get(Model.Properties.REACTANT_ID_E1, String.class)).get(Model.Properties.CYTOSCAPE_ID).as(Long.class),
								 e2 = model.getReactantByCytoscapeName(edgeRow.get(Model.Properties.REACTANT_ID_E2, String.class)).get(Model.Properties.CYTOSCAPE_ID).as(Long.class);
							//System.err.println("Scenario 2, E1 = " + e1 + ", isActive? " + edgeAttributes.getBooleanAttribute(edge.getIdentifier(), Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1) + ", level = " + nodeAttributes.getDoubleAttribute(e1, Model.Properties.SHOWN_LEVEL));
							if (((edgeRow.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1, Boolean.class) && net.getRow(net.getNode(e1)).get(Model.Properties.SHOWN_LEVEL, Double.class) == 0)
								|| (!edgeRow.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1, Boolean.class) && net.getRow(net.getNode(e1)).get(Model.Properties.SHOWN_LEVEL, Double.class) == 1))
								||
								((edgeRow.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2, Boolean.class) && net.getRow(net.getNode(e2)).get(Model.Properties.SHOWN_LEVEL, Double.class) == 0)
								|| (!edgeRow.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2, Boolean.class) && net.getRow(net.getNode(e2)).get(Model.Properties.SHOWN_LEVEL, Double.class) == 1))) {
								//concentration = 0;
								candidate = true;
							}
							break;
						default:
							candidate = false;
							break;
					}
					if (!candidate && convergingEdges.get(edge.getSUID()) != null) {
						//If at least one edge is NOT a candidate for having 0 activityRatio, then all edges will have their activityRatio set as it comes from the result
						convergingEdges.get(edge.getSUID()).first = false;
					}
					
					Animo.setRowValue(edgeRow, Model.Properties.SHOWN_LEVEL, Double.class, concentration);
				}
			}
			if (t != 0) { //At the initial time we have already done what was needed, i.e. remove the attribute (TODO: adesso non lo rimuovo mica!)
				for (Pair<Boolean, List<Long>> edgeGroup : convergingEdges.values()) {
					if (edgeGroup.first) { //All the edges of this group were still candidates at the end of the first cycle, so we will set all their activityRatios to 0
						for (Long i : edgeGroup.second) {
							Animo.setRowValue(net.getRow(net.getEdge(i)), Model.Properties.SHOWN_LEVEL, Double.class, 0.0);
						}
						edgeGroup.first = false;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView().updateView();
		if (!this.slider.getValueIsAdjusting()) {
			//System.err.println("Non stai slidando, quindi provo ad aggiornare");
		//	NodeDialog.tryNetworkViewUpdate();
		}
	}
	

}
