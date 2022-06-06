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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ambari.server.stack.upgrade.ConfigUpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.state.stack.ExtensionMetainfoXml;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Provides functionality to unmarshal stack definition files to their
 * corresponding object representations.
 */
public class ModuleFileUnmarshaller {

  private static final Logger LOG = LoggerFactory.getLogger(ModuleFileUnmarshaller.class);

  /**
   * Map of class to JAXB context
   */
  private static final Map<Class<?>, JAXBContext> jaxbContexts = new HashMap<>();
  private static final Map<String, Schema> jaxbSchemas = new HashMap<>();


  /**
   * Unmarshal a file to it's corresponding object type.
   *
   * @param clz   class of the object representation
   * @param file  file to unmarshal
   *
   * @return object representation of the specified file
   * @throws JAXBException if unable to unmarshal the file
   * @throws XMLStreamException
   * @throws SAXException
   * @throws FileNotFoundException
   */
  public <T> T unmarshal(Class<T> clz, File file) throws JAXBException, IOException, XMLStreamException, SAXException {
    return unmarshal(clz, file, false);
  }

  /**
   * Unmarshal a file to it's corresponding object type.
   *
   * @param clz     class of the object representation
   * @param file    file to unmarshal
   * @param logXsd  log XSD information
   *
   * @return object representation of the specified file
   * @throws JAXBException if unable to unmarshal the file
   * @throws XMLStreamException
   * @throws SAXException
   * @throws FileNotFoundException
   */
  public <T> T unmarshal(Class<T> clz, File file, boolean logXsd) throws JAXBException, IOException, XMLStreamException, SAXException {
    Unmarshaller u = jaxbContexts.get(clz).createUnmarshaller();

    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();

    FileReader reader = new FileReader(file);
    XMLStreamReader xmlReader = xmlFactory.createXMLStreamReader(reader);

    xmlReader.nextTag();
    String xsdName = xmlReader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "noNamespaceSchemaLocation");

    InputStream xsdStream = null;

    if (null != xsdName) {
      if (logXsd) {
        LOG.info("Processing " + file.getAbsolutePath() + " with " + xsdName);
      }
      if (jaxbSchemas.containsKey(xsdName)) {
        u.setSchema(jaxbSchemas.get(xsdName));
      } else {

        xsdStream = clz.getClassLoader().getResourceAsStream(xsdName);

        if (null != xsdStream) {
          SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
          Schema schema = factory.newSchema(new StreamSource(xsdStream));

          u.setSchema(schema);

          jaxbSchemas.put(xsdName, schema);
        } else if (logXsd) {
          LOG.info("Schema '" + xsdName + "' for " + file.getAbsolutePath() + " was not found, ignoring");
        }
      }
    } else if (logXsd) {
      LOG.info("NOT processing " + file.getAbsolutePath() + "; there is no XSD");
    }

    try {
      return clz.cast(u.unmarshal(file));
    } catch (Exception unmarshalException) {

      Throwable cause = ExceptionUtils.getRootCause(unmarshalException);

      LOG.error("Cannot parse {}", file.getAbsolutePath());
      if (null != cause) {
        LOG.error(cause.getMessage(), cause);
      }

      throw unmarshalException;
    } finally {
      IOUtils.closeQuietly(xsdStream);
    }
  }

  // statically register the JAXB contexts
  static {
    try {
      // three classes define the top-level element "metainfo", so we need 3 contexts for them
      JAXBContext ctx = JAXBContext.newInstance(StackMetainfoXml.class, RepositoryXml.class,
          ConfigurationXml.class, UpgradePack.class, ConfigUpgradePack.class);

      jaxbContexts.put(StackMetainfoXml.class, ctx);
      jaxbContexts.put(RepositoryXml.class, ctx);
      jaxbContexts.put(ConfigurationXml.class, ctx);
      jaxbContexts.put(UpgradePack.class, ctx);
      jaxbContexts.put(ConfigUpgradePack.class, ctx);
      jaxbContexts.put(ServiceMetainfoXml.class, JAXBContext.newInstance(ServiceMetainfoXml.class));
      jaxbContexts.put(ExtensionMetainfoXml.class, JAXBContext.newInstance(ExtensionMetainfoXml.class));
    } catch (JAXBException e) {
      throw new RuntimeException (e);
    }
  }
}
