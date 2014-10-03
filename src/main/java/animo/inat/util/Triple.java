package animo.inat.util;

public class Triple<T, S, U>
{
    public T first;
    public S second;
    public U third;

    public Triple()
    {
    }

    public Triple(T first, S second, U third)
    {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * returns a (deep) copy of this
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Triple<T, S, U> copy()
    {
        Triple<T, S, U> triple = new Triple<T, S, U>();
        Class c1 = this.first.getClass(), c2 = this.second.getClass(), c3 = this.third.getClass();
        if (Table.class.equals(c1))
        {
            triple.first = (T) ((Table) this.first).copy();
        }
        else if (Pair.class.equals(c1))
        {
            triple.first = (T) ((Pair) this.first).copy();
        }
        else if (Triple.class.equals(c1))
        {
            triple.first = (T) ((Triple) this.first).copy();
        }
        else if (Quadruple.class.equals(c1))
        {
            triple.first = (T) ((Quadruple) this.first).copy();
        }
        else
        {
            triple.first = this.first;
        }
        if (Table.class.equals(c2))
        {
            triple.second = (S) ((Table) this.second).copy();
        }
        else if (Pair.class.equals(c2))
        {
            triple.second = (S) ((Pair) this.second).copy();
        }
        else if (Triple.class.equals(c2))
        {
            triple.second = (S) ((Triple) this.second).copy();
        }
        else if (Quadruple.class.equals(c2))
        {
            triple.second = (S) ((Quadruple) this.second).copy();
        }
        else
        {
            triple.second = this.second;
        }
        if (Table.class.equals(c3))
        {
            triple.third = (U) ((Table) this.third).copy();
        }
        else if (Pair.class.equals(c3))
        {
            triple.third = (U) ((Pair) this.third).copy();
        }
        else if (Triple.class.equals(c3))
        {
            triple.third = (U) ((Triple) this.third).copy();
        }
        else if (Quadruple.class.equals(c3))
        {
            triple.third = (U) ((Quadruple) this.third).copy();
        }
        else
        {
            triple.third = this.third;
        }
        return triple;
    }
}
