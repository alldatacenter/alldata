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

package org.apache.ambari.server.stack;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.stack.ExtensionMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates IO operations on a extension definition extension directory.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
//todo: Normalize all path return values.
//todo: Currently some are relative and some are absolute.
//todo: Current values were dictated by the ExtensionInfo expectations.
public class ExtensionDirectory extends StackDefinitionDirectory {

  /**
   * collection of service directories
   */
  private Collection<ServiceDirectory> serviceDirectories;

  /**
   * metainfo file representation
   */
  private ExtensionMetainfoXml metaInfoXml;

  /**
   * file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * extensions directory name
   */
  public final static String EXTENSIONS_FOLDER_NAME = "extensions";

  /**
   * metainfo file name
   */
  private static final String EXTENSION_METAINFO_FILE_NAME = "metainfo.xml";

  /**
   * logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(ExtensionDirectory.class);


  /**
   * Constructor.
   *
   * @param directory  extension directory
   * @throws AmbariException if unable to parse the stack directory
   */
  public ExtensionDirectory(String directory) throws AmbariException {
    super(directory);
    parsePath();
  }

  /**
   * Obtain the extension directory name.
   *
   * @return extension directory name
   */
  public String getExtensionDirName() {
    return getDirectory().getParentFile().getName();
  }

  /**
   * Obtain the object representation of the extension metainfo.xml file.
   *
   * @return object representation of the extension metainfo.xml file
   */
  public ExtensionMetainfoXml getMetaInfoFile() {
    return metaInfoXml;
  }

  /**
   * Obtain a collection of all service directories.
   *
   * @return collection of all service directories
   */
  public Collection<ServiceDirectory> getServiceDirectories() {
    return serviceDirectories;
  }

  /**
   * Parse the extension directory.
   *
   * @throws AmbariException if unable to parse the directory
   */
  private void parsePath() throws AmbariException {
    Collection<String> subDirs = Arrays.asList(directory.list());
    parseServiceDirectories(subDirs);
    parseMetaInfoFile();
  }

  /**
   * Parse the extension metainfo file.
   *
   * @throws AmbariException if unable to parse the extension metainfo file
   */
  private void parseMetaInfoFile() throws AmbariException {
    File extensionMetaInfoFile = new File(getAbsolutePath()
        + File.separator + EXTENSION_METAINFO_FILE_NAME);

    //todo: is it ok for this file not to exist?
    if (extensionMetaInfoFile.exists()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reading extension version metainfo from file {}", extensionMetaInfoFile.getAbsolutePath());
      }

      try {
        metaInfoXml = unmarshaller.unmarshal(ExtensionMetainfoXml.class, extensionMetaInfoFile);
      } catch (Exception e) {
        metaInfoXml = new ExtensionMetainfoXml();
        metaInfoXml.setValid(false);
        metaInfoXml.addError("Unable to parse extension metainfo.xml file at location: " +
            extensionMetaInfoFile.getAbsolutePath());
      }
    }
  }

  /**
   * Parse the extension's service directories extension
   * @param subDirs  extension sub directories
   * @throws AmbariException  if unable to parse the service directories
   */
  private void parseServiceDirectories(Collection<String> subDirs) throws AmbariException {
    Collection<ServiceDirectory> dirs = new HashSet<>();

    if (subDirs.contains(ServiceDirectory.SERVICES_FOLDER_NAME)) {
      String servicesDir = getAbsolutePath() + File.separator + ServiceDirectory.SERVICES_FOLDER_NAME;
      File baseServiceDir = new File(servicesDir);
      File[] serviceFolders = baseServiceDir.listFiles(StackDirectory.FILENAME_FILTER);
      if (serviceFolders != null) {
        for (File d : serviceFolders) {
          if (d.isDirectory()) {
            try {
              dirs.add(new StackServiceDirectory(d.getAbsolutePath()));
            } catch (AmbariException e) {
              //todo: this seems as though we should propagate this exception
              //todo: eating it now to keep backwards compatibility
              LOG.warn(String.format("Unable to parse extension definition service at '%s'.  Ignoring service. : %s",
                  d.getAbsolutePath(), e.toString()));
            }
          }
        }
      }
    }

    if (dirs.isEmpty()) {
      //todo: what does it mean for a extension to have no services?
      LOG.info("The extension defined at '" + getAbsolutePath() + "' contains no services");
    }
    serviceDirectories = dirs;
  }
}
