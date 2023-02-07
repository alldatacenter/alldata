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
package org.apache.drill.exec.server.rest.profile;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TableBuilder {
  private static final String NO_BGCOLOR = "";
  private final NumberFormat format = NumberFormat.getInstance(Locale.US);
  private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
  private final DecimalFormat dec = new DecimalFormat("0.00");
  private final DecimalFormat intformat = new DecimalFormat("#,###");

  private StringBuilder sb;
  private int w = 0;
  private int width;

  public TableBuilder(final String[] columns, final String[] columnTooltip) {
    this(columns, columnTooltip, false);
  }

  public TableBuilder(final String[] columns, final String[] columnTooltip, final boolean isSortable) {
    sb = new StringBuilder();
    width = columns.length;

    format.setMaximumFractionDigits(3);

    sb.append("<table class=\"table table-bordered text-right"+(isSortable? " sortable" : NO_BGCOLOR)+"\">\n<thead><tr>");
    for (int i = 0; i < columns.length; i++) {
      String cn = columns[i];
      String ctt = NO_BGCOLOR;
      if (columnTooltip != null) {
        String tooltip = columnTooltip[i];
        if (tooltip != null) {
          ctt = " title=\""+tooltip+"\"";
        }
      }
      sb.append("<th" + ctt + ">" + cn + "</th>");
    }
    sb.append("</tr></thead>\n<tbody>\n");
  }

  public void appendCell(final String s) {
    appendCell(s, NO_BGCOLOR, null);
  }

  public void appendCell(final String s, final String backgroundColor) {
    appendCell(s, backgroundColor, null);
  }

  public void appendCell(final String s, final Map<String, String> kvPairs) {
    appendCell(s, NO_BGCOLOR, kvPairs);
  }

  //Inject value into a table cell. Start or end a row if required
  public void appendCell(final String s, final String rowBackgroundColor, final Map<String, String> kvPairs) {
    //Check if this is first column?
    if (w == 0) {
      sb.append("<tr"
          + (rowBackgroundColor == null || rowBackgroundColor == NO_BGCOLOR ? "" : " style=\"background-color:"+rowBackgroundColor+"\"")
          + ">");
    }
    StringBuilder tdElemSB = new StringBuilder("<td");
    //Extract other attributes for injection into element
    if (kvPairs != null) {
      for (String attributeName : kvPairs.keySet()) {
        String attributeText = " " + attributeName + "=\"" + kvPairs.get(attributeName) + "\"";
        tdElemSB.append(attributeText);
      }
    }
    //Inserting inner text value and closing <td>
    tdElemSB.append(">").append(s).append("</td>");
    sb.append(tdElemSB);
    if (++w >= width) {
      sb.append("</tr>\n");
      w = 0;
    }
  }

  public void appendRepeated(final String s, final int n) {
    appendRepeated(s, n, null);
  }

  //Inject a value repeatedly into a table cell
  public void appendRepeated(final String s, final int n, final Map<String, String> attributeMap) {
    for (int i = 0; i < n; i++) {
      appendCell(s, attributeMap);
    }
  }

  public void appendTime(final long d) {
    appendTime(d, null);
  }

  //Inject timestamp/date value with ordering into a table cell
  public void appendTime(final long d, Map<String, String> attributeMap) {
    //Embedding dataTable's data-order attribute
    if (attributeMap == null) {
      attributeMap = new HashMap<>();
    }
    attributeMap.put(HtmlAttribute.DATA_ORDER, String.valueOf(d));
    appendCell(dateFormat.format(d), null, attributeMap);
  }

  public void appendMillis(final long p) {
    appendMillis(p, null);
  }

  //Inject millisecond based time value with ordering into a table cell
  public void appendMillis(final long p, Map<String, String> attributeMap) {
    //Embedding dataTable's data-order attribute
    if (attributeMap == null) {
      attributeMap = new HashMap<>();
    }
    attributeMap.put(HtmlAttribute.DATA_ORDER, String.valueOf(p));
    appendCell((new SimpleDurationFormat(0, p)).compact(), NO_BGCOLOR, attributeMap);
  }

  public void appendNanos(final long p) {
    appendNanos(p, null);
  }

  public void appendNanos(final long p, Map<String, String> attributeMap) {
    appendMillis(Math.round(p / 1000.0 / 1000.0), attributeMap);
  }

  public void appendPercent(final double percentAsFraction) {
    appendCell(dec.format(100*percentAsFraction).concat("%"), NO_BGCOLOR, null);
  }

  //Inject value as a percentage with value between 0 and 100 into a table cell
  public void appendPercent(final double percentAsFraction, Map<String, String> attributeMap) {
    appendCell(dec.format(100*percentAsFraction).concat("%"), NO_BGCOLOR, attributeMap);
  }

  public void appendFormattedNumber(final Number n) {
    appendCell(format.format(n), NO_BGCOLOR, null);
  }

  public void appendFormattedNumber(final Number n, Map<String, String> attributeMap) {
    appendCell(format.format(n), NO_BGCOLOR, attributeMap);
  }

  public void appendFormattedInteger(final long n) {
    appendCell(intformat.format(n), NO_BGCOLOR, null);
  }

  public void appendFormattedInteger(final long n, Map<String, String> attributeMap) {
    appendCell(intformat.format(n), NO_BGCOLOR, attributeMap);
  }

  public void appendInteger(final long l, Map<String, String> attributeMap) {
    appendCell(Long.toString(l), NO_BGCOLOR, attributeMap);
  }

  public void appendBytes(final long l) {
    appendBytes(l, null);
  }

  //Inject print-friendly byte value with ordering into a table cell
  public void appendBytes(final long l, Map<String, String> attributeMap) {
    //Embedding dataTable's data-order attribute
    if (attributeMap == null) {
      attributeMap = new HashMap<>();
    }
    attributeMap.put(HtmlAttribute.DATA_ORDER, String.valueOf(l));
    appendCell(bytePrint(l), NO_BGCOLOR, attributeMap);
  }

  //Generate a print-friendly representation of a byte count
  private String bytePrint(final long size) {
    final double t = size / Math.pow(1024, 4);
    if (t > 1) {
      return dec.format(t).concat("TB");
    }

    final double g = size / Math.pow(1024, 3);
    if (g > 1) {
      return dec.format(g).concat("GB");
    }

    final double m = size / Math.pow(1024, 2);
    if (m > 1) {
      return intformat.format(m).concat("MB");
    }

    final double k = size / 1024;
    if (k >= 1) {
      return intformat.format(k).concat("KB");
    }

    // size < 1 KB
    return "-";
  }

  public String build() {
    String rv;
    rv = sb.append("\n</tbody>\n</table>").toString();
    sb = null;
    return rv;
  }
}