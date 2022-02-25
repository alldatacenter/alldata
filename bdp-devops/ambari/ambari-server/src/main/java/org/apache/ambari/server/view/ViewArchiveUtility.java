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

package org.apache.ambari.server.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ambari.server.view.configuration.ViewConfig;
import org.xml.sax.SAXException;

/**
 * Helper class for basic view archive utility.
 */
public class ViewArchiveUtility {

  /**
   * Constants
   */
  private static final String VIEW_XML = "view.xml";
  private static final String WEB_INF_VIEW_XML = "WEB-INF/classes/" + VIEW_XML;
  private static final String VIEW_XSD = "view.xsd";


  // ----- ViewArchiveUtility ------------------------------------------------

  /**
   * Get the view configuration from the given archive file.
   *
   * @param archiveFile  the archive file
   *
   * @return the associated view configuration
   *
   * @throws JAXBException if xml is malformed
   */
  public ViewConfig getViewConfigFromArchive(File archiveFile)
      throws JAXBException, IOException {
    ClassLoader cl = URLClassLoader.newInstance(new URL[]{archiveFile.toURI().toURL()});

    InputStream configStream = cl.getResourceAsStream(VIEW_XML);
    if (configStream == null) {
      configStream = cl.getResourceAsStream(WEB_INF_VIEW_XML);
      if (configStream == null) {
        throw new IllegalStateException(
            String.format("Archive %s doesn't contain a view descriptor.", archiveFile.getAbsolutePath()));
      }
    }

    try {

      JAXBContext jaxbContext       = JAXBContext.newInstance(ViewConfig.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    } finally {
      configStream.close();
    }
  }

  /**
   * Get the view configuration from the extracted archive file.
   *
   * @param archivePath  path to extracted archive
   * @param validate     indicates whether or not the view configuration should be validated
   *
   * @return the associated view configuration
   *
   * @throws JAXBException if xml is malformed
   * @throws IOException if xml can not be read
   * @throws SAXException if the validation fails
   */
  public ViewConfig getViewConfigFromExtractedArchive(String archivePath, boolean validate)
      throws JAXBException, IOException, SAXException {
    File configFile = new File(archivePath + File.separator + VIEW_XML);

    if (!configFile.exists()) {
      configFile = new File(archivePath + File.separator + WEB_INF_VIEW_XML);
    }

    if (validate) {
      validateConfig(new FileInputStream(configFile));
    }

    InputStream  configStream = new FileInputStream(configFile);
    try {

      JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    } finally {
      configStream.close();
    }
  }

  /**
   * Get a new file instance for the given path.
   *
   * @param path  the path
   *
   * @return a new file instance
   */
  public File getFile(String path) {
    return new File(path);
  }

  /**
   * Get a new file output stream for the given file.
   *
   * @param file  the file
   *
   * @return a new file output stream
   */
  public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
    return new FileOutputStream(file);
  }

  /**
   * Get a new jar file stream from the given file.
   *
   * @param file  the file
   *
   * @return a new jar file stream
   */
  public JarInputStream getJarFileStream(File file) throws IOException {
    return new JarInputStream(new FileInputStream(file));
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Validate the given view descriptor file against the view schema.
   *
   * @param configStream  input stream of view descriptor file to be validated
   *
   * @throws SAXException if the validation fails
   * @throws IOException if the descriptor file can not be read
   */
  protected void validateConfig(InputStream  configStream) throws SAXException, IOException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    URL schemaUrl = getClass().getClassLoader().getResource(VIEW_XSD);
    Schema schema = schemaFactory.newSchema(schemaUrl);

    schema.newValidator().validate(new StreamSource(configStream));
  }
}
