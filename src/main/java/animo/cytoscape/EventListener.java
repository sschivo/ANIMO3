package animo.cytoscape;

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
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesEvent;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.TableAddedEvent;
import org.cytoscape.model.events.TableAddedListener;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.events.VisualStyleChangedEvent;
import org.cytoscape.view.vizmap.events.VisualStyleChangedListener;
import org.cytoscape.view.vizmap.events.VisualStyleSetEvent;
import org.cytoscape.view.vizmap.events.VisualStyleSetListener;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EventListener implements AddedEdgesListener, AddedNodesListener, SessionAboutToBeSavedListener,
		SessionLoadedListener, NetworkAddedListener, NetworkViewAddedListener, VisualStyleChangedListener,
		VisualStyleSetListener, CytoPanelComponentSelectedListener, TableAddedListener {

	public static final String APPNAME = "AppSession";
	private static boolean listenerStatus = true; //The switch to activate/deactivate the methods to deal with node/edge added events

	public static void setListenerStatus(boolean newStatus) {
		listenerStatus = newStatus;
	}
	
	public static boolean getListenerStatus() {
		return listenerStatus;
	}
	
	/**
	 * Adds the newly created Edge to the Model
	 */
	@Override
	public void handleEvent(AddedEdgesEvent e) {
		if (!listenerStatus) return;
		final CyNetwork network = e.getSource();
		Collection<CyEdge> edgeSet = e.getPayloadCollection();
//		CyTable hiddenEdgeTable = network.getTable(CyEdge.class, CyNetwork.HIDDEN_ATTRS);
		for (final CyEdge edge : edgeSet) {
//			if (hiddenEdgeTable.getColumn(Model.Properties.AUTOMATICALLY_ADDED) != null
//				&& hiddenEdgeTable.getRow(edge).isSet(Model.Properties.AUTOMATICALLY_ADDED)
//				&& hiddenEdgeTable.getRow(edge).get(Model.Properties.AUTOMATICALLY_ADDED, Boolean.class)) {
//			
//				System.err.println("NON mostro il dialog per l'edge " + edge);
//				//Set the property to false, so that next time we can react as usual
//				hiddenEdgeTable.getRow(edge).set(Model.Properties.AUTOMATICALLY_ADDED, false);
//			} else {
//				System.err.println("Mostro il dialog per l'edge " + edge + ", perche':");
//				System.err.println("\tColonna esiste? " + (hiddenEdgeTable.getColumn(Model.Properties.AUTOMATICALLY_ADDED) != null));
//				if (hiddenEdgeTable.getColumn(Model.Properties.AUTOMATICALLY_ADDED) != null) {
//					System.err.println("\tE' settata per l'edge attuale? " + hiddenEdgeTable.getRow(edge).isSet(Model.Properties.AUTOMATICALLY_ADDED));
//					if (hiddenEdgeTable.getRow(edge).isSet(Model.Properties.AUTOMATICALLY_ADDED)) {
//						System.err.println("\tHa valore true? " + hiddenEdgeTable.getRow(edge).get(Model.Properties.AUTOMATICALLY_ADDED, Boolean.class));
//					}
//				}
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						EdgeDialog edgeDialog = new EdgeDialog(network, edge);
						edgeDialog.showYourself();
					}
				});
//			}
		}
	}

	/**
	 * Adds the newly created Node to the Model
	 */
	@Override
	public void handleEvent(AddedNodesEvent e) {
		if (!listenerStatus) return;
		final CyNetwork network = e.getSource();
		Collection<CyNode> nodeSet = e.getPayloadCollection();
//		CyTable hiddenNodeTable = network.getTable(CyNode.class, CyNetwork.HIDDEN_ATTRS);
		for (final CyNode node : nodeSet) {
//			if (hiddenNodeTable.getColumn(Model.Properties.AUTOMATICALLY_ADDED) != null
//				&& hiddenNodeTable.getRow(node).isSet(Model.Properties.AUTOMATICALLY_ADDED)
//				&& hiddenNodeTable.getRow(node).get(Model.Properties.AUTOMATICALLY_ADDED, Boolean.class)) {
//				
//				//Set the property to false, so that next time we can react as usual
//				System.err.println("NON mostro il dialog per il nodo " + node);
//				hiddenNodeTable.getRow(node).set(Model.Properties.AUTOMATICALLY_ADDED, false);
//			} else {
//				System.err.println("Mostro il dialog per il nodo " + node);
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						NodeDialog nodeDialog = new NodeDialog(network, node);
						nodeDialog.showYourself();
					}
				});
