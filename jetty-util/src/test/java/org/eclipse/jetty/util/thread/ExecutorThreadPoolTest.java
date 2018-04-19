package org.eclipse.jetty.util.thread;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * A minimal reproducer for an NPE when instantiating a new {@link ExecutorThreadPool} subclass
 * that overrides {@link ExecutorThreadPool#getMinThreads()}.
 */
public class ExecutorThreadPoolTest {

  @Test public void repro() {
    ThreadPoolExecutor ex = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new SynchronousQueue<>());
    new MyExecutorThreadPool(ex);
  }

  private static class MyExecutorThreadPool extends ExecutorThreadPool implements
      ThreadPool.SizedThreadPool {

    private final ThreadPoolExecutor ex;

    MyExecutorThreadPool(ThreadPoolExecutor ex) {
      super(ex);
      this.ex = ex;
    }

    @Override public int getMinThreads() {
      return ex.allowsCoreThreadTimeOut() ? 0 : ex.getCorePoolSize(); // NPE here
    }

    @Override public int getMaxThreads() {
      return ex.getMaximumPoolSize();
    }

    @Override public void setMinThreads(int threads) {
      throw new UnsupportedOperationException();
    }

    @Override public void setMaxThreads(int threads) {
      throw new UnsupportedOperationException();
    }
  }
}
