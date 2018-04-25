package org.ecplipse.jetty.threadtest;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool.SizedThreadPool;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.jetty.http.HttpVersion.HTTP_1_1;

/**
 * A minimal repro for an elevated thread count issue in Jetty.
 */
public class ThreadCountRepro {

  private static final int SERVER_PORT = 1338;

  private static final int CORE_THREADS = 300;
  private static final int MAX_THREADS = 300;
  private static final int THREAD_KEEPALIVE_SECS = 20;

  private static final String KEYSTORE_PATH = "/data/app/keys.jceks";
  private static final String KEYSTORE_PASS = "CHANGE";

  public static void main(String ...args) throws Exception {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS,
        THREAD_KEEPALIVE_SECS, SECONDS, new ArrayBlockingQueue<>(MAX_THREADS),
        (ThreadFactory) Thread::new);
    executor.allowCoreThreadTimeOut(true);

    Server server = new Server(new SizedExecutorThreadPool(executor));

    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setExcludeCipherSuites();
    sslContextFactory.setIncludeProtocols("TLSv1.2");
    sslContextFactory.setIncludeCipherSuites("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");

    try {
      KeyStore ks = KeyStore.getInstance("JCEKS");

      ks.load(new FileInputStream(KEYSTORE_PATH), KEYSTORE_PASS.toCharArray());
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, KEYSTORE_PASS.toCharArray());

      KeyManager[] kms = kmf.getKeyManagers();

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kms, new TrustManager[]{new TrustAllTrustManager()}, null);

      sslContextFactory.setSslContext(sslContext);
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }

    HttpConfiguration httpsConfig = new HttpConfiguration();
    ServerConnector httpsConnector =
        new ServerConnector(server, -1, -1, /* use the default accept and select threads */
            new SslConnectionFactory(sslContextFactory, HTTP_1_1.toString()),
            new HttpConnectionFactory(httpsConfig));
    httpsConnector.setPort(SERVER_PORT);
    server.setConnectors(new Connector[]{httpsConnector});

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new MyServlet()),"/*");
    server.setHandler(context);

    server.start();
    Thread.sleep(1_000_000); // run for "a while"
    server.stop();
  }

  /**
   * Returns 200 for HEAD requests.
   */
  private static class MyServlet extends HttpServlet {

    public MyServlet() {
    }

    @Override protected void doHead(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setStatus(200);
    }
  }

  private static class SizedExecutorThreadPool extends ExecutorThreadPool
      implements SizedThreadPool {

    private final ThreadPoolExecutor ex;

    SizedExecutorThreadPool(ThreadPoolExecutor ex) {
      super(ex);
      this.ex = ex;
    }

    @Override public int getMinThreads() {
      return ex.allowsCoreThreadTimeOut() ? 0 : ex.getCorePoolSize();
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

  private static class TrustAllTrustManager implements X509TrustManager {
    @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
        throws CertificateException {}

    @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
        throws CertificateException {}

    @Override public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }
}
