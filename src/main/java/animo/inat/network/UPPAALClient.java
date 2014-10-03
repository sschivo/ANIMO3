package animo.inat.network;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import animo.inat.analyser.AnalysisException;
import animo.inat.analyser.SMCResult;
import animo.inat.analyser.uppaal.SimpleLevelResult;
import animo.inat.model.Model;

/**
 * The class used to access the remote server.
 */
public class UPPAALClient
{
    private iUPPAALServer server = null;

    public UPPAALClient(String serverHost, Integer serverPort) throws MalformedURLException, RemoteException, NotBoundException
    {
        //System.setProperty("java.rmi.server.hostname", "130.89.14.18");
        System.setSecurityManager(new java.rmi.RMISecurityManager());
        server = (iUPPAALServer) Naming.lookup("rmi://" + serverHost + ":" + serverPort + "/UPPAALServer");
    }

    public SimpleLevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeAvgStdDev, boolean overlayPlot) throws AnalysisException,
            IOException
    {
        return server.analyze(m, timeTo, nSimulationRuns, computeAvgStdDev, overlayPlot);
    }

    public SMCResult analyzeSMC(Model m, String smcQuery) throws AnalysisException, IOException
    {
        return server.analyze(m, smcQuery);
    }
}
