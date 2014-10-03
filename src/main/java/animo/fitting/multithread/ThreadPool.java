package animo.fitting.multithread;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ThreadPool
{
    private int nThreads = 0;
    private LinkedList<Runnable> tasks = new LinkedList<Runnable>();
    private List<ThreadTask> workers = new ArrayList<ThreadTask>();
    int countIdle = 0;

    public ThreadPool(int size)
    {
        this.nThreads = size;
        int idx = 0;
        for (int i = 0; i < size; i++)
        {
            ThreadTask thread = new ThreadTask(this, idx++);
            workers.add(thread);
            thread.start();
        }
    }

    public void addTask(Runnable task)
    {
        synchronized (tasks)
        {
            tasks.addLast(task);
            tasks.notify();
        }
    }

    public synchronized void decreaseIdle()
    {
        countIdle--;
    }

    public Runnable getNext()
    {
        Runnable returnVal = null;
        synchronized (tasks)
        {
            while (tasks.isEmpty())
            {
                try
                {
                    tasks.wait();
                }
                catch (InterruptedException ex)
                {
                    //System.err.println("Interrupted");
                    return null;
                }
            }
            if (tasks.isEmpty())
                return null;
            returnVal = tasks.removeFirst();
        }
        return returnVal;
    }

    public synchronized void increaseIdle()
    {
        countIdle++;
    }

    public boolean isEmpty()
    {
        return countIdle == nThreads;
    }

    public void terminateAll()
    {
        for (ThreadTask t : workers)
        {
            t.finish();
            t.interrupt();
        }
        synchronized (tasks)
        {
            tasks.notifyAll();
        }
    }
}
