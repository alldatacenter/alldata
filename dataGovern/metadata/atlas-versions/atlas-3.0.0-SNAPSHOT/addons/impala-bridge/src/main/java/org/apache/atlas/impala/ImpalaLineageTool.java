/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala;

import org.apache.atlas.impala.hook.ImpalaLineageHook;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of actual implementation of Impala lineage tool. It reads the lineage records in
 * lineage log. It then calls instance of ImpalaLineageHook to convert lineage records to
 * lineage notifications and send them to Atlas.
 */
public class ImpalaLineageTool {
  private static final Logger LOG = LoggerFactory.getLogger(ImpalaLineageTool.class);
  private static final String WAL_FILE_EXTENSION = ".wal";
  private static final String WAL_FILE_PREFIX = "WAL";
  private String directoryName;
  private String prefix;

  public ImpalaLineageTool(String[] args) {
    try {
      Options options = new Options();
      options.addOption("d", "directory", true, "the lineage files' folder");
      options.addOption("p", "prefix", true, "the prefix of the lineage files");

      CommandLine cmd = new DefaultParser().parse(options, args);
      directoryName = cmd.getOptionValue("d");
      prefix    = cmd.getOptionValue("p");
    } catch(ParseException e) {
      LOG.warn("Failed to parse command arguments. Error: ", e.getMessage());
      printUsage();

      throw new RuntimeException(e);
    }
  }

  public void run() {
    ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

    File[] currentFiles = getCurrentFiles();
    int fileNum = currentFiles.length;

    for(int i = 0; i < fileNum; i++) {
      String filename = currentFiles[i].getAbsolutePath();
      String walFilename = directoryName + WAL_FILE_PREFIX + currentFiles[i].getName() + WAL_FILE_EXTENSION;

      LOG.info("Importing: {}", filename);
      importHImpalaEntities(impalaLineageHook, filename, walFilename);

      if(i != fileNum - 1) {
        deleteLineageAndWal(currentFiles[i], walFilename);
      }
    }
    LOG.info("Impala bridge processing: Done! ");
  }

  public static void main(String[] args) {
    if (args != null && args.length != 4) {
      // The lineage file location and prefix should be input as the parameters
      System.out.println("Impala bridge: wrong number of arguments. Please try again");
      printUsage();
      return;
    }

    ImpalaLineageTool instance = new ImpalaLineageTool(args);
    instance.run();
  }

  /**
   * Delete the used lineage file and wal file
   * @param currentFile The current file
   * @param wal The wal file
   */
  public static void deleteLineageAndWal(File currentFile, String wal) {
    if(currentFile.exists() && currentFile.delete()) {
      LOG.info("Lineage file {} is deleted successfully", currentFile.getPath());
    } else {
      LOG.info("Failed to delete the lineage file {}", currentFile.getPath());
    }

    File file = new File(wal);

    if(file.exists() && file.delete()) {
      LOG.info("Wal file {} deleted successfully", wal);
    } else {
      LOG.info("Failed to delete the wal file {}", wal);
    }
  }

  private static void printUsage() {
    System.out.println();
    System.out.println();
    System.out.println("Usage: import-impala.sh [-d <directory>] [-p <prefix>]"  );
    System.out.println("    Imports specified lineage files by given directory and file prefix.");
    System.out.println();
  }

  /**
   * This function figures out the right lineage file path+name to process sorted by the last
   * time they are modified. (old -> new)
   * @return get the lineage files from given directory with given prefix.
   */
  public File[] getCurrentFiles() {
    try {
      LOG.info("Scanning: " + directoryName);
      File folder = new File(directoryName);
      File[] listOfFiles = folder.listFiles((FileFilter) new PrefixFileFilter(prefix, IOCase.SENSITIVE));

      if ((listOfFiles == null) || (listOfFiles.length == 0)) {
        LOG.info("Found no lineage files.");
        return new File[0];
      }

      if(listOfFiles.length > 1) {
        Arrays.sort(listOfFiles, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
      }

      LOG.info("Found {} lineage files" + listOfFiles.length);
      return listOfFiles;
    } catch(Exception e) {
      LOG.error("Import lineage file failed.", e);
    }
    return new File[0];
  }

  private boolean processImpalaLineageHook(ImpalaLineageHook impalaLineageHook, List<String> lineageList) {
    boolean allSucceed = true;

    // returns true if successfully sent to Atlas
    for (String lineageRecord : lineageList) {
      try {
        impalaLineageHook.process(lineageRecord);
      } catch (Exception ex) {
        String errorMessage = String.format("Exception at query {} \n", lineageRecord);
        LOG.error(errorMessage, ex);

        allSucceed = false;
      }
    }

    return allSucceed;
  }

  /**
   * Create a list of lineage queries based on the lineage file and the wal file
   * @param name
   * @param walfile
   * @return
   */
  public void importHImpalaEntities(ImpalaLineageHook impalaLineageHook, String name, String walfile) {
    List<String> lineageList = new ArrayList<>();

    try {
      File lineageFile = new File(name); //use current file length to minus the offset
      File walFile = new File(walfile);
      // if the wal file does not exist, create one with 0 byte read, else, read the number
      if(!walFile.exists()) {
        BufferedWriter writer = new BufferedWriter(new FileWriter(walfile));
        writer.write("0, " + name);
        writer.close();
      }

      LOG.debug("Reading: " + name);
      String lineageRecord = FileUtils.readFileToString(lineageFile, "UTF-8");

      lineageList.add(lineageRecord);

      // call instance of ImpalaLineageHook to process the list of Impala lineage record
      if(processImpalaLineageHook(impalaLineageHook, lineageList)) {
        // write how many bytes the current file is to the wal file
        FileWriter newWalFile = new FileWriter(walfile, true);
        BufferedWriter newWalFileBuf = new BufferedWriter(newWalFile);
        newWalFileBuf.newLine();
        newWalFileBuf.write(String.valueOf(lineageFile.length()) + "," + name);

        newWalFileBuf.close();
        newWalFile.close();
      } else {
        LOG.error("Error sending some of impala lineage records to ImpalaHook");
      }
    } catch (Exception e) {
      LOG.error("Error in processing lineage records. Exception: " + e.getMessage());
    }
  }

}