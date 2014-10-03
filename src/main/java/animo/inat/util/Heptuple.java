package animo.inat.util;

public class Heptuple<T, S, U, V, W, X, Y>
{
    public Quadruple<T, S, U, V> first4;
    public Triple<W, X, Y> second3;
    public T first;
    public S second;
    public U third;
    public V fourth;
    public W fifth;
    public X sixth;
    public Y seventh;

    public Heptuple()
    {
    }

    public Heptuple(Quadruple<T, S, U, V> first4, Triple<W, X, Y> second3)
    {
        this.first4 = first4;
        first = first4.first;
        second = first4.second;
        third = first4.third;
        fourth = first4.fourth;
        this.second3 = second3;
        fifth = second3.first;
        sixth = second3.second;
        seventh = second3.third;
    }

    public Heptuple(T first, S second, U third, V fourth, W fifth, X sixth, Y seventh)
    {
        this(new Quadruple<T, S, U, V>(first, second, third, fourth), new Triple<W, X, Y>(fifth, sixth, seventh));
    }


    /**
     * returns a (deep) copy of this
     * @return
     */
    public Heptuple<T, S, U, V, W, X, Y> copy()
    {
        Quadruple<T, S, U, V> firstCopy = first4.copy();
        Triple<W, X, Y> secondCopy = second3.copy();
        return new Heptuple<T, S, U, V, W, X, Y>(firstCopy, secondCopy);
    }

}
