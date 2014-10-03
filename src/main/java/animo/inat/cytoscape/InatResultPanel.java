package animo.inat.cytoscape;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
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

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import animo.inat.analyser.uppaal.ResultAverager;
import animo.inat.analyser.uppaal.SimpleLevelResult;
import animo.inat.graph.FileUtils;
import animo.inat.graph.Graph;
import animo.inat.graph.GraphScaleListener;
import animo.inat.graph.Scale;
import animo.inat.model.Model;
import animo.inat.util.Heptuple;
import animo.inat.util.Pair;

/**
 * The Inat result panel.
 * 
 * @author Brend Wanders
 */
public class InatResultPanel extends JPanel implements ChangeListener, GraphScaleListener
{

    private static final long serialVersionUID = -163756255393221954L;


    private static String TAB_NAME = "animo";

    /**
     * Do exactly the opposite of encodeObjectToBase64
     * @param encodedObject The encoded object as a CDATA text
     * @return The decoded object
     * @throws Exception
     */
    private static Object decodeObjectFromBase64(String encodedObject) throws Exception
    {
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
    private JPanel container;
    /**
     * The slider to allow the user to choose a moment in the simulation time, which will be reflected on the network window as node colors, indicating the corresponding reactant activity level.
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
     * The attributes of the nodes in the saved network
     */
    private HashMap<String, HashMap<String, Object>> savedNodeAttributes;
    /**
     * The attributes of the edges in the saved network
     */
    private HashMap<String, HashMap<String, Object>> savedEdgeAttributes;
    private static final String PROP_POSITION_X = "Position.X";
    private static final String PROP_POSITION_Y = "Position.Y";
    private static final String DEFAULT_TITLE = "ANIMO Results";
    /**
     * The three strings here are for one button. Everybody normally shows START_DIFFERENCE. When the user presses on the button (differenceWith takes the value of this for the InatResultPanel where the button was pressed),
     */
    private static final String START_DIFFERENCE = "Difference with...";
    /**
     * every other panel shows the END_DIFFERENCE. If the user presses a button with END_DIFFERENCE, a new InatResultPanel is created with the difference between the data in differenceWith and the panel where END_DIFFERENCE was pressed. Then every panel goes back to START_DIFFERENCE.
     */
    private static final String END_DIFFERENCE = "Difference with this";
    /**
     * CANCEL_DIFFERENCE is shown as text of the button only on the panel whare START_DIFFERENCE was pressed. If CANCEL_DIFFERENCE is pressed, then no difference is computed and every panel simply goes back to START_DIFFERENCE
     */
    private static final String CANCEL_DIFFERENCE = "Cancel difference";

    private HashMap<Long, Pair<Boolean, List<Long>>> convergingEdges = null;


    public static InatResultPanel loadFromSessionSimFile(File simulationDataFile)
    {
        Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>> simulationData = loadSimulationData(
                simulationDataFile, false);
        InatResultPanel panel = new InatResultPanel(simulationData.first, simulationData.second, simulationData.third, simulationData.fourth, null);
        panel.savedNetwork = Animo.getCytoscapeApp().getCyNetworkManager().getNetwork(Long.parseLong(simulationData.fifth));
        if (panel.savedNetwork != null)
        {
            //TODO Currently only tries the first element of the Collection
            CyNetworkView savedNetworkView = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViews(panel.savedNetwork).iterator().next();
            if (savedNetworkView != null)
            { //Keep all saved networks hidden
                Animo.getCytoscapeApp().getCyNetworkViewManager().destroyNetworkView(savedNetworkView);
                CyNetworkView otherView = null;
                Collection<CyNetworkView> c = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViewSet();
                if (!c.isEmpty())
                {
                    otherView = c.iterator().next();
                }
                if (otherView != null)
                {
                    Animo.getCytoscapeApp().getCyApplicationManager().setCurrentNetworkView(otherView);
                }
            }
        }
        panel.savedNetworkImage = null; //We cannot save BufferedImages to file as objects, as they are not serializable, so we will show no image of the network
        panel.savedNodeAttributes = simulationData.sixth;
        panel.savedEdgeAttributes = simulationData.seventh;
        allExistingPanels.add(panel);
        return panel;
    }

    private String title; //The title to show to the user
    private Graph g;
    private JButton differenceButton = null;
    private static InatResultPanel differenceWith = null;
    private static List<InatResultPanel> allExistingPanels = new ArrayList<InatResultPanel>();


    /**
     * Encode the given object to Base64, so that it can be included in a CDATA section in an xml file
     * @param o The object to be encoded
     * @return The encoded object
     * @throws Exception Any exception that may be thrown by ByteArrayInputStream, ObjectOutputStream and Base64
     */
    private static String encodeObjectToBase64(Object o) throws Exception
    {
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
     * @param inputFile The file name
     * @param normalFile True if we are loading from an user-chosen file and not a cytoscape-associated file.
     * In the latter case, we need also to load the saved network id, image and properties
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>> loadSimulationData(
            File inputFile, boolean normalFile)
    {
        try
        {
            Model model = null;
            SimpleLevelResult result = null;
            Double scale = null;
            String title = null;
            String networkId = null;
            HashMap<String, HashMap<String, Object>> nodeProperties = null, edgeProperties = null;
            if (normalFile)
            {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
                model = (Model) in.readObject();
                result = (SimpleLevelResult) in.readObject();
                scale = in.readDouble();
                title = in.readObject().toString();
                in.close();
            }
            else
            {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
                doc.getDocumentElement().normalize();
                NodeList children = doc.getFirstChild().getChildNodes(); //The first child is unique and is the "root" node
                for (int i = 0; i < children.getLength(); i++)
                {
                    Node n = children.item(i);
                    String name = n.getNodeName();
                    if (name.equals("model"))
                    {
                        model = (Model) decodeObjectFromBase64(n.getFirstChild().getTextContent());
                    }
                    else if (name.equals("result"))
                    {
                        result = (SimpleLevelResult) decodeObjectFromBase64(n.getFirstChild().getTextContent());
                    }
                    else if (name.equals("scale"))
                    {
                        scale = Double.parseDouble(n.getFirstChild().getTextContent());
                    }
                    else if (name.equals("title"))
                    {
                        title = n.getFirstChild().getTextContent();
                    }
                    else if (name.equals("networkId"))
                    {
                        networkId = n.getFirstChild().getTextContent();
                    }
                    else if (name.equals("nodeProperties"))
                    {
                        nodeProperties = (HashMap<String, HashMap<String, Object>>) decodeObjectFromBase64(n.getFirstChild().getTextContent());
                    }
                    else if (name.equals("edgeProperties"))
                    {
                        edgeProperties = (HashMap<String, HashMap<String, Object>>) decodeObjectFromBase64(n.getFirstChild().getTextContent());
                    }
                }
            }
            return new Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>>(
                    model, result, scale, title, networkId, nodeProperties, edgeProperties);
        }
        catch (Exception ex)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ex.printStackTrace(ps);
            JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
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
     * @param model The representation of the current Cytoscape network, to which the simulation data
     * will be coupled (it is REQUIRED that the simulation was made on the same network, or else the node IDs
     * will not be the same, and the slider will not work properly)
     * @param simulationDataFile The file from which to load the data. For the format, see saveSimulationData()
     */
    public InatResultPanel(File simulationDataFile)
    {
        this(loadSimulationData(simulationDataFile, true));
    }

    public InatResultPanel(
            Heptuple<Model, SimpleLevelResult, Double, String, String, HashMap<String, HashMap<String, Object>>, HashMap<String, HashMap<String, Object>>> simulationData)
    {
        this(simulationData.first, simulationData.second, simulationData.third, simulationData.fourth, null);
    }

    public InatResultPanel(Model model, SimpleLevelResult result, double scale, CyNetwork originalNetwork)
    {
        this(model, result, scale, DEFAULT_TITLE, originalNetwork);
    }

    /**
     * The panel constructor.
     * 
     * @param model the model this panel uses
     * @param result the results object this panel uses
     */
    public InatResultPanel(final Model model, final SimpleLevelResult result, double scale, String title, CyNetwork originalNetwork)
    {
        super(new BorderLayout(), true);
        allExistingPanels.add(this);
        this.model = model;
        this.result = result;
        this.scale = scale;
        this.title = title;
        if (originalNetwork != null)
        {
            try
            {
                //TODO Currently only the first CyNetworkView is tested
                CyNetworkView view = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViews(originalNetwork).iterator().next();
                List<View<CyNode>> hiddenNodes = new ArrayList<View<CyNode>>();
                for (View<CyNode> o : view.getNodeViews())
                {
                    o.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
                    if (o.getVisualProperty(BasicVisualLexicon.NODE_WIDTH) < 0)
                    { 
                        hiddenNodes.add(o);
                    }
                }
                //at this point, all nodes are visible and nodesToBeReHiddenAfterDoneHere contains the indexes of those nodes which were hidden before we started. They will be re-hidden after we have finished copying the needed things

                CyNetworkView currentNetworkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();


                Component componentToBeSaved = Animo.getCytoscape().getJFrame();//TODO: Vies lelijk enzo, ipv netwerk saved ie nu volledige window, moet eleganter kunnen, was: currentNetworkView.getComponent();

                savedNetworkImage = new BufferedImage(currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH).intValue() / 2,
                        currentNetworkView.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT).intValue() / 2, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphic = savedNetworkImage.createGraphics();
                graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphic.scale(0.5, 0.5);
                graphic.setPaint(Animo.getCytoscape().getJFrame().getBackground()); //Cytoscape.getVisualMappingManager().getVisualStyle().getGlobalAppearanceCalculator().getDefaultBackgroundColor());
                graphic.fillRect(0, 0, componentToBeSaved.getWidth(), componentToBeSaved.getHeight());
                componentToBeSaved.paint(graphic);
                CyNetworkView originalView = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViews(originalNetwork).iterator().next(); //TODO confirm there's only one network view
                Animo.getCytoscapeApp().getCyApplicationManager().setCurrentNetworkView(originalView);
                CytoPanel controlPanel = Animo.getCytoscape().getCytoPanel(CytoPanelName.WEST);
                controlPanel.setSelectedIndex(controlPanel.indexOfComponent(TAB_NAME));
                for (View<CyNode> n : hiddenNodes)
                { //re-hide the hidden nodes after finishing
                    n.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
                }

                //TODO make focus
                //Animo.getCytoscape().getJFrame().getFrame(originalView).setSelected(true);
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Exception: " + ex);
                ex.printStackTrace();
            }
        }
        else
        {
            this.savedNetwork = null;
            this.savedNetworkImage = null;
            this.savedNodeAttributes = null;
            this.savedEdgeAttributes = null;
        }

        JPanel sliderPanel = new JPanel(new BorderLayout());
        this.slider = new JSlider();
        this.slider.setOrientation(SwingConstants.HORIZONTAL);
        this.slider.setMinimum(0);
        if (result.isEmpty())
        {
            this.scaleForConcentration = 1;
        }
        else
        {
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
        for (int i = 0; i <= nLabels; i++)
        {
            double val = 1.0 * i / nLabels * this.maxValueOnGraph;
            String valStr = formatter.format(val);
            labels.put((int) Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
        }
        
        this.slider.setLabelTable(labels);
        this.slider.setValue(0);
        this.slider.getModel().addChangeListener(this);

        JButton setParameters;
        try
        {
            URL url = getClass().getResource("animo/resources/copy20x20.png");
            setParameters = new JButton(new ImageIcon(url));
        }
        catch (Exception ex)
        {
            setParameters = new JButton("Copy");
        }
        
        setParameters.setToolTipText("Copy the currently shown activity levels as initial activity levels in the model");
        setParameters.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //set the initial activity levels of the reactants in the network as they are in this point of the simulation ("this point" = where the slider is currently)
                double t = getSliderTime();
                CyNetwork network = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
                int nLevels = result.getNumberOfLevels();
                for (String r : result.getReactantIds())
                {
                    if (model.getReactant(r) == null)
                        continue;
                    final String id = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
                    final double level = result.getConcentration(r, t) / nLevels
                            * network.getRow(network.getNode(Long.parseLong(id))).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class); //model.getReactant(r).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); //We also rescale the value to the correct number of levels of each node. Attention: we need to use the CURRENT number of levels of the node, or we will get inconsistent results!
                    Animo.setRowValue(network.getRow(network.getNode(Long.parseLong(id))), Model.Properties.INITIAL_LEVEL, Integer.class, Math.round(level));
                }
            }
        });
        sliderPanel.add(setParameters, BorderLayout.WEST);

        sliderPanel.add(this.slider, BorderLayout.CENTER);

        this.add(sliderPanel, BorderLayout.SOUTH);

        g = new Graph();
        //We map reactant IDs to their corresponding aliases (canonical names, i.e., the names displayed to the user in the network window), so that
        //we will be able to use graph series names consistent with what the user has chosen.
        Map<String, String> seriesNameMapping = new HashMap<String, String>();
        List<String> filteredSeriesNames = new ArrayList<String>(); //profit from the cycle for the series mapping to create a filter for the series to be actually plotted
        for (String r : result.getReactantIds())
        {
            if (r.charAt(0) == 'E')
                continue; //If the identifier corresponds to an edge (e.g. "Enode123 (DefaultEdge) node456") we don't consider it, as we are looking only at series to be plotted in the graph, and those data instead are used for the slider (edge highlighting corresponding to reaction "strength")
            String name = null;
            String stdDevReactantName = null;
            if (model.getReactant(r) != null)
            { //we can also refer to a name not present in the reactant collection
                name = model.getReactant(r).getName(); //if an alias is set, we prefer it
                if (name == null)
                {
                    name = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
                }
            }
            else if (r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()))
            {
                stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.STD_DEV));
                if (model.getReactant(stdDevReactantName).getName() != null)
                {
                    name = model.getReactant(stdDevReactantName).getName() + ResultAverager.STD_DEV;
                }
                else
                {
                    name = r; //in this case, I simply don't know what we are talking about =)
                }
            }
            else if (r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase()))
            {
                stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.OVERLAY_NAME));
                if (model.getReactant(stdDevReactantName).getName() != null)
                {
                    name = model.getReactant(stdDevReactantName).getName();
                    seriesNameMapping.put(stdDevReactantName, name);
                }
                else
                {
                    name = r; //boh
                }
            }
            if ((!r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) && !r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())
                    && model.getReactant(r) != null && model.getReactant(r).get(Model.Properties.PLOTTED).as(Boolean.class))
                    || ((r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) || r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase()))
                            && model.getReactant(stdDevReactantName) != null && model.getReactant(stdDevReactantName).get(Model.Properties.PLOTTED)
                            .as(Boolean.class)))
            {

                filteredSeriesNames.add(r);
            }
            seriesNameMapping.put(r, name);
        }
        g.parseLevelResult(result.filter(filteredSeriesNames), seriesNameMapping, scale); //Add all series to the graph, using the mapping we built here to "translate" the names into the user-defined ones.
        g.setXSeriesName("Time (min)");
        g.setYLabel("Protein activity (a. u.)");

        if (!model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).isNull())
        { //if we find a maximum value for activity levels, we declare it to the graph, so that other added graphs (such as experimental data) will be automatically rescaled to match us
            int nLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
            g.declareMaxYValue(nLevels);
            double maxTime = scale * result.getTimeIndices().get(result.getTimeIndices().size() - 1);
            g.setDrawArea(0, maxTime, 0, nLevels); //This is done because the graph automatically computes the area to be shown based on minimum and maximum values for X and Y, including StdDev. So, if the StdDev of a particular series (which represents an average) in a particular point is larger that the value of that series in that point, the minimum y value would be negative. As this is not very nice to see, I decided that we will recenter the graph to more strict bounds instead.
                                                   //Also, if the maximum value reached during the simulation is not the maximum activity level, the graph does not loook nice
        }
        this.add(g, BorderLayout.CENTER);
        g.addGraphScaleListener(this);
    }

    /**
     * Add the InatResultPanel to the given Cytoscape panel
     * @param cytoPanel
     */
    public void addToPanel(final CytoPanel cytoPanel)
    {
        container = new JPanel(new BorderLayout(2, 2));
        container.add(this, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout()); //new GridLayout(2, 2, 2, 2));

        resetToThisNetwork = null;
        if (savedNetwork != null)
        {
            resetToThisNetwork = new JButton(new AbstractAction("Reset to here")
            {
                private static final long serialVersionUID = 7265495749842776073L;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (savedNetwork == null)
                    {
                        JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(),
                                "No network was actually saved, so no network will be restored.\n(Please report this as a bug)");
                        return; //The button does nothing when it discovers that the session was changed and the network is not available anymore
                    }

                    if (savedNetwork.getNodeCount() < 1 || countSessionChanges > 1)
                    { //If there are no nodes, I am not deleting the whole network to replace it with something useless
                        resetToThisNetwork.setEnabled(false);
                        return;
                    }
                    
                    CyApplicationManager cyApplicationManager = Animo.getCytoscapeApp().getCyApplicationManager();
                    CyNetwork net = cyApplicationManager.getCurrentNetwork();

                    List<CyNode> nodes = net.getNodeList();
                    List<CyEdge> edges = net.getEdgeList();
                    net.removeEdges(edges);
                    net.removeNodes(nodes);
                    HashMap<CyNode, CyNode> map = new HashMap<>();
                    for (CyEdge edge : savedNetwork.getEdgeList())
                    {
                        CyNode source = edge.getSource();
                        CyNode target = edge.getTarget();
                        CyNode newSource;
                        CyNode newTarget;
                        if (map.containsKey(source))
                        {
                            newSource = map.get(source);
                        }
                        else
                        {
                            newSource = net.addNode();
                            map.put(source, newSource);
                        }
                        if (map.containsKey(target))
                        {
                            newTarget = map.get(target);
                        }
                        else
                        {
                            newTarget = net.addNode();
                            map.put(target, newTarget);
                        }
                        net.addEdge(newSource, newTarget, true);
                    }
                    for (CyNode node : net.getNodeList())
                    {
                        if (!map.containsKey(node))
                        {
                            net.addNode();
                        }
                    }
                    CyNetworkView currentView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
                    nodes = net.getNodeList();
                    for (CyNode n : nodes)
                    {
                        if (savedNodeAttributes.containsKey(n.getSUID().toString()))
                        {
                            HashMap<String, Object> m = savedNodeAttributes.get(n.getSUID().toString());
                            for (String k : m.keySet())
                            {
                                Object o = m.get(k);
                                if (o instanceof Boolean)
                                {
                                    Animo.setRowValue(net.getRow(n), k, Boolean.class, o);
                                }
                                else if (o instanceof Double)
                                {
                                    Animo.setRowValue(net.getRow(n), k, Double.class, o);
                                }
                                else if (o instanceof Float)
                                {
                                    Animo.setRowValue(net.getRow(n), k, Double.class, new Double((Float) o));
                                }
                                else if (o instanceof Integer)
                                {
                                    Animo.setRowValue(net.getRow(n), k, Integer.class, o);
                                }
                                else if (o instanceof List)
                                {
                                    Animo.setRowValue(net.getRow(n), k, List.class, o);
                                }
                                else if (o instanceof Map)
                                {
                                    Animo.setRowValue(net.getRow(n), k, Map.class, o);
                                }
                                else if (o instanceof String)
                                {
                                    Animo.setRowValue(net.getRow(n), k, String.class, o);
                                }
                            }
                        }
                    }
                    edges = net.getEdgeList();
                    for (CyEdge ed : edges)
                    {
                        if (savedEdgeAttributes.containsKey(ed.getSUID()))
                        {
                            HashMap<String, Object> m = savedEdgeAttributes.get(ed.getSUID());
                            for (String k : m.keySet())
                            {
                                Object o = m.get(k);
                                if (o instanceof Boolean)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, Boolean.class, o);
                                }
                                else if (o instanceof Double)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, Double.class, o);
                                }
                                else if (o instanceof Float)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, Double.class, new Double((Float) o));
                                }
                                else if (o instanceof Integer)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, Integer.class, o);
                                }
                                else if (o instanceof List)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, List.class, o);
                                }
                                else if (o instanceof Map)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, Map.class, o);
                                }
                                else if (o instanceof String)
                                {
                                    Animo.setRowValue(net.getRow(ed), k, String.class, o);
                                }
                            }
                        }
                    }
                    for (View<CyNode> n : currentView.getNodeViews())
                    {
                        n.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, savedNetwork.getRow(n).get(PROP_POSITION_X, Double.class));
                        n.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, savedNetwork.getRow(n).get(PROP_POSITION_Y, Double.class));
                    }
                }
            });
            try
            {
                if (savedNetworkImage != null)
                {
                    File tmpFile = File.createTempFile("ANIMOimg", ".png");
                    tmpFile.deleteOnExit();
                    ImageIO.write(savedNetworkImage, "png", tmpFile);
                    resetToThisNetwork.setToolTipText("<html>Reset the network to the input that gave this result, i.e. this network:<br/><img src=\"file:"
                            + tmpFile.getCanonicalPath() + "\"/></html>");
                }
                else
                {
                    throw new IOException(); //Just to do the same when we have no image as when the image has problems
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                resetToThisNetwork.setToolTipText("Reset the network to the input that gave this result");
            }
        }

        JButton close = new JButton(new AbstractAction("Close")
        {
            private static final long serialVersionUID = 4327349309742276633L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                closeResultsPanel(cytoPanel);
            }
        });

        JButton save = new JButton(new AbstractAction("Save simulation data...")
        {
            private static final long serialVersionUID = -2492923184151760584L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                String fileName = FileUtils.save(".sim", "ANIMO simulation data", Animo.getCytoscape().getJFrame());
                if (fileName != null)
                    saveSimulationData(new File(fileName), true);
            }
        });

        JButton changeTitle = new JButton(new AbstractAction("Change title")
        {
            private static final long serialVersionUID = 7093059357198172376L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                String newTitle = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Please give a new title", title);
                if (newTitle == null)
                {
                    return;
                }
                setTitle(newTitle);
            }
        });

        differenceButton = new JButton(START_DIFFERENCE);
        differenceButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                differenceButtonPressed(differenceButton.getText());
            }
        });

        buttons.add(changeTitle);
        if (resetToThisNetwork != null)
        {
            buttons.add(resetToThisNetwork);
        }
        buttons.add(differenceButton);
        if (!isDifference)
        { //The differences are not saved (for the moment)
            buttons.add(save);
        }
        buttons.add(close);
        container.add(buttons, BorderLayout.NORTH);


        if (cytoPanel.getState().equals(CytoPanelState.HIDE))
        {
            cytoPanel.setState(CytoPanelState.DOCK); //We show the Results panel if it was hidden.
        }
        fCytoPanel = cytoPanel;

        container.setName(this.getTitle());
        Animo.getResultPanelContainer().addTab(container);
        resetDivider();
    }

    public void closeResultsPanel(CytoPanel cytoPanel)
    {
        if (savedNetwork != null && countSessionChanges <= 1)
        { //We destroy network & vizmap only if we are still in "our" session. Otherwise, we would risk destroying a network from another session
            try
            {
                if (Animo.getCytoscapeApp().getCyNetworkManager().networkExists(savedNetwork.getSUID()))
                    Animo.getCytoscapeApp().getCyNetworkManager().destroyNetwork(savedNetwork);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        Animo.getResultPanelContainer().removeTab(container);
        allExistingPanels.remove(this);
    }

    protected CyNetwork copyNetwork(CyNetwork originalNetwork)
    {

        savedNetwork = Animo.getCytoscapeApp().getCyNetworkFactory().createNetwork(SavePolicy.DO_NOT_SAVE);
        List<CyNode> origNodes = originalNetwork.getNodeList();
        List<CyEdge> origEdges = originalNetwork.getEdgeList();
        savedNodeAttributes = new HashMap<String, HashMap<String, Object>>();
        savedEdgeAttributes = new HashMap<String, HashMap<String, Object>>();

        Map<CyNode, CyNode> nodeToNodeMap = new HashMap<CyNode, CyNode>();


        for (CyNode n : origNodes)
        {
            CyNode newNode = savedNetwork.addNode();
            nodeToNodeMap.put(n, newNode);
            for (Map.Entry<String, Object> field : originalNetwork.getRow(n).getAllValues().entrySet())
            {
                Animo.setRowValue(savedNetwork.getRow(newNode), field.getKey(), field.getValue().getClass(), field.getValue());
            }
        }

        for (CyEdge e : origEdges)
        {

            CyEdge newEdge = savedNetwork.addEdge(nodeToNodeMap.get(e.getSource()), nodeToNodeMap.get(e.getTarget()), true);
            for (Map.Entry<String, Object> field : originalNetwork.getRow(e).getAllValues().entrySet())
            {
                if (field.getKey() != null && field.getValue() != null)
                {
                    Animo.setRowValue(savedNetwork.getRow(newEdge), field.getKey(), field.getValue().getClass(), field.getValue());
                }
            }
        }
        CyNetworkView originalView = Animo.getCytoscapeApp().getCyNetworkViewManager().getNetworkViews(originalNetwork).iterator().next();

        for (View<CyNode> n : originalView.getNodeViews())
        { //sets offset for all nodes
            n.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, savedNetwork.getRow(n).get(PROP_POSITION_X, Double.class));
            n.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, savedNetwork.getRow(n).get(PROP_POSITION_Y, Double.class));
        }

        return savedNetwork;
    }

    private void differenceButtonPressed(String caption)
    {
        if (caption.equals(START_DIFFERENCE))
        {
            differenceButton.setText(CANCEL_DIFFERENCE);
            differenceWith = this;
            for (InatResultPanel others : allExistingPanels)
            {
                if (others == this)
                    continue;
                others.differenceButton.setText(END_DIFFERENCE);
            }
        }
        else if (caption.equals(END_DIFFERENCE))
        {
            if (differenceWith != null)
            {
                Map<String, String> hisMapCytoscapeIDtoModelID = differenceWith.model.getMapCytoscapeIDtoReactantID();
                Map<String, String> myMapCytoscapeIDtoModelID = this.model.getMapCytoscapeIDtoReactantID();
                Map<String, String> myMapModelIDtoCytoscapeID = new HashMap<String, String>();

                for (String k : myMapCytoscapeIDtoModelID.keySet())
                {
                    myMapModelIDtoCytoscapeID.put(myMapCytoscapeIDtoModelID.get(k), k);
                }

                SimpleLevelResult diff = this.result.difference(differenceWith.result, myMapModelIDtoCytoscapeID, hisMapCytoscapeIDtoModelID);
                if (diff.isEmpty())
                {
                    JOptionPane
                            .showMessageDialog(Animo.getCytoscape().getJFrame(),
                                    "Error: empty difference. Please contact the developers and send them the current model,\nwith a reference to which simulations were used for the difference.");
                    return;
                }
                InatResultPanel newPanel = new InatResultPanel(differenceWith.model, diff, differenceWith.scale, differenceWith.title + " - " + this.title,
                        null);
                newPanel.isDifference = true;
                double maxY = Math.max(this.g.getScale().getMaxY(), differenceWith.g.getScale().getMaxY());
                Scale s = newPanel.g.getScale();
                newPanel.g.setDrawArea((int) s.getMinX(), (int) s.getMaxX(), (int) -maxY, (int) maxY); //(int)scale.getMaxY());
                if (fCytoPanel != null)
                {
                    newPanel.addToPanel(fCytoPanel);
                }
                for (InatResultPanel panel : allExistingPanels)
                {
                    panel.differenceButton.setText(START_DIFFERENCE);
                }
            }
        }
        else if (caption.equals(CANCEL_DIFFERENCE))
        {
            differenceWith = null;
            for (InatResultPanel panel : allExistingPanels)
            {
                panel.differenceButton.setText(START_DIFFERENCE);
            }
        }
    }

    public CyNetwork getSavedNetwork()
    {
        return this.savedNetwork;
    }


    public BufferedImage getSavedNetworkImage()
    {
        return this.savedNetworkImage;
    }

    private double getSliderTime()
    {
        return (1.0 * this.slider.getValue() / this.slider.getMaximum() * (this.maxValueOnGraph - this.minValueOnGraph) + this.minValueOnGraph) / this.scale; //1.0 * this.slider.getValue() / this.slider.getMaximum() * this.scaleForConcentration; //this.slider.getValue() / scale;
    }

    public String getTitle()
    {
        return this.title;
    }

    private void resetDivider()
    {
        JSplitPane par = (JSplitPane) (fCytoPanel.getThisComponent().getParent());
        CyNetworkView p2 = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
        int width = 0;
        if (p2 != null)
        {
            width = fCytoPanel.getThisComponent().getWidth();
        }
        if (width == 0)
        {
            width = lastWidth;
        }

        par.setDividerLocation(width);
        lastWidth = width;
    }

    /**
     * Save the simulation data to a file
     * @param outputFile
     * @param normalFile True if the file is user-chosen.
     * Otherwise, we need to save also all saved network-related data (network id, image, properties)
     */
    public void saveSimulationData(File outputFile, boolean normalFile)
    {
        if (isDifference)
            return; //We don't save the differences!
        try
        {
            if (normalFile)
            {
                FileOutputStream fOut = new FileOutputStream(outputFile);
                ObjectOutputStream out = new ObjectOutputStream(fOut);
                out.writeObject(model);
                out.writeObject(result);
                out.writeDouble(new Double(scale));
                out.writeObject(title);
                out.close();
            }
            else
            {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                org.w3c.dom.Element rootNode = doc.createElement("root"), modelNode = doc.createElement("model"), resultNode = doc.createElement("result"), scaleNode = doc
                        .createElement("scale"), titleNode = doc.createElement("title"), networkIdNode = doc.createElement("networkId"), nodePropertiesNode = doc
                        .createElement("nodeProperties"), edgePropertiesNode = doc.createElement("edgeProperties");
                CDATASection modelValue = doc.createCDATASection(encodeObjectToBase64(model)), resultValue = doc
                        .createCDATASection(encodeObjectToBase64(result)), nodePropsValue = doc.createCDATASection(encodeObjectToBase64(savedNodeAttributes)), edgePropsValue = doc
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
        }
        catch (Exception ex)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ex.printStackTrace(ps);
            JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * When the graph scale changes (due to a zoom or axis range change),
     * we change the min and max of the JSlider to adapt it to the graph area.
     */
    @Override
    public void scaleChanged(Scale newScale)
    {
        this.minValueOnGraph = newScale.getMinX();
        this.maxValueOnGraph = newScale.getMaxX();
        this.scaleForConcentration = this.maxValueOnGraph / this.scale;

        Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
        DecimalFormat formatter = new DecimalFormat("0.###");
        int nLabels = 10;
        double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
        for (int i = 0; i <= nLabels; i++)
        {
            double val = this.minValueOnGraph + 1.0 * i / nLabels * graphWidth;
            String valStr = formatter.format(val);
            labels.put((int) Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
        }
        this.slider.setLabelTable(labels);
        stateChanged(null); //Note that we don't use that parameter, so I can also call the function with null
    }

    public void setSavedNetworkImage(BufferedImage image)
    {
        this.savedNetworkImage = image;
    }

    public void setTitle(String newTitle)
    {
        title = newTitle;
        JTabbedPane pane = (JTabbedPane) this.getParent().getParent();
        pane.setTitleAt(pane.getSelectedIndex(), title);
        resetDivider();
    }

    /**
     * When the user moves the time slider, we update the activity ratio (SHOWN_LEVEL) of
     * all nodes in the network window, so that, thanks to the continuous Visual Mapping
     * defined when the interface is augmented (see AugmentAction), different colors will
     * show different activity levels. 
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        final double t = getSliderTime();

        double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
        g.setRedLinePosition(1.0 * this.slider.getValue() / this.slider.getMaximum() * graphWidth);
        CyApplicationManager cyApplicationManager = Animo.getCytoscapeApp().getCyApplicationManager();
        CyNetwork net = cyApplicationManager.getCurrentNetwork();
        if (convergingEdges == null)
        {
            convergingEdges = new HashMap<Long, Pair<Boolean, List<Long>>>();
        }
        else
        {
            for (Pair<Boolean, List<Long>> group : convergingEdges.values())
            {
                group.first = true; //Each edge in each group is initially assumed to be a candidate for having 0 activityRatio
            }
        }

        final int levels = this.model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); //at this point, all levels have already been rescaled to the maximum (= the number of levels of the model), so we use it as a reference for the number of levels to show on the network nodes 
        for (String r : this.result.getReactantIds())
        {
            if (this.model.getReactant(r) == null)
                continue;
            final String id = this.model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
            final double level = this.result.getConcentration(r, t);
            net.getRow(net.getNode(Long.valueOf(id))).set(Model.Properties.SHOWN_LEVEL, level / levels);
        }
        try
        {
            for (String r : this.result.getReactantIds())
            {
                if (!(r.charAt(0) == 'E'))
                    continue;
                String edgeId = r.substring(1);
                CyEdge edge = null;
                edge = net.getEdge(Long.valueOf(edgeId));
                if (edge == null)
                    continue;
                if (t == 0)
                {
                    if (net.getRow(edge).isSet(Model.Properties.SHOWN_LEVEL))
                    {
                        net.getRow(edge).getTable().deleteColumn(Model.Properties.SHOWN_LEVEL);
                    }
                }
            }
        }
        catch (NumberFormatException ex)
        {
        }

        Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView().updateView();
    }
}
