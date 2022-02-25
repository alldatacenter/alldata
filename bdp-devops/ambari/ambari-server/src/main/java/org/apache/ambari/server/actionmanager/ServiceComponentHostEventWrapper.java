/*
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
package org.apache.ambari.server.actionmanager;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.utils.StageUtils;

public class ServiceComponentHostEventWrapper {

  private ServiceComponentHostEvent event = null;
  private String eventJson = null;

  public ServiceComponentHostEventWrapper(ServiceComponentHostEvent event) {
    this.event  = event;
  }
  
  public ServiceComponentHostEventWrapper(String eventJson) {
    this.eventJson = eventJson;
  }

  public ServiceComponentHostEvent getEvent() {
    if (event != null) {
      return event;
    } else if (eventJson != null) {
      try {
        event = StageUtils.fromJson(eventJson, ServiceComponentHostEvent.class);
        return event;
      } catch (IOException e) {
        throw new RuntimeException("Illegal Json for event", e);
      }
    }
    return null;
  }
  
  public String getEventJson() { 
    if (eventJson != null) {
      return eventJson;
    } else if (event != null) {
      try {
        eventJson = StageUtils.jaxbToString(event);
        return eventJson;
      } catch (JAXBException | IOException e) {
        throw new RuntimeException("Couldn't get json", e);
      }
    } else {
      return null;
    }
  }
  
  @Override
  public String toString() {
    if (event != null) {
      return event.toString();
    } else if (eventJson != null) {
      return eventJson;
    }
    return "null";
  }
}
