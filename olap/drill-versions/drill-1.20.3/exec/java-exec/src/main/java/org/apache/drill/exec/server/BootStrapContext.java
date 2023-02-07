/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.server;

import com.codahale.metrics.MetricRegistry;
import io.netty.channel.EventLoopGroup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.SynchronousQueue;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.KerberosUtil;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.metrics.DrillMetrics;
import org.apache.drill.exec.rpc.NamedThreadFactory;
import org.apache.drill.exec.rpc.TransportCheck;
import org.apache.drill.exec.rpc.security.AuthenticatorProvider;
import org.apache.drill.exec.rpc.security.AuthenticatorProviderImpl;
import org.apache.drill.exec.server.options.OptionDefinition;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.security.UserGroupInformation;

public class BootStrapContext implements AutoCloseable {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BootStrapContext.class);
  // Tests and embedded servers need a small footprint, so the minimum
  // scan count is small. The actual value is set by the
  // ExecConstants.SCAN_THREADPOOL_SIZE. If the number below
  // is large, then tests cannot shrink the number using the
  // config property.
  private static final int MIN_SCAN_THREADPOOL_SIZE = 4; // Magic num

  // DRILL_HOST_NAME sets custom host name. See drill-env.sh for details.
  private static final String customHostName = System.getenv("DRILL_HOST_NAME");
  private static final String processUserName = System.getProperty("user.name");

  private final DrillConfig config;
  private final CaseInsensitiveMap<OptionDefinition> definitions;
  private final AuthenticatorProvider authProvider;
  private final EventLoopGroup loop;
  private final EventLoopGroup loop2;
  private final MetricRegistry metrics;
  private final BufferAllocator allocator;
  private final ScanResult classpathScan;
  private final ExecutorService executor;
  private final ExecutorService scanExecutor;
  private final ExecutorService scanDecodeExecutor;
  private final String hostName;

  public BootStrapContext(DrillConfig config, CaseInsensitiveMap<OptionDefinition> definitions,
                          ScanResult classpathScan) throws DrillbitStartupException {
    this.config = config;
    this.definitions = definitions;
    this.classpathScan = classpathScan;
    this.hostName = getCanonicalHostName();
    login(config);
    this.authProvider = new AuthenticatorProviderImpl(config, classpathScan);
    this.loop = TransportCheck.createEventLoopGroup(config.getInt(ExecConstants.BIT_SERVER_RPC_THREADS), "BitServer-");
    this.loop2 = TransportCheck.createEventLoopGroup(config.getInt(ExecConstants.BIT_SERVER_RPC_THREADS), "BitClient-");
    // Note that metrics are stored in a static instance
    this.metrics = DrillMetrics.getRegistry();
    this.allocator = RootAllocatorFactory.newRoot(config);
    this.executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(),
        new NamedThreadFactory("drill-executor-")) {
      @Override
      protected void afterExecute(final Runnable r, final Throwable t) {
        if (t != null) {
          logger.error("{}.run() leaked an exception.", r.getClass().getName(), t);
        }
        super.afterExecute(r, t);
      }
    };
    // Setup two threadpools one for reading raw data from disk and another for decoding the data
    // A good guideline is to have the number threads in the scan pool to be a multiple (fractional
    // numbers are ok) of the number of disks.
    // A good guideline is to have the number threads in the decode pool to be a small multiple (fractional
    // numbers are ok) of the number of cores.
    final int numCores = Runtime.getRuntime().availableProcessors();
    final int numScanThreads = (int) (config.getDouble(ExecConstants.SCAN_THREADPOOL_SIZE));
    final int numScanDecodeThreads = (int) config.getDouble(ExecConstants.SCAN_DECODE_THREADPOOL_SIZE);
    final int scanThreadPoolSize =
        MIN_SCAN_THREADPOOL_SIZE > numScanThreads ? MIN_SCAN_THREADPOOL_SIZE : numScanThreads;
    final int scanDecodeThreadPoolSize =
        (numCores + 1) / 2 > numScanDecodeThreads ? (numCores + 1) / 2 : numScanDecodeThreads;
    this.scanExecutor = Executors.newFixedThreadPool(scanThreadPoolSize, new NamedThreadFactory("scan-"));
    this.scanDecodeExecutor =
        Executors.newFixedThreadPool(scanDecodeThreadPoolSize, new NamedThreadFactory("scan-decode-"));
  }

  private void login(final DrillConfig config) throws DrillbitStartupException {
    try {
      if (config.hasPath(ExecConstants.SERVICE_PRINCIPAL)) {
        // providing a service principal => Kerberos mechanism
        final Configuration loginConf = new Configuration();
        loginConf.set(CommonConfigurationKeys.HADOOP_SECURITY_AUTHENTICATION,
            UserGroupInformation.AuthenticationMethod.KERBEROS.toString());

        // set optional user name mapping
        if (config.hasPath(ExecConstants.KERBEROS_NAME_MAPPING)) {
          loginConf.set(CommonConfigurationKeys.HADOOP_SECURITY_AUTH_TO_LOCAL,
              config.getString(ExecConstants.KERBEROS_NAME_MAPPING));
        }

        UserGroupInformation.setConfiguration(loginConf);

        // service principal canonicalization
        final String principal = config.getString(ExecConstants.SERVICE_PRINCIPAL);
        final String parts[] = KerberosUtil.splitPrincipalIntoParts(principal);
        if (parts.length != 3) {
          throw new DrillbitStartupException(
            String.format("Invalid %s, Drill service principal must be of format 'primary/instance@REALM' or 'primary@REALM'",
              ExecConstants.SERVICE_PRINCIPAL));
        }

        parts[1] = ("".equals(parts[1])) ? "" : KerberosUtil.canonicalizeInstanceName(parts[1], hostName);

        final String canonicalizedPrincipal = KerberosUtil.getPrincipalFromParts(parts[0], parts[1], parts[2]);
        final String keytab = config.getString(ExecConstants.SERVICE_KEYTAB_LOCATION);

        // login to KDC (AS)
        // Note that this call must happen before any call to UserGroupInformation#getLoginUser,
        // but there is no way to enforce the order (this static init. call and parameters from
        // DrillConfig are both required).
        UserGroupInformation.loginUserFromKeytab(canonicalizedPrincipal, keytab);

        logger.info("Process user name: '{}' and logged in successfully as '{}'", processUserName,
            canonicalizedPrincipal);
      } else {
        UserGroupInformation.getLoginUser(); // init
      }

      // ugi does not support logout
    } catch (final IOException e) {
      throw new DrillbitStartupException("Failed to login.", e);
    }

  }

  private static String getCanonicalHostName() throws DrillbitStartupException {
    try {
      return customHostName != null ? customHostName : InetAddress.getLocalHost().getCanonicalHostName();
    } catch (final UnknownHostException e) {
      throw new DrillbitStartupException("Could not get canonical hostname.", e);
    }
  }

  public String getHostName() {
    return hostName;
  }

  public ExecutorService getExecutor() {
    return executor;
  }

  public ExecutorService getScanExecutor() {
    return scanExecutor;
  }

  public ExecutorService getScanDecodeExecutor() {
    return scanDecodeExecutor;
  }

  public DrillConfig getConfig() {
    return config;
  }

  public CaseInsensitiveMap<OptionDefinition> getDefinitions() {
    return definitions;
  }

  public EventLoopGroup getBitLoopGroup() {
    return loop;
  }

  public EventLoopGroup getBitClientLoopGroup() {
    return loop2;
  }

  public MetricRegistry getMetrics() {
    return metrics;
  }

  public BufferAllocator getAllocator() {
    return allocator;
  }

  public ScanResult getClasspathScan() {
    return classpathScan;
  }

  public AuthenticatorProvider getAuthProvider() {
    return authProvider;
  }

  @Override
  public void close() {
    try {
      DrillMetrics.resetMetrics();
    } catch (Error | Exception e) {
      logger.warn("failure resetting metrics.", e);
    }

    if (executor != null) {
      executor.shutdown(); // Disable new tasks from being submitted
      try {
        // Wait a while for existing tasks to terminate
        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
          executor.shutdownNow(); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
            logger.error("Pool did not terminate");
          }
        }
      } catch (InterruptedException ie) {
        logger.warn("Executor interrupted while awaiting termination");

        // (Re-)Cancel if current thread also interrupted
        executor.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }

    if (scanExecutor != null) {
      scanExecutor.shutdown();
    }

    if (scanDecodeExecutor != null) {
      scanDecodeExecutor.shutdownNow();
    }

    try {
      AutoCloseables.close(allocator, authProvider);
      shutdown(loop);
      shutdown(loop2);

    } catch (final Exception e) {
      logger.error("Error while closing", e);
    }
  }

  private static void shutdown(EventLoopGroup loopGroup) {
    if (loopGroup != null && !(loopGroup.isShutdown() || loopGroup.isShuttingDown())) {
      try {
        loopGroup.shutdownGracefully();

      } catch (final Exception e) {
        logger.error("Error while closing", e);
      }
    }
  }
}
