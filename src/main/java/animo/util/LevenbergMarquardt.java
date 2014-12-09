package animo.util;

import static org.ejml.ops.CommonOps.add;
import static org.ejml.ops.CommonOps.multTransB;
import static org.ejml.ops.CommonOps.scale;
import static org.ejml.ops.CommonOps.solve;
import static org.ejml.ops.CommonOps.subtract;
import static org.ejml.ops.CommonOps.subtractEquals;
import static org.ejml.ops.SpecializedOps.diffNormF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import org.ejml.data.DenseMatrix64F;

import animo.core.analyser.LevelResult;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.graph.Graph;
import animo.core.graph.P;
import animo.core.graph.SmartTokenizer;
import animo.fitting.LevenbergMarquardtFitter.LMSwingWorker;

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
    
    private LMSwingWorker swingWorker;
    public void setSwingWorker(LMSwingWorker worker) {
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
            		int progresso = (int)Math.round(100.0 - 100.0 * cost / initialCost);
            		if (progresso < 0) {
            			progresso = 0;
            		}
            		if (progresso > 100) {
            			progresso = 100;
            		}
            		swingWorker.setProgresso(new Integer(progresso));
                
            		if (swingWorker.getMustTerminate()) { //We must end before completing the computation: return the best result up to now (stored in minimumCostParameters, with cost minCost)
            			finalCost = minCost;
            			param.set(minimumCostParameters);
            			return false;
            		}
                }
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
	
	@SuppressWarnings("unused")
	private static void printMatrix(DenseMatrix64F x) {
    	for (int r = 0; r < x.getNumRows(); r++) {
			System.out.print("[ ");
			for (int c = 0; c < x.getNumCols()-1; c++) {
				System.out.print(x.get(r, c) + ", ");
			}
			System.out.println(x.get(r, x.getNumCols() - 1) + " ]");
		}
    }
	
}
