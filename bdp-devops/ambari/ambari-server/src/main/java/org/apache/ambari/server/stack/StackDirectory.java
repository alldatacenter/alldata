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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * Encapsulates IO operations on a stack definition stack directory.
 */
//todo: Normalize all path return values.
//todo: Currently some are relative and some are absolute.
//todo: Current values were dictated by the StackInfo expectations.
public class StackDirectory extends StackDefinitionDirectory {
  public static final String SERVICE_CONFIG_FOLDER_NAME = "configuration";
  public static final String SERVICE_PROPERTIES_FOLDER_NAME = "properties";
  public static final String SERVICE_THEMES_FOLDER_NAME = "themes";
  public static final String SERVICE_QUICKLINKS_CONFIGURATIONS_FOLDER_NAME = "quicklinks";
  public static final String SERVICE_CONFIG_FILE_NAME_POSTFIX = ".xml";
  public static final String RCO_FILE_NAME = "role_command_order.json";
  public static final String SERVICE_METRIC_FILE_NAME = "metrics.json";
  public static final String SERVICE_ALERT_FILE_NAME = "alerts.json";
  public static final String SERVICE_ADVISOR_FILE_NAME = "service_advisor.py";

  /**
   * Allows stacks to provide their own JARs, such as those which implement
   * classes in the SPI.
   */
  public static final String LIB_FOLDER_NAME = "lib";

  /**
   * The filename for a Kerberos descriptor preconfigure file at either the stack or service level
   */
  public static final String KERBEROS_DESCRIPTOR_PRECONFIGURE_FILE_NAME = "kerberos_preconfigure.json";
  /**
   * Filename for theme file at service layer
   */
  public static final String SERVICE_THEME_FILE_NAME = "theme.json";

  /**
   * upgrades directory path
   */
  private String upgradesDir;

  /**
   * rco file path
   */
  private String rcoFilePath;

  /**
   * The fully-qualified path to the stack's library directory where JARs can be
   * provided, such as those which implement the SPI.
   */
  private String libraryDir;

  /**
   * kerberos descriptor (preconfigure) file path
   */
  private String kerberosDescriptorPreconfigureFilePath;

  /**
   * repository file
   */
  private RepositoryXml repoFile;

  /**
   * role command order
   */
  private StackRoleCommandOrder roleCommandOrder;

  /**
   * repository directory
   */
  private String repoDir;

  /**
   * collection of service directories
   */
  private Collection<ServiceDirectory> serviceDirectories;

  /**
   * map of upgrade pack name to upgrade pack
   */
  private Map<String, UpgradePack> upgradePacks;

  /**
   * Config delta from prev stack
   */
  private ConfigUpgradePack configUpgradePack;

  /**
   * metainfo file representation
   */
  private StackMetainfoXml metaInfoXml;

  /**
   * A {@link ClassLoader} for any JARs discovered in the stack's library
   * folder.
   */
  private URLClassLoader libraryClassLoader;

  /**
   * file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  public static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String s) {
      return !(s.equals(".svn") || s.equals(".git"));
    }
  };

  /**
   * repository directory name
   */
  private final static String REPOSITORY_FOLDER_NAME = "repos";

  /**
   * metainfo file name
   */
  private static final String STACK_METAINFO_FILE_NAME = "metainfo.xml";

  /**
   * upgrades directory name
   */
  private static final String UPGRADE_PACK_FOLDER_NAME = "upgrades";

  /**
   * logger instance
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackDirectory.class);


  /**
   * Constructor.
   *
   * @param directory stack directory
   * @throws AmbariException if unable to parse the stack directory
   */
  public StackDirectory(String directory) throws AmbariException {
    super(directory);
    parsePath();
  }

  /**
   * Obtain the stack directory name.
   *
   * @return stack directory name
   */
  public String getStackDirName() {
    return getDirectory().getParentFile().getName();
  }

  /**
   * Obtain the upgrades directory path.
   *
   * @return upgrades directory path
   */
  public String getUpgradesDir() {
    return upgradesDir;
  }

