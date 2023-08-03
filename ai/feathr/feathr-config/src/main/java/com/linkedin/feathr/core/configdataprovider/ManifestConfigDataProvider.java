package com.linkedin.feathr.core.configdataprovider;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A Config Data Provider that reads a manifest file, and provides Reader objects for each config file listed in the
 * said manifest.
 * <p>
 * An example manifest file is shown below: It'll contain at most FeatureDef and Metadata config file locations,
 * never Join config file locations.
 *
 * <pre>{@code
 * manifest: [
 *   {
 *     jar: local
 *     conf: [config/online/feature-prod.conf]
 *   },
 *   {
 *     jar: frame-feature-waterloo-online-1.1.4.jar
 *     conf: [config/online/prod/feature-prod.conf]
 *   }
 * ]
 * }</pre>
 *
 */
/*
 * TODO: The manifest file currently lumps all config files in the "conf" field. It should be modified to list
 *  FeatureDef and Metadata config files in "featureDefConf" and "metadataConf" fields respectively. This will also
 *  necessitate changes in ConfigDataProvider interface.
 */
public class ManifestConfigDataProvider extends BaseConfigDataProvider {
  private static final Logger logger = LogManager.getLogger(ManifestConfigDataProvider.class);

  /*
   * The various config keys and value in the manifest file
   */
  private static final String MANIFEST_KEY = "manifest";
  private static final String JAR_KEY = "jar";
  private static final String CONF_KEY = "conf";
  private static final String LOCAL_VALUE = "local";

  private String _manifestResourceName;

  private Config _manifestConfig;

  private List<JarFile> _jarFiles;

  public ManifestConfigDataProvider(String manifestResourceName) {
    Objects.requireNonNull(manifestResourceName, "Manifest resource name can't be null");

    _manifestResourceName = manifestResourceName;

    _jarFiles = new ArrayList<>();

    ConfigRenderOptions renderOptions = ConfigRenderOptions.defaults()
        .setComments(false)
        .setOriginComments(false)
        .setFormatted(true)
        .setJson(true);

    _manifestConfig = ConfigFactory.parseResources(manifestResourceName);
    logger.debug("Manifest config: \n" + _manifestConfig.root().render(renderOptions.setJson(false)));
  }

  @Override
  public List<Reader> getConfigDataReaders() {
    List<? extends Config> jarConfConfigList = _manifestConfig.getConfigList(MANIFEST_KEY);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    /*
     * Iterate over all jar-conf pairs. If the jar file is 'local', that is, it's the current library
     * then read the conf files as resources else read them from the specified jar file. In both cases,
     * build a Reader object for each conf file.
     */
    for (Config jarConfConfig : jarConfConfigList) {
      String jarFileName = jarConfConfig.getString(JAR_KEY);

      List<String> confFileNames = jarConfConfig.getStringList(CONF_KEY);

      if (jarFileName.equalsIgnoreCase(LOCAL_VALUE)) {
        createReaders(loader, confFileNames, _readers);
      } else {
        createReaders(loader, jarFileName, confFileNames, _readers);
      }
    }

    return _readers;
  }

  @Override
  public String getConfigDataInfo() {
    return "Manifest: " + _manifestResourceName;
  }

  /*
   * This method is provided here so that JarFile objects, if any, can be closed.
   */
  @Override
  public void close() {
    super.close();

    try {
      for (JarFile jf : _jarFiles) {
        jf.close();
      }
    } catch (IOException e) {
      logger.warn("Unable to close a jar file");
    }
    logger.debug("Closed " + _jarFiles.size() + " jar files");

    _jarFiles.clear();
  }

  private void createReaders(ClassLoader loader, List<String> confFileNames, List<Reader> readers) {
    for (String resName : confFileNames) {
      InputStream in = loader.getResourceAsStream(resName);
      if (in == null) {
        throw new ConfigDataProviderException("Config file " + resName + " can't be obtained as an input stream");
      }

      Reader reader = new BufferedReader(new InputStreamReader(in));
      // Since the conf files are local, they may be overrides. As such add them to the head of the list.
      readers.add(0, reader);
    }
  }

  private void createReaders(ClassLoader loader, String jarFileName, List<String> confFileNames,
      List<Reader> readers) {
    // load the jar file as a URL, and check for validity
    URL jarFileUrl = loader.getResource(jarFileName);
    if (jarFileUrl == null) {
      throw new ConfigDataProviderException("Unable to load jar file " + jarFileName);
    }

    /*
     *  Create JarFile -> InputStream -> InputStreamReader -> wrap in BufferedReader
     */
    String jarFilePath = jarFileUrl.getPath();

    /*
     * Create a JarFile object that is used to get a JarEntry for each conf file. Each JarEntry
     * is used to get an InputStream which is then wrapped by InputStreamReader and BufferedReader.
     */
    try {
      JarFile jarFile = new JarFile(jarFilePath);
      _jarFiles.add(jarFile);   // Hold on to these JarFile objects, they'll be closed during close() invocation

      for (String confFileName : confFileNames) {
        JarEntry entry = jarFile.getJarEntry(confFileName);

        InputStream inStream = jarFile.getInputStream(entry);
        InputStreamReader inStreamReader = new InputStreamReader(inStream);
        BufferedReader reader = new BufferedReader(inStreamReader);
        readers.add(reader);
      }
    } catch (Exception e) {
      throw new ConfigDataProviderException("Error in creating config file readers from jar " + jarFileName, e);
    }
  }
}
