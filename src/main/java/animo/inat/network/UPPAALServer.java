package animo.inat.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import animo.inat.InatBackend;
import animo.inat.analyser.AnalysisException;
import animo.inat.analyser.SMCResult;
import animo.inat.analyser.uppaal.ResultAverager;
import animo.inat.analyser.uppaal.SimpleLevelResult;
import animo.inat.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.inat.exceptions.InatException;
import animo.inat.model.Model;

/**
 * The remote server. Implements the methods for simulation run analysis and
 * SMC analysis. Listens for remote connections on the given port.
 */
public class UPPAALServer extends UnicastRemoteObject implements iUPPAALServer
{
    private static final long serialVersionUID = 5030971508567718530L;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    private static final int DEFAULT_RMI_PORT = 1234, DEFAULT_SERVICE_PORT = 1235;

    public static void main(String[] args) throws InatException
    {
        try
        {
            int rmiPort = DEFAULT_RMI_PORT, //TODO: These two ports MUST be opened in the firewall
            servicePort = DEFAULT_SERVICE_PORT;
            if (args.length < 2)
            {
                System.err.println("No ports given: defaults are " + rmiPort + " for RMI registry, and " + servicePort + " for the actual server");
            }
            else
            {
                try
                {
                    rmiPort = Integer.parseInt(args[0]);
                    servicePort = Integer.parseInt(args[1]);
                }
                catch (NumberFormatException ex)
                {
                    throw new IOException("Invalid port number", ex);
                }
                System.err.println("Using " + rmiPort + " port for RMI registry, and " + servicePort + " for the actual server");
            }
            System.err.println("!! PLEASE NOTICE !! The two ports above NEED to be opened in the firewall/router/whateverits");
            InatBackend.initialise(new File("ANIMO-configuration.xml"));

            URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            String ip = in.readLine(); //you get the IP as a String
            System.setProperty("java.rmi.server.hostname", ip); //82.75.157.15
            System.out.println("My host name is " + ip);


            @SuppressWarnings("unused")
            UPPAALServer server = new UPPAALServer(rmiPort, servicePort);
            System.out.println(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " Server started and listening on port " + rmiPort + ".");
        }
        catch (IOException ex)
        {
            System.err.println("Problems in starting server!");
            ex.printStackTrace();
        }
    }

    protected UPPAALServer(int rmiPort, int servicePort) throws RemoteException
    {
        super(servicePort);
        try
        {
            LocateRegistry.createRegistry(rmiPort);
            Naming.rebind("rmi://localhost:" + rmiPort + "/UPPAALServer", this);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public SimpleLevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeAvgStdDev, boolean overlayPlot) throws IOException,
            AnalysisException
    {
        System.out.println(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " Analyzing \"normal\" model with simulation up to " + timeTo);
        SimpleLevelResult result;
        if (nSimulationRuns > 1)
        {
            if (computeAvgStdDev)
            {
                result = new ResultAverager(null, null).analyzeAverage(m, timeTo, nSimulationRuns);
            }
            else if (overlayPlot)
            {
                result = new ResultAverager(null, null).analyzeOverlay(m, timeTo, nSimulationRuns);
            }
            else
            {
                result = null;
            }
        }
        else
        {
            //result = new UppaalModelAnalyserFasterConcrete(null, null).analyze(m, timeTo);
            result = new UppaalModelAnalyserSMC/*FasterSymbolicConcretized*/(null, null).analyze(m, timeTo);
        }
        System.out.println(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " Done.");
        System.out.println();
        return result;
    }

    @Override
    public SMCResult analyze(Model m, String smcQuery) throws AnalysisException, IOException
    {
        System.out.println(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " Analyzing \"SMC\" model with query " + smcQuery);
        SMCResult result = new UppaalModelAnalyserSMC/*FasterSymbolicConcretized*/(null, null).analyzeSMC(m, smcQuery); //new UppaalModelAnalyserFasterConcrete(null, null).analyzeSMC(m, smcQuery);
        System.out.println(DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " Done.");
        System.out.println();
        return result;
    }

}
