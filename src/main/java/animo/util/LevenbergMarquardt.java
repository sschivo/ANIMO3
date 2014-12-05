package animo.util;

import static org.ejml.ops.CommonOps.add;
import static org.ejml.ops.CommonOps.multTransB;
import static org.ejml.ops.CommonOps.scale;
import static org.ejml.ops.CommonOps.solve;
import static org.ejml.ops.CommonOps.subtract;
import static org.ejml.ops.CommonOps.subtractEquals;
import static org.ejml.ops.SpecializedOps.diffNormF;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.ejml.data.DenseMatrix64F;

import animo.core.analyser.LevelResult;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.analyser.uppaal.VariablesModel;
import animo.core.graph.Graph;
import animo.core.graph.P;
import animo.core.graph.SmartTokenizer;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;
import animo.core.model.Scenario;
import animo.cytoscape.Animo;
import animo.cytoscape.RunAction;
import animo.fitting.ScenarioCfg;

/**
 * <p>
 * This is a straight forward implementation of the Levenberg-Marquardt (LM) algorithm. LM is used to minimize
 * non-linear cost functions:<br>
 * <br>
 * S(P) = Sum{ i=1:m , [y<sub>i</sub> - f(x<sub>i</sub>,P)]<sup>2</sup>}<br>
 * <br>
 * where P is the set of parameters being optimized.
 * </p>
 *
 * <p>
 * In each iteration the parameters are updated using the following equations:<br>
 * <br>
 * P<sub>i+1</sub> = (H + &lambda; I)<sup>-1</sup> d <br>
 * d =  (1/N) Sum{ i=1..N , (f(x<sub>i</sub>;P<sub>i</sub>) - y<sub>i</sub>) * jacobian(:,i) } <br>
 * H =  (1/N) Sum{ i=1..N , jacobian(:,i) * jacobian(:,i)<sup>T</sup> }
 * </p>
 * <p>
 * Whenever possible the allocation of new memory is avoided.  This is accomplished by reshaping matrices.
 * A matrix that is reshaped won't grow unless the new shape requires more memory than it has available.
 * </p>
 * @author Peter Abeles
 */
public class LevenbergMarquardt {
    // how much the numerical jacobian calculation perturbs the parameters by.
    // In better implementation there are better ways to compute this delta.  See Numerical Recipes.
    public double DELTA = 1e-8;
    
    public double MIN_COST = 0.001; //The point at which we already are happy enough to stop
    private double minCost = Double.NaN; //The minimum cost up to now
    private DenseMatrix64F minimumCostParameters; //The parameters that give the minimum cost up to this moment

    private double initialLambda;

    // the function that is optimized
    private Function func;

    // the optimized parameters and associated costs
    private DenseMatrix64F param;
    private double initialCost;
    private double finalCost;

    // used by matrix operations
    private DenseMatrix64F d;
    private DenseMatrix64F H;
    private DenseMatrix64F negDelta;
    private DenseMatrix64F tempParam;
    private DenseMatrix64F A;

    // variables used by the numerical jacobian algorithm
    private DenseMatrix64F temp0;
    private DenseMatrix64F temp1;
    // used when computing d and H variables
    private DenseMatrix64F tempDH;

    // Where the numerical Jacobian is stored.
    private DenseMatrix64F jacobian;

    /**
     * Creates a new instance that uses the provided cost function.
     *
     * @param funcCost Cost function that is being optimized.
     */
    public LevenbergMarquardt( Function funcCost )
    {
        this.initialLambda = 1;

        // declare data to some initial small size. It will grow later on as needed.
        int maxElements = 1;
        int numParam = 1;

        this.temp0 = new DenseMatrix64F(maxElements,1);
        this.temp1 = new DenseMatrix64F(maxElements,1);
        this.tempDH = new DenseMatrix64F(maxElements,1);
        this.jacobian = new DenseMatrix64F(numParam,maxElements);

        this.func = funcCost;

        this.param = new DenseMatrix64F(numParam,1);
        this.d = new DenseMatrix64F(numParam,1);
        this.H = new DenseMatrix64F(numParam,numParam);
        this.negDelta = new DenseMatrix64F(numParam,1);
        this.tempParam = new DenseMatrix64F(numParam,1);
        this.A = new DenseMatrix64F(numParam,numParam);
    }


    public double getInitialCost() {
        return initialCost;
    }

    public double getFinalCost() {
        return finalCost;
    }

    public DenseMatrix64F getParameters() {
        return param;
    }
    
    @SuppressWarnings("rawtypes")
	private SwingWorker swingWorker;
    public void setSwingWorker(@SuppressWarnings("rawtypes") SwingWorker worker) {
    	this.swingWorker = worker;
    }

    /**
     * Finds the best fit parameters.
     *
     * @param initParam The initial set of parameters for the function.
     * @param X The inputs to the function.
     * @param Y The "observed" output of the function
     * @return true if it succeeded and false if it did not.
     */
    public boolean optimize( DenseMatrix64F initParam ,
                             DenseMatrix64F X ,
                             DenseMatrix64F Y )
    {
        configure(initParam,X,Y);

        // save the cost of the initial parameters so that it knows if it improves or not
        minCost = initialCost = cost(param,X,Y);
        minimumCostParameters = new DenseMatrix64F(initParam); //Keep a copy of the initial parameters: it will be updated if parameters with better cost are found (the cost will be saved in minCost)

//        // iterate until the difference between the costs is insignificant
//        // or it iterates too many times
//        if( !adjustParam(X, Y, initialCost) ) {
//            finalCost = Double.NaN;
//            return false;
//        }
        //pensa ai casi tui
        return adjustParam(X, Y, initialCost);

//        return true;
    }

