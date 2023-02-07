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
package org.apache.drill.yarn.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Container;

public class DoYUtil {
  static final private Log LOG = LogFactory.getLog(DoYUtil.class);

  private DoYUtil() {
  }

  public static String join(String separator, List<String> list) {
    StringBuilder buf = new StringBuilder();
    String sep = "";
    for (String item : list) {
      buf.append(sep);
      buf.append(item);
      sep = separator;
    }
    return buf.toString();
  }

  public static void addNonEmpty(List<String> list, String value) {
    if ( ! isBlank( value ) ) {
      list.add(value.trim( ));
    }
  }

  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  public static String toIsoTime(long timestamp) {

    // Uses old-style dates rather than java.time because
    // the code still must compile for JDK 7.

    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    fmt.setTimeZone(TimeZone.getDefault());
    return fmt.format(new Date(timestamp));
  }

  public static String labelContainer(Container container) {
    StringBuilder buf = new StringBuilder()
        .append("[id: ")
        .append(container.getId())
        .append(", host: ")
        .append(container.getNodeId().getHost())
        .append(", priority: ")
        .append(container.getPriority())
        .append("]");
    return buf.toString();
  }

  /**
   * Utility method to display YARN container information in a useful way for
   * log messages.
   *
   * @param container
   * @return
   */

  public static String describeContainer(Container container) {
    StringBuilder buf = new StringBuilder()
        .append("[id: ")
        .append(container.getId())
        .append(", host: ")
        .append(container.getNodeId().getHost())
        .append(", priority: ")
        .append(container.getPriority())
        .append(", memory: ")
        .append(container.getResource().getMemory())
        .append(" MB, vcores: ")
        .append(container.getResource().getVirtualCores())
        .append("]");
    return buf.toString();
  }

  /**
   * The tracking URL given to YARN is a redirect URL. When giving the URL to
   * the user, "unwrap" that redirect URL to get the actual site URL.
   *
   * @param trackingUrl
   * @return
   */

  public static String unwrapAmUrl(String trackingUrl) {
    return trackingUrl.replace("/redirect", "/");
  }

  public static Object dynamicCall(Object target, String fnName, Object args[],
      Class<?> types[]) {

    // First, look for the method using the names and types provided.

    final String methodLabel = target.getClass().getName() + "." + fnName;
    Method m;
    try {
      m = target.getClass().getMethod(fnName, types);
    } catch (NoSuchMethodException e) {

      // Ignore, but log: the method does not exist in this distribution.

      StringBuilder buf = new StringBuilder();
      if (types != null) {
        String sep = "";
        for (Class<?> type : types) {
          buf.append(sep);
          buf.append(type.getName());
          sep = ",";
        }
      }
      LOG.trace("Not supported in this YARN distribution: " + methodLabel + "("
          + buf.toString() + ")");
      CodeSource src = target.getClass().getProtectionDomain().getCodeSource();
      if (src != null) {
        java.net.URL jar = src.getLocation();
        LOG.trace("Class found in URL: " + jar.toString());
      }
      return null;
    } catch (SecurityException e) {
      LOG.error("Security prevents dynamic method calls", e);
      return null;
    }

    // Next, call the method with the arguments provided.

    Object ret = null;
    try {
      ret = m.invoke(target, args);
    } catch (IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      LOG.error("Failed to dynamically call " + methodLabel, e);
      return null;
    }
    StringBuilder buf = new StringBuilder();
    if (args != null) {
      String sep = "";
      for (Object arg : args) {
        buf.append(sep);
        buf.append(arg == null ? "null" : arg.toString());
        sep = ",";
      }
    }
    LOG.trace(
        "Successfully called " + methodLabel + "( " + buf.toString() + ")");

    // Return any return value. Will be null if the method is returns void.

    return ret;
  }

  public static void callSetDiskIfExists(Object target, double arg) {
    dynamicCall(target, "setDisks", new Object[] { arg },
        new Class<?>[] { Double.TYPE });
  }

  public static double callGetDiskIfExists(Object target) {
    Object ret = dynamicCall(target, "getDisks", null, null);
    return (ret == null) ? 0.0 : (Double) ret;
  }
}
