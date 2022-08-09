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
package org.apache.ambari.log4j.common;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.spi.LoggingEvent;

public class LoggingThreadRunnable implements Runnable {
  private static final Log LOG = LogFactory.getLog(LoggingThreadRunnable.class);
  private static long WAIT_EMPTY_QUEUE = 60000;
  private final Queue<LoggingEvent> events;
  private final LogParser parser;
  private final LogStore store;
  private final AtomicBoolean done = new AtomicBoolean(false);
  
  public LoggingThreadRunnable(
      Queue<LoggingEvent> events, 
      LogParser parser, 
      LogStore provider) {
    this.events = events;
    this.store = provider;
    this.parser = parser;
  }
  
  @Override
  public void run() {
    while (!done.get()) {
      LoggingEvent event = null;
      while ((event = events.poll()) != null) {
        Object result = null;
        try {
          parser.addEventToParse(event);
          while ((result = parser.getParseResult()) != null) {
            try {
              store.persist(event, result);
            } catch (IOException e) {
              LOG.warn("Failed to persist " + result);
            }
          }
        } catch (IOException ioe) {
          LOG.warn("Failed to parse log-event: " + event);
        }
      }
      try {
        Thread.sleep(WAIT_EMPTY_QUEUE);
      } catch(InterruptedException ie) {
        //ignore and continue
      }
    	  
    }
    try {
      store.close();
    } catch (IOException ioe) {
      LOG.info("Failed to close logStore", ioe);
    }
  }
  
  public void close() throws IOException {
    done.set(true);
  }
}
