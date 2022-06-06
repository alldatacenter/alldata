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

package org.apache.ambari.server.serveraction.kerberos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * AbstractKerberosDataFileWriter provides a generic facility to write a data file using some
 * underlying record-based file writer.
 * <p/>
 * This class encapsulates a {@link org.apache.commons.csv.CSVPrinter} to create a CSV-formatted file.
 */
public abstract class AbstractKerberosDataFileWriter {

  private File file;
  private CSVPrinter csvPrinter;

  /**
   * Creates a new KerberosConfigDataFileWriter
   * <p/>
   * The file is opened upon creation, so there is no need to manually open it unless manually
   * closed before using.
   *
   * @param file a File declaring where to write the data
   * @throws java.io.IOException
   */
  public AbstractKerberosDataFileWriter(File file) throws IOException {
    this.file = file;
    open();
  }


  /**
   * Opens the data file for writing.
   * <p/>
   * This may be called multiple times and the appropriate action will occur depending on if the
   * file has been previously opened or closed.
   *
   * @throws java.io.IOException
   */
  public void open() throws IOException {
    if (isClosed()) {
      if (file == null) {
        throw new IOException("Missing file path");
      } else {
        csvPrinter = new CSVPrinter(new FileWriter(file, true), CSVFormat.DEFAULT);

        // If the file is empty, write the header; else don't write the header.
        if (file.length() == 0) {
          // Write the header....
          Iterable<?> headerRecord = getHeaderRecord();
          csvPrinter.printRecord(headerRecord);
        }
      }
    }
  }

  /**
   * Tests this KerberosConfigDataFileWriter to see if the data file is closed.
   *
   * @return true if closed; otherwise false
   */
  public boolean isClosed() {
    return csvPrinter == null;
  }

  /**
   * Closes the data file
   *
   * @throws java.io.IOException
   */
  public void close() throws IOException {
    if (csvPrinter != null) {
      csvPrinter.close();
      csvPrinter = null;
    }
  }

  /**
   * Appends a new record to the data file
   *
   * @param record a collection of Strings declaring values for the columns of the file
   * @throws java.io.IOException
   */
  protected void appendRecord(String... record) throws IOException {

    if (csvPrinter == null) {
      throw new IOException("Data file is not open");
    }

    csvPrinter.printRecord(record);
  }


  /**
   * Gets the header record for the CSV file
   *
   * @return an Iterable containing the (ordered) list of Strings declaring the columns names
   */
  protected abstract Iterable<String> getHeaderRecord();


}
