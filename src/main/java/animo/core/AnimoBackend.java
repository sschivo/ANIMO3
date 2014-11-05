/**
 * 
 */
package animo.core;

import java.io.File;

import org.xml.sax.SAXException;

import animo.exceptions.AnimoException;
import animo.util.XmlConfiguration;
import animo.util.XmlEnvironment;

/**
 * The ANIMO backend singleton is used to initialise the ANIMO backend, and to retrieve configuration.
 * 
 * @author B. Wanders
 */
public class AnimoBackend {
	/**
	 * The singleton instance.
	 */
	private static AnimoBackend instance;

	/**
	 * The configuration properties.
	 */
	private XmlConfiguration configuration = null;

	// private static final String NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS, //Property that can belong to a node or to a network. If related to a single node, it
	// represents the maximum number of levels for that single reactant. If related to a complete network, it is the maximum value of the NUMBER_OF_LEVELS property among all nodes
	// in the network. Expressed as integer number in [0, 100] (chosen by the user).
	// INITIAL_LEVEL = Model.Properties.INITIAL_LEVEL, //Property belonging to a node. The initial activity level for a node. Expressed as an integer number in [0, NUMBER_OF_LEVELS
	// for that node]
	// SHOWN_LEVEL = Model.Properties.SHOWN_LEVEL, //Property belonging to a node. The current activity level of a node. Expressed as a relative number representing INITIAL_LEVEL /
	// NUMBER_OF_LEVELS, so it is a double number in [0, 1]
	// SECONDS_PER_POINT = Model.Properties.SECONDS_PER_POINT, //Property belonging to a network. The number of real-life seconds represented by a single UPPAAL time unit.
	// ENABLED = Model.Properties.ENABLED; //Whether the node is enabled (included in the exported UPPAAL model)

	// SCENARIO = Model.Properties.SCENARIO; //Property belonging to an edge. The id of the scenario on which the reaction corresponding to the edge computes its time tables.

	/**
	 * Returns the singleton instance.
	 * 
	 * @return the single instance
	 */
	public static AnimoBackend get() {
		assert isInitialised() : "ANIMO Backend not yet initialised.";
		return instance;
	}

	/**
	 * Initialises the ANIMO backend with the given configuration.
	 * 
	 * @param configuration
	 *            the location of the configuration file
	 * @throws AnimoException
	 *             if the backend could not be initialised
	 */
	public static void initialise(File configuration) throws AnimoException {
		assert !isInitialised() : "Can not re-initialise ANIMO backend.";

		AnimoBackend.instance = new AnimoBackend(configuration);
	}

	/**
	 * Returns whether the ANIMO backend is initialised.
	 * 
	 * @return whether the backend is initialised
	 */
	public static boolean isInitialised() {
		return instance != null;
	}

	/**
	 * Constructor.
	 * 
	 * @param configuration
	 *            the configuration file location
	 * @throws AnimoException
	 *             if the ANIMO backend could not be initialised
	 */
	private AnimoBackend(File configuration) throws AnimoException {

		// read configuration file
		try {
			// initialise the XML environment
			XmlEnvironment.getInstance();

			try {
				// read config from file
				this.configuration = new XmlConfiguration(XmlEnvironment.parse(configuration), configuration);
			} catch (SAXException ex) {
				// create default configuration
				this.configuration = new XmlConfiguration(configuration);
			}
			// } catch (SAXException e) {
			// throw new InatException("Could not parse configuration file '" + configuration + "'", e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AnimoException("An error has occured");
		}
	}

	/**
	 * Returns the configuration properties.
	 * 
	 * @return the configuration
	 */
	public XmlConfiguration configuration() {
		return this.configuration;
	}
}
