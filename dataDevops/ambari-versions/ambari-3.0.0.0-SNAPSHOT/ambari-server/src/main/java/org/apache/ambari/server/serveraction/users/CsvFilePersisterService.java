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

package org.apache.ambari.server.serveraction.users;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class CsvFilePersisterService implements CollectionPersisterService<String, List<String>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvFilePersisterService.class);
  private String NEW_LINE_SEPARATOR = "\n";

  private String csvFile;
  private CSVPrinter csvPrinter;
  private FileWriter fileWriter;

  @AssistedInject
  public CsvFilePersisterService(@Assisted String csvFile) {
    this.csvFile = csvFile;
  }

  public Set<PosixFilePermission> getCsvPermissions() {
    Set<PosixFilePermission> permissionsSet = new HashSet<>();
    permissionsSet.add(PosixFilePermission.OWNER_READ);
    permissionsSet.add(PosixFilePermission.OWNER_WRITE);
    permissionsSet.add(PosixFilePermission.GROUP_READ);
    permissionsSet.add(PosixFilePermission.OTHERS_READ);
    return permissionsSet;
  }

  @Inject
  public void init() throws IOException {

    Path csv = Files.createFile(Paths.get(csvFile));
    Files.setPosixFilePermissions(Paths.get(csvFile), getCsvPermissions());
    fileWriter = new FileWriter(csv.toFile());

    csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR));
  }


  @Override
  public boolean persist(Collection<List<String>> collectionData) {

    try {
      LOGGER.info("Persisting collection to csv file");

      csvPrinter.printRecords(collectionData);

      LOGGER.info("Collection successfully persisted to csv file.");

      return true;
    } catch (IOException e) {
      LOGGER.error("Failed to persist the collection to csv file", e);
      return false;
    } finally {
      try {
        fileWriter.flush();
        fileWriter.close();
        csvPrinter.close();
      } catch (IOException e) {
        LOGGER.error("Error while flushing/closing fileWriter/csvPrinter", e);
      }
    }
  }

  @Override
  public boolean persistMap(Map<String, List<String>> mapData) {

    LOGGER.info("Persisting map data to csv file");
    Collection<List<String>> collectionData = new ArrayList<>();

    for (String key : mapData.keySet()) {
      List<String> record = new ArrayList<>();
      record.add(key);
      record.addAll(mapData.get(key));
      collectionData.add(record);
    }

    return persist(collectionData);
  }

}
