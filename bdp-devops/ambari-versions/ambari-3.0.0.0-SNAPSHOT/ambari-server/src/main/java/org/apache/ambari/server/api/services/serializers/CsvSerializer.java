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

package org.apache.ambari.server.api.services.serializers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.utils.Closeables;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * CSV serializer used to generate a CSV-formatted document from a result.
 */
public class CsvSerializer implements ResultSerializer {
  /**
   * Property name for the CsvSerializer-specific column map where the value of this property
   * contains a map of resource property names to header descriptive names.
   * <p/>
   * If not specified, no header record will be serialized.
   */
  public static final String PROPERTY_COLUMN_MAP = "csv_column_map";

  /**
   * Property name for the CsvSerializer-specific column order where the value of this property
   * contains a list of resource property names in the order to export.
   * <p/>
   * If not specified, the order will be taken from key order in the csv_column_map property (if
   * available) or from the "natural" order of the properties in the resource.
   */
  public static final String PROPERTY_COLUMN_ORDER = "csv_column_order";

  /**
   * Serialize the result into a CSV-formatted text document.
   * <p/>
   * It is expected that the result set is a collection of flat resources - no sub-resources will be
   * included in the output.  The root of the tree structure may have a column map (csv_column_map)
   * and a column order (csv_column_order) property set to indicate the header record and ordering
   * of the columns.
   * <p/>
   * The csv_column_map is a map of resource property names to header descriptive names.  If not
   * specified, a header record will not be serialized.
   * <p/>
   * The csv_column_order is a list of resource property names declaring the order of the columns.
   * If not specified, the order will be taken from the key order of csv_column_map or the "natural"
   * ordering of the resource property names, both may be unpredictable.
   *
   * @param result internal result
   * @return a String containing the CSV-formatted document
   */
  @Override
  public Object serialize(Result result) {
    if (result.getStatus().isErrorState()) {
      return serializeError(result.getStatus());
    } else {
      CSVPrinter csvPrinter = null;
      try {
        // A StringBuffer to store the CSV-formatted document while building it.  It may be
        // necessary to use file-based storage if the data set is expected to be really large.
        StringBuffer buffer = new StringBuffer();

        TreeNode<Resource> root = result.getResultTree();

        if (root != null) {
          csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT);

          // TODO: recursively handle tree structure, for now only handle single level of detail
          if ("true".equalsIgnoreCase(root.getStringProperty("isCollection"))) {
            List<String> fieldNameOrder = processHeader(csvPrinter, root);

            Collection<TreeNode<Resource>> children = root.getChildren();
            if (children != null) {
              // Iterate over the child nodes of the collection an add each as a new record in the
              // CSV document.
              for (TreeNode<Resource> child : children) {
                processRecord(csvPrinter, child, fieldNameOrder);
              }
            }
          }
        }

        return buffer.toString();
      } catch (IOException e) {
        //todo: exception handling.  Create ResultStatus 500 and call serializeError
        throw new RuntimeException("Unable to serialize to csv: " + e, e);
      } finally {
        Closeables.closeSilently(csvPrinter);
      }
    }
  }

  @Override
  public Object serializeError(ResultStatus error) {
    CSVPrinter csvPrinter = null;
    try {
      StringBuffer buffer = new StringBuffer();
      csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT);

      csvPrinter.printRecord(Arrays.asList("status", "message"));
      csvPrinter.printRecord(Arrays.asList(error.getStatus().getStatus(), error.getMessage()));

      return buffer.toString();
    } catch (IOException e) {
      //todo: exception handling.  Create ResultStatus 500 and call serializeError
      throw new RuntimeException("Unable to serialize to csv: " + e, e);
    } finally {
      Closeables.closeSilently(csvPrinter);
    }
  }

  /**
   * Generate a CSV record by processing the resource embedded in the specified node.  The order of
   * the fields are to be set as specified.
   *
   * @param csvPrinter     the CSVPrinter used to create the record
   * @param node           the relevant node in the collection
   * @param fieldNameOrder a list of field names indicating order
   * @throws IOException if an error occurs creating the CSV record
   */
  private void processRecord(CSVPrinter csvPrinter, TreeNode<Resource> node, List<String> fieldNameOrder)
      throws IOException {

    if (node != null) {
      Resource recordResource = node.getObject();
      if (recordResource != null) {
        List<Object> values = new ArrayList<>();

        if (fieldNameOrder != null) {
          for (String fieldName : fieldNameOrder) {
            values.add(recordResource.getPropertyValue(fieldName));
          }
        } else {
          Map<String, Map<String, Object>> properties = recordResource.getPropertiesMap();
          if (properties != null) {

            for (Map.Entry<String, Map<String, Object>> outer : properties.entrySet()) {
              Map<String, Object> innerProperties = outer.getValue();

              if (innerProperties != null) {
                for (Map.Entry<String, Object> inner : innerProperties.entrySet()) {
                  values.add(inner.getValue());
                }
              }
            }
          }
        }

        if (!values.isEmpty()) {
          csvPrinter.printRecord(values);
        }
      }
    }
  }

  /**
   * Optionally generate the CSV header record and establish the field order by processing the
   * csv_column_map and csv_column_order node properties.
   *
   * @param csvPrinter the CSVPrinter used to create the record
   * @param node       a node containing header and ordering information
   * @return a list indicating the field order for the CSV records
   * @throws IOException if an error occurs creating the CSV header
   */
  private List<String> processHeader(CSVPrinter csvPrinter, TreeNode<Resource> node) throws IOException {
    Map<String, String> header;
    List<String> fieldNameOrder;
    Object object;

    // Get the explicitly set header property for the current tree node. This may be null if no
    // header needs to be written out. The header map is expected to be a map of field names to
    // descriptive header values.
    object = node.getProperty(PROPERTY_COLUMN_MAP);
    if (object instanceof Map) {
      header = (Map<String, String>) object;
    } else {
      header = null;
    }

    // Determine the field name order.  If explicitly set, use it, else grab it from the header map
    // (if available).
    object = node.getProperty(PROPERTY_COLUMN_ORDER);
    if (object instanceof List) {
      // Use the explicitly set ordering
      fieldNameOrder = (List<String>) object;
    } else if (header != null) {
      // Use the ordering specified by the map.
      fieldNameOrder = new ArrayList<>(header.keySet());
    } else {
      fieldNameOrder = null;
    }

    if (header != null) {
      // build the header record
      List<String> headerNames = new ArrayList<>();
      for (String fieldName : fieldNameOrder) {
        headerNames.add(header.get(fieldName));
      }

      // write out the header...
      csvPrinter.printRecord(headerNames);
    }

    return fieldNameOrder;
  }
}
