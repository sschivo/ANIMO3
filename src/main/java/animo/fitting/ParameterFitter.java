package animo.fitting;

import java.util.List;
import java.util.Vector;

public abstract class ParameterFitter {
	protected List<ParameterFitterObserver> observers = null;
	
	public ParameterFitter() {
		observers = new Vector<ParameterFitterObserver>();
	}
	
	public void registerObserver(ParameterFitterObserver o) {
		this.observers.add(o);
	}
	
	protected void notifyObservers() {
		for (ParameterFitterObserver o : observers) {
			o.notifyDone();
		}
	}
}
