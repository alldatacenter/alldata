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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Manages all stack related behavior including parsing of stacks and providing access to
 * stack information.
 */
public class StackManager {

  public static final String PROPERTY_SCHEMA_PATH = "configuration-schema.xsd";
  /**
   * Delimiter used for parent path string
   * Example:
   *  HDP/2.0.6/HDFS
   *  common-services/HDFS/2.1.0.2.0
   */
  public static final String PATH_DELIMITER = "/";

  /**
   * Prefix used for common services parent path string
   */
  public static final String COMMON_SERVICES = "common-services";

  /**
   * Prefix used for extension services parent path string
   */
  public static final String EXTENSIONS = "extensions";

  public static final String METAINFO_FILE_NAME = "metainfo.xml";

  /**
   * Provides access to non-stack server functionality
   */
  private StackContext stackContext;

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackManager.class);

  /**
   * Map of stack id to stack info
   */
  protected NavigableMap<String, StackInfo> stackMap = new TreeMap<>();
  protected Map<String, ServiceModule> commonServiceModules;
  protected Map<String, StackModule> stackModules;
  protected Map<String, ExtensionModule> extensionModules;

  /**
   * Map of extension id to extension info
   */
  private Map<String, ExtensionInfo> extensionMap = new HashMap<>();

  private AmbariManagementHelper helper;

  /**
   * Constructor. Initialize stack manager.
   *
   * @param stackRootDir
   *          stack root directory
   * @param commonServicesRoot
   *          common services root directory
   * @param extensionRoot
   *          extensions root directory
   * @param osFamily
   *          the OS family read from resources
   * @param metaInfoDAO
   *          metainfo DAO automatically injected
   * @param actionMetadata
   *          action meta data automatically injected
   * @param stackDao
   *          stack DAO automatically injected
   * @param extensionDao
   *          extension DAO automatically injected
   * @param linkDao
   *          extension link DAO automatically injected
   * @param helper
   *          Ambari management helper automatically injected
   *
   * @throws AmbariException
   *           if an exception occurs while processing the stacks
   */
  @AssistedInject
  public StackManager(@Assisted("stackRoot") File stackRoot,
      @Assisted("commonServicesRoot") @Nullable File commonServicesRoot,
      @Assisted("extensionRoot") @Nullable File extensionRoot,
      @Assisted OsFamily osFamily, @Assisted boolean validate,
      MetainfoDAO metaInfoDAO, ActionMetadata actionMetadata, StackDAO stackDao,
      ExtensionDAO extensionDao, ExtensionLinkDAO linkDao, AmbariManagementHelper helper)
      throws AmbariException {

    LOG.info("Initializing the stack manager...");

    if (validate) {
      validateStackDirectory(stackRoot);
      validateCommonServicesDirectory(commonServicesRoot);
      validateExtensionDirectory(extensionRoot);
    }

    stackMap = new TreeMap<>();
    stackContext = new StackContext(metaInfoDAO, actionMetadata, osFamily);
    extensionMap = new HashMap<>();
    this.helper = helper;

    parseDirectories(stackRoot, commonServicesRoot, extensionRoot);

    //Read the extension links from the DB
    for (StackModule module : stackModules.values()) {
      StackInfo stack = module.getModuleInfo();
      List<ExtensionLinkEntity> entities = linkDao.findByStack(stack.getName(), stack.getVersion());
      for (ExtensionLinkEntity entity : entities) {
        String name = entity.getExtension().getExtensionName();
        String version = entity.getExtension().getExtensionVersion();
        String key = name + StackManager.PATH_DELIMITER + version;
        ExtensionModule extensionModule = extensionModules.get(key);
        if (extensionModule != null) {
          LOG.info("Adding extension to stack/version: " + stack.getName() + "/" + stack.getVersion() +
                   " extension/version: " + name + "/" + version);
          //Add the extension to the stack
          module.getExtensionModules().put(key, extensionModule);
        }
      }
    }

    fullyResolveCommonServices(stackModules, commonServiceModules, extensionModules);
    fullyResolveExtensions(stackModules, commonServiceModules, extensionModules);
    fullyResolveStacks(stackModules, commonServiceModules, extensionModules);

    populateDB(stackDao, extensionDao);
  }

  protected void parseDirectories(File stackRoot, File commonServicesRoot, File extensionRoot) throws AmbariException {
    commonServiceModules = parseCommonServicesDirectory(commonServicesRoot);
    stackModules = parseStackDirectory(stackRoot);
    LOG.info("About to parse extension directories");
    extensionModules = parseExtensionDirectory(extensionRoot);
  }

  private void populateDB(StackDAO stackDao, ExtensionDAO extensionDao) throws AmbariException {
    // for every stack read in, ensure that we have a database entry for it;
    // don't put try/catch logic around this since a failure here will
    // cause other things to break down the road
    Collection<StackInfo> stacks = getStacks();
    for(StackInfo stack : stacks){
      String stackName = stack.getName();
      String stackVersion = stack.getVersion();

      if (stackDao.find(stackName, stackVersion) == null) {
        LOG.info("Adding stack {}-{} to the database", stackName, stackVersion);

        StackEntity stackEntity = new StackEntity();
        stackEntity.setStackName(stackName);
        stackEntity.setStackVersion(stackVersion);

        stackDao.create(stackEntity);
      }
    }

    // for every extension read in, ensure that we have a database entry for it;
    // don't put try/catch logic around this since a failure here will
    // cause other things to break down the road
    Collection<ExtensionInfo> extensions = getExtensions();
    for(ExtensionInfo extension : extensions){
      String extensionName = extension.getName();
      String extensionVersion = extension.getVersion();

      if (extensionDao.find(extensionName, extensionVersion) == null) {
        LOG.info("Adding extension {}-{} to the database", extensionName, extensionVersion);

        ExtensionEntity extensionEntity = new ExtensionEntity();
        extensionEntity.setExtensionName(extensionName);
        extensionEntity.setExtensionVersion(extensionVersion);

        extensionDao.create(extensionEntity);
      }
    }

    createLinks();
  }

  /**
   * Attempts to automatically create links between extension versions and stack versions.
   * This is limited to 'active' extensions that have the 'autolink' attribute set (in the metainfo.xml).
   * Stack versions are selected based on the minimum stack versions that the extension supports.
   * The extension and stack versions are processed in order of most recent to oldest.
   * In this manner, the newest extension version will be autolinked before older extension versions.
   * If a different version of the same extension is already linked to a stack version then that stack version
   * will be skipped.
   */
  private void createLinks() {
    LOG.info("Creating links");
    Collection<ExtensionInfo> extensions = getExtensions();
    Set<String> names = new HashSet<>();
    for(ExtensionInfo extension : extensions){
      names.add(extension.getName());
    }
    for(String name : names) {
      createLinksForExtension(name);
    }
  }

  /**
   * Attempts to automatically create links between versions of a particular extension and stack versions they support.
   * This is limited to 'active' extensions that have the 'autolink' attribute set (in the metainfo.xml).
   * Stack versions are selected based on the minimum stack versions that the extension supports.
   * The extension and stack versions are processed in order of most recent to oldest.
   * In this manner, the newest extension version will be autolinked before older extension versions.
   * If a different version of the same extension is already linked to a stack version then that stack version
   * will be skipped.
   */
  private void createLinksForExtension(String name) {
    Collection<ExtensionInfo> collection = getExtensions(name);
    List<ExtensionInfo> extensions = new ArrayList<>(collection.size());
    extensions.addAll(collection);
    try {
      helper.createExtensionLinks(this, extensions);
    }
    catch (AmbariException e) {
      String msg = String.format("Failed to create link for extension: %s with exception: %s", name, e.getMessage());
      LOG.error(msg);
    }
  }

  /**
   * Obtain the stack info specified by name and version.
   *
   * @param name     name of the stack
   * @param version  version of the stack
   * @return The stack corresponding to the specified name and version.
   *         If no matching stack exists, null is returned.
   */
  public StackInfo getStack(String name, String version) {
    return stackMap.get(name + StackManager.PATH_DELIMITER + version);
  }

  /**
   * Obtain all stacks for the given name.
   *
   * @param name  stack name
   * @return A collection of all stacks with the given name.
   *         If no stacks match the specified name, an empty collection is returned.
   */
  public Collection<StackInfo> getStacks(String name) {
    Collection<StackInfo> stacks = new HashSet<>();
    for (StackInfo stack: stackMap.values()) {
      if (stack.getName().equals(name)) {
        stacks.add(stack);
      }
    }
    return stacks;
  }

  /**
   * Obtain all a map of all stacks by name.
   *
   * @return A map of all stacks with the name as the key.
   */
  public Map<String, List<StackInfo>> getStacksByName() {
    Map<String, List<StackInfo>> stacks = new HashMap<>();
    for (StackInfo stack: stackMap.values()) {
      List<StackInfo> list = stacks.get(stack.getName());
      if (list == null) {
        list = new ArrayList<>();
        stacks.put(stack.getName(),  list);
      }
      list.add(stack);
    }
    return stacks;
  }

  /**
   * Obtain all stacks.
   *
   * @return collection of all stacks
   */
  public Collection<StackInfo> getStacks() {
    return stackMap.values();
  }

  /**
   * Obtain the extension info specified by name and version.
   *
   * @param name     name of the extension
   * @param version  version of the extension
   * @return The extension corresponding to the specified name and version.
   *         If no matching stack exists, null is returned.
   */
  public ExtensionInfo getExtension(String name, String version) {
    return extensionMap.get(name + StackManager.PATH_DELIMITER + version);
  }

  /**
   * Obtain all extensions for the given name.
   *
   * @param name  extension name
   * @return A collection of all extensions with the given name.
   *         If no extensions match the specified name, an empty collection is returned.
   */
  public Collection<ExtensionInfo> getExtensions(String name) {
    Collection<ExtensionInfo> extensions = new HashSet<>();
    for (ExtensionInfo extension: extensionMap.values()) {
      if (extension.getName().equals(name)) {
	  extensions.add(extension);
      }
    }
    return extensions;
  }

  /**
   * Obtain all extensions.
   *
   * @return collection of all extensions
   */
  public Collection<ExtensionInfo> getExtensions() {
    return extensionMap.values();
  }

  /**
   * Determine if all tasks which update stack repo urls have completed.
   *
   * @return true if all of the repo update tasks have completed; false otherwise
   */
  public boolean haveAllRepoUrlsBeenResolved() {
    return stackContext.haveAllRepoTasksCompleted();
  }

  /**
   * Fully resolve all stacks.
   *
   * @param stackModules          map of stack id which contains name and version to stack module.
   * @param commonServiceModules  map of common service id which contains name and version to stack module.
   * @throws AmbariException if unable to resolve all stacks
   */
  private void fullyResolveStacks(
      Map<String, StackModule> stackModules, Map<String, ServiceModule> commonServiceModules, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    // Resolve all stacks without finalizing the stacks.
    for (StackModule stack : stackModules.values()) {
      if (stack.getModuleState() == ModuleState.INIT) {
        stack.resolve(null, stackModules, commonServiceModules, extensions);
      }
    }
    // Finalize the common services and stacks to remove sub-modules marked for deletion.
    // Finalizing the stacks AFTER all stacks are resolved ensures that the sub-modules marked for deletion are
    // inherited into the child module when explicit parent is defined and thereby ensuring all modules from parent module
    // are inlined into the child module even if the module is marked for deletion.
    for(ServiceModule commonService : commonServiceModules.values()) {
      commonService.finalizeModule();
    }
    for (ExtensionModule extension : extensions.values()) {
      extension.finalizeModule();
    }
    for (StackModule stack : stackModules.values()) {
      stack.finalizeModule();
    }
    // Execute all of the repo tasks in a single thread executor
    stackContext.executeRepoTasks();
  }

  /**
   * Fully resolve common services.
   *
   * @param stackModules          map of stack id which contains name and version to stack module.
   * @param commonServiceModules  map of common service id which contains name and version to common service module.
   * @throws AmbariException if unable to resolve all common services
   */
  private void fullyResolveCommonServices(
      Map<String, StackModule> stackModules, Map<String, ServiceModule> commonServiceModules, Map<String, ExtensionModule> extensions)
      throws AmbariException {
    for(ServiceModule commonService : commonServiceModules.values()) {
      if (commonService.getModuleState() == ModuleState.INIT) {
        commonService.resolveCommonService(stackModules, commonServiceModules, extensions);
      }
    }
  }

  /**
   * Fully resolve extensions.
   *
   * @param extensionModules      map of extension id which contains name and version to extension module.
   * @param stackModules          map of stack id which contains name and version to stack module.
   * @param commonServiceModules  map of common service id which contains name and version to common service module.
   * @throws AmbariException if unable to resolve all extensions
   */
  private void fullyResolveExtensions(Map<String, StackModule> stackModules, Map<String, ServiceModule> commonServiceModules,
      Map<String, ExtensionModule> extensionModules)
      throws AmbariException {
    for(ExtensionModule extensionModule : extensionModules.values()) {
      if (extensionModule.getModuleState() == ModuleState.INIT) {
        extensionModule.resolve(null, stackModules, commonServiceModules, extensionModules);
      }
    }
  }

  /**
   * Validate that the specified common services root is a valid directory.
   *
   * @param commonServicesRoot the common services root directory to validate
   * @throws AmbariException if the specified common services root directory is invalid
   */
  private void validateCommonServicesDirectory(File commonServicesRoot) throws AmbariException {
    if(commonServicesRoot != null) {
      LOG.info("Validating common services directory {} ...",
          commonServicesRoot);

      String commonServicesRootAbsolutePath = commonServicesRoot.getAbsolutePath();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Loading common services information, commonServicesRoot = {}", commonServicesRootAbsolutePath);
      }

      if (!commonServicesRoot.isDirectory() && !commonServicesRoot.exists()) {
        throw new AmbariException("" + Configuration.COMMON_SERVICES_DIR_PATH
            + " should be a directory with common services"
            + ", commonServicesRoot = " + commonServicesRootAbsolutePath);
      }
    }
  }

  /**
   * Validate that the specified stack root is a valid directory.
   *
   * @param stackRoot  the stack root directory to validate
   * @throws AmbariException if the specified stack root directory is invalid
   */
  private void validateStackDirectory(File stackRoot) throws AmbariException {
    LOG.info("Validating stack directory {} ...", stackRoot);

    String stackRootAbsPath = stackRoot.getAbsolutePath();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stack information, stackRoot = {}", stackRootAbsPath);
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists()) {
      throw new AmbariException("" + Configuration.METADATA_DIR_PATH
          + " should be a directory with stack"
          + ", stackRoot = " + stackRootAbsPath);
    }
    Validator validator = getPropertySchemaValidator();

    validateAllPropertyXmlsInFolderRecursively(stackRoot, validator);
  }

  public static Validator getPropertySchemaValidator() throws AmbariException {
    SchemaFactory factory =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema;
    ClassLoader classLoader = StackManager.class.getClassLoader();
    try {
      schema = factory.newSchema(classLoader.getResource(PROPERTY_SCHEMA_PATH));
    } catch (SAXException e) {
      throw new AmbariException(String.format("Failed to parse property schema file %s", PROPERTY_SCHEMA_PATH), e);
    }
    return schema.newValidator();
  }

  public static void validateAllPropertyXmlsInFolderRecursively(File stackRoot, Validator validator) throws AmbariException {
    Collection<File> files = FileUtils.listFiles(stackRoot, new String[]{"xml"}, true);
    for (File file : files) {
      try {
        if (file.getParentFile().getName().contains("configuration")) {
          validator.validate(new StreamSource(file));
        }
      } catch (Exception e) {
        String msg = String.format("File %s didn't pass the validation. Error message is : %s", file.getAbsolutePath(), e.getMessage());
        LOG.error(msg);
        throw new AmbariException(msg);
      }
    }
  }

  /**
   * Validate that the specified extension root is a valid directory.
   *
   * @param extensionRoot  the extension root directory to validate
   * @throws AmbariException if the specified extension root directory is invalid
   */
  private void validateExtensionDirectory(File extensionRoot) throws AmbariException {
    LOG.info("Validating extension directory {} ...", extensionRoot);

    if (extensionRoot == null) {
      return;
    }

    String extensionRootAbsPath = extensionRoot.getAbsolutePath();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading extension information, extensionRoot = {}", extensionRootAbsPath);
    }

    //For backwards compatibility extension directory may not exist
    if (extensionRoot.exists() && !extensionRoot.isDirectory()) {
      throw new AmbariException("" + Configuration.METADATA_DIR_PATH
          + " should be a directory"
          + ", extensionRoot = " + extensionRootAbsPath);
    }
  }

  /**
   * Parse the specified common services root directory
   *
   * @param commonServicesRoot  the common services root directory to parse
   * @return map of common service id which contains name and version to common service module.
   * @throws AmbariException if unable to parse all common services
   */
  private Map<String, ServiceModule> parseCommonServicesDirectory(File commonServicesRoot) throws AmbariException {
    Map<String, ServiceModule> commonServiceModules = new HashMap<>();

    if(commonServicesRoot != null) {
      File[] commonServiceFiles = commonServicesRoot.listFiles(StackDirectory.FILENAME_FILTER);
      for (File commonService : commonServiceFiles) {
        if (commonService.isFile()) {
          continue;
        }
        for (File serviceFolder : commonService.listFiles(StackDirectory.FILENAME_FILTER)) {
          ServiceDirectory serviceDirectory = new CommonServiceDirectory(serviceFolder.getPath());
          ServiceMetainfoXml metaInfoXml = serviceDirectory.getMetaInfoFile();
          if (metaInfoXml != null) {
            if (metaInfoXml.isValid()) {
              for (ServiceInfo serviceInfo : metaInfoXml.getServices()) {
                ServiceModule serviceModule = new ServiceModule(stackContext, serviceInfo, serviceDirectory, true);

                String commonServiceKey = serviceInfo.getName() + StackManager.PATH_DELIMITER + serviceInfo.getVersion();
                commonServiceModules.put(commonServiceKey, serviceModule);
              }
            } else {
              ServiceModule serviceModule = new ServiceModule(stackContext, new ServiceInfo(), serviceDirectory, true);
              serviceModule.setValid(false);
              serviceModule.addErrors(metaInfoXml.getErrors());
              commonServiceModules.put(metaInfoXml.getSchemaVersion(), serviceModule);
              metaInfoXml.setSchemaVersion(null);
            }
          }
        }
      }
    }
    return commonServiceModules;
  }

  /**
   * Parse the specified stack root directory
   *
   * @param stackRoot  the stack root directory to parse
   * @return map of stack id which contains name and version to stack module.
   * @throws AmbariException if unable to parse all stacks
   */
  private Map<String, StackModule> parseStackDirectory(File stackRoot) throws AmbariException {
    Map<String, StackModule> stackModules = new HashMap<>();

    File[] stackFiles = stackRoot.listFiles(StackDirectory.FILENAME_FILTER);
    for (File stack : stackFiles) {
      if (stack.isFile()) {
        continue;
      }
      for (File stackFolder : stack.listFiles(StackDirectory.FILENAME_FILTER)) {
        if (stackFolder.isFile()) {
          continue;
        }
        String stackName = stackFolder.getParentFile().getName();
        String stackVersion = stackFolder.getName();

        StackModule stackModule = new StackModule(new StackDirectory(stackFolder.getPath()), stackContext);
        String stackKey = stackName + StackManager.PATH_DELIMITER + stackVersion;
        stackModules.put(stackKey, stackModule);
        stackMap.put(stackKey, stackModule.getModuleInfo());
      }
    }

    if (stackMap.isEmpty()) {
      throw new AmbariException("Unable to find stack definitions under " +
          "stackRoot = " + stackRoot.getAbsolutePath());
    }
    return stackModules;
  }

  public void linkStackToExtension(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    stack.addExtension(extension);
  }

  public void unlinkStackAndExtension(StackInfo stack, ExtensionInfo extension) throws AmbariException {
    stack.removeExtension(extension);
  }

  /**
   * Parse the specified extension root directory
   *
   * @param extensionRoot  the extension root directory to parse
   * @return map of extension id which contains name and version to extension module.
   * @throws AmbariException if unable to parse all extensions
   */
  private Map<String, ExtensionModule> parseExtensionDirectory(File extensionRoot) throws AmbariException {
    Map<String, ExtensionModule> extensionModules = new HashMap<>();
    if (extensionRoot == null || !extensionRoot.exists()) {
      return extensionModules;
    }

    File[] extensionFiles = extensionRoot.listFiles(StackDirectory.FILENAME_FILTER);
    for (File extensionNameFolder : extensionFiles) {
      if (extensionNameFolder.isFile()) {
        continue;
      }
      for (File extensionVersionFolder : extensionNameFolder.listFiles(StackDirectory.FILENAME_FILTER)) {
        if (extensionVersionFolder.isFile()) {
          continue;
        }
        String extensionName = extensionNameFolder.getName();
        String extensionVersion = extensionVersionFolder.getName();

        ExtensionModule extensionModule = new ExtensionModule(new ExtensionDirectory(extensionVersionFolder.getPath()), stackContext);
        String extensionKey = extensionName + StackManager.PATH_DELIMITER + extensionVersion;
        extensionModules.put(extensionKey, extensionModule);
        extensionMap.put(extensionKey, extensionModule.getModuleInfo());
      }
    }

    if (stackMap.isEmpty()) {
      throw new AmbariException("Unable to find extension definitions under " +
          "extensionRoot = " + extensionRoot.getAbsolutePath());
    }
    return extensionModules;
  }

  public void removeStack(StackEntity stackEntity) {
    String stackKey = stackEntity.getStackName() + StackManager.PATH_DELIMITER +  stackEntity.getStackVersion();
    stackMap.remove(stackKey);
  }
}