    /**
     * Iterate until the difference between the costs is insignificant
     * or it iterates too many times
     */
    private boolean adjustParam(DenseMatrix64F X, DenseMatrix64F Y,
                                double prevCost) {
        // lambda adjusts how big of a step it takes
        double lambda = initialLambda;
        // the difference between the current and previous cost
        double difference = 1000;

        for( int iter = 0; iter < 20 || difference < 1e-6 ; iter++ ) {
            // compute some variables based on the gradient
            computeDandH(param,X,Y);

            // try various step sizes and see if any of them improve the
            // results over what has already been done
            boolean foundBetter = false;
            for( int i = 0; i < 5; i++ ) {
                computeA(A,H,lambda);

                if( !solve(A,d,negDelta) ) {
                    return false;
                }
                // compute the candidate parameters
                subtract(param, negDelta, tempParam);

                double cost = cost(tempParam,X,Y);

                if (cost < minCost) {
                	minCost = cost;
                	minimumCostParameters.set(tempParam);
                }
                
                if (swingWorker != null) {
	                try { //Complicated, but we cannot set the progress outside of that class (the function is protected). As a bonus, I will declare the class "in the future", so at the moment the method is not visible and I need to use reflection to find it
	                	Method setProgresso = null;
	                	try {
	                		setProgresso = swingWorker.getClass().getMethod("setProgresso", Integer.class);
	                	} catch (NoSuchMethodException ex) {
	                		setProgresso = null;
	                	}
	                	if (setProgresso != null) {
	                		int progresso = (int)Math.round(100.0 - 100.0 * cost / initialCost);
	                		if (progresso < 0) {
	                			progresso = 0;
	                		}
	                		if (progresso > 100) {
	                			progresso = 100;
	                		}
	                		setProgresso.invoke(swingWorker, new Integer(progresso));
	                	}
	                } catch (Exception ex) {
	                	ex.printStackTrace(System.err);
	                }
                
	                try {
	                	Method getMustTerminate = null;
	                	try {
	                		getMustTerminate = swingWorker.getClass().getMethod("getMustTerminate");
	                	} catch (NoSuchMethodException ex) {
	                		getMustTerminate = null;
	                		return false;
	                	}
	                	if (getMustTerminate != null) {
	                		//System.err.println("Il metodo l'ho anche trovato.. " + getMustTerminate + "\n\tAllora lo invoco su " + swingWorker);
	                		Object res = getMustTerminate.invoke(swingWorker);
	                		//System.err.println("E ottengo " + res);
	                		boolean bRes = false;
	                		try {
	                			bRes = Boolean.parseBoolean(res.toString());
	                		} catch (Exception ex) {
	                			bRes = false;
	                		}
	                		if (bRes) { //We must end before completing the computation: return the best result up to now (stored in minimumCostParameters, with cost minCost)
	                			finalCost = minCost;
	                			param.set(minimumCostParameters);
	                			return false;
	                		}
	                	}
	                } catch (Exception ex) {
	                	ex.printStackTrace(System.err);
	                }
                }
                //System.out.println("Costo: " + cost);
                if (cost < MIN_COST) {
                	finalCost = cost;
                	param.set(tempParam);
                	return true;
                }
                if( cost < prevCost ) {
                    // the candidate parameters produced better results so use it
                    foundBetter = true;
                    param.set(tempParam);
                    difference = prevCost - cost;
                    prevCost = cost;
                    lambda /= 10.0;
                } else {
                    lambda *= 10.0;
                }
            }

            // it reached a point where it can't improve so exit
            if( !foundBetter )
                break;
        }
        finalCost = prevCost;
        return true;
    }

    /**
     * Performs sanity checks on the input data and reshapes internal matrices.  By reshaping
     * a matrix it will only declare new memory when needed.
     */
    protected void configure( DenseMatrix64F initParam , DenseMatrix64F X , DenseMatrix64F Y )
    {
        if( Y.getNumRows() != X.getNumRows() ) {
            throw new IllegalArgumentException("Different vector lengths");
        } else if( Y.getNumCols() != 1 || X.getNumCols() != 1 ) {
            throw new IllegalArgumentException("Inputs must be a column vector");
        }

        int numParam = initParam.getNumElements();
        int numPoints = Y.getNumRows();

        if( param.getNumElements() != initParam.getNumElements() ) {
            // reshaping a matrix means that new memory is only declared when needed
            this.param.reshape(numParam,1, false);
            this.d.reshape(numParam,1, false);
            this.H.reshape(numParam,numParam, false);
            this.negDelta.reshape(numParam,1, false);
            this.tempParam.reshape(numParam,1, false);
            this.A.reshape(numParam,numParam, false);
        }

        param.set(initParam);

        // reshaping a matrix means that new memory is only declared when needed
        temp0.reshape(numPoints,1, false);
        temp1.reshape(numPoints,1, false);
        tempDH.reshape(numPoints,1, false);
        jacobian.reshape(numParam,numPoints, false);


    }

    /**
     * Computes the d and H parameters.  Where d is the average error gradient and
     * H is an approximation of the hessian.
     */
    private void computeDandH( DenseMatrix64F param , DenseMatrix64F x , DenseMatrix64F y )
    {
        func.compute(param,x, tempDH);
        subtractEquals(tempDH, y);

        computeNumericalJacobian(param,x,jacobian);

        int numParam = param.getNumElements();
        int length = x.getNumElements();

        // d = average{ (f(x_i;p) - y_i) * jacobian(:,i) }
        for( int i = 0; i < numParam; i++ ) {
            double total = 0;
            for( int j = 0; j < length; j++ ) {
                total += tempDH.get(j,0)*jacobian.get(i,j);
            }
            d.set(i,0,total/length);
        }

        // compute the approximation of the hessian
        multTransB(jacobian,jacobian,H);
        scale(1.0/length,H);
    }

    /**
     * A = H + lambda*I <br>
     * <br>
     * where I is an identity matrix.
     */
    private void computeA( DenseMatrix64F A , DenseMatrix64F H , double lambda )
    {
        final int numParam = param.getNumElements();

        A.set(H);
        for( int i = 0; i < numParam; i++ ) {
            A.set(i,i, A.get(i,i) + lambda);
        }
    }

    /**
     * Computes the "cost" for the parameters given.
     *
     * cost = (1/N) Sum (f(x;p) - y)^2
     */
    private double cost( DenseMatrix64F param , DenseMatrix64F X , DenseMatrix64F Y)
    {
        func.compute(param,X, temp0);

        double error = diffNormF(temp0,Y);

        return error*error / (double)X.numRows;
    }

