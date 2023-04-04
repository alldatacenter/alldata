/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.ams.server.maintainer.RepairUtil;
import com.netease.arctic.catalog.CatalogManager;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.ChangeLocationKind;
import com.netease.arctic.table.LocationKind;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.TableMetadataParser;
import org.apache.iceberg.hadoop.HadoopTableOperations;
import org.apache.iceberg.hadoop.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RepairTableOperation {

  private static final Logger LOG = LoggerFactory.getLogger(HadoopTableOperations.class);

  private static final Pattern VERSION_PATTERN = Pattern.compile("v([^\\.]*)\\..*");

  private String location;

  private ArcticFileIO fileIO;

  RepairTableOperation(CatalogManager catalogManager, TableIdentifier identifier, LocationKind locationKind) {
    try {
      this.fileIO = RepairUtil.arcticFileIO(catalogManager.getThriftAddress(), identifier.getCatalog());
      if (locationKind == ChangeLocationKind.INSTANT) {
        this.location = RepairUtil.tableChangeLocation(catalogManager.getThriftAddress(), identifier);
      } else if (locationKind == BaseLocationKind.INSTANT) {
        this.location = RepairUtil.tableBaseLocation(catalogManager.getThriftAddress(), identifier);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  int findVersion() {
    Path versionHintFile = versionHintFile();

    try (InputStreamReader fsr = new InputStreamReader(
        fileIO.newInputFile(versionHintFile.toString()).newStream(),
        StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(fsr)) {
      return Integer.parseInt(in.readLine().replace("\n", ""));
    } catch (Exception e) {
      try {
        if (fileIO.exists(metadataRoot().toString())) {
          LOG.warn("Error reading version hint file {}", versionHintFile, e);
        } else {
          LOG.debug("Metadata for table not found in directory {}", metadataRoot(), e);
          return 0;
        }

        // List the metadata directory to find the version files, and try to recover the max available version
        FileStatus[] fileStatuses = fileIO.list(metadataRoot().toString())
            .stream()
            .filter(name -> VERSION_PATTERN.matcher(name.getPath().getName()).matches())
            .toArray(s -> new FileStatus[s]);
        int maxVersion = 0;

        for (FileStatus file : fileStatuses) {
          int currentVersion = version(file.getPath().getName());
          if (currentVersion > maxVersion && getMetadataFile(currentVersion) != null) {
            maxVersion = currentVersion;
          }
        }

        return maxVersion;
      } catch (IOException io) {
        LOG.warn("Error trying to recover version-hint.txt data for {}", versionHintFile, e);
        return 0;
      }
    }
  }

  private int version(String fileName) {
    Matcher matcher = VERSION_PATTERN.matcher(fileName);
    if (!matcher.matches()) {
      return -1;
    }
    String versionNumber = matcher.group(1);
    try {
      return Integer.parseInt(versionNumber);
    } catch (NumberFormatException ne) {
      return -1;
    }
  }

  Path versionHintFile() {
    return metadataPath(Util.VERSION_HINT_FILENAME);
  }

  private Path metadataFilePath(int metadataVersion, TableMetadataParser.Codec codec) {
    return metadataPath("v" + metadataVersion + TableMetadataParser.getFileExtension(codec));
  }

  private Path oldMetadataFilePath(int metadataVersion, TableMetadataParser.Codec codec) {
    return metadataPath("v" + metadataVersion + TableMetadataParser.getOldFileExtension(codec));
  }

  private Path metadataPath(String filename) {
    return new Path(metadataRoot(), filename);
  }

  private Path metadataRoot() {
    return new Path(location, "metadata");
  }

  Path getMetadataFile(int metadataVersion) throws IOException {
    for (TableMetadataParser.Codec codec : TableMetadataParser.Codec.values()) {
      Path metadataFile = metadataFilePath(metadataVersion, codec);
      if (fileIO.exists(metadataFile.toString())) {
        return metadataFile;
      }

      if (codec.equals(TableMetadataParser.Codec.GZIP)) {
        // we have to be backward-compatible with .metadata.json.gz files
        metadataFile = oldMetadataFilePath(metadataVersion, codec);
        if (fileIO.exists(metadataFile.toString())) {
          return metadataFile;
        }
      }
    }

    return null;
  }

  public void removeVersionHit() {
    fileIO.deleteFile(versionHintFile().toString());
  }

  public List<Path> getMetadataCandidateFiles(int metadataVersion) {
    List<Path> paths = new ArrayList<>();
    for (TableMetadataParser.Codec codec : TableMetadataParser.Codec.values()) {
      Path metadataFile = metadataFilePath(metadataVersion, codec);
      paths.add(metadataFile);

      if (codec.equals(TableMetadataParser.Codec.GZIP)) {
        // we have to be backward-compatible with .metadata.json.gz files
        metadataFile = oldMetadataFilePath(metadataVersion, codec);
        paths.add(metadataFile);
      }
    }
    return paths;
  }
}