//			}
		}
	}

	@Override
	public void handleEvent(NetworkViewAddedEvent e) {
		Animo.getVSA().applyVisualStyleTo(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE, e.getNetworkView()); //Default to normal style for newly created networks
		//Before (Cytoscape 2.8.x) we added here also the listeners to node/edge double click events. Now they are added in Animo.hookListeners(), and they are disguised as Edge/NodeViewTaskFactories
		System.err.println("Network view added (rete " + e.getNetworkView().getModel() + "): dovrebbe succedere DOPO network added");
	}
	
	
	@Override
	public void handleEvent(NetworkAddedEvent e) {
		Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE); //Default to normal style for newly created networks
		Animo.selectAnimoControlPanel();
		System.err.println("Network added (rete " + e.getNetwork() + "): dovrebbe succedere PRIMA di network view added"); //peccato che la prima rete che creo non lanci questa proprieta'...
	}
	
	@Override
	public void handleEvent(TableAddedEvent e) {
		Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE); //Default to normal style for newly created networks
		Animo.selectAnimoControlPanel();
		System.err.println("Table added (titolo: " + e.getTable().getTitle() + "): dovrebbe succedere DOPO di network added");
	}

	/**
	 * Saves the files for the session.
	 */
	@Override
	public void handleEvent(SessionAboutToBeSavedEvent sessionEvent) {
		List<File> files = sessionEvent.getAppFileListMap().get(APPNAME);
		this.saveSessionStateFiles(files);
	}

	/**
	 * Loads the files for the session.
	 */
	@Override
	public void handleEvent(SessionLoadedEvent sessionEvent) {
		List<File> files = sessionEvent.getLoadedSession().getAppFileListMap().get(APPNAME);
		this.restoreSessionState(files);
		//Once we loaded a session, we show the ANIMO control panel
		Animo.selectAnimoControlPanel();
		//and apply our default visual style
		Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE);
	}

	/**
	 * Retrieve the relevant information for ANIMO in the saved files.
	 */
	private void restoreSessionState(List<File> myFiles) {
		CytoPanel results = Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST);
		if (results != null) {
			List<Component> panelsToBeRemoved = new ArrayList<Component>();
			for (int i = 0; i < results.getCytoPanelComponentCount(); i++) {
				Component c = results.getComponentAt(i);
				panelsToBeRemoved.add(c);
			}
		}
		// Then load the .sim files from the list of files
		if ((myFiles == null) || myFiles.isEmpty()) {
			// No simulations to restore
			return;
		}
		Map<String, BufferedImage> mapIdToImage = new HashMap<String, BufferedImage>();
		for (File f : myFiles) {
			String name = f.getName();
			if (!name.endsWith(".png"))
				continue;
			BufferedImage image;// = ImageIO.read(f);
			Document doc;
			try {
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			doc.getDocumentElement().normalize();
			byte[] decodedImage = DatatypeConverter.parseBase64Binary(doc.getFirstChild().getFirstChild()
					.getTextContent());
			// The first child is called "image", and its child is the CDATA section with all the binary data

			try {
				image = ImageIO.read(new ByteArrayInputStream(decodedImage));
				mapIdToImage.put(name.substring(0, name.lastIndexOf(".png")), image);
			} catch (IOException ex) {
				// Hopefully we could load the image. Otherwise, it's no drama
				ex.printStackTrace();
			}
		}
		for (File f : myFiles) {
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

	private void saveAnyOpenResults(List<File> myFiles, Component c) {
		if (!(c instanceof AnimoResultPanel) && c instanceof Container) {
			for (Component c2 : ((Container)c).getComponents()) {
				if (c2 instanceof AnimoResultPanel) {
					c = c2;
					break;
				}
			}
		}
		if (c instanceof AnimoResultPanel) {
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
			try {
				ImageIO.write(panel.getSavedNetworkImage(), "png", baos);
				/*
				 * "Why do all this mess instead of simply writing the image to a file with ImageIO?" , you may ask. Well, Cytoscape does not allow you to simply save binary files
				 * (images or files output with ObjectOutputStream) because it preprocesses (adding some useless encoding stuff) when opening them. While if we save them as binary
				 * inside an xml (thus in a CDATA section) all is good.
				 */
				baos.flush();
				encodedImage = DatatypeConverter.printBase64Binary(baos.toByteArray());
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			Document doc;
			try {
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return;
			}
			Element root = doc.createElement("image");
			doc.appendChild(root);
			CDATASection node = doc.createCDATASection(encodedImage);
			root.appendChild(node);
			Transformer tra;
			try {
				tra = TransformerFactory.newInstance().newTransformer();
			} catch (Throwable e) {
				e.printStackTrace();
				return;
			}
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(outputFile);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return;
			}
			try {
				tra.transform(new DOMSource(doc), new StreamResult(fos));
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			try {
				fos.flush();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			myFiles.add(outputFile);
		} else if (c instanceof Container) {
			for (Component o : ((Container) c).getComponents()) {
				saveAnyOpenResults(myFiles, o);
			}
		}
	}

	/**
	 * Save the needed files in this session, so that we will be able to retrieve them when reloading the same session later.
	 */
	private void saveSessionStateFiles(List<File> myFiles) {
		// Save any open results panel as a .sim file and add it to the list
		CytoPanel results = Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST);
		if (results != null) {
			for (int i = 0; i < results.getCytoPanelComponentCount(); i++) {
				Component c = results.getComponentAt(i);
				saveAnyOpenResults(myFiles, c);
			}
		}
	}

	//The visual style has changed, we need to update the legends.
	@Override
	public void handleEvent(VisualStyleChangedEvent e) {
		//if (Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView() != null) {
		Animo.getVSA().updateLegends();
		//}
	}
	
	@Override
	public void handleEvent(VisualStyleSetEvent arg0) {
		Animo.getVSA().updateLegends();
	}
	
	//A new tab has been selected: if it was in the Results Panel and it was one of ours, then resize the panel suitably
	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent ev) {
		if (ev.getCytoPanel().getCytoPanelName().equals(CytoPanelName.EAST)) {
			if (ev.getCytoPanel().getSelectedComponent() != null && ev.getCytoPanel().getSelectedComponent() instanceof ResultPanelContainer) {
				AnimoResultPanel.adjustDivider();
				((ResultPanelContainer)ev.getCytoPanel().getSelectedComponent()).ensureCorrectVisualStyle();
			}
		}
	}
}
