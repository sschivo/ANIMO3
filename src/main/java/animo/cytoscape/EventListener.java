package animo.cytoscape;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JSplitPane;
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
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesEvent;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.events.VisualStyleChangedEvent;
import org.cytoscape.view.vizmap.events.VisualStyleChangedListener;
import org.cytoscape.view.vizmap.events.VisualStyleSetEvent;
import org.cytoscape.view.vizmap.events.VisualStyleSetListener;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class EventListener implements AddedEdgesListener, AddedNodesListener, SessionAboutToBeSavedListener,
		SessionLoadedListener, NetworkAddedListener, NetworkViewAddedListener, VisualStyleChangedListener,
		VisualStyleSetListener, CytoPanelComponentSelectedListener {

	public static final String APPNAME = "AppSession";

	/**
	 * Adds the newly created Edge to the Model
	 */
	@Override
	public void handleEvent(AddedEdgesEvent arg0) {
		final CyNetwork network = arg0.getSource();
		Collection<CyEdge> edgeSet = arg0.getPayloadCollection();
		for (final CyEdge edge : edgeSet) {
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					EdgeDialog edgeDialog = new EdgeDialog(network, edge);
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
	public void handleEvent(AddedNodesEvent arg0) {
		final CyNetwork network = arg0.getSource();
		Collection<CyNode> nodeSet = arg0.getPayloadCollection();
		for (final CyNode node : nodeSet) {
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					NodeDialog nodeDialog = new NodeDialog(network, node);
					nodeDialog.pack();
					nodeDialog.setCreatedNewNode();
					nodeDialog.setVisible(true);
				}
			});
		}
	}

	@Override
	public void handleEvent(NetworkViewAddedEvent e) {
		Animo.getVSA().applyVisualStyleTo(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE, e.getNetworkView()); //Default to normal style for newly created networks
		
		//Horrible and supercomplex way to reach the view window and hopefully enable the double-click to edit nodes/edges, while still maintaining the normal click features
		JSplitPane par = (JSplitPane)(Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST).getThisComponent().getParent());
		for (Component c : par.getComponents()) {
			if (c instanceof JDesktopPane) {
				JDesktopPane pane = (JDesktopPane)c;
				JInternalFrame frame = pane.getSelectedFrame();
				for (Component c2 : frame.getRootPane().getComponents()) {
					if (c2 instanceof JLayeredPane) {
						JLayeredPane pane2 = (JLayeredPane)c2;
						for (Component c3 : pane2.getComponents()) {
							//System.err.println("Ecco un componente: " + c3.getName() + " (tipo: " + c3.getClass().getName() + ")");
							//Nei mouse listener implementati in org.cytoscape.ding.impl.InnerCanvas si parla di esporre il metodo void processMouseEvent(MouseEvent)
							//per permettere ai canvas "on top of us" di passare a InnerCanvas gli eventi che loro non vogliono processare.
							//Ho trovato il commit in Cytoscape 2 in cui questo metodo veniva aggiunto, e nel commento al commit si dice che questo permetterebbe
							//al "foreground canvas" di "pass mouse events down". Il foreground canvas esattamente chi e'? E dove lo trovo? Sarebbe utile usare quello?
							//A parte l'InnerCanvas, nel LayeredPane ci sono anche 2 (due!) org.cytoscape.ding.impl.ArbitraryGraphicsCanvas e un javax.swing.JPanel normale
							//Gli ArbitraryGraphicsCanvas sono quelli per il background e il foreground (per distinguerli: il foreground ha un mouseListener registrato e l'altro no)
							if (c3.getClass().getName().endsWith("InnerCanvas")) {
								//c'e' qualcun altro dentro questo frame: dobbiamo attaccarci a lui, che e' quello dove vien disegnata la rete: del frame si riesce a cliccare direttamente solo il contorno!
								//Dobbiamo anche rimuovere il mouse listener concorrente, perche' altrimenti l'InnerCanvas mi mangia i click in quanto ci mette troppo a processare mouse up/down
								for (MouseListener listener : c3.getMouseListeners()) {
									//System.err.println("Mouse listener trovato: " + listener.getClass().getName());
									c3.removeMouseListener(listener);
									//System.err.println("\t..e rimosso");
								}
								final Component innerCanvas = c3;
								c3.addMouseListener(new MouseListener() {
									private Method mouseEntered,
												   mouseExited,
												   mousePressed,
												   mouseReleased;
									
									{
										try {
											mouseEntered = innerCanvas.getClass().getMethod("mouseEntered", MouseEvent.class);
											mouseExited = innerCanvas.getClass().getMethod("mouseExited", MouseEvent.class);
											mousePressed = innerCanvas.getClass().getMethod("mousePressed", MouseEvent.class);
											mouseReleased = innerCanvas.getClass().getMethod("mouseReleased", MouseEvent.class);
										} catch (NoSuchMethodException ex) {
										}
									}
									
									@Override
									public void mouseClicked(MouseEvent e) {
										if (e.getClickCount() == 2 && !e.isConsumed()) {
											e.consume();
											//System.err.println("Eseguo doppio-click");
											//Per reagire a doppio click su nodo/edge, reagisci solo se uno e un solo nodo/edge e' selezionato in questo momento
											CyNetwork currentNetwork = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
											CyNetworkView currentNetworkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
											//The first click of this double-click was "too fast" and triggered the execution of mousePressed + mouseReleased here below,
											//so if the double click event occurred over a node or edge, the node or edge should be selected
											List<CyNode> nodeList = CyTableUtil.getNodesInState(currentNetwork, "selected", true);
											List<CyEdge> edgeList = CyTableUtil.getEdgesInState(currentNetwork, "selected", true);
											if (nodeList.size() == 1) { //If exactly one node is selected, then we have double-clicked on a node: show the edit reactant dialog
												NodeDialog dialog = new NodeDialog(currentNetwork, nodeList.get(0));
												dialog.pack();
												dialog.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
												dialog.setVisible(true);
											} else if (nodeList.size() > 1) {
												//Come cacchio lo identifico il nodo su cui ho cliccato veramente?? Sembra che non ci sia il metodo della networkview per farsi dare il nodo dati x e y..
												try {
													Method getPickedNodeView = currentNetworkView.getClass().getMethod("getPickedNodeView", Point2D.class);
													@SuppressWarnings("unchecked")
													View<CyNode> nodeView = (View<CyNode>)getPickedNodeView.invoke(currentNetworkView, new Point2D.Float(e.getX(), e.getY()));
													NodeDialog dialog = new NodeDialog(currentNetwork, nodeView.getModel());
													dialog.showYourself();
												} catch (Exception ex) {
													NodeDialog dialog = new NodeDialog(currentNetwork, nodeList.get(0));
													dialog.showYourself();
												}
											} else if (edgeList.size() == 1) { //Same thing for the edge
												EdgeDialog dialog = new EdgeDialog(currentNetwork, edgeList.get(0));
												dialog.showYourself();
											} else if (edgeList.size() > 1) {
												try {
													Method getPickedEdgeView = currentNetworkView.getClass().getMethod("getPickedEdgeView", Point2D.class);
													@SuppressWarnings("unchecked")
													View<CyEdge> edgeView = (View<CyEdge>)getPickedEdgeView.invoke(currentNetworkView, new Point2D.Float(e.getX(), e.getY()));
													EdgeDialog dialog = new EdgeDialog(currentNetwork, edgeView.getModel());
													dialog.showYourself();
												} catch (Exception ex) {
													EdgeDialog dialog = new EdgeDialog(currentNetwork, edgeList.get(0));
													dialog.showYourself();
												}
											}
										} else if (e.getClickCount() == 1 && !e.isConsumed() && clickTooFast) {
											//System.err.println("Eseguo mousePressed + mouseReleased");
											doMousePressed(e);
											doMouseReleased(e);
										}
									}

									@Override
									public void mouseEntered(MouseEvent e) {
										if (mouseEntered == null) return;
										try {
											mouseEntered.invoke(innerCanvas, e);
										} catch (SecurityException ex) {
											ex.printStackTrace();
										} catch (IllegalAccessException ex) {
											ex.printStackTrace();
										} catch (IllegalArgumentException ex) {
											ex.printStackTrace();
										} catch (InvocationTargetException ex) {
											ex.printStackTrace();
										}
									}

									@Override
									public void mouseExited(MouseEvent e) {
										if (mouseExited == null) return;
										try {
											mouseExited.invoke(innerCanvas, e);
										} catch (SecurityException ex) {
											ex.printStackTrace();
										} catch (IllegalAccessException ex) {
											ex.printStackTrace();
										} catch (IllegalArgumentException ex) {
											ex.printStackTrace();
										} catch (InvocationTargetException ex) {
											ex.printStackTrace();
										}
									}

									private long lastMouseDown = -1;
									private int lastX = -1, lastY = -1;
									private boolean processMouseDown = false,
													mousePressedDecided = false,
													clickTooFast = false;
									final long CLICK_SENSITIVITY = 75;
									
									@Override
									public void mousePressed(final MouseEvent e) {
										lastMouseDown = e.getWhen();
										lastX = e.getX();
										lastY = e.getY();
										processMouseDown = true;
										new Thread() {
											public void run() {
												mousePressedDecided = false;
												try {
													Thread.sleep(CLICK_SENSITIVITY);
												} catch (Exception ex) {
												}
												if (!processMouseDown) {
													//System.err.println("NON eseguo mousePressed");
													mousePressedDecided = true;
													return;
												}
												//System.err.println("Eseguo mousePressed");
												doMousePressed(e);
												mousePressedDecided = true;
											}
										}.start();
									}
									
									@Override
									public void mouseReleased(MouseEvent e) {
										clickTooFast = false;
										if (e.getX() == lastX && e.getY() == lastY && e.getWhen() - lastMouseDown < CLICK_SENSITIVITY) { //If it was a (double)click, don't process here
											lastX = lastY = -1;
											lastMouseDown = -1;
											processMouseDown = false;
											clickTooFast = true;
											//System.err.println("NON eseguo mouseReleased");
											return;
										}
										try { //Try to do the mouseReleased after the mousePressed, so wait until it has decided to not occur or has finished occurring
											int c = 0;
											while (c < 10 && !mousePressedDecided) {
												Thread.sleep(15 * CLICK_SENSITIVITY / 10);
												c++;
											}
										} catch (Exception ex) {
										}
										//System.err.println("Eseguo mouseReleased");
										doMouseReleased(e);
									}
									
									private void doMousePressed(MouseEvent e) {
										if (mousePressed == null) return;
										try {
											mousePressed.invoke(innerCanvas, e);
										} catch (SecurityException ex) {
											ex.printStackTrace();
										} catch (IllegalAccessException ex) {
											ex.printStackTrace();
										} catch (IllegalArgumentException ex) {
											ex.printStackTrace();
										} catch (InvocationTargetException ex) {
											ex.printStackTrace();
										}
									}
									
									private void doMouseReleased(MouseEvent e) {
										if (mouseReleased == null) return;
										try {
											mouseReleased.invoke(innerCanvas, e);
										} catch (SecurityException ex) {
											ex.printStackTrace();
										} catch (IllegalAccessException ex) {
											ex.printStackTrace();
										} catch (IllegalArgumentException ex) {
											ex.printStackTrace();
										} catch (InvocationTargetException ex) {
											ex.printStackTrace();
										}
									}
								});
								//System.err.println("Mio mouse listener aggiunto all'InnerCanvas");
							}
						}
					}
				}
			}
		}
	}
	
	
	@Override
	public void handleEvent(NetworkAddedEvent e) {
		Animo.getVSA().applyVisualStyle(VisualStyleAnimo.ANIMO_NORMAL_VISUAL_STYLE); //Default to normal style for newly created networks
		Animo.selectAnimoControlPanel();
	}

	/**
	 * Saves the files for the session.
	 */
	@Override
	public void handleEvent(SessionAboutToBeSavedEvent arg0) {
		List<File> files = arg0.getAppFileListMap().get(APPNAME);
		this.saveSessionStateFiles(files);
	}

	/**
	 * Loads the files for the session.
	 */
	@Override
	public void handleEvent(SessionLoadedEvent arg0) {
		List<File> files = arg0.getLoadedSession().getAppFileListMap().get(APPNAME);
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
			} catch (SAXException | IOException | ParserConfigurationException e) {
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
			} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
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
	public void handleEvent(VisualStyleChangedEvent arg0) {
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
