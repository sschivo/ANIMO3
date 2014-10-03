/**
 * 
 */
package animo.inat;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import animo.inat.exceptions.InatException;
import animo.inat.model.Model;
import animo.inat.util.XmlConfiguration;
import animo.inat.util.XmlEnvironment;

/**
 * The ANIMO backend singleton is used to initialise the ANIMO backend, and to
 * retrieve configuration.
 * 
 * @author B. Wanders
 */
public class InatBackend
{
    /**
     * The singleton instance.
     */
    private static InatBackend instance;

    /**
     * The configuration properties.
     */
    private XmlConfiguration configuration = null;

    private static final String NUMBER_OF_LEVELS = Model.Properties.NUMBER_OF_LEVELS, //Property that can belong to a node or to a network. If related to a single node, it represents the maximum number of levels for that single reactant. If related to a complete network, it is the maximum value of the NUMBER_OF_LEVELS property among all nodes in the network. Expressed as integer number in [0, 100] (chosen by the user).
            INITIAL_LEVEL = Model.Properties.INITIAL_LEVEL, //Property belonging to a node. The initial activity level for a node. Expressed as an integer number in [0, NUMBER_OF_LEVELS for that node]
            SHOWN_LEVEL = Model.Properties.SHOWN_LEVEL, //Property belonging to a node. The current activity level of a node. Expressed as a relative number representing INITIAL_LEVEL / NUMBER_OF_LEVELS, so it is a double number in [0, 1]
            SECONDS_PER_POINT = Model.Properties.SECONDS_PER_POINT, //Property belonging to a network. The number of real-life seconds represented by a single UPPAAL time unit.
            ENABLED = Model.Properties.ENABLED; //Whether the node is enabled (included in the exported UPPAAL model)

    //SCENARIO = Model.Properties.SCENARIO; //Property belonging to an edge. The id of the scenario on which the reaction corresponding to the edge computes its time tables.

    /**
     * Returns the singleton instance.
     * 
     * @return the single instance
     */
    public static InatBackend get()
    {
        assert isInitialised() : "ANIMO Backend not yet initialised.";
        return instance;
    }

    /**
     * Initialises the ANIMO backend with the given configuration.
     * 
     * @param configuration the location of the configuration file
     * @throws InatException if the backend could not be initialised
     */
    public static void initialise(File configuration) throws InatException
    {
        assert !isInitialised() : "Can not re-initialise ANIMO backend.";

        InatBackend.instance = new InatBackend(configuration);
    }

    /**
     * Returns whether the ANIMO backend is initialised.
     * 
     * @return whether the backend is initialised
     */
    public static boolean isInitialised()
    {
        return instance != null;
    }

    /**
     * Constructor.
     * 
     * @param configuration the configuration file location
     * @throws InatException if the ANIMO backend could not be initialised
     */
    private InatBackend(File configuration) throws InatException
    {

        // read configuration file
        try
        {
            // initialise the XML environment
            XmlEnvironment.getInstance();

            try
            {
                // read config from file
                this.configuration = new XmlConfiguration(XmlEnvironment.parse(configuration), configuration);
            }
            catch (SAXException ex)
            {
                // create default configuration
                this.configuration = new XmlConfiguration(configuration);
            }
            //} catch (SAXException e) {
            // throw new InatException("Could not parse configuration file '" + configuration + "'", e);
        }
        catch (ParserConfigurationException | TransformerException | IOException e)
        {
            e.printStackTrace();
            throw new InatException("An error has occured");
        }
    }

    /**
     * Returns the configuration properties.
     * 
     * @return the configuration
     */
    public XmlConfiguration configuration()
    {
        return this.configuration;
    }
}
