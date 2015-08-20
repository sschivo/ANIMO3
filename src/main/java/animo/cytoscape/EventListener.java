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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesEvent;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.session.CySession;
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

import animo.core.graph.FileUtils;
import animo.core.model.Model;

public class EventListener implements AddedEdgesListener, AddedNodesListener, SessionAboutToBeSavedListener,
		SessionLoadedListener, NetworkAddedListener, NetworkViewAddedListener, VisualStyleChangedListener,
		VisualStyleSetListener, CytoPanelComponentSelectedListener, RowsSetListener {

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
	public void handleEvent(final NetworkViewAddedEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
				Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE);
				//Animo.getVSA().applyVisualStyleTo(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE, e.getNetworkView()); //Default to normal style for newly created networks
				Animo.selectAnimoControlPanel();
				Animo.getVSA().updateLegends();
				//Before (Cytoscape 2.8.x) we added here also the listeners to node/edge double click events. Now they are added in Animo.hookListeners(), and they are disguised as Edge/NodeViewTaskFactories
				//System.err.println("Network view added (rete " + e.getNetworkView().getModel() + "): dovrebbe succedere DOPO network added");
		    }
		});
	}
	
	//This does NOT seem to be called the first time a network is created in a Cytoscape session!!
	//Also, we can set a visual style only when we have a network view, so this is not very useful..
	@Override
	public void handleEvent(NetworkAddedEvent e) {
//		SwingUtilities.invokeLater(new Runnable() {
//		    public void run() {
//				Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE); //Default to normal style for newly created networks
//				Animo.selectAnimoControlPanel();
//				Animo.getVSA().updateLegends();
//				//System.err.println("Network added (rete " + e.getNetwork() + "): dovrebbe succedere PRIMA di network view added"); //peccato che la prima rete che creo non lanci questa proprieta'...
//		    }
//		});
	}

	/**
	 * Saves the files for the session.
	 */
	@Override
	public void handleEvent(SessionAboutToBeSavedEvent sessionEvent) {
		List<File> files = sessionEvent.getAppFileListMap().get(Animo.APP_NAME);
		this.saveSessionStateFiles(files);
	}

	/**
	 * Loads the files for the session.
	 */
	@Override
	public void handleEvent(SessionLoadedEvent sessionEvent) {
		FileUtils.resetCurrentDirectory(); //The next time we use an open dialog, it will try to start from the directory where the session file was found
		List<File> files = sessionEvent.getLoadedSession().getAppFileListMap().get(Animo.APP_NAME);
		if (files == null) {
			files = sessionEvent.getLoadedSession().getAppFileListMap().get("InatPlugin"); //Try the old name before abandoning all hope
		}
		this.restoreSessionState(files);
		//Once we loaded a session, we show the ANIMO control panel
		Animo.selectAnimoControlPanel();
		//and apply our default visual style
		Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE);
		
		try {
			//Use the Cytoscape ID mapping service provided by the session to translate an ANIMO 2.x model into a new one (just the SUIDs referred in the edges table)
			//For ANIMO 3.x models, change the references to node/edge SUIDs from the previous session to the current, to make sure that they still refer to existing objects
			CySession session = sessionEvent.getLoadedSession();
			boolean translatedOld2xToNew3x = false;
			for (CyNetwork network : session.getNetworks()) {
//				System.err.println("Rete " + network.getRow(network).get(CyNetwork.NAME, String.class));
				//Apparently, we need to use the LOCAL tables, not the default ones!
//				CyTable edgesTable = network.getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS), //network.getDefaultEdgeTable(),
//						nodesTable = network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS); //network.getDefaultNodeTable();
//				if (edgesTable == null) {
//					edgesTable = network.getDefaultEdgeTable();
//				}
//				if (nodesTable == null) {
//					nodesTable = network.getDefaultNodeTable();
//				}
				
				CyTable edgesTable, nodesTable;
				String tableTypes[] = new String[] { CyNetwork.LOCAL_ATTRS/*, CyNetwork.DEFAULT_ATTRS, CyNetwork.HIDDEN_ATTRS*/ };
				for (String tableType : tableTypes) { //Repeat all this stuff for all the available tables. Probably it will save us from errors. Actually, it works better if we just use the local table
//					System.err.println("Tabella " + tableType);
					edgesTable = network.getTable(CyEdge.class, tableType);
					nodesTable = network.getTable(CyNode.class, tableType);
					
					if (edgesTable == null || nodesTable == null) continue;
					
					String colNames[] = new String[] {
										   Model.Properties.REACTANT_ID_E1,
										   Model.Properties.REACTANT_ID_E2,
										   Model.Properties.OUTPUT_REACTANT };
					
					boolean areAllColumnsString = true,
							areAllColumnsLong = true;
					for (String colName : colNames) {
						CyColumn column = edgesTable.getColumn(colName);
//						System.err.println("Colonna " + colName + ": " + column);
						if (column == null) {
							if (colName.equals(Model.Properties.OUTPUT_REACTANT)) { //This can happen (because in a previous version we didn't consider this column (!!)), but we can guess the value for that: the downstream node
								edgesTable.createColumn(colName, areAllColumnsString?String.class:Long.class, false);
								column = edgesTable.getColumn(colName);
							} else {
								areAllColumnsLong = areAllColumnsString = false;
								break;
							}
						}
						if (!column.getType().equals(String.class)) {
							areAllColumnsString = false;
						}
						if (!column.getType().equals(Long.class)) {
							areAllColumnsLong = false;
						}
						if (!areAllColumnsString && !areAllColumnsLong) break;
					}
					if (areAllColumnsString) { //We have an old ANIMO 2.x model: we must convert these node names into SUIDs
	//					System.err.println("Traduco un modello 2.x in 3.x");
//						System.err.println("La rete " + network.getRow(network).get(CyNetwork.NAME, String.class) + " ha le colonne richieste, versione 2.x");
						Map<Long, String[]> savedValues = new HashMap<Long, String[]>();
						for (CyEdge edge : network.getEdgeList()) { //Look at each edge and save the values for the required columns (see colNames for the column names)
							CyRow edgeRow = edgesTable.getRow(edge.getSUID());
							String nodeIDs[] = new String[colNames.length];
							if (!edgeRow.isSet(Model.Properties.REACTANT_ID_E1) || !edgeRow.isSet(Model.Properties.REACTANT_ID_E2)) continue; //If we just miss the output reactant (strange, but it can happen..), we will guess it to be the downstream node
	//						System.err.print("L'edge " + edgeRow.get(Model.Properties.CANONICAL_NAME, String.class) + " ha questi valori: ");
							for (int i=0; i<colNames.length; i++) {
								String colName = colNames[i];
								String oldId = null;
								if (edgeRow.isSet(colName) && (oldId = edgeRow.get(colName, String.class)) != null) {
									nodeIDs[i] = oldId; //These values are the IDs in the old Cytoscape format, so they are strings. We translate them to the corresponding SUID
	//								System.err.print(nodeIDs[i] + ", ");
								}
							}
	//						System.err.println();
							savedValues.put(edge.getSUID(), nodeIDs);
						}
						for (String colName : colNames) { //Change the the type of the columns from String to Long
							edgesTable.deleteColumn(colName);
							edgesTable.createColumn(colName, Long.class, false);
						}
						for (Long edgeId : savedValues.keySet()) { //Copy back the values for those edges that had them, translating the old String ID in the new Long SUID
							CyEdge edge = network.getEdge(edgeId);
							String[] oldIDs = savedValues.get(edgeId);
	//						System.err.println("Recupero i valori per l'edge " + network.getRow(edge).get(Model.Properties.CANONICAL_NAME, String.class));
							for (int i=0; i<oldIDs.length; i++) {
								String oldId = oldIDs[i];
								if (oldId == null && colNames[i].equals(Model.Properties.OUTPUT_REACTANT)) { //We didn't have a value for this field, but we can guess it to be the downstream node
									edgesTable.getRow(edge.getSUID()).set(colNames[i], edge.getTarget().getSUID());
								} else {
									CyNode node = session.getObject(oldId, CyNode.class);
									if (node == null) { //If I can't find the node by looking at its name, I can't do much more
		//								System.err.println("Problema: il nodo che prima chiamavo " + oldId + " ora non lo trovo più!");
										Collection<CyRow> matchingRows = nodesTable.getMatchingRows(CyNetwork.NAME, oldId);
										for (CyRow nodeRow : matchingRows) {
											try {
												node = network.getNode(nodeRow.get(CyNode.SUID, Long.class));
		//										System.err.println("Il nome " + nodeRow.get(CyNetwork.NAME, String.class) + " sembra aver funzionato");
												break;
											} catch (Exception ex) {
												//node is still null
		//										System.err.println("Il nome " + nodeRow.get(CyNetwork.NAME, String.class) + " non risulta usabile");
											}
										}
										if (node == null) {
		//									System.err.println("Non sono riuscito a trovarlo neanche cercandolo per nome");
											continue;
										}
									}
									Long nodeId = node.getSUID();
		//							System.err.println("Ho trovato il nodo: prima si chiamava " + oldId + " e ora ha ID " + nodeId + " (di nome fa " + network.getRow(node).get(Model.Properties.CANONICAL_NAME, String.class) + ")");
									edgesTable.getRow(edge.getSUID()).set(colNames[i], nodeId);
								}
							}
						}
						translatedOld2xToNew3x = true;
					} else if (areAllColumnsLong) { //ANIMO 3.x model: map the saved node SUIDs to current SUIDs
	//					System.err.println("Traduco un modello 3.x in 3.x");
//						System.err.println("La rete " + network.getRow(network).get(CyNetwork.NAME, String.class) + " ha le colonne richieste, versione 3.x");
						for (CyEdge edge : network.getEdgeList()) {
							CyRow edgeRow = edgesTable.getRow(edge.getSUID());
							for (String colName : colNames) {
								if (colName.equals(Model.Properties.OUTPUT_REACTANT) && !edgeRow.isSet(colName)) { //If no output reactant is set, we guess it to be the downstream node
									edgeRow.set(colName, edge.getTarget().getSUID());
								} else {
									if (!edgeRow.isSet(colName)) {
										continue;
									}
									Long oldId = edgeRow.get(colName, Long.class);
									CyNode foundNode = session.getObject(oldId, CyNode.class);
									if (foundNode == null) {
		//								System.err.println("Problema: il nodo che prima chiamavo " + oldId + " ora non lo trovo!");
										continue;
									}
									edgeRow.set(colName, foundNode.getSUID());
								}
							}
						}
					} /*else {
						System.err.println("La rete " + network.getRow(network).get(CyNetwork.NAME, String.class) + " NON ha tutte le colonne richieste");
					}*/
				}
				
			}
			if (translatedOld2xToNew3x) { //If we translated an old 2.x model into the new version (String IDs to Long IDs), we need to warn the user that saving this model will make it unreadable for older versions of ANIMO
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "If you save this model, you will not be able to open it\nwith an old version of ANIMO (Cytoscape 2.8.x)", "Old model converted to new", JOptionPane.WARNING_MESSAGE);
					}
				});
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	//If a node was enabled or disabled, check the surrounding (incoming/outgoing) edges and make them also enabled/disabled
	//But ONLY if the property actually changed! If it was set to its previous value I don't want to interfere...
	//Unfortunately, it is not possible to know the previous value of the property, so I have instead done so that I check before setting it in the NodeDialog
	public void handleEvent(RowsSetEvent ev) {
		//System.err.println("handleEvent per " + ev.getColumnRecords(Model.Properties.ENABLED).size() + " record!");
		if (   !ev.containsColumn(Model.Properties.ENABLED)
			&& !ev.containsColumn(Model.Properties.NUMBER_OF_LEVELS)) {
			
			return;
		}
		
		if (ev.containsColumn(Model.Properties.ENABLED)) { //A node or edge has been enabled/disabled
			CyNetwork network = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
			CyTable nodesDefaultTable = network.getDefaultNodeTable(),
					nodesLocalTable = network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS);
			for (RowSetRecord rec : ev.getColumnRecords(Model.Properties.ENABLED)) {
				if (!rec.getRow().getTable().equals(nodesDefaultTable)
					&& !rec.getRow().getTable().equals(nodesLocalTable)) { //Only look at nodes
					continue;
				}
				Long nodeID = rec.getRow().get(CyNode.SUID, Long.class);
				if (nodeID == null) continue;
				CyNode node = network.getNode(nodeID);
				if (node == null) continue;
				boolean status = rec.getRow().get(Model.Properties.ENABLED, Boolean.class);
				//If the node was enabled, we want to enable those of its adjacent edges that connect enabled nodes. If it was disabled, we disable all adjacent edges.
				for (CyEdge edge : network.getAdjacentEdgeList(node, CyEdge.Type.ANY)) { //Look at both incoming and outgoing edges
					CyNode source = (CyNode)edge.getSource(),
						   target = (CyNode)edge.getTarget();
					CyRow rowSource = network.getRow(source),
						  rowTarget = network.getRow(target),
						  rowEdge = network.getRow(edge);
					
					//Node enabled: only if both source and target are enabled can an edge become enabled
					if (status
						&& (rowSource.isSet(Model.Properties.ENABLED) && rowSource.get(Model.Properties.ENABLED, Boolean.class))
						&& (rowTarget.isSet(Model.Properties.ENABLED) && rowTarget.get(Model.Properties.ENABLED, Boolean.class))) {
						rowEdge.set(Model.Properties.ENABLED, true);
					} else if (!status) { //Node disabled: all edges connected to it must be disabled
						rowEdge.set(Model.Properties.ENABLED, false);
					}
				}
			}
		}
		
		if (ev.containsColumn(Model.Properties.NUMBER_OF_LEVELS)) { //See if we need to update the maximum number of levels stored as a network property
			CyNetwork network = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
			boolean onlyNetworkChanged = true; //If only the number of levels of the network was changed, take no action (we change it here, so we don't want to react to our own change, but we also want to allow the user to change the number of levels in a network by hand (it nearly only influences the scale of the graphs)
			CyTable networkDefaultTable = network.getDefaultNetworkTable(),
					networkLocalTable = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
			for (RowSetRecord rec : ev.getColumnRecords(Model.Properties.NUMBER_OF_LEVELS)) {
				if (!rec.getRow().getTable().equals(networkDefaultTable)
					&& !rec.getRow().getTable().equals(networkLocalTable)) {
					onlyNetworkChanged = false;
					break;
				}
			}
			if (!onlyNetworkChanged) {
				int currentMaxNLevels = 1;
				for (CyNode n : network.getNodeList()) {
					int nL = network.getRow(n).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
					if (nL > currentMaxNLevels) {
						currentMaxNLevels = nL;
					}
				}
				CyRow netRow = network.getRow(network);
				int currentNetworkLevels = netRow.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
				if (currentMaxNLevels != currentNetworkLevels) {
					netRow.set(Model.Properties.NUMBER_OF_LEVELS, currentMaxNLevels);
				}
			}
		}
	}
	
	private CytoPanelComponent findResultPanel(Component c) {
		if (c instanceof CytoPanelComponent) {
			return (CytoPanelComponent)c;
		}
		if (c instanceof Container) {
			Container cont = (Container)c;
			for (Component comp : cont.getComponents()) {
				CytoPanelComponent res = findResultPanel(comp);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve the relevant information for ANIMO in the saved files.
	 */
	private void restoreSessionState(List<File> myFiles) {
		System.err.println("Ristoro la sessione dai file " + myFiles);
		CytoPanel results = Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST);
		if (results != null) {
			List<Component> panelsToBeRemoved = new ArrayList<Component>();
			for (int i = 0; i < results.getCytoPanelComponentCount(); i++) {
				Component c = results.getComponentAt(i);
				panelsToBeRemoved.add(c);
			}
			for (Component c : panelsToBeRemoved) {
				Animo.removeResultPanel(findResultPanel(c));
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
			if (panel == null) { //The panel may be null if some of the classes for the data saved in the .sim file are not available anymore (i.e., we have an ANIMO 2.x simulation)
				f.delete();
				new File(name.substring(0, name.lastIndexOf(".sim")) + ".png").delete(); //Clean up also the useless saved image
				continue;
			}
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
