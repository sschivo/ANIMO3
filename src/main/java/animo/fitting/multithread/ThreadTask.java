package animo.fitting.multithread;

public class ThreadTask extends Thread {
	private ThreadPool pool;
	private boolean finished = false;
	private int myIdx = 0;

	public ThreadTask(ThreadPool thePool, int idx) {
		pool = thePool;
		myIdx = idx;
	}

	public void finish() {
		this.finished = true;
	}

	public int getIdx() {
		return myIdx;
	}

	@Override
	public void run() {
		while (!finished) {
			// blocks until job
			pool.increaseIdle();
			Runnable job = pool.getNext();
			if (finished || job == null)
				return;

			pool.decreaseIdle();
			job.run(); // run, non start: lo voglio eseguire in questo thread

		}
	}
}
