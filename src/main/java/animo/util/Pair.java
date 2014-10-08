package animo.util;

public class Pair<T, S> {
	public T first;
	public S second;

	public Pair() {
	}

	public Pair(T first, S second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * returns a (deep) copy of this
	 * 
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Pair<T, S> copy() {
		Pair<T, S> pair = new Pair<T, S>();
		Class c1 = this.first.getClass(), c2 = this.second.getClass();
		if (Table.class.equals(c1)) {
			pair.first = (T) ((Table) this.first).copy();
		} else if (Pair.class.equals(c1)) {
			pair.first = (T) ((Pair) this.first).copy();
		} else if (Triple.class.equals(c1)) {
			pair.first = (T) ((Triple) this.first).copy();
		} else if (Quadruple.class.equals(c1)) {
			pair.first = (T) ((Quadruple) this.first).copy();
		} else {
			pair.first = this.first;
		}
		if (Table.class.equals(c2)) {
			pair.second = (S) ((Table) this.second).copy();
		} else if (Pair.class.equals(c2)) {
			pair.second = (S) ((Pair) this.second).copy();
		} else if (Triple.class.equals(c2)) {
			pair.second = (S) ((Triple) this.second).copy();
		} else if (Quadruple.class.equals(c2)) {
			pair.second = (S) ((Quadruple) this.second).copy();
		} else {
			pair.second = this.second;
		}
		return pair;
	}
}
