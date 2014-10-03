package animo.cytoscape;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

import animo.core.ANIMOBackend;
import animo.core.analyser.AnalysisException;
import animo.core.analyser.SMCResult;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.model.Model;
import animo.cytoscape.modelchecking.PathFormula;
import animo.exceptions.AnimoException;
import animo.util.XmlConfiguration;

public class ModelCheckAction extends AnimoActionTask
{
    private class RunTask extends AbstractTask
    {

        /**
         * Send the (already translated) formula to the analyser.
         * Show the result in a message window.
         * @param model
         * @throws AnalysisException
         */
        private void performModelChecking(final Model model, final String formula, final String humanFormula, TaskMonitor monitor) throws AnalysisException
        {
            monitor.setStatusMessage("Analyzing model with UPPAAL");
            monitor.setProgress(-1);

            // analyse model
            final SMCResult result;

            /*if (remoteUppaal.isSelected()) {
                UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
                result = client.analyzeSMC(model, probabilisticFormula);
            } else {*///this was mestesso
            result = new UppaalModelAnalyserSMC(monitor, modelCheckAction).analyzeSMC(model, formula);
            //}

            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Property \"" + humanFormula + "\"" + System.getProperty("line.separator")
                            + "is " + result.toString(), "Result", JOptionPane.INFORMATION_MESSAGE);
                    if (result.getResultType() == SMCResult.RESULT_TYPE_TRACE)
                    { //We have a trace to show
                        double scale = model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class) / 60.0;
                        final AnimoResultPanel resultViewer = new AnimoResultPanel(model, result.getLevelResult(), scale, Animo.getCytoscapeApp()
                                .getCyApplicationManager().getCurrentNetwork());
                        resultViewer.addToPanel(Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST));
                        resultViewer.setTitle(humanFormula + " (" + (result.getBooleanResult() ? "True" : "False") + ")");
                    }
                }
            });

        }

        @Override
        public void run(final TaskMonitor monitor) throws AnalysisException, AnimoException
        {
            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    needToStop = false;
                    monitor.setStatusMessage("Creating model representation");
                    monitor.setProgress(0);

                    boolean generateTables = false;
                    XmlConfiguration configuration = ANIMOBackend.get().configuration();
                    String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
                    if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES))
                    {
                        generateTables = true;
                    }
                    Model model = null;
                    try
                    {
                        model = Model.generateModelFromCurrentNetwork(monitor, null, generateTables);
                    }
                    catch (AnimoException e1)
                    {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    formulaToCheck.setReactantIDs(model); //This is enough to set the proper model IDs to all reactants in the formula
                    model.getProperties().let(Model.Properties.MODEL_CHECKING_TYPE).be(Model.Properties.STATISTICAL_MODEL_CHECKING);
                    try
                    {
                        performModelChecking(model, formulaToCheck.toString(), formulaToCheck.toHumanReadable(), monitor);
                    }
                    catch (AnalysisException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    private static final long serialVersionUID = 1147435660037202034L;
    private PathFormula formulaToCheck;

    private PrintStream logStream;
    private ModelCheckAction modelCheckAction;

    public ModelCheckAction()
    {
        super("<html>Model <br/>checking...</html>");
        this.modelCheckAction = this;
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        final RunTask task = new RunTask();

        final long startTime = System.currentTimeMillis();
        Date now = new Date(startTime);
        Calendar nowCal = Calendar.getInstance();
        File logFile = null;
        final PrintStream oldErr = System.err;
        try
        {
            if (UppaalModelAnalyserSMC.areWeUnderWindows())
            {
                logFile = File.createTempFile(
                        "ANIMO_run_" + nowCal.get(Calendar.YEAR) + "-" + nowCal.get(Calendar.MONTH) + "-" + nowCal.get(Calendar.DAY_OF_MONTH) + "_"
                                + nowCal.get(Calendar.HOUR_OF_DAY) + "-" + nowCal.get(Calendar.MINUTE) + "-" + nowCal.get(Calendar.SECOND), ".log"); //windows doesn't like long file names..
            }
            else
            {
                logFile = File.createTempFile("ANIMO run " + now.toString(), ".log");
            }
            logFile.deleteOnExit();
            logStream = new PrintStream(new FileOutputStream(logFile));
            System.setErr(logStream);
        }
        catch (IOException ex)
        {
            //We have no log file, bad luck: we will have to use System.err.
        }

        formulaToCheck = null;
        final JDialog dialog = new JDialog(Animo.getCytoscape().getJFrame(), "Model checking templates", Dialog.ModalityType.APPLICATION_MODAL);
        Box boxContent = new Box(BoxLayout.Y_AXIS);
        final PathFormula pathFormula = new PathFormula();
        boxContent.add(pathFormula);
        Box buttonsBox = new Box(BoxLayout.X_AXIS);
        buttonsBox.add(Box.createGlue());
        JButton doWork = new JButton("Start model checking");
        doWork.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                formulaToCheck = pathFormula.getSelectedFormula();
                dialog.dispose();
            }
        });
        buttonsBox.add(doWork);
        buttonsBox.add(Box.createGlue());
        boxContent.add(buttonsBox);
        dialog.getContentPane().add(boxContent);
        dialog.pack();
        dialog.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
        dialog.setVisible(true);

        if (formulaToCheck == null)
            return; //If the user simply closed the window, the analysis is cancelled
        System.err.println("Checking formula " + formulaToCheck.toHumanReadable());

        // Execute Task in New Thread; pops open JTask Dialog Box.
        TaskManager<?, ?> taskManager = Animo.getCytoscapeApp().getTaskManager();
        taskManager.execute(new TaskIterator(task));

        long endTime = System.currentTimeMillis();
        System.err.println("Time taken: " + AnimoActionTask.timeDifferenceFormat(startTime, endTime));
        System.err.flush();
        System.setErr(oldErr);
        if (logStream != null)
        {
            logStream.close();
        }
    }
}
