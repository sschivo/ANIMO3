/**
 * 
 */
package animo.util;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.graph.FileUtils;
import animo.cytoscape.Animo;

/**
 * An XML configuration file.
 * 
 * @author B. Wanders
 */
public class XmlConfiguration {
	/**
	 * Path to the verifyta tool to simulate and check Timed Automata models.
	 */
	public static final String VERIFY_KEY = "/" + Animo.APP_NAME + "/UppaalInvoker/verifyta";
	public static final String DEFAULT_VERIFY = "\\uppaal-4.1.11\\bin-Win32\\verifyta.exe";

	/**
	 * Are we the developer? If so, enable more options and more debugging
	 */
	public static final String DEVELOPER_KEY = "/" + Animo.APP_NAME + "/Developer";
	public static final String DEFAULT_DEVELOPER = Boolean.FALSE.toString();

	// The value for uncertainty in the reaction parameters
	public static final String UNCERTAINTY_KEY = "/" + Animo.APP_NAME + "/Uncertainty";
	public static final String DEFAULT_UNCERTAINTY = "5"; //Default to 5, but we propose to set it to 0 when asked to perform model checking

	// We can use reaction-centered model (default), reaction-centered with tables, and reactant-centered.
	public static final String MODEL_TYPE_KEY = "/" + Animo.APP_NAME + "/ModelType";
	public static final String MODEL_TYPE_REACTION_CENTERED = "ReactionCentered",
			MODEL_TYPE_REACTION_CENTERED_TABLES = "ReactionCenteredTables",
			MODEL_TYPE_REACTION_CENTERED_TABLES_OLD = "ReactionCenteredTablesOld",
			MODEL_TYPE_REACTANT_CENTERED = "ReactantCentered",
			MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE = "ReactantCenteredMorePrecise",
			MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE_NEW = "ReactantCenteredMorePreciseNew",
			MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE_NEW4 = "ReactantCenteredMorePreciseNew4",
			MODEL_TYPE_REACTANT_CENTERED_SUPER_DETERMINISTIC = "ReactantCenteredSuperDeterministic",
			MODEL_TYPE_REACTANT_CENTERED_MAYBE_DETERMINISTIC = "ReactantCenteredMaybeDeterministic",
			MODEL_TYPE_REACTANT_CENTERED_MAYBE_DETERMINISTIC2 = "ReactantCenteredMaybeDeterministic2",
			MODEL_TYPE_REACTANT_CENTERED_OPAAL = "ReactantCenteredOpaal",
			MODEL_TYPE_ODE = "ODEforUPPAAL";
	public static final String DEFAULT_MODEL_TYPE = MODEL_TYPE_REACTANT_CENTERED;

	/**
	 * The document that backs this configuration.
	 */
	private Document document;
	private File configFile = null;

	// The configuration pairs key->val on which to base the document writing
	private HashMap<String, String> sourceConfig = new HashMap<String, String>();

