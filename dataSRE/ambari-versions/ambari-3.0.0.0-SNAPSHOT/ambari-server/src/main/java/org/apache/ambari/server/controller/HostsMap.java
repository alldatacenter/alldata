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

package org.apache.ambari.server.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Stores the mapping of hostnames to be used in any configuration on 
 * the server.
 *  
 */
@Singleton
public class HostsMap {
  private final static Logger LOG = LoggerFactory
      .getLogger(HostsMap.class);

  private String hostsMapFile;
  private Properties hostsMap;

  @Inject
  public HostsMap(Configuration conf) {
    hostsMapFile = conf.getHostsMapFile();
    setupMap();
  }
  
  public HostsMap(String file) {
    hostsMapFile = file;
  }

  public void setupMap() {
    InputStream inputStream = null;
    LOG.info("Using hostsmap file " + this.hostsMapFile);
    try {
      if (hostsMapFile != null) {
        hostsMap = new Properties();
        inputStream = new FileInputStream(new File(hostsMapFile));
        // load the properties
        hostsMap.load(inputStream);
      }
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + hostsMapFile + " found in classpath.", fnf);
    } catch (IOException ie) {
      throw new IllegalArgumentException("Can't read configuration file " +
          hostsMapFile, ie);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch(IOException io) {
          //ignore 
        }
      }
    }
  }

/**
 * Return map of the hostname if available
 * @param hostName hostname map
 * @return 
 */
public String getHostMap(String hostName) {
  if (hostsMapFile == null) 
    return hostName;
  return hostsMap.getProperty(hostName, hostName);
}

}