  /**
   * Obtain the rco file path.
   *
   * @return rco file path
   */
  public String getRcoFilePath() {
    return rcoFilePath;
  }

  /**
   * Gets the fully qualified path to the stack's library directory.
   *
   * @return the library directory.
   */
  public String getLibraryPath() {
    return libraryDir;
  }

  /**
   * Obtain the path to the (stack-level) Kerberos descriptor pre-configuration file
   *
   * @return the path to the (stack-level) Kerberos descriptor pre-configuration file
   */
  public String getKerberosDescriptorPreconfigureFilePath() {
    return kerberosDescriptorPreconfigureFilePath;
  }

  /**
   * Obtain the repository directory path.
   *
   * @return repository directory path
   */
  public String getRepoDir() {
    return repoDir;
  }

  /**
   * Obtain the repository file object representation.
   *
   * @return repository file object representation
   */
  public RepositoryXml getRepoFile() {
    return repoFile;
  }

  /**
   * Obtain the object representation of the stack metainfo.xml file.
   *
   * @return object representation of the stack metainfo.xml file
   */
  public StackMetainfoXml getMetaInfoFile() {
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
   * Obtain a map of all upgrade packs.
   *
   * @return map of upgrade pack name to upgrade pack or null if no packs available
   */
  public Map<String, UpgradePack> getUpgradePacks() {
    return upgradePacks;
  }

  /**
   * @return Config delta from prev stack or null if no config upgrade patches available
   */
  public ConfigUpgradePack getConfigUpgradePack() {
    return configUpgradePack;
  }

  /**
   * Obtain the object representation of the stack role_command_order.json file
   *
   * @return object representation of the stack role_command_order.json file
   */
  public StackRoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrder;
  }

  /**
   * Gets the {@link ClassLoader} that can be used to load classes found in JARs
   * in the stack's library folder.
   *
   * @return the class loader for 3rd party JARs supplied by the stack or
   *         {@code null} if there are no libraries for this stack.
   */
  public @Nullable URLClassLoader getLibraryClassLoader() {
    return libraryClassLoader;
  }

  /**
   * Parse the stack directory.
   *
   * @throws AmbariException if unable to parse the directory
   */
  private void parsePath() throws AmbariException {
    Collection<String> subDirs = Arrays.asList(directory.list());
    if (subDirs.contains(RCO_FILE_NAME)) {
      // rcoFile is expected to be absolute
      rcoFilePath = getAbsolutePath() + File.separator + RCO_FILE_NAME;
    }

    if (subDirs.contains(LIB_FOLDER_NAME)) {
      libraryDir = getAbsolutePath() + File.separator + LIB_FOLDER_NAME;
    }

    if (subDirs.contains(KERBEROS_DESCRIPTOR_PRECONFIGURE_FILE_NAME)) {
      // kerberosDescriptorPreconfigureFilePath is expected to be absolute
      kerberosDescriptorPreconfigureFilePath = getAbsolutePath() + File.separator + KERBEROS_DESCRIPTOR_PRECONFIGURE_FILE_NAME;
    }

    parseUpgradePacks(subDirs);
    parseServiceDirectories(subDirs);
    parseRepoFile(subDirs);
    parseMetaInfoFile();
    parseRoleCommandOrder();
    parseLibraryClassLoader();

  }

  /**
   * Parse the repository file.
   *
   * @param subDirs stack directory sub directories
   */
  private void parseRepoFile(Collection<String> subDirs) {
    RepositoryFolderAndXml repoDirAndXml = RepoUtil.parseRepoFile(directory, subDirs, unmarshaller);
    repoDir = repoDirAndXml.repoDir.orNull();
    repoFile = repoDirAndXml.repoXml.orNull();

    if (repoFile == null || !repoFile.isValid()) {
      LOG.warn("No repository information defined for "
          + ", stackName=" + getStackDirName()
          + ", stackVersion=" + getPath()
          + ", repoFolder=" + getPath() + File.separator + REPOSITORY_FOLDER_NAME);
    }
  }