	/**
	 * Read the configuration from already existing file.
	 * 
	 * @param doc
	 *            the configuration document
	 */
	public XmlConfiguration(Document doc, File configuration) {
		this.document = doc;
		this.configFile = configuration;
		String v;
		v = this.get(VERIFY_KEY, null);
		if (v != null && new File(v).exists()) { //Always check that the location we know for verifyta still exists
			sourceConfig.put(VERIFY_KEY, v);
		} else {
			//sourceConfig.put(VERIFY_KEY, DEFAULT_VERIFY); It makes no sense to have a default location for uppaal verifyta if it's very unlikely to work..
			v = null;
			try {
				v = findOrInstallVerifyta();
			} catch (Exception ex) {
				v = null;
			}
			if (v == null) {
				sourceConfig.put(VERIFY_KEY, DEFAULT_VERIFY);
				//JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "ANIMO cannot perform any analysis.\nPlease press the \"Options...\" button\nat the bottom of the ANIMO panel\nwhen you know the location of the\nUPPAAL verifyta program.", "No UPPAAL verifyta program was found", JOptionPane.WARNING_MESSAGE);
			} else {
				sourceConfig.put(VERIFY_KEY, v);
			}
		}
		v = this.get(DEVELOPER_KEY, null);
		if (v != null) {
			sourceConfig.put(DEVELOPER_KEY, v);
		} else {
			sourceConfig.put(DEVELOPER_KEY, DEFAULT_DEVELOPER);
		}
		v = this.get(UNCERTAINTY_KEY, null);
		if (v != null) {
			sourceConfig.put(UNCERTAINTY_KEY, v);
		} else {
			sourceConfig.put(UNCERTAINTY_KEY, DEFAULT_UNCERTAINTY);
		}
		v = this.get(MODEL_TYPE_KEY, null);
		if (v != null) {
			sourceConfig.put(MODEL_TYPE_KEY, v);
		} else {
			sourceConfig.put(MODEL_TYPE_KEY, DEFAULT_MODEL_TYPE);
		}
		try {
			writeConfigFile();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean done = false;
	private String verifytaLocationStr;
	private String findVerifytaLocation() {
		String verifytaFileName = "verifyta";
		if (UppaalModelAnalyserSMC.areWeUnderWindows()) {
			verifytaFileName += ".exe";
		}
		//System.err.println("Provo prima a cercare il file da solo...");
		final String verifytaFileName_ = verifytaFileName;
		final ObservableTask findFileTask = new ObservableTask() {
			private File verifytaFile = null;
			private Thread threadWhereTheSearchHappens = null;
			
			
			@Override
			public void cancel() {
				//done = true; The done status should be set only by the task observer, not by the task!
				threadWhereTheSearchHappens.interrupt(); //This way we can signal the search process to stop
			}

			@Override
			public void run(TaskMonitor monitor) throws Exception {
				monitor.setTitle("ANIMO initial configuration");
				monitor.setProgress(0);
				monitor.setStatusMessage("Looking for UPPAAL verifyta program");
				// startTime = System.currentTimeMillis();
				threadWhereTheSearchHappens = Thread.currentThread();
				verifytaFile = FileUtils.findFileInDirectory(verifytaFileName_, "uppaal");
				if (Thread.interrupted()) { //This also clears the last setting for interrupt (if I started it in cancel())
					monitor.setStatusMessage("Search interrupted by the user.");
				}
				//long endTime = System.currentTimeMillis();
				//System.err.println("La ricerca e' durata " + AnimoActionTask.timeDifferenceFormat((endTime - startTime)/1000));
				if (verifytaFile != null && verifytaFile.exists()) {
					monitor.setStatusMessage("verifyta found at " + verifytaFile.getAbsolutePath());
				} else {
					monitor.setStatusMessage("verifyta not found");
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public <R> R getResults(Class<? extends R> i) {
				return (R)verifytaFile;
			}
		};
		TaskObserver observer = new TaskObserver() {
			@Override
			public void allFinished(FinishStatus s) {
				//System.err.println("Tutti i task sono finiti!");
				//System.err.println("Risultati " + findFileTask.getResults(null) + " (di tipo " + findFileTask.getResults(null).getClass() + ")");
				File verifytaFile = null;
				verifytaLocationStr = null;
				if (s.getType().equals(FinishStatus.Type.SUCCEEDED)
						&& findFileTask.getResults(null) != null && ((File)findFileTask.getResults(null)).exists()) {
					verifytaFile = (File)findFileTask.getResults(null);
					//System.err.println("Trovato verifyta al posto \"" + verifytaFile.getAbsolutePath() + "\"!!");
					JOptionPane.showMessageDialog(null, "ANIMO was correctly installed.\nUPPAAL verifyta program was found at\n" + verifytaFile.getAbsolutePath() + ".\nIf you want to change this, press \"Options...\"\nat the bottom of the ANIMO panel.");
					verifytaLocationStr = verifytaFile.getAbsolutePath();
					done = true;
				} else {
					//System.err.println("Non trovato il verifyta: mostro l'open dialog!");
					JOptionPane.showMessageDialog(
							Animo.getCytoscape().getJFrame(),
							"Please, find and select the \"verifyta\" tool.\nIt is usually located in the \"bin\" directory of UPPAAL.",
							"Verifyta", JOptionPane.QUESTION_MESSAGE);
					//System.err.println("Apro il dialogo per verifyta");
					//For some reason, if I pass Animo.getCytoscape().getJFrame() in this case (when Cytoscape has just started), the dialog is NOT shown and Cytoscape does not respond anymore.. o_O
					//Using null as a parent has the same effect.
					//So I changed the open function: if it gets a null parent, it creates a 1x1 pixel window for a parent, then destroys it when the dialog has been used
					//Apparently that didn't work either. If I run the thing on a different thread after this function has done, we get what we want.
					SwingUtilities.invokeLater(new Thread() {
						public void run() {
							verifytaLocationStr = FileUtils.open(verifytaFileName_, "Verifyta Executable", "Find UPPAAL's verifyta program", null); //Animo.getCytoscape().getJFrame());
							//System.err.println("Acquisita la stringa: " + verifytaLocationStr);
							done = true;
						}
					});
				}
			}

			@Override
			public void taskFinished(ObservableTask s) {
				//just look at allFinished
			}
		};
		TaskManager<?, ?> tm = Animo.getCyServiceRegistrar().getService(TaskManager.class);
		done = false;
		tm.execute(new TaskIterator(findFileTask), observer);
		while (!done) {
			try {
				Thread.sleep(100);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
		return verifytaLocationStr;
	}
	
	//This should be called when looking for verifyta: it does all the work, including bringing the user to the
	//UPPAAL download page and trying again to look for verifyta
	private String findOrInstallVerifyta() {
		verifytaLocationStr = null;
		try {
			verifytaLocationStr = findVerifytaLocation();
		} catch (Exception ex) {
			verifytaLocationStr = null;
		}
		if (verifytaLocationStr != null) {
			//sourceConfig.put(VERIFY_KEY, verifytaLocationStr);
			return verifytaLocationStr;
		} else {
			//sourceConfig.put(VERIFY_KEY, DEFAULT_VERIFY);
			//JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "ANIMO cannot perform any analysis.\nPlease press the \"Options...\" button\nat the bottom of the ANIMO panel\nwhen you know the location of the\nUPPAAL verifyta program.", "No UPPAAL verifyta program was found", JOptionPane.WARNING_MESSAGE);
			int answer = JOptionPane.showConfirmDialog(Animo.getCytoscape().getJFrame(), "ANIMO needs UPPAAL (at least version 4.1) to perform analyses.\nYou can freely download UPPAAL (for academic use)\nfrom www.uppaal.org: would you like\nto visit that page now?", "UPPAAL not detected", JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				boolean useClipboard = true;
				if(Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(new URI("http://www.it.uu.se/research/group/darts/uppaal/download.shtml"));
						useClipboard = false;
					} catch (IOException e) {
						e.printStackTrace(System.err);
					} catch (URISyntaxException e) {
						e.printStackTrace(System.err);
					}
				}
				if (useClipboard) { //This is done if either the direct opening of a browser is not supported, or if an error occurred while opening the browser
					StringSelection sel = new StringSelection("http://www.it.uu.se/research/group/darts/uppaal/download.shtml");
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
					JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "I was unable to open the browser,\nbut the UPPAAL download page has been copied\nto your clipboard.\nJust open a web browser and paste\nin the address bar to download UPPAAL.", "Couldn't open browser", JOptionPane.WARNING_MESSAGE);
				}
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Once you have unzipped the UPPAAL archive somewhere\n(make sure it is at least version 4.1!),\npress the OK button to try and find it.");
				try {
					verifytaLocationStr = findVerifytaLocation();
				} catch (Exception ex) {
					verifytaLocationStr = null;
				}
				if (verifytaLocationStr == null) {
					JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "ANIMO cannot perform any analysis.\nPlease press the \"Options...\" button\nat the bottom of the ANIMO panel\nwhen you have installed UPPAAL.", "No UPPAAL program was found", JOptionPane.WARNING_MESSAGE);
				} else {
					//sourceConfig.put(VERIFY_KEY, verifytaLocationStr);
					return verifytaLocationStr;
				}
			} else {
				JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "ANIMO cannot perform any analysis.\nPlease press the \"Options...\" button\nat the bottom of the ANIMO panel\nwhen you have installed UPPAAL.", "No UPPAAL program was found", JOptionPane.WARNING_MESSAGE);
			}
		}
		return null;
	}
	
	/**
	 * Create the default configuration file.
	 * 
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public XmlConfiguration(final File configuration) throws ParserConfigurationException, TransformerException, IOException {
		verifytaLocationStr = findOrInstallVerifyta();
		if (verifytaLocationStr != null) {
			sourceConfig.put(VERIFY_KEY, verifytaLocationStr);
		} else {
			sourceConfig.put(VERIFY_KEY, DEFAULT_VERIFY);
		}
		/*
		 * JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Please, find and select the \"tracer\" tool.", "Tracer", JOptionPane.QUESTION_MESSAGE); String tracerLocation =
		 * FileUtils.open(null, "Tracer Executable", Cytoscape.getDesktop()); if (tracerLocation != null) { sourceConfig.put(TRACER_KEY, tracerLocation); } else {
		 * sourceConfig.put(TRACER_KEY, "\\uppaal-4.1.4\\bin-Win32\\tracer.exe"); }
		 */
		sourceConfig.put(DEVELOPER_KEY, DEFAULT_DEVELOPER);

		sourceConfig.put(UNCERTAINTY_KEY, DEFAULT_UNCERTAINTY);

		sourceConfig.put(MODEL_TYPE_KEY, DEFAULT_MODEL_TYPE);

		try {
			writeConfigFile(configuration);
		} catch (ParserConfigurationException e) {
			e.printStackTrace(System.err);
		} catch (TransformerException e) {
			e.printStackTrace(System.err);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Evaluates the given XPath expression in the context of this document.
	 * 
	 * @param expression
	 *            the expression to evaluate
	 * @param resultType
	 *            the result type
	 * @return an object or {@code null}
	 */
	private Object evaluate(String expression, QName resultType) {
		try {
			AXPathExpression xpath = XmlEnvironment.hardcodedXPath(expression);
			return xpath.evaluate(this.document, resultType);
		} catch (XPathExpressionException e) {
			return null;
		}
	}

	/**
	 * Returns a string from this document.
	 * 
	 * @param xpath
	 *            the selection expression
	 * @return the string, or {@code null}
	 */
	public String get(String xpath) {
		return (String) this.evaluate(xpath, XPathConstants.STRING);
	}

	/**
	 * Returns a string from this document, or the default value if the string is not present.
	 * 
	 * @param xpath
	 *            the selection expression
	 * @param defaultValue
	 *            the default value
	 * @return the string from the document or the default value
	 */
	public String get(String xpath, String defaultValue) {
		if (this.has(xpath)) {
			return this.get(xpath);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Returns a node from this document.
	 * 
	 * @param xpath
	 *            the selection expression
	 * @return a node
	 */
	public Node getNode(String xpath) {
		return (Node) this.evaluate(xpath, XPathConstants.NODE);
	}

	/**
	 * Returns a set of nodes from this document.
	 * 
	 * @param xpath
	 *            the selection expression
	 * @return a set of nodes
	 */
	public ANodeList getNodes(String xpath) {
		return new ANodeList((NodeList) this.evaluate(xpath, XPathConstants.NODESET));
	}

	/**
	 * Checks to see whether this document matches the given expression.
	 * 
	 * @param xpath
	 *            the expression to test
	 * @return {@code true} if the document matches, {@code false} otherwise
	 */
	public boolean has(String xpath) {
		return (Boolean) this.evaluate(xpath, XPathConstants.BOOLEAN);
	}

	public void set(String k, String v) {
		sourceConfig.put(k, v);
	}

	public void writeConfigFile() throws ParserConfigurationException, TransformerException, IOException {
		if (configFile == null)
			throw new IOException("Configuration file null");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		docBuilder = docFactory.newDocumentBuilder();
		document = docBuilder.newDocument();

		Element rootElement = document.createElement(Animo.APP_NAME);
		document.appendChild(rootElement);

		Element uppaalInvoker = document.createElement("UppaalInvoker");
		rootElement.appendChild(uppaalInvoker);

		/*
		 * Element tracerLocation = document.createElement("tracer"); tracerLocation.appendChild(document.createTextNode(sourceConfig.get(TRACER_KEY)));
		 * uppaalInvoker.appendChild(tracerLocation);
		 */

		Element verifytaLocation = document.createElement("verifyta");
		verifytaLocation.appendChild(document.createTextNode(sourceConfig.get(VERIFY_KEY)));

		uppaalInvoker.appendChild(verifytaLocation);
		
		Element developerNode = document.createElement("Developer");
		developerNode.appendChild(document.createTextNode(sourceConfig.get(DEVELOPER_KEY)));
		rootElement.appendChild(developerNode);

		Element uncertaintyNode = document.createElement("Uncertainty");
		uncertaintyNode.appendChild(document.createTextNode(sourceConfig.get(UNCERTAINTY_KEY)));
		rootElement.appendChild(uncertaintyNode);

		Element modelTypeNode = document.createElement("ModelType");
		modelTypeNode.appendChild(document.createTextNode(sourceConfig.get(MODEL_TYPE_KEY)));
		rootElement.appendChild(modelTypeNode);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(configFile);

		transformer.transform(source, result);
	}

	private void writeConfigFile(File configuration) throws ParserConfigurationException, TransformerException,
			IOException {
		if (configuration == null)
			throw new IOException("Configuration file null");
		this.configFile = configuration;
		writeConfigFile();
	}
}
