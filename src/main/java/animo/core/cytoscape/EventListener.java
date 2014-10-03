package animo.core.cytoscape;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesEvent;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class EventListener implements AddedEdgesListener, AddedNodesListener, SessionAboutToBeSavedListener, SessionLoadedListener, NetworkViewAddedListener
{

    public static final String APPNAME = "AppSession";

    /**
     * Adds the newly created Edge to the Model
     */
    @Override
    public void handleEvent(AddedEdgesEvent arg0)
    {
        final CyNetwork network = arg0.getSource();
        Collection<CyEdge> edgeSet = arg0.getPayloadCollection();
        for (final CyEdge edge : edgeSet)
        {
            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    EdgeDialog edgeDialog = new EdgeDialog(edge, network);
                    edgeDialog.pack();
                    edgeDialog.setCreatedNewEdge();
                    edgeDialog.setVisible(true);
                }
            });
        }
    }

    /**
     * Adds the newly created Node to the Model
     */
    @Override
    public void handleEvent(AddedNodesEvent arg0)
    {
        final CyNetwork network = arg0.getSource();
        Collection<CyNode> nodeSet = arg0.getPayloadCollection();
        for (final CyNode node : nodeSet)
        {
            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    NodeDialog nodeDialog = new NodeDialog(network, node);
                    nodeDialog.pack();
                    nodeDialog.setCreatedNewNode();
                    nodeDialog.setVisible(true);
                }
            });
        }
    }

    @Override
    public void handleEvent(NetworkViewAddedEvent e)
    {
        Animo.getVSA().applyTo(e.getNetworkView());


        // TODO Auto-generated method stub

    }

    /**
     * Saves the files for the session.
     */
    @Override
    public void handleEvent(SessionAboutToBeSavedEvent arg0)
    {
        List<File> files = arg0.getAppFileListMap().get(APPNAME);
        this.saveSessionStateFiles(files);
    }

    /**
     * Loads the files for the session.
     */
    @Override
    public void handleEvent(SessionLoadedEvent arg0)
    {
        List<File> files = arg0.getLoadedSession().getAppFileListMap().get(APPNAME);
        this.restoreSessionState(files);
    }

    /**
     * Retrieve the relevant information for ANIMO in the saved files.
     */
    private void restoreSessionState(List<File> myFiles)
    {
        CytoPanel results = Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST);
        if (results != null)
        {
            List<Component> panelsToBeRemoved = new ArrayList<Component>();
            for (int i = 0; i < results.getCytoPanelComponentCount(); i++)
            {
                Component c = results.getComponentAt(i);
                panelsToBeRemoved.add(c);
            }
        }
        // Then load the .sim files from the list of files
        if ((myFiles == null) || myFiles.isEmpty())
        {
            // No simulations to restore
            return;
        }
        Map<String, BufferedImage> mapIdToImage = new HashMap<String, BufferedImage>();
        for (File f : myFiles)
        {
            String name = f.getName();
            if (!name.endsWith(".png"))
                continue;
            BufferedImage image;// = ImageIO.read(f);
            Document doc;
            try
            {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            }
            catch (SAXException | IOException | ParserConfigurationException e)
            {
                e.printStackTrace();
                continue;
            }
            doc.getDocumentElement().normalize();
            byte[] decodedImage = DatatypeConverter.parseBase64Binary(doc.getFirstChild().getFirstChild().getTextContent());
            // The first child
            // is called
            // "image", and its
            // child is the
            // CDATA section
            // with all the
            // binary data

            try
            {
                image = ImageIO.read(new ByteArrayInputStream(decodedImage));
                mapIdToImage.put(name.substring(0, name.lastIndexOf(".png")), image);
            }
            catch (IOException ex)
            {
                // Hopefully we could load the image. Otherwise, it's no drama
                ex.printStackTrace();
            }
        }
        for (File f : myFiles)
        {
            String name = f.getName();
            if (!name.endsWith(".sim"))
                continue;
            AnimoResultPanel panel = AnimoResultPanel.loadFromSessionSimFile(f);
            BufferedImage image = mapIdToImage.get(name.substring(0, name.lastIndexOf(".sim")));
            // System.err.println("Retrievo immagine di id " + name.substring(0,
            // name.lastIndexOf(".sim")) + ": " + image);
            panel.setSavedNetworkImage(image);
            panel.addToPanel(results);
        }

    }

    private void saveAnyOpenResults(List<File> myFiles, Component c)
    {
        if (c instanceof AnimoResultPanel)
        {
            String tmpDir = System.getProperty("java.io.tmpdir");
            AnimoResultPanel panel = (AnimoResultPanel) c;
            CyNetwork savedNetwork = panel.getSavedNetwork();
            if (savedNetwork == null)
                return;
            File outputFile = new File(tmpDir, savedNetwork.getSUID() + ".sim");
            outputFile.deleteOnExit();
            panel.saveSimulationData(outputFile, false);
            myFiles.add(outputFile);
            outputFile = new File(tmpDir, savedNetwork.getSUID() + ".png");
            outputFile.deleteOnExit();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String encodedImage;
            try
            {
                ImageIO.write(panel.getSavedNetworkImage(), "png", baos);
                /*
                 * "Why do all this mess instead of simply writing the image to a file with ImageIO?"
                 * , you may ask. Well, Cytoscape does not allow you to simply
                 * save binary files (images or files output with
                 * ObjectOutputStream) because it preprocesses (adding some
                 * useless encoding stuff) when opening them. While if we save
                 * them as binary inside an xml (thus in a CDATA section) all is
                 * good.
                 */
                baos.flush();
                encodedImage = DatatypeConverter.printBase64Binary(baos.toByteArray());
                baos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
            Document doc;
            try
            {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            }
            catch (ParserConfigurationException e)
            {
                e.printStackTrace();
                return;
            }
            Element root = doc.createElement("image");
            doc.appendChild(root);
            CDATASection node = doc.createCDATASection(encodedImage);
            root.appendChild(node);
            Transformer tra;
            try
            {
                tra = TransformerFactory.newInstance().newTransformer();
            }
            catch (TransformerConfigurationException | TransformerFactoryConfigurationError e)
            {
                e.printStackTrace();
                return;
            }
            tra.setOutputProperty(OutputKeys.INDENT, "yes");
            tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            FileOutputStream fos;
            try
            {
                fos = new FileOutputStream(outputFile);
            }
            catch (FileNotFoundException e1)
            {
                e1.printStackTrace();
                return;
            }
            try
            {
                tra.transform(new DOMSource(doc), new StreamResult(fos));
            }
            catch (TransformerException e)
            {
                e.printStackTrace();
            }
            try
            {
                fos.flush();
                fos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            myFiles.add(outputFile);
        }
        else if (c instanceof Container)
        {
            for (Component o : ((Container) c).getComponents())
            {
                saveAnyOpenResults(myFiles, o);
            }
        }
    }

    /**
     * Save the needed files in this session, so that we will be able to
     * retrieve them when reloading the same session later.
     */
    private void saveSessionStateFiles(List<File> myFiles)
    {
        // Save any open results panel as a .sim file and add it to the list
        CytoPanel results = Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST);
        if (results != null)
        {
            for (int i = 0; i < results.getCytoPanelComponentCount(); i++)
            {
                Component c = results.getComponentAt(i);
                saveAnyOpenResults(myFiles, c);
            }
        }
    }
}