  /**
   * Parse the stack metainfo file.
   *
   * @throws AmbariException if unable to parse the stack metainfo file
   */
  private void parseMetaInfoFile() throws AmbariException {
    File stackMetaInfoFile = new File(getAbsolutePath()
        + File.separator + STACK_METAINFO_FILE_NAME);

    //todo: is it ok for this file not to exist?
    if (stackMetaInfoFile.exists()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reading stack version metainfo from file {}", stackMetaInfoFile.getAbsolutePath());
      }

      try {
        metaInfoXml = unmarshaller.unmarshal(StackMetainfoXml.class, stackMetaInfoFile);
      } catch (Exception e) {
        metaInfoXml = new StackMetainfoXml();
        metaInfoXml.setValid(false);
        String msg = "Unable to parse stack metainfo.xml file at location: " +
            stackMetaInfoFile.getAbsolutePath();
        metaInfoXml.addError(msg);
        LOG.warn(msg);
      }
    }
  }

  /**
   * Parse the stacks service directories.
   *
   * @param subDirs stack sub directories
   * @throws AmbariException if unable to parse the service directories
   */
  private void parseServiceDirectories(Collection<String> subDirs) throws AmbariException {
    Collection<ServiceDirectory> dirs = new HashSet<>();

    if (subDirs.contains(ServiceDirectory.SERVICES_FOLDER_NAME)) {
      String servicesDir = getAbsolutePath() + File.separator + ServiceDirectory.SERVICES_FOLDER_NAME;
      File baseServiceDir = new File(servicesDir);
      File[] serviceFolders = baseServiceDir.listFiles(FILENAME_FILTER);
      if (serviceFolders != null) {
        for (File d : serviceFolders) {
          if (d.isDirectory()) {
            try {
              dirs.add(new StackServiceDirectory(d.getAbsolutePath()));
            } catch (AmbariException e) {
              //todo: this seems as though we should propagate this exception
              //todo: eating it now to keep backwards compatibility
              LOG.warn(String.format("Unable to parse stack definition service at '%s'.  Ignoring service. : %s",
                  d.getAbsolutePath(), e.toString()));
            }
          }
        }
      }
    }

    if (dirs.isEmpty()) {
      //todo: what does it mean for a stack to have no services?
      LOG.info("The stack defined at '" + getAbsolutePath() + "' contains no services");
    }
    serviceDirectories = dirs;
  }


  /**
   * Parse all stack upgrade files for the stack.
   *
   * @param subDirs stack sub directories
   * @throws AmbariException if unable to parse stack upgrade file
   */
  private void parseUpgradePacks(Collection<String> subDirs) throws AmbariException {
    Map<String, UpgradePack> upgradeMap = new HashMap<>();
    ConfigUpgradePack configUpgradePack = null;
    if (subDirs.contains(UPGRADE_PACK_FOLDER_NAME)) {
      File f = new File(getAbsolutePath() + File.separator + UPGRADE_PACK_FOLDER_NAME);
      if (f.isDirectory()) {
        upgradesDir = f.getAbsolutePath();
        for (File upgradeFile : f.listFiles(XML_FILENAME_FILTER)) {
          if (upgradeFile.getName().toLowerCase().startsWith(CONFIG_UPGRADE_XML_FILENAME_PREFIX)) {
            if (configUpgradePack == null) {
              if(upgradeFile.length() != 0) {
                configUpgradePack = parseConfigUpgradePack(upgradeFile);
              }
            } else { // If user messed things up with lower/upper case filenames
              throw new AmbariException(String.format("There are multiple files with name like %s" + upgradeFile.getAbsolutePath()));
            }
          } else {
            String upgradePackName = FilenameUtils.removeExtension(upgradeFile.getName());
            if(upgradeFile.length() != 0) {
              UpgradePack pack = parseUpgradePack(upgradePackName, upgradeFile);
              pack.setName(upgradePackName);
              upgradeMap.put(upgradePackName, pack);
            }
          }
        }
      }
    }

    if (upgradesDir == null) {
      LOG.info("Stack '{}' doesn't contain an upgrade directory ", getPath());
    }

    if (!upgradeMap.isEmpty()) {
      upgradePacks = upgradeMap;
    }

    if (configUpgradePack != null) {
      this.configUpgradePack = configUpgradePack;
    } else {
      ConfigUpgradePack emptyConfigUpgradePack = new ConfigUpgradePack();
      emptyConfigUpgradePack.services = new ArrayList<>();
      this.configUpgradePack = emptyConfigUpgradePack;
      LOG.info("Stack '{}' doesn't contain config upgrade pack file", getPath());
    }
  }

  private UpgradePack parseUpgradePack(final String packName, File upgradeFile) throws AmbariException {
    UpgradePack pack = null;
    try {
      pack = unmarshaller.unmarshal(UpgradePack.class, upgradeFile);
      pack.setName(packName);
    } catch (Exception e) {
      if (upgradeFile == null) {
        throw new AmbariException("Null upgrade pack");
      }
      throw new AmbariException("Unable to parse stack upgrade file at location: " + upgradeFile.getAbsolutePath(), e);
    }
    return pack;
  }

  private ConfigUpgradePack parseConfigUpgradePack(File upgradeFile) throws AmbariException {
    ConfigUpgradePack pack = null;
    try {
      pack = unmarshaller.unmarshal(ConfigUpgradePack.class, upgradeFile);
    } catch (Exception e) {
      if (upgradeFile == null) {
        throw new AmbariException("Null config upgrade pack");
      }
      throw new AmbariException("Unable to parse stack upgrade file at location: " + upgradeFile.getAbsolutePath(), e);
    }
    return pack;
  }

  /**
   * Parse role command order file
   */
  private void parseRoleCommandOrder() {
    HashMap<String, Object> result = null;
    ObjectMapper mapper = new ObjectMapper();
    try {
      TypeReference<Map<String, Object>> rcoElementTypeReference = new TypeReference<Map<String, Object>>() {
      };
      if (rcoFilePath != null) {
        File file = new File(rcoFilePath);
        result = mapper.readValue(file, rcoElementTypeReference);
        LOG.info("Role command order info was loaded from file: {}", file.getAbsolutePath());
      } else {
        LOG.info("Stack '{}' doesn't contain role command order file", getPath());
        result = new HashMap<>();
      }
      roleCommandOrder = new StackRoleCommandOrder(result);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Role Command Order for {}", rcoFilePath);
        roleCommandOrder.printRoleCommandOrder(LOG);
      }
    } catch (IOException e) {
      LOG.error(String.format("Can not read role command order info %s", rcoFilePath), e);
    }
  }

  /**
   * Loads any classes found in the {@link #libraryDir} and creates a
   * {@link URLClassLoader} for them.
   */
  private void parseLibraryClassLoader() {
    if (null == libraryDir) {
      return;
    }

    Path libraryPath = Paths.get(libraryDir);
    if (Files.notExists(libraryPath) || !Files.isDirectory(libraryPath)) {
      return;
    }

    try {
      List<URI> jarUris = Files.list(libraryPath).filter(
          file -> file.toString().endsWith(".jar")).map(Path::toUri).collect(Collectors.toList());

      List<URL> jarUrls = new ArrayList<>(jarUris.size());
      for (URI jarUri : jarUris) {
        try {
          jarUrls.add(jarUri.toURL());
        } catch (MalformedURLException malformedURLException) {
          LOG.error("Unable to load the stack library {}", jarUri, malformedURLException);
        }
      }

      URL[] jarUrlArray = new URL[jarUris.size()];
      libraryClassLoader = new URLClassLoader(jarUrls.toArray(jarUrlArray),
          ClassUtils.getDefaultClassLoader());
    } catch (IOException ioException) {
      LOG.error("Unable to load libraries from {}", libraryPath, ioException);
    }
  }
}