    /**
     * Computes a simple numerical Jacobian.
     *
     * @param param The set of parameters that the Jacobian is to be computed at.
     * @param pt The point around which the Jacobian is to be computed.
     * @param deriv Where the jacobian will be stored
     */
    protected void computeNumericalJacobian( DenseMatrix64F param ,
                                             DenseMatrix64F pt ,
                                             DenseMatrix64F deriv )
    {
        double invDelta = 1.0/DELTA;

        func.compute(param,pt, temp0);

        // compute the jacobian by perturbing the parameters slightly
        // then seeing how it effects the results.
        for( int i = 0; i < param.numRows; i++ ) {
            param.data[i] += DELTA;
            func.compute(param,pt, temp1);
            // compute the difference between the two parameters and divide by the delta
            add(invDelta,temp1,-invDelta,temp0,temp1);
            // copy the results into the jacobian matrix
            System.arraycopy(temp1.data,0,deriv.data,i*pt.numRows,pt.numRows);

            param.data[i] -= DELTA;
        }
    }

    /**
     * The function that is being optimized.
     */
    public interface Function {
        /**
         * Computes the output for each value in matrix x given the set of parameters.
         *
         * @param param The parameter for the function.
         * @param x the input points.
         * @param y the resulting output.
         */
        public void compute( DenseMatrix64F param , DenseMatrix64F x , DenseMatrix64F y );
    }
    
    public static DenseMatrix64F readCSVtoMatrix(String csvFileName, Collection<String> selectedColumns, double untilTime) throws IOException {
		LevelResult levelResult = readCSVtoLevelResult(csvFileName, selectedColumns, untilTime);
		return levelResultToMatrix(levelResult);
	}
    
    public static DenseMatrix64F levelResultToMatrix(LevelResult levelResult) {
    	return levelResultToMatrix(levelResult, 1.0);
    }
    
    public static DenseMatrix64F levelResultToMatrix(LevelResult levelResult, double scaleFactor) {
    	return levelResultToMatrix(levelResult, 1.0, Collections.<Double>emptyList());
    }
	
	public static DenseMatrix64F levelResultToMatrix(LevelResult levelResult, double scaleFactor, List<Double> timePoints) {
		Vector<String> columnNames = new Vector<String>();
		columnNames.addAll(levelResult.getReactantIds());
		List<Double> timeIndices;
		if (!timePoints.isEmpty()) {
			timeIndices = timePoints;
		} else {
			timeIndices = levelResult.getTimeIndices();
		}
		double data[][] = new double[(/*1 + */columnNames.size()) * timeIndices.size()][1];
		int cnt = 0;
//		System.out.println("Lette " + columnNames.size() + " colonne, e " + timeIndices.size() + " righe.");
		for (double t : timeIndices) {
			//data[cnt++][0] = t;
			for (String col : columnNames) {
				double v = levelResult.getConcentration(col, t / scaleFactor);
				data[cnt++][0] = v;
//				System.out.println(col + "[" + t + "] = " + data[cnt-1][0]);
			}
		}
		return new DenseMatrix64F(data);
	}
	
	public static LevelResult readCSVtoLevelResult(String csvFileName, Collection<String> selectedColumns, double untilTime) throws IOException {
		File f = new File(csvFileName);
		BufferedReader is = new BufferedReader(new FileReader(f));
		String firstLine = is.readLine();
		if (firstLine == null) {
			is.close();
			throw new IOException("Error: the file " + csvFileName + " is empty!");
		}
		StringTokenizer tritatutto = new StringTokenizer(firstLine, ",");
		int nColonne = tritatutto.countTokens();
		String[] columnNames = new String[nColonne - 1];
		Vector<Vector<P>> dataSeries = new Vector<Vector<P>>(columnNames.length);
		Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();
		@SuppressWarnings("unused")
		String xSeriesName = tritatutto.nextToken().replace('\"', ' '); // il primo e' la X (tempo)
		boolean mustRescaleYValues = false; //We assume that Y values are in the [0, 100] interval. Otherwise, a column named like animo.core.graph.Graph.MAX_Y_STRING will tell us (in the first value it contains) the maximal Y value on which to rescale (we assume minimum = 0)
		for (int i = 0; i < columnNames.length; i++) {
			columnNames[i] = tritatutto.nextToken();
			columnNames[i] = columnNames[i].replace('\"', ' ');
			if (columnNames[i].toLowerCase().contains(Graph.MAX_Y_STRING.toLowerCase())) {
				mustRescaleYValues = true;
			}
			dataSeries.add(new Vector<P>());
			if (selectedColumns.contains(columnNames[i])) {
				levels.put(columnNames[i], new TreeMap<Double, Double>());
			}
		}
		while (true) {
			String result = is.readLine();
			if (result == null || result.length() < 2) {
				break;
			}
			SmartTokenizer rigaSpezzata = new SmartTokenizer(result, ",");
			String s = rigaSpezzata.nextToken();
			double xValue = Double.parseDouble(s); // here s can't be null (differently from below) because there is absolutely no sense in not giving the x value for the entire
													// line
			if (untilTime >= 0 && xValue > untilTime) { //We can stop early if we don't need to read the whole time series
				break;
			}
			int lungRiga = rigaSpezzata.countTokens();
			for (int i = 0; i < lungRiga; i++) {
				s = rigaSpezzata.nextToken();
				if (s == null || s.trim().length() < 1)
					continue; // there could be one of the series which does not have a point in this line: we skip it
				dataSeries.elementAt(i).add(new P(xValue, Double.parseDouble(s)));
				if (!mustRescaleYValues && selectedColumns.contains(columnNames[i])) {
					levels.get(columnNames[i]).put(xValue, Double.parseDouble(s));
				}
			}
		}
		is.close();
		
		double maxYValue = 100.0;
		if (mustRescaleYValues) {
			int indexForOtherMaxY = -1;
			maxYValue = 0;
			for (int i = 0; i < columnNames.length; i++) {
				if (columnNames[i].equals(Graph.MAX_Y_STRING)) {
					indexForOtherMaxY = i;
					maxYValue = dataSeries.elementAt(i).elementAt(0).y;
					break;
				}
			}
			for (int i = 0; i < columnNames.length; i++) {
				if (i == indexForOtherMaxY || !selectedColumns.contains(columnNames[i]))
					continue;
				P[] grafico = new P[1];
				grafico = dataSeries.elementAt(i).toArray(grafico);
				if (grafico != null && grafico.length > 1) {
					for (P p : grafico) { // before adding the graph data, we update it by rescaling the y values
						p.y /= maxYValue;
						levels.get(columnNames[i]).put(p.x, p.y);
					}
				}
			}
		} else {
			maxYValue = 100.0;
		}
		return new SimpleLevelResult((int)Math.round(maxYValue), levels);

//		for (Series s : data) {
//			if (s.getName().toLowerCase().trim().endsWith(Series.SLAVE_SUFFIX)) {
//				for (Series s2 : data) {
//					if (s2.getName()
//							.trim()
//							.equals(s.getName().trim()
//									.substring(0, s.getName().toLowerCase().trim().lastIndexOf(Series.SLAVE_SUFFIX)))) {
//						s.setMaster(s2);
//					}
//				}
//			}
//			if (this.selectedColumns.size() > 0 && !this.selectedColumns.contains(s.getName())) {
//				s.setEnabled(false);
//			} else {
//				s.setEnabled(true);
//			}
//		}
//
//		Collections.sort(data);
	}
	
