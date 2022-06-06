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
package org.apache.ambari.server.utils;


import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;

/**
 * Static Helper methods for XML processing.
 */
public class XmlUtils {

  public static boolean isValidXml(String input) {
    boolean result = true;
    try {
      if (StringUtils.isBlank(input)) {
        result = false;
      } else {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // skip dtd references
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        dBuilder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
      }
    } catch (Exception e) {
      result = false;
    }
    return result;
  }

}
