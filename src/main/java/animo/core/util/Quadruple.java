package animo.core.util;

public class Quadruple<T, S, U, V>
{
    public T first;
    public S second;
    public U third;
    public V fourth;

    public Quadruple()
    {
    }

    public Quadruple(T first, S second, U third, V fourth)
    {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }


    /**
     * returns a (deep) copy of this
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Quadruple<T, S, U, V> copy()
    {
        Quadruple<T, S, U, V> quadruple = new Quadruple<T, S, U, V>();
        Class c1 = this.first.getClass(), c2 = this.second.getClass(), c3 = this.third.getClass(), c4 = this.fourth.getClass();
        if (Table.class.equals(c1))
        {
            quadruple.first = (T) ((Table) this.first).copy();
        }
        else if (Pair.class.equals(c1))
        {
            quadruple.first = (T) ((Pair) this.first).copy();
        }
        else if (Triple.class.equals(c1))
        {
            quadruple.first = (T) ((Triple) this.first).copy();
        }
        else if (Quadruple.class.equals(c1))
        {
            quadruple.first = (T) ((Quadruple) this.first).copy();
        }
        else
        {
            quadruple.first = this.first;
        }
        if (Table.class.equals(c2))
        {
            quadruple.second = (S) ((Table) this.second).copy();
        }
        else if (Pair.class.equals(c2))
        {
            quadruple.second = (S) ((Pair) this.second).copy();
        }
        else if (Triple.class.equals(c2))
        {
            quadruple.second = (S) ((Triple) this.second).copy();
        }
        else if (Quadruple.class.equals(c2))
        {
            quadruple.second = (S) ((Quadruple) this.second).copy();
        }
        else
        {
            quadruple.second = this.second;
        }
        if (Table.class.equals(c3))
        {
            quadruple.third = (U) ((Table) this.third).copy();
        }
        else if (Pair.class.equals(c3))
        {
            quadruple.third = (U) ((Pair) this.third).copy();
        }
        else if (Triple.class.equals(c3))
        {
            quadruple.third = (U) ((Triple) this.third).copy();
        }
        else if (Quadruple.class.equals(c3))
        {
            quadruple.third = (U) ((Quadruple) this.third).copy();
        }
        else
        {
            quadruple.third = this.third;
        }
        if (Table.class.equals(c4))
        {
            quadruple.fourth = (V) ((Table) this.fourth).copy();
        }
        else if (Pair.class.equals(c4))
        {
            quadruple.fourth = (V) ((Pair) this.fourth).copy();
        }
        else if (Triple.class.equals(c4))
        {
            quadruple.fourth = (V) ((Triple) this.fourth).copy();
        }
        else if (Quadruple.class.equals(c4))
        {
            quadruple.fourth = (V) ((Quadruple) this.fourth).copy();
        }
        else
        {
            quadruple.fourth = this.fourth;
        }
        return quadruple;
    }

}
