/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Utils {
	private static final String XML_INDENT_SPACES = "4";
	private static final String XML_INDENT_AMT_PROP_NAME = "{http://xml.apache.org/xslt}indent-amount";
	private final static Logger LOGGER = LoggerFactory
			.getLogger(Utils.class);
	private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	public String formatXml(String xml) {

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			StreamResult result = new StreamResult(new StringWriter());
			Document document = db
					.parse(new InputSource(new StringReader(xml)));
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(XML_INDENT_AMT_PROP_NAME,
					XML_INDENT_SPACES);
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);
			return result.getWriter().toString();
		} catch (ParserConfigurationException | SAXException | IOException
				| TransformerFactoryConfigurationError | TransformerException e) {
			LOGGER.error("Error in formatting xml", e);
			throw new RuntimeException(e);
		}
	}
	public String generateXml(Document doc){
		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(XML_INDENT_AMT_PROP_NAME,
					XML_INDENT_SPACES);
			try {
				transformer.transform(domSource, result);
			} catch (TransformerException e) {
				throw new RuntimeException(e);
			}
			return writer.toString();
		} catch (TransformerConfigurationException tce) {
			throw new RuntimeException(tce);
		}
		
	}

	public Map<String, String> getHeaders(HttpHeaders headers) {
		MultivaluedMap<String, String> requestHeaders = headers
				.getRequestHeaders();
		Set<Entry<String, List<String>>> headerEntrySet = requestHeaders
				.entrySet();
		HashMap<String, String> headersMap = new HashMap<String, String>();
		for (Entry<String, List<String>> headerEntry : headerEntrySet) {
			String key = headerEntry.getKey();
			List<String> values = headerEntry.getValue();
			headersMap.put(key, strJoin(values, ","));
		}
		return headersMap;
	}

	public String strJoin(List<String> strings, String separator) {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0, il = strings.size(); i < il; i++) {
			if (i > 0) {
				stringBuilder.append(separator);
			}
			stringBuilder.append(strings.get(i));
		}
		return stringBuilder.toString();
	}
	public MediaType deduceType(String stringResponse) {
		if (stringResponse.startsWith("{")) {
			return MediaType.APPLICATION_JSON_TYPE;
		} else if (stringResponse.startsWith("<")) {
			return MediaType.TEXT_XML_TYPE;
		} else {
			return MediaType.APPLICATION_JSON_TYPE;
		}
	}

	public String convertParamsToUrl(MultivaluedMap<String, String> parameters) {
		StringBuilder urlBuilder = new StringBuilder();
		boolean firstEntry = true;
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			if (firstEntry) {
				urlBuilder.append("?");
			} else {
				urlBuilder.append("&");
			}
			boolean firstVal = true;
			for (String val : entry.getValue()) {
				urlBuilder.append(firstVal ? "" : "&").append(entry.getKey())
						.append("=").append(val);
				firstVal = false;
			}
			firstEntry = false;
		}
		return urlBuilder.toString();
	}


  public boolean isXml(String postBody) {
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(postBody));
			Document doc = db.parse(is);
			return true;
		} catch (Exception e) {
			return false;
		}
  }

  public StreamingOutput streamResponse(final InputStream is) {
    return new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
        WebApplicationException {
        BufferedInputStream bis = new BufferedInputStream(is);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        try {
          int data;
          while ((data = bis.read()) != -1) {
            bos.write(data);
          }
          bos.flush();
          is.close();
        } catch (IOException e) {
          LOGGER.error(e.getMessage(), e);
          throw e;
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
          throw new RuntimeException(e);
        } finally {
          bis.close();
        }
      }
    };
  }
}
