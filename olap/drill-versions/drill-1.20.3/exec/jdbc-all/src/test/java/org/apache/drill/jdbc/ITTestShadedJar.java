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
package org.apache.drill.jdbc;

import org.apache.drill.test.BaseTest;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ITTestShadedJar extends BaseTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ITTestShadedJar.class);

  private static DrillbitClassLoader drillbitLoader;
  private static ClassLoader rootClassLoader;
  private static int userPort;

  @ClassRule
  public static final TestWatcher testWatcher = new TestWatcher() {

    @Override
    protected void starting(Description description) {
      super.starting(description);

      try {
        drillbitLoader = new DrillbitClassLoader();
        drillbitLoader.loadClass("org.apache.commons.io.FileUtils");
        rootClassLoader = Thread.currentThread().getContextClassLoader();

        Class<?> clazz = drillbitLoader.loadClass("org.apache.drill.test.BaseTestQuery");
        Class<?> watcherClazz = drillbitLoader.loadClass("org.apache.drill.test.BaseDirTestWatcher");

        // Setup tmp dirs
        runMethod("starting", description);

        // Configure tmp dir
        Object watcher = clazz.getField("dirTestWatcher").get(null);
        Method method = watcherClazz.getDeclaredMethod("getTmpDir");
        File tmpDir = (File) method.invoke(watcher);
        System.setProperty("DRILL_CONF_DIR", tmpDir.getAbsolutePath());

        // Start the drill cluster
        try {
          runWithLoader("DrillbitStartThread", drillbitLoader);
        } catch (Exception e) {
          printClassesLoaded("root", rootClassLoader);
          throw e;
        }

        DrillbitStartThread.SEM.acquire();

        // After starting the drill cluster get the client port
        userPort = (Integer) clazz.getMethod("getUserPort").invoke(null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void finished(Description description) {
      super.finished(description);
      done();
      runMethod("finished", description);
    }

    @Override
    protected void failed(Throwable e, Description description) {
      super.failed(e, description);
      done();
      runMethod("failed", description);
    }

    private void done() {
      try {
        runWithLoader("DrillbitStopThread", drillbitLoader);
        DrillbitStopThread.SEM.acquire();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private void runMethod(String name, Description description) {
      try {
        Class<?> clazz = drillbitLoader.loadClass("org.apache.drill.test.BaseTestQuery");
        Class<?> watcherClazz = drillbitLoader.loadClass("org.junit.rules.TestWatcher");
        Object watcher = clazz.getField("dirTestWatcher").get(null);
        Method method = watcherClazz.getDeclaredMethod(name, Description.class);
        method.setAccessible(true);
        method.invoke(watcher, description);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  };

  private static URL getJdbcUrl() throws MalformedURLException {
    return new URL(
        String.format("%s../../target/drill-jdbc-all-%s.jar",
            ClassLoader.getSystemClassLoader().getResource("").toString(),
            System.getProperty("project.version")
            ));

  }

  @Test
  public void testDatabaseVersion() throws Exception {
    final URLClassLoader loader = URLClassLoader.newInstance(new URL[] {getJdbcUrl()});

    Class<?> clazz = loader.loadClass("org.apache.drill.jdbc.Driver");
    try {
      Driver driver = (Driver) clazz.newInstance();
      try (Connection c = driver.connect("jdbc:drill:drillbit=localhost:" + userPort, null)) {
        DatabaseMetaData metadata = c.getMetaData();
        assertEquals("Apache Drill JDBC Driver", metadata.getDriverName());
        assertEquals("Apache Drill Server", metadata.getDatabaseProductName());
      }
    } catch (Exception ex) {
      throw ex;
    }

  }

  @Test
  public void executeJdbcAllQuery() throws Exception {
    final URLClassLoader loader = URLClassLoader.newInstance(new URL[] {getJdbcUrl()});

    Class<?> clazz = loader.loadClass("org.apache.drill.jdbc.Driver");
    try {
      Driver driver = (Driver) clazz.newInstance();
      try (Connection c = driver.connect("jdbc:drill:drillbit=localhost:" + userPort, null)) {
        printQuery(c, "select * from cp.`types.json`");
      }
    } catch (Exception ex) {
      throw ex;
    }

  }

  @Test
  public void serviceFileContainsCorrectDriver() throws IOException {
    URLClassLoader loader = URLClassLoader.newInstance(new URL[]{getJdbcUrl()});
    try (InputStream resourceStream = loader.getResourceAsStream("META-INF/services/java.sql.Driver")) {
      assertNotNull("java.sql.Driver is not present in the jdbc jar", resourceStream);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream))) {
        String driverClass = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        assertEquals("org.apache.drill.jdbc.Driver", driverClass);
      }
    }
  }

  private static void printQuery(Connection c, String query) throws SQLException {
    final StringBuilder sb = new StringBuilder();

    try (Statement s = c.createStatement();
         ResultSet result = s.executeQuery(query)) {
      while (result.next()) {
        final int columnCount = result.getMetaData().getColumnCount();
        for(int i = 1; i < columnCount+1; i++){
          sb.append(result.getObject(i));
          sb.append('\t');
        }

        sb.append(result.getObject(1)).append('\n');
      }
    }

    logger.info(sb.toString());
  }

  private static int getClassesLoadedCount(ClassLoader classLoader) {
    try {
      Field f = ClassLoader.class.getDeclaredField("classes");
      f.setAccessible(true);
      Vector<Class<?>> classes = (Vector<Class<?>>) f.get(classLoader);
      return classes.size();
    } catch (Exception e) {
      logger.error("Exception ", e);
      return -1;
    }
  }

  private static void printClassesLoaded(String prefix, ClassLoader classLoader) {
    final StringBuilder sb = new StringBuilder();

    try {
      Field f = ClassLoader.class.getDeclaredField("classes");
      f.setAccessible(true);
      Vector<Class<?>> classes = (Vector<Class<?>>) f.get(classLoader);
      for (Class<?> c : classes) {
        sb.append(prefix).append(": ").append(c.getName()).append('\n');
      }
    } catch (Exception e) {
      logger.error("Exception ", e);
    }

    logger.info(sb.toString());
  }

  private static void runWithLoader(String name, ClassLoader loader) throws Exception {
    Class<?> clazz = loader.loadClass(ITTestShadedJar.class.getName() + "$" + name);
    Object instance = clazz.getDeclaredConstructors()[0].newInstance(loader);
    clazz.getMethod("go").invoke(instance);
  }

  public abstract static class AbstractLoaderThread extends Thread {
    private Exception ex;
    protected final ClassLoader loader;

    public AbstractLoaderThread(ClassLoader loader) {
      this.setContextClassLoader(loader);
      this.loader = loader;
    }

    @Override
    public final void run() {
      try {
        internalRun();
      } catch (Exception e) {
        this.ex = e;
      }
    }

    protected abstract void internalRun() throws Exception;

    public void go() throws Exception {
      start();
      join();
      if (ex != null) {
        throw ex;
      }
    }
  }

  public static class DrillbitStartThread extends AbstractLoaderThread {
    public static final Semaphore SEM = new Semaphore(0);

    public DrillbitStartThread(ClassLoader loader) {
      super(loader);
    }

    @Override
    protected void internalRun() throws Exception {
      Class<?> clazz = loader.loadClass("org.apache.drill.test.BaseTestQuery");
      clazz.getMethod("setupDefaultTestCluster").invoke(null);

      // loader.loadClass("org.apache.drill.exec.exception.SchemaChangeException");

      // execute a single query to make sure the drillbit is fully up
      clazz.getMethod("testNoResult", String.class, Object[].class)
          .invoke(null, "select * from (VALUES 1)", new Object[] {});

      SEM.release();
    }

  }

  public static class DrillbitStopThread extends AbstractLoaderThread {
    public static final Semaphore SEM = new Semaphore(0);

    public DrillbitStopThread(ClassLoader loader) {
      super(loader);
    }

    @Override
    protected void internalRun() throws Exception {
      Class<?> clazz = loader.loadClass("org.apache.drill.test.BaseTestQuery");
      clazz.getMethod("closeClient").invoke(null);
      SEM.release();
    }
  }

}
