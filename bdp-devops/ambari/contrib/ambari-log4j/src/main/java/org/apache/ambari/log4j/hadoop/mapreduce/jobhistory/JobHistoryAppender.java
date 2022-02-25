/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.log4j.hadoop.mapreduce.jobhistory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.ambari.log4j.common.LogParser;
import org.apache.ambari.log4j.common.LogStore;
import org.apache.ambari.log4j.common.LoggingThreadRunnable;
import org.apache.ambari.log4j.common.store.DatabaseStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.tools.rumen.HistoryEvent;
import org.apache.hadoop.util.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class JobHistoryAppender extends AppenderSkeleton implements Appender {

  private static final Log LOG = LogFactory.getLog(JobHistoryAppender.class);
  
  private final Queue<LoggingEvent> events;
  private LoggingThreadRunnable logThreadRunnable;
  private Thread logThread;

  private final LogParser logParser;

  private final LogStore nullStore =
      new LogStore() {
        @Override
        public void persist(LoggingEvent originalEvent, Object parsedEvent) 
            throws IOException {
          LOG.info(((HistoryEvent)parsedEvent).toString());
        }

        @Override
        public void close() throws IOException {}
  };

  private String driver;
  private String database;
  private String user;
  private String password;
  
  private LogStore logStore;
  
  public JobHistoryAppender() {
    events = new LinkedBlockingQueue<LoggingEvent>();
    logParser = new MapReduceJobHistoryParser();
    logStore = nullStore;
  }
  
  /* Getters & Setters for log4j */
  
  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }
  
  public String getDriver() {
    return driver;
  }

  public void setDriver(String driver) {
    this.driver = driver;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /* --------------------------- */

  @Override
  public void activateOptions() {
    synchronized (this) {
      //if (true) { 
      if (database.equals("none")) {
        logStore = nullStore;
        LOG.info("database set to 'none'");
      } else {
        try {
          logStore = 
              new DatabaseStore(driver, database, user, password, 
                  new MapReduceJobHistoryUpdater());
        } catch (IOException ioe) {
          LOG.debug("Failed to connect to db " + database, ioe);
          System.err.println("Failed to connect to db " + database + 
              " as user " + user + " password " + password + 
              " and driver " + driver + " with " + 
              StringUtils.stringifyException(ioe));
          throw new RuntimeException(
              "Failed to create database store for " + database, ioe);
        } catch (Exception e) {
          LOG.debug("Failed to connect to db " + database, e);
          System.err.println("Failed to connect to db " + database + 
              " as user " + user + " password " + password + 
              " and driver " + driver + " with " + 
              StringUtils.stringifyException(e));
          throw new RuntimeException(
              "Failed to create database store for " + database, e);
        }
      }
      logThreadRunnable = 
          new LoggingThreadRunnable(events, logParser, logStore);
      logThread = new Thread(logThreadRunnable);
      logThread.setDaemon(true);
      logThread.start();

      super.activateOptions();
    }
  }

  @Override
  public void close() {
    try {
      logThreadRunnable.close();
    } catch (IOException ioe) {
      LOG.info("Failed to close logThreadRunnable", ioe);
    }
    try {
      logThread.join(1000);
    } catch (InterruptedException ie) {
      LOG.info("logThread interrupted", ie);
    }
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  @Override
  protected void append(LoggingEvent event) {
    events.add(event);
  }
}
