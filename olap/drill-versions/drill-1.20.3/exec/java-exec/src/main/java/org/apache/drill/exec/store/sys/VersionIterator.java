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
package org.apache.drill.exec.store.sys;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.common.util.DrillVersionInfo;

public class VersionIterator implements Iterator<Object>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VersionIterator.class);

  public boolean beforeFirst = true;

  public static class VersionInfo {
    public String version = "Unknown";
    public String commit_id = "Unknown";
    public String commit_message = "";
    public String commit_time = "";
    public String build_email = "Unknown";
    public String build_time = "";

    public VersionInfo(){
      try {
        version = DrillVersionInfo.getVersion(); // get drill version (x.y.z)
        URL u = Resources.getResource("git.properties");
        if(u != null){
          Properties p = new Properties();
          p.load(Resources.asByteSource(u).openStream());
          commit_id = p.getProperty("git.commit.id");
          build_email = p.getProperty("git.build.user.email");
          commit_time = p.getProperty("git.commit.time");
          build_time = p.getProperty("git.build.time");
          commit_message = p.getProperty("git.commit.message.short");

        }
      } catch (IOException | IllegalArgumentException e) {
        logger.warn("Failure while trying to load \"git.properties\" file.", e);
      }
    }
  }

  @Override
  public boolean hasNext() {
    return beforeFirst;
  }

  @Override
  public Object next() {
    if(!beforeFirst){
      throw new IllegalStateException();
    }
    beforeFirst = false;
    return new VersionInfo();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
