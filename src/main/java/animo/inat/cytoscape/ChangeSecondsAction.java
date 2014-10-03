package animo.inat.cytoscape;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;

/**
 * The button to change the number of real-life seconds represented
 * by a single UPPAAL time unit in the model.
 */
public class ChangeSecondsAction extends AbstractCyAction
{

    private static final long serialVersionUID = -9023326560269020342L;

    private static final String SECONDS_PER_POINT = "seconds per point"; //The name of the network property to which the number of real-life seconds per UPPAAL time unit is associated

    @SuppressWarnings("unused")
    private AbstractButton associatedButton;
    private CyApplicationManager cyApplicationManager;

    public ChangeSecondsAction(CyApplicationManager cyApplicationManager, AbstractButton associatedButton)
    {
        super("Change Seconds");
        this.associatedButton = associatedButton;
        this.cyApplicationManager = cyApplicationManager;
        associatedButton.setAction(this);
        CyNetwork network = cyApplicationManager.getCurrentNetwork();

        if (network != null)
        {
            String text = network.getRow(network).get(SECONDS_PER_POINT, String.class);
            if (text == null)
            {
                associatedButton.setText("Choose seconds/step");
            }
            else
            {
                associatedButton.setText("" + text + " seconds/step");
            }
        }
        else
        {
            associatedButton.setText("Choose seconds/step");
        }
    }

    /**
     * Depending on whether the number of seconds per time unit was already set,
     * we ask a slightly different question to the user. Apart from that, we
     * simply set the property to what the user has chosen.
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        CyNetwork network = cyApplicationManager.getCurrentNetwork();


        String message;
        Double currentSecondsPerPoint = network.getRow(network).get(SECONDS_PER_POINT, Double.class);
        if (currentSecondsPerPoint == null)
        {
            message = "Missing number of seconds per point for the network.\nPlease insert the number of real-life seconds a simulation point will represent";
            currentSecondsPerPoint = new Double(1);
        }
        else
        {
            message = "Please insert the number of real-life seconds a simulation point will represent";
        }
        String inputSecs = JOptionPane.showInputDialog(message, currentSecondsPerPoint);
        Double nSecPerPoint;
        if (inputSecs != null)
        {
            try
            {
                nSecPerPoint = new Double(inputSecs);
            }
            catch (NumberFormatException ex)
            {
                nSecPerPoint = currentSecondsPerPoint;
            }
        }
        else
        {
            nSecPerPoint = currentSecondsPerPoint;
        }
        // network.getRow(network).set(SECONDS_PER_POINT, nSecPerPoint);
        Animo.setRowValue(network.getRow(network), SECONDS_PER_POINT, Double.class, nSecPerPoint);
    }

}