	private static void printMatrix(DenseMatrix64F x) {
    	for (int r = 0; r < x.getNumRows(); r++) {
			System.out.print("[ ");
			for (int c = 0; c < x.getNumCols()-1; c++) {
				System.out.print(x.get(r, c) + ", ");
			}
			System.out.println(x.get(r, x.getNumCols() - 1) + " ]");
		}
    }
		
    
    public static class FaiIlLavoro {
    	public void vai() {
    		DenseMatrix64F scass = null;
    		try {
    			//c:\Users\stefano\Desktop\FOS 2014
				scass = readCSVtoMatrix("/Users/stefano/Documents/Lavoro/Prometheus/Data_Wnt_0-240_erk-frzld.csv", Arrays.asList("ERK data", "Frizzled data"), 240);
				//Graph.plotGraph(new File("/Users/stefano/Documents/Lavoro/Prometheus/Data_Wnt_0-240_erk-frzld.csv"));
				//printMatrix(scass);
			} catch (IOException e) {
				e.printStackTrace();
			}
    		class MyFunction implements Function {
    			private Model model;
    			private Reaction wnt_frzld,
    							 frzld_frzldInt,
    							 frzldInt_frzld,
    							 frzld_erk,
    							 erkP_erk;
    			int minTimeModel;
    			int maxTimeModel;
    			private Graph graph;
    			
        		private int contaTentativi = 0;
        		
        		public MyFunction(Graph graph) {
        			this();
        			this.graph = graph;
        		}
        		
        		public MyFunction() {
        			minTimeModel = Integer.MAX_VALUE;
        			maxTimeModel = Integer.MIN_VALUE;
        			model = new Model();
        			model.getProperties().let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		    		model.getProperties().let(Model.Properties.SECONDS_PER_POINT).be(0.01);
		    		model.getProperties().let(Model.Properties.SECS_POINT_SCALE_FACTOR).be(100.0);
		    		
		    		Reactant reactant = new Reactant("R0");
					reactant.let(Model.Properties.REACTANT_NAME).be("Wnt");
					reactant.setName("Wnt");
					reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
					reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
					reactant.let(Model.Properties.ENABLED).be(true);
					reactant.let(Model.Properties.PLOTTED).be(false);
					reactant.let(Model.Properties.INITIAL_LEVEL).be(100);
					model.add(reactant);
					
					reactant = new Reactant("R1");
					reactant.let(Model.Properties.REACTANT_NAME).be("FRZLD");
					reactant.setName("FRZLD");
					reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
					reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
					reactant.let(Model.Properties.ENABLED).be(true);
					reactant.let(Model.Properties.PLOTTED).be(false);
					reactant.let(Model.Properties.INITIAL_LEVEL).be(0);
					model.add(reactant);
					
					reactant = new Reactant("R2");
					reactant.let(Model.Properties.REACTANT_NAME).be("FRZLD_Int");
					reactant.setName("FRZLD_Int");
					reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
					reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
					reactant.let(Model.Properties.ENABLED).be(true);
					reactant.let(Model.Properties.PLOTTED).be(false);
					reactant.let(Model.Properties.INITIAL_LEVEL).be(0);
					model.add(reactant);
					
					reactant = new Reactant("R3");
					reactant.let(Model.Properties.REACTANT_NAME).be("ERK");
					reactant.setName("ERK");
					reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
					reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
					reactant.let(Model.Properties.ENABLED).be(true);
					reactant.let(Model.Properties.PLOTTED).be(true);
					reactant.let(Model.Properties.INITIAL_LEVEL).be(0);
					model.add(reactant);
					
					reactant = new Reactant("R4");
					reactant.let(Model.Properties.REACTANT_NAME).be("ERK P");
					reactant.setName("ERK P");
					reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
					reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
					reactant.let(Model.Properties.ENABLED).be(true);
					reactant.let(Model.Properties.PLOTTED).be(false);
					reactant.let(Model.Properties.INITIAL_LEVEL).be(100);
					model.add(reactant);
					
					
					wnt_frzld = new Reaction("Wnt -> FRZLD");
					wnt_frzld.let(Model.Properties.ENABLED).be(true);
					wnt_frzld.let(Model.Properties.INCREMENT).be(1);
					wnt_frzld.let(Model.Properties.CYTOSCAPE_ID).be("E0");
					wnt_frzld.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
					wnt_frzld.let(Model.Properties.REACTANT).be("R1"); //target
					wnt_frzld.let(Model.Properties.CATALYST).be("R0"); //source
					setScenario(model, wnt_frzld, 1, 0.01, 1);
					model.add(wnt_frzld);
					
					frzld_frzldInt = new Reaction("FRZLD -> FRZLD Int");
					frzld_frzldInt.let(Model.Properties.ENABLED).be(true);
					frzld_frzldInt.let(Model.Properties.INCREMENT).be(1);
					frzld_frzldInt.let(Model.Properties.CYTOSCAPE_ID).be("E1");
					frzld_frzldInt.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
					frzld_frzldInt.let(Model.Properties.REACTANT).be("R2"); //target
					frzld_frzldInt.let(Model.Properties.CATALYST).be("R1"); //source
					setScenario(model, frzld_frzldInt, 1, 0.01, 1);
					model.add(frzld_frzldInt);
					
					frzldInt_frzld = new Reaction("FRZLD Int -| FRZLD");
					frzldInt_frzld.let(Model.Properties.ENABLED).be(true);
					frzldInt_frzld.let(Model.Properties.INCREMENT).be(-1);
					frzldInt_frzld.let(Model.Properties.CYTOSCAPE_ID).be("E2");
					frzldInt_frzld.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
					frzldInt_frzld.let(Model.Properties.REACTANT).be("R1"); //target
					frzldInt_frzld.let(Model.Properties.CATALYST).be("R2"); //source
					setScenario(model, frzldInt_frzld, 0, 0.01, -1);
					model.add(frzldInt_frzld);
					
					frzld_erk = new Reaction("FRZLD -> ERK");
					frzld_erk.let(Model.Properties.ENABLED).be(true);
					frzld_erk.let(Model.Properties.INCREMENT).be(1);
					frzld_erk.let(Model.Properties.CYTOSCAPE_ID).be("E3");
					frzld_erk.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
					frzld_erk.let(Model.Properties.REACTANT).be("R3"); //target
					frzld_erk.let(Model.Properties.CATALYST).be("R1"); //source
					setScenario(model, frzld_erk, 1, 0.01, 1);
					model.add(frzld_erk);
					
					erkP_erk = new Reaction("ERK P -| ERK");
					erkP_erk.let(Model.Properties.ENABLED).be(true);
					erkP_erk.let(Model.Properties.INCREMENT).be(-1);
					erkP_erk.let(Model.Properties.CYTOSCAPE_ID).be("E4");
					erkP_erk.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
					erkP_erk.let(Model.Properties.REACTANT).be("R3"); //target
					erkP_erk.let(Model.Properties.CATALYST).be("R4"); //source
					setScenario(model, erkP_erk, 0, 0.01, -1);
					model.add(erkP_erk);
					
					if (minTimeModel == Integer.MAX_VALUE) {
						minTimeModel = VariablesModel.INFINITE_TIME;
					}
					model.getProperties().let(Model.Properties.MINIMUM_DURATION).be(minTimeModel);
					if (maxTimeModel == Integer.MIN_VALUE) {
						maxTimeModel = VariablesModel.INFINITE_TIME;
					}
					model.getProperties().let(Model.Properties.MAXIMUM_DURATION).be(maxTimeModel);
					
        		}
        		
        		private void updateParameters(DenseMatrix64F params) {
        			double p0 = params.get(0),
        				   p1 = params.get(1),
        				   p2 = params.get(2),
        				   p3 = params.get(3),
        				   p4 = params.get(4);
//        			System.err.println("Nuovi parameteri: " + p0 + ", " + p1 + ", " + p2 + ", " + p3 + ", " + p4);
        			setScenario(model, wnt_frzld, 1, p0, 1);
        			setScenario(model, frzld_frzldInt, 1, p1, 1);
        			setScenario(model, frzldInt_frzld, 0, p2, -1);
        			setScenario(model, frzld_erk, 1, p3, 1);
        			setScenario(model, erkP_erk, 0, p4, -1);

					if (minTimeModel == Integer.MAX_VALUE) {
						minTimeModel = VariablesModel.INFINITE_TIME;
					}
					model.getProperties().let(Model.Properties.MINIMUM_DURATION).be(minTimeModel);
					if (maxTimeModel == Integer.MIN_VALUE) {
						maxTimeModel = VariablesModel.INFINITE_TIME;
					}
					model.getProperties().let(Model.Properties.MAXIMUM_DURATION).be(maxTimeModel);
        		}
        		
        		private void setScenario(Model model, Reaction reaction, Integer scenarioIdx, double parameter, int increment) {
        			double secStepFactor = model.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class);
        			int nLevelsR1 = 100, nLevelsR2 = 100;
        			Scenario[] scenarios = Scenario.THREE_SCENARIOS;
					Scenario scenario;
					double levelsScaleFactor = 1.0;
					String reactant = reaction.get(Model.Properties.REACTANT).as(String.class),
						   catalyst = reaction.get(Model.Properties.CATALYST).as(String.class);
					scenario = scenarios[scenarioIdx];
					switch (scenarioIdx) {
						case 0:
							levelsScaleFactor = 1.0
									/ model.getReactant(reactant).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class)
									* model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
							break;
						case 1:
							levelsScaleFactor = 1.0 * model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS)
									.as(Integer.class);
							break;
						case 2:
							//Assuming no scenario 3 here in the small example, for simplicity's sake...
							break;
						default:
							levelsScaleFactor = 1.0;
							break;
					}
					reaction.let(Model.Properties.SCENARIO).be(scenarioIdx);
					reaction.let(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").be(levelsScaleFactor);
					reaction.let(Model.Properties.SCENARIO_PARAMETER_K).be(parameter);

					String[] parameters = scenario.listVariableParameters();
					HashMap<String, Double> scenarioParameterValues = new HashMap<String, Double>();
					for (int j = 0; j < parameters.length; j++) {
						Double parVal = parameter; //I ASSUME THAT THE PARAMETER IS ONLY ONE: k
						scenario.setParameter(parameters[j], parVal);
						scenarioParameterValues.put(parameters[j], parVal);
					}
					reaction.let(Model.Properties.SCENARIO_CFG).be(new ScenarioCfg(scenarioIdx, scenarioParameterValues));
					reaction.let(Model.Properties.OUTPUT_REACTANT).be(reactant);
					
					String reactionAlias;
					reactionAlias = model.getReactant(reaction.get(Model.Properties.CATALYST).as(String.class)).getName();
					if (scenarioIdx == 2) {
						reactionAlias += " AND "
								+ model.getReactant(reaction.get(Model.Properties.REACTANT).as(String.class)).getName();
					}
					if (reaction.get(Model.Properties.INCREMENT).as(Integer.class) >= 0) {
						reactionAlias += " --> ";
					} else {
						reactionAlias += " --| ";
					}
					if (scenarioIdx == 2) {
						reactionAlias += model.getReactant(reaction.get(Model.Properties.OUTPUT_REACTANT).as(String.class)).getName();
					} else {
						reactionAlias += model.getReactant(reaction.get(Model.Properties.REACTANT).as(String.class)).getName();
					}
					reaction.setName(reactionAlias);

					boolean activeR1 = true, activeR2 = false;
					boolean reactant1IsDownstream = false, reactant2IsDownstream = true;

					if (scenarioIdx == 0 || scenarioIdx == 1) {
						activeR1 = true;
						if (increment >= 0) {
							activeR2 = false;
						} else {
							activeR2 = true;
						}
					} else {
						// This should never happen, because we have already made these checks
						activeR1 = activeR2 = true;
					}
					reaction.let(Model.Properties.R1_IS_DOWNSTREAM).be(reactant1IsDownstream);
					reaction.let(Model.Properties.R2_IS_DOWNSTREAM).be(reactant2IsDownstream);

					double uncertainty = 0.0;
					Double maxValueFormula = Double.POSITIVE_INFINITY, minValueFormula;
					int maxValueInTables, minValueInTables;
					int colMax, rowMax, incrementColMax, incrementRowMax, colMin, rowMin;
					if (activeR1 && !activeR2) {
						colMax = 0;
						rowMax = nLevelsR2; // The largest number should be in the lower-left corner (the first not to be considered INFINITE_TIME)
						incrementColMax = 1;
						incrementRowMax = -1;
						colMin = nLevelsR1;
						rowMin = 0; // The smallest number should be in the top-right corner
					} else if (activeR1 && activeR2) {
						colMax = 0;
						rowMax = 0; // The largest number should be in the top-left corner (the first != INF)
						incrementColMax = 1;
						incrementRowMax = 1;
						colMin = nLevelsR1;
						rowMin = nLevelsR2; // The smallest number should be in the lower right corner
					} else if (!activeR1 && !activeR2) {
						colMax = nLevelsR1;
						rowMax = nLevelsR2; // The largest number should be in the lower right corner (the first != INF)
						incrementColMax = -1;
						incrementRowMax = -1;
						colMin = 0;
						rowMin = 0; // The smallest number should be in the top-left corner
					} else if (!activeR1 && activeR2) {
						colMax = nLevelsR1;
						rowMax = 0; // The largest number should be in the top-right corner (the first != INF)
						incrementColMax = -1;
						incrementRowMax = 1;
						colMin = 0;
						rowMin = nLevelsR2; // The smallest number should be in the lower-left corner
					} else {
						// This should never happen, as we have already considered all 4 possibilities for activeR1 and activeR2
						colMax = rowMax = colMin = rowMin = incrementColMax = incrementRowMax = 1;
					}
					minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
					while (Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0
							&& rowMax <= nLevelsR2) {
						colMax = colMax + incrementColMax;
						rowMax = rowMax + incrementRowMax;
						maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
					}

					if (Double.isInfinite(minValueFormula)) {
						minValueInTables = VariablesModel.INFINITE_TIME;
					} else {
						if (uncertainty == 0) {
							minValueInTables = Math.max(0,
									(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula));
						} else {
							minValueInTables = Math.max(
									0,
									(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula
											* (1 - uncertainty / 100.0)));
						}
					}
					if (Double.isInfinite(maxValueFormula)) {
						maxValueInTables = VariablesModel.INFINITE_TIME;
					} else {
						if (uncertainty == 0) {
							maxValueInTables = Math.max(0,
									(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula));
						} else {
							maxValueInTables = Math.max(
									0,
									(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula
											* (1 + uncertainty / 100.0)));
						}
					}
					reaction.let(Model.Properties.MINIMUM_DURATION).be(minValueInTables);
					reaction.let(Model.Properties.MAXIMUM_DURATION).be(maxValueInTables);
					if (minValueInTables != VariablesModel.INFINITE_TIME
							&& (minTimeModel == Integer.MAX_VALUE || minValueInTables < minTimeModel)) {
						minTimeModel = minValueInTables;
					}
					if (maxValueInTables != VariablesModel.INFINITE_TIME
							&& (maxTimeModel == Integer.MIN_VALUE || maxValueInTables > maxTimeModel)) {
						maxTimeModel = maxValueInTables;
					}

					String r1Id = reaction.get(Model.Properties.CATALYST).as(String.class);
					String r2Id = reaction.get(Model.Properties.REACTANT).as(String.class);
					String rOutput = reaction.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
					reaction.setId(r1Id + "_" + r2Id + ((rOutput.equals(r2Id)) ? "" : "_" + rOutput));

        		}
        		
