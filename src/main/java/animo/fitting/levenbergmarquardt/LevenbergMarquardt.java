package animo.fitting.levenbergmarquardt;

import static org.ejml.ops.CommonOps.add;
import static org.ejml.ops.CommonOps.multTransB;
import static org.ejml.ops.CommonOps.scale;
import static org.ejml.ops.CommonOps.solve;
import static org.ejml.ops.CommonOps.subtract;
import static org.ejml.ops.CommonOps.subtractEquals;
import static org.ejml.ops.SpecializedOps.diffNormF;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.ejml.data.DenseMatrix64F;

import animo.core.analyser.LevelResult;
import animo.core.graph.Graph;
import animo.fitting.levenbergmarquardt.LevenbergMarquardtFitter.LMSwingWorker;

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
    private double finalCost = -1;

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

        for( int iter = 0; iter < 20 && difference > 1e-6 ; iter++ ) {
            // compute some variables based on the gradient
            computeDandH(param,X,Y);

            // try various step sizes and see if any of them improve the
            // results over what has already been done
//            boolean foundBetter = false;
            for( int i = 0; i < 5; i++ ) {
                computeA(A,H,lambda);

                if( !solve(A,d,negDelta) ) { //TODO Why is this so bad??
                	finalCost = minCost;
        			param.set(minimumCostParameters);
                    return false;
        			//continue;
                }
                // compute the candidate parameters
                subtract(param, negDelta, tempParam);

                double cost = cost(tempParam,X,Y);

                if (cost <= minCost) { //I use also the '=' to get the most updated parameter values that gave the minimum cost
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
//                if (cost < MIN_COST) {
//                	finalCost = cost;
//                	param.set(tempParam);
//                	return true;
//                }
//                if (cost <= 0) { //Maybe having a MIN_COST is not that wise, but if we reach a cost = 0 we could think about going home =)
//                	finalCost = cost;
//                	param.set(tempParam);
//                	return true;
//                }
                
                if( cost < prevCost ) {
                    // the candidate parameters produced better results so use it
//                    foundBetter = true;
                    param.set(tempParam);
                    difference = prevCost - cost;
                    prevCost = cost;
                    lambda /= 10.0;
                } else {
                    lambda *= 10.0;
                }
            }

            //I think it works better if we comment the check below
//            // it reached a point where it can't improve so exit
//            if( !foundBetter )
//                break;
        }
//        finalCost = prevCost;
        
        //Instead of stopping at the last state, we return the state with the minimum cost
        finalCost = minCost;
		param.set(minimumCostParameters);
		
        return finalCost < initialCost; //Comparison with MIN_COST is made already as soon as possible
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
//    	System.err.println("Valuto i parametri");
//    	printMatrix(param);
		
    	for (int i = 0; i < param.getNumRows(); i++) {
    		if (param.get(i) < 0) { //ANIMO models require parameters to be strictly positive!
//    			System.err.println("Parametro negativo trovato!");
    			return Double.POSITIVE_INFINITY;
    		}
    	}
//    	System.err.println();
        func.compute(param, X, temp0);
        
        double error = diffNormF(temp0, Y);

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
    
    public static DenseMatrix64F readCSVtoMatrix(String csvFileName, List<String> selectedColumns, double untilTime) throws IOException {
		LevelResult levelResult = Graph.readCSVtoLevelResult(csvFileName, selectedColumns, untilTime);
		return levelResultToMatrix(levelResult, selectedColumns);
	}
    
    public static DenseMatrix64F levelResultToMatrix(LevelResult levelResult, List<String> seriesOrder) {
    	return levelResultToMatrix(levelResult, seriesOrder, 1.0);
    }
    
    public static DenseMatrix64F levelResultToMatrix(LevelResult levelResult, List<String> seriesOrder, double scaleFactor) {
    	return levelResultToMatrix(levelResult, seriesOrder, 1.0, Collections.<Double>emptyList());
    }
	
	public static DenseMatrix64F levelResultToMatrix(LevelResult levelResult, List<String> seriesOrder, double scaleFactor, List<Double> timePoints) {
		List<String> columnNames = new Vector<String>();
		columnNames.addAll(levelResult.getReactantIds());
		if (!columnNames.containsAll(seriesOrder)) { //We must have the same data indices, of course!
			System.err.print("The list [");
			for (String s : columnNames) {
				System.err.print(s + ", ");
			}
			System.err.print("\b\b] does not contain the list [");
			for (String s : seriesOrder) {
				System.err.print(s + ", ");
			}
			System.err.println("\b\b]!");
			return null;
		} else {
			columnNames = seriesOrder; //This way we keep the correct order
		}
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
	
	
	public static void printMatrix(DenseMatrix64F x) {
    	for (int r = 0; r < x.getNumRows(); r++) {
			System.out.print("[ ");
			for (int c = 0; c < x.getNumCols()-1; c++) {
				System.out.print(x.get(r, c) + ", ");
			}
			System.out.println(x.get(r, x.getNumCols() - 1) + " ]");
		}
    }
	
}
