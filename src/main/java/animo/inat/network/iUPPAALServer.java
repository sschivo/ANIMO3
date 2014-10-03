package animo.inat.network;

import java.io.IOException;
import java.rmi.Remote;

import animo.inat.analyser.AnalysisException;
import animo.inat.analyser.SMCResult;
import animo.inat.analyser.uppaal.SimpleLevelResult;
import animo.inat.model.Model;

/**
 * Remotely accessible features: simulation run or SMC analysis
 */
public interface iUPPAALServer extends Remote
{

    SimpleLevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeAvgStdDev, boolean overlayPlot) throws IOException, AnalysisException;

    SMCResult analyze(Model m, String smcQuery) throws IOException, AnalysisException;
}