    			@Override
    			public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y) {
//    				y.set(0, 0, x.get(0, 0));
//    				y.set(1, 0, x.get(1, 0));
//    				y.set(2, 0, x.get(2, 0));
////    				for (int t = 5; t <= 10; t += 5) {
////    					y.set(0, 0, t);
////    					y.set(1, 0, x.get(1, 0));
////    					y.set(2, 0, y.get(2, 0) + y.get(1, 0) * param.get(0, 0));
////    				}
//    				for (int i = 1; i < 3; i++) {
//    					y.set(i * 3, 0, i * 5);
//    					y.set(i * 3 + 1, 0, y.get((i-1)*3+1, 0));
//    					y.set(i * 3 + 2, 0, y.get((i-1)*3+2, 0) + y.get((i-1)*3+1, 0) * param.get(0, 0) * (15 - y.get((i-1)*3+2, 0)));
//    				}
//    				contaTentativi++;
//    				System.out.println("Tentativo " + contaTentativi + ". p = " + param.get(0, 0) + ", res Y:");
//    				printMatrix(y);
    				
    				contaTentativi++;
//    				System.out.println("Tentativo " + contaTentativi); // + ". p = " + param);
    				updateParameters(param);
    				int nMinutesToSimulate = 240;
    				int timeTo = (int) (nMinutesToSimulate * 60.0 / model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class));
    				double scale = (double) nMinutesToSimulate / timeTo;
    				SimpleLevelResult result = null;
    				try {
    					PrintStream sysErr = System.err;
    					System.setErr(new PrintStream(new OutputStream() {
    					    public void write(int b) {
    					    	//Not interested in UPPAAL analysis output here
    					    }
    					}));
    					result = (SimpleLevelResult)new UppaalModelAnalyserSMC(null, null).analyze(model, timeTo).filter(Arrays.asList("R3", "R1"));
    					System.setErr(sysErr);
    				} catch (Exception ex) {
    					ex.printStackTrace(System.out);
    				}
    				//graph.reset();
    				Map<String, String> seriesNameMapping = new HashMap<String, String>();
    				seriesNameMapping.put("R3", "ERK");
    				seriesNameMapping.put("R1", "Frizzled");
    				graph.reset();
    				graph.parseLevelResult(result, seriesNameMapping, scale);
    				double maxTime = scale * result.getTimeIndices().get(result.getTimeIndices().size() - 1);
    				try {
						graph.parseCSV("/Users/stefano/Documents/Lavoro/Prometheus/Data_Wnt_0-240_erk-frzld.csv");
						Vector<String> seriesNames = graph.getSeriesNames();
						for (int i = 0; i < seriesNames.size(); i++) {
							String name = seriesNames.get(i);
							if (name.equals("ERK")) {
								graph.setSeriesColor(i, Color.BLUE.brighter());
							} else if (name.startsWith("ERK")) {
								graph.setSeriesColor(i, Color.BLUE.darker());
							} else if (name.equals("Frizzled")) {
								graph.setSeriesColor(i, Color.RED.brighter());
							} else if (name.startsWith("Frizzled")) {
								graph.setSeriesColor(i, Color.RED.darker());
							}
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace(System.err);
					} catch (IOException e) {
						e.printStackTrace(System.err);
					}
    				graph.declareMaxYValue(100);
    				graph.setDrawArea(0, maxTime, 0, 100);
    				Component w = graph.getParent();
    				while (w != null) {
	    				if (w instanceof JFrame) {
	    					JFrame f = (JFrame)w;
	    					f.setTitle("Attempt nr. " + contaTentativi);
	    					break;
	    				}
	    				w = w.getParent();
    				}
    				graph.repaint();
//    				try {
//    					Thread.sleep(500);
//    				} catch (Exception ex) {
//    				}
    				y.set(levelResultToMatrix(result, scale, Arrays.asList(0.0, 30.0, 60.0, 120.0, 240.0)));
    			}
        	};
        	final Graph graph = new Graph();
        	final JFrame frame = new JFrame();
        	frame.getContentPane().add(graph, BorderLayout.CENTER);
        	final JProgressBar progressBar = new JProgressBar(0, 100);
        	frame.getContentPane().add(progressBar, BorderLayout.SOUTH);
        	frame.setBounds(100, 100, 600, 400);
        	frame.setVisible(true);
        	final Function function = new MyFunction(graph);
    		final LevenbergMarquardt lm = new LevenbergMarquardt(function);
        	lm.MIN_COST = 0.5;
        	lm.DELTA = 0.0001;
        	final DenseMatrix64F initParam, X, Y;
        	//exact parameters: {0.000625}, {0.0001}, {0.0008}, {0.04}, {0.015}
        	//with scenarios:        1         0        1         1       0
        	/*
        	After 61 attempts (starting from {0.0004}, {0.0004}, {0.0032}, {0.008}, {0.002}, all with scenario = 1 apart from the last which is 0),
        	which generate the data series [0, 31, 21, 0, 0] 
        	we get to these parameters
			[ 4.668297736019977E-4 ]
			[ 5.564644858790185E-5 ]
			[ 0.0032720555713418465 ]
			[ 0.00804136519174234 ]
			[ 0.0024406512494464087 ]
			which generate the data series [0, 38, 52, 36, 6] which compared to [0, 41, 51, 38, 0] is quite good (!!!)
        	 */
        	/* It seems to work also with 2 series. Using the same scenarios (if I use scenario 1 in place of 0 for FRZLD_INH --| FRZLD I cannot expect to match both FRZLD and ERK with the data coming from a scenario 0..),
        	 * starting from these parameters {0.0005}, {0.0002}, {0.0007}, {0.04}, {0.015}
        	 * which give the series ERK [0, 31, 39, 9, 0] and FRZLD [0, 55, 62, 41, 12]
        	 * after 33 attempts I get these parameters
				[ 6.261770864790466E-4 ]
				[ 9.820281731098279E-5 ]
				[ 8.054458653958224E-4 ]
				[ 0.03993577706209611 ]
				[ 0.014830950368103588 ]
			  which give the data series ERK [0, 42, 52, 39, 0] and FRZLD [0, 65, 76, 61, 34]
        	 */
        	initParam = new DenseMatrix64F(new double[][]{{0.0005}, {0.0002}, {0.0007}, {0.04}, {0.015}}); //{0.0004}, {0.0004}, {0.0032}, {0.008}, {0.002}}); //{0.0004}, {0.0001}, {0.0008}, {0.04}, {0.015}});
        	//X = new DenseMatrix64F(new double[][]{{0}, {15}, {0}, {5}, {15}, {0}, {10}, {15}, {0}}); //{10}, {15}, {0}}); //Cosi' va in 5 tentativi. Ora proviamo a salvare tutta una serie di dati in riga (perche' vuole le righe??)
        	//Y = new DenseMatrix64F(new double[][]{{0}, {15}, {0}, {5}, {15}, {10}, {10}, {15}, {15}}); //{10}, {15}, {15}});
        	X = new DenseMatrix64F(new double[][]{{0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}}); //{0}, {0}, {30}, {0}, {60}, {0}, {120}, {0}
        	Y = scass;
