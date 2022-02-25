/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Default audit logger implementation, logs messages into log file
 */
@Singleton
public class AuditLoggerDefaultImpl implements AuditLogger {

  private static final Logger LOG = LoggerFactory.getLogger("audit");

  /**
   * Indicates if audit log feature is enabled
   */
  private final boolean isEnabled;

  private ThreadLocal<DateFormat> dateFormatThreadLocal = new ThreadLocal<DateFormat>(){
    @Override
    protected DateFormat initialValue() {
      //2016-03-11T10:42:36.376Z
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
    }
  };

  @Inject
  public AuditLoggerDefaultImpl(Configuration configuration) {
    isEnabled = configuration.isAuditLogEnabled();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Subscribe
  public void log(AuditEvent event) {
    if(!isEnabled) {
      return;
    }

    Date date = new Date(event.getTimestamp());
    LOG.info("{}, {}", dateFormatThreadLocal.get().format(date), event.getAuditMessage());
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }
}
