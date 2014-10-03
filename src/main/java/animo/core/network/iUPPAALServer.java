package animo.core.network;

import java.io.IOException;
import java.rmi.Remote;

import animo.core.analyser.AnalysisException;
import animo.core.analyser.SMCResult;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.model.Model;

/**
 * Remotely accessible features: simulation run or SMC analysis
 */
public interface iUPPAALServer extends Remote
{

    SimpleLevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeAvgStdDev, boolean overlayPlot) throws IOException, AnalysisException;

    SMCResult analyze(Model m, String smcQuery) throws IOException, AnalysisException;
}