//        	System.out.println("X");
        	printMatrix(X);
//        	System.out.println("Y");
        	printMatrix(Y);
//        	System.out.println("Fine Y");
//        	boolean success = false;
        	//success = lm.optimize(initParam, X, Y);
        	class MySwingWorker extends SwingWorker<Boolean, Void> {
        		private boolean success = false;
        		private boolean mustTerminate = false;
        		
        		@SuppressWarnings("unused")
				public boolean getMustTerminate() {
        			return mustTerminate;
        		}
        		
        		public void setMustTerminate(boolean mustTerminate) {
        			this.mustTerminate = mustTerminate;
        		}
        		
        		@SuppressWarnings("unused")
				public void setProgresso(Integer progress) {
        			this.setProgress(progress);
        		}
        		
				@Override
				protected Boolean doInBackground() throws Exception {
					this.setProgress(0);
					lm.setSwingWorker(this);
					success = lm.optimize(initParam, X, Y);
					this.setProgress(100);
					return success;
				}
				
				@Override
				protected void done() {
					System.err.println("Ho fatto: success = " + success);
				}
        		
        	};
        	final MySwingWorker worker = new MySwingWorker();
        	JButton chiudi = new JButton("Chiudi");
        	chiudi.addActionListener(new ActionListener() {
        		public void actionPerformed(ActionEvent e) {
        			//JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Chiudo qui!");
        			worker.setMustTerminate(true); //Just close the window to cancel the job
        		}
        	});
        	frame.add(chiudi, BorderLayout.EAST);
//        	frame.addWindowListener(new WindowAdapter() {
//        		@Override
//        		public void windowClosed(WindowEvent e) {
//        			new Thread() {
//        				public void run() {
//		        			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Chiudo qui!");
//		        			worker.setMustTerminate(true); //Just close the window to cancel the job
//        				}
//        			}.start();
//        		}
//        	});
        	System.err.print("Eseguo l'ottimizzazione");
        	final long startTime = System.currentTimeMillis();
        	worker.addPropertyChangeListener(new PropertyChangeListener() {
        		int contaFalsiAllarmi = 0;
        		
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					//The other default property is "progress", i.e. the progress, and its event is generated every time the setProgress is called in the worker
					if (!evt.getPropertyName().equals("state") || !worker.getState().equals(SwingWorker.StateValue.DONE)) {
						contaFalsiAllarmi++;
						if (evt.getPropertyName().equals("progress")) {
							System.err.println("Progresso: " + worker.getProgress());
							progressBar.setValue(worker.getProgress());
						}
						return;
					}
					System.err.println("Fatto! (forse?), con " + contaFalsiAllarmi + " falsi allarmi..");
					boolean success = false;
		        	try {
						success = worker.get();
					} catch (InterruptedException e) {
						e.printStackTrace(System.err);
					} catch (ExecutionException e) {
						e.printStackTrace(System.err);
					}
//		        	function.compute(initParam, X, Y);
//		        	printMatrix(Y);
		        	long endTime = System.currentTimeMillis();

		        	JFrame newFrame = new JFrame("Resulting graph (cost " + lm.getFinalCost() + ")");
		        	graph.reset();
		        	function.compute(lm.getParameters(), X, Y);
		        	newFrame.getContentPane().add(graph, BorderLayout.CENTER);
		        	newFrame.setBounds(frame.getBounds());
		        	frame.setVisible(false);
		        	frame.dispose();
		        	
		        	if (success) {
		        		System.out.println("Ho trovato i parametri migliori: " + lm.getParameters());
		        		System.out.println("Costo iniziale: " + lm.getInitialCost() + ". Costo finale: " + lm.getFinalCost());
		        		printMatrix(lm.getParameters());
		        		JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Done in " + RunAction.timeDifferenceFormat(startTime, endTime) + ".\nFinal cost: " + lm.getFinalCost() + "\nParameters: " + lm.getParameters(), "Good result found!", JOptionPane.INFORMATION_MESSAGE);
		        	} else {
		        		System.out.println("Niente parametri migliori. Ho questi: " + lm.getParameters());
		        		System.out.println("Costo iniziale: " + lm.getInitialCost() + ". Costo finale: " + lm.getFinalCost());
		        		JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Done in " + RunAction.timeDifferenceFormat(startTime, endTime) + ".\nMinimum cost found: " + lm.getFinalCost() + "\nParameters with that cost: " + lm.getParameters(), "No good result found!", JOptionPane.ERROR_MESSAGE);
		        	}
		        	
		        	newFrame.setVisible(true);
				}
        		
        	});
        	worker.execute();
//        	int p = worker.getProgress();
//        	while (p < 100) {
//        		System.err.print(".");
//        		try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//				}
//        		p = worker.getProgress();
//        	}
//        	System.err.println(" Fatto!");
//        	try {
//				success = worker.get();
//			} catch (InterruptedException e) {
//				e.printStackTrace(System.err);
//			} catch (ExecutionException e) {
//				e.printStackTrace(System.err);
//			}
////        	function.compute(initParam, X, Y);
////        	printMatrix(Y);
//        	if (success) {
//        		System.out.println("Ho trovato i parametri migliori: " + lm.getParameters());
//        		System.out.println("Costo iniziale: " + lm.getInitialCost() + ". Costo finale: " + lm.getFinalCost());
//        		printMatrix(lm.getParameters());
//        	} else {
//        		System.out.println("Niente parametri migliori. Ho questi: " + lm.getParameters());
//        		System.out.println("Costo iniziale: " + lm.getInitialCost() + ". Costo finale: " + lm.getFinalCost());
//        	}
    	}
    }
    
    public static void main(String args[]) {
    	new FaiIlLavoro().vai();
    }
}
