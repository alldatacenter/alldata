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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.utils.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlConfigChanger {

	private static final String EMPTY_TOKEN = "%EMPTY%";
	private static final String EMPTY_TOKEN_VALUE = "";

	public static final String ROOT_NODE_NAME = "configuration";
	public static final String NAME_NODE_NAME = "name";
	public static final String PROPERTY_NODE_NAME = "property";
	public static final String VALUE_NODE_NAME = "value";

	private File inpFile;
	private File outFile;
	private File confFile;
	private File propFile;
	
	private Document doc;
	Properties installProperties = new Properties();

	public static void main(String[] args) {
		XmlConfigChanger xmlConfigChanger = new XmlConfigChanger();
		xmlConfigChanger.parseConfig(args);
		try {
			xmlConfigChanger.run();
		}
		catch(Throwable t) {
			System.err.println("*************************************************************************");
			System.err.println("******* ERROR: unable to process xml configuration changes due to error:" + t.getMessage());
			t.printStackTrace();
			System.err.println("*************************************************************************");
			System.exit(1);
		}
	}

	@SuppressWarnings("static-access")
	public void parseConfig(String[] args) {
		
		
		Options options = new Options();

		Option inputOption = OptionBuilder.hasArgs(1).isRequired().withLongOpt("input").withDescription("Input xml file name").create('i');
		options.addOption(inputOption);

		Option outputOption = OptionBuilder.hasArgs(1).isRequired().withLongOpt("output").withDescription("Output xml file name").create('o');
		options.addOption(outputOption);

		Option configOption = OptionBuilder.hasArgs(1).isRequired().withLongOpt("config").withDescription("Config file name").create('c');
		options.addOption(configOption);

		Option installPropOption = OptionBuilder.hasArgs(1).isRequired(false).withLongOpt("installprop").withDescription("install.properties").create('p');
		options.addOption(installPropOption);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			String header = "ERROR: " + e;
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java " + XmlConfigChanger.class.getName(), header, options, null, true);
			throw new RuntimeException(e);
		}

		String inputFileName = cmd.getOptionValue('i');
		this.inpFile = new File(inputFileName);
		if (! this.inpFile.canRead()) {
			String header = "ERROR: Input file [" + this.inpFile.getAbsolutePath() + "] can not be read.";
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java " + XmlConfigChanger.class.getName(), header, options, null, true);
			throw new RuntimeException(header);
		}
		
		String outputFileName = cmd.getOptionValue('o');
		this.outFile = new File(outputFileName);
		if (this.outFile.exists()) {
			String header = "ERROR: Output file [" + this.outFile.getAbsolutePath() + "] already exists. Specify a filepath for creating new output file for the input [" + this.inpFile.getAbsolutePath() + "]";
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java " + XmlConfigChanger.class.getName(), header, options, null, true);
			throw new RuntimeException(header);
		}
		
		String configFileName = cmd.getOptionValue('c');
		this.confFile  = new File(configFileName);
		if (! this.confFile.canRead()) {
			String header = "ERROR: Config file [" + this.confFile.getAbsolutePath() + "] can not be read.";
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java " + XmlConfigChanger.class.getName(), header, options, null, true);
			throw new RuntimeException(header);
		}

		String installPropFileName = (cmd.hasOption('p') ? cmd.getOptionValue('p') : null );
		if (installPropFileName != null) {
			this.propFile = new File(installPropFileName);
			if (! this.propFile.canRead()) {
				String header = "ERROR: Install Property file [" + this.propFile.getAbsolutePath() + "] can not be read.";
				HelpFormatter helpFormatter = new HelpFormatter();
				helpFormatter.printHelp("java " + XmlConfigChanger.class.getName(), header, options, null, true);
				throw new RuntimeException(header);
			}
		}
		
	}

	public void run() throws ParserConfigurationException, SAXException, IOException, TransformerException {
		loadInstallProperties();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc = builder.parse(inpFile);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(confFile));

			String line = null;

			@SuppressWarnings("unused")
			int lineNo = 0;
			Properties variables = new Properties();
			while ((line = reader.readLine()) != null) {
				lineNo++;
				line = line.trim();
				if (line.isEmpty() )
					continue;
				if (line.startsWith("#")) {
					continue;
				}
				if (line.contains("#")) {
					int len = line.indexOf("#");
					line = line.substring(0,len);
				}
				String[] tokens = line.split("\\s+");
				String propName = tokens[0];
				String propValue = null;
				try {
					if (propnameContainsVariables(propName)) {
						propName = replaceProp(propName, variables);
					}
					propValue = replaceProp(tokens[1],installProperties);
				} catch (ValidationException e) {
					// throw new RuntimeException("Unable to replace tokens in the line: \n[" + line + "]\n in file [" + confFile.getAbsolutePath() + "] line number:["  + lineNo + "]" );
					throw new RuntimeException(e);
				}

				String actionType = tokens[2];
				String options = (tokens.length > 3 ? tokens[3] : null);
				boolean createIfNotExists = (options != null && options.contains("create-if-not-exists"));
				
				
				if ("add".equals(actionType)) {
					addProperty(propName, propValue);
				}
				else if ("mod".equals(actionType)) {
					modProperty(propName, propValue,createIfNotExists);
				}
				else if ("del".equals(actionType)) {
					delProperty(propName);
				}
				else if ("append".equals(actionType)) {
					String curVal =  getProperty(propName);
					if (curVal == null) {
						if (createIfNotExists) {
							addProperty(propName, propValue);
						}
					}
					else {
						String appendDelimitor = (tokens.length > 4 ? tokens[4] : " ");
						if (! curVal.contains(propValue)) {
							String newVal = null;
							if (curVal.length() == 0) {
								newVal = propValue;
							}
							else {
								newVal = curVal + appendDelimitor + propValue;
							}
							modProperty(propName, newVal,createIfNotExists);
						}
					}
				}
				else if ("delval".equals(actionType)) {
					String curVal =  getProperty(propName);
					if (curVal != null) {
						String appendDelimitor = (tokens.length > 4 ? tokens[4] : " ");
						if (curVal.contains(propValue)) {
							String[] valTokens = curVal.split(appendDelimitor);
							StringBuilder sb = new StringBuilder();
							for(String v : valTokens) {
								if (! v.equals(propValue)) {
									if (sb.length() > 0) {
										sb.append(appendDelimitor);
									}
									sb.append(v);
								}
							}
							String newVal = sb.toString();
							modProperty(propName, newVal,createIfNotExists);
						}
					}
				}
				else if ("var".equals(actionType)) {
					variables.put(propName, propValue);
				}
				else {
					throw new RuntimeException("Unknown Command Found: [" + actionType + "], Supported Types:  add modify del append");
				}
				
			}
			
			TransformerFactory tfactory = TransformerFactory.newInstance();
			tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
			Transformer transformer = tfactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			
			DOMSource source = new DOMSource(doc);
			FileOutputStream out = new FileOutputStream(outFile);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
			out.close();

		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}

	}

	/**
	 * Check if prop name contains a substitution variable embedded in it, e.g. %VAR_NAME%.
	 * @param propName
	 * @return true if propname contains at least 2 '%' characters in it, else false
	 */
	private boolean propnameContainsVariables(String propName) {

		if (propName != null) {
			int first = propName.indexOf('%');
			if (first != -1) {
				// indexof is safe even if 2nd argument is beyond size of string, i.e. if 1st percent was the last character of the string.
				int second = propName.indexOf('%', first + 1);
				if (second != -1) {
					return true;
				}
			}
		}
		return false;
	}


	private void addProperty(String propName, String val) {
		NodeList nl = doc.getElementsByTagName(ROOT_NODE_NAME);
		Node rootConfig = nl.item(0);
		rootConfig.appendChild(createNewElement(propName,val));
	}

	private void modProperty(String propName, String val, boolean createIfNotExists) {
		Node node = findProperty(propName);
		if (node != null) {
			NodeList cnl = node.getChildNodes();
			for (int j = 0; j < cnl.getLength() ; j++) {
				String nodeName = cnl.item(j).getNodeName();
				if (nodeName.equals(VALUE_NODE_NAME)) {
					if (cnl.item(j).hasChildNodes()) {
						cnl.item(j).getChildNodes().item(0).setNodeValue(val);
					}
					else {
						Node propValueNode = cnl.item(j);
						Node txtNode = doc.createTextNode(val);
						propValueNode.appendChild(txtNode);
						txtNode.setNodeValue(val);
					}
					return;
				}
			}
		}
		if (createIfNotExists) {
			addProperty(propName, val);
		}
	}

	private String getProperty(String propName) {
		String ret = null;
		try {
			Node node = findProperty(propName);
			if (node != null) {
				NodeList cnl = node.getChildNodes();
				for (int j = 0; j < cnl.getLength() ; j++) {
					String nodeName = cnl.item(j).getNodeName();
					if (nodeName.equals(VALUE_NODE_NAME)) {
						Node valueNode = null;
						if (cnl.item(j).hasChildNodes()) {
							valueNode = cnl.item(j).getChildNodes().item(0);
						}
						if (valueNode == null) {	// Value Node is defined with
							ret = "";
						}
						else {
							ret = valueNode.getNodeValue();
						}
						break;
					}
				}
			}
		}
		catch(Throwable t) {
			throw new RuntimeException("getProperty(" + propName + ") failed.", t);
		}
		return ret;
	}

	
	private void delProperty(String propName) {
		Node node = findProperty(propName);
		if (node != null) {
			node.getParentNode().removeChild(node);
		}
	}
	
	
	private Node findProperty(String propName) {
		Node ret = null;
		try {
			NodeList nl = doc.getElementsByTagName(PROPERTY_NODE_NAME);
			
			for(int i = 0; i < nl.getLength() ; i++) {
				NodeList cnl = nl.item(i).getChildNodes();
				boolean found = false;
				for (int j = 0; j < cnl.getLength() ; j++) {
					String nodeName = cnl.item(j).getNodeName();
					if (nodeName.equals(NAME_NODE_NAME)) {
						String pName = cnl.item(j).getChildNodes().item(0).getNodeValue();
						found = pName.equals(propName);
						if (found)
							break;
					}
				}
				if (found) {
					ret = nl.item(i);
					break;
				}
			}
		}
		catch(Throwable t) {
			throw new RuntimeException("findProperty(" + propName + ") failed.", t);
		}
		return ret;
	}
	
	
	private Element createNewElement(String propName, String val) {
		Element ret = null;
		
		try {
			if (doc != null) {
				ret = doc.createElement(PROPERTY_NODE_NAME);
				Node propNameNode  = doc.createElement(NAME_NODE_NAME);
				Node txtNode = doc.createTextNode(propName);
				propNameNode.appendChild(txtNode);
				propNameNode.setNodeValue(propName);
				ret.appendChild(propNameNode);

				Node propValueNode = doc.createElement(VALUE_NODE_NAME);
				txtNode = doc.createTextNode(val);
				propValueNode.appendChild(txtNode);
				propValueNode.setNodeValue(propName);
				ret.appendChild(propValueNode);
			}
		}
		catch(Throwable t) {
			throw new RuntimeException("createNewElement(" + propName + ") with value [" + val + "] failed.", t);
		}

		return ret;
	}

	private void loadInstallProperties() {
		if (propFile != null) {
			try (FileInputStream in = new FileInputStream(propFile)) {
				installProperties.load(in);
			} catch (IOException ioe) {
				System.err.println("******* ERROR: load file failure. The reason: " + ioe.getMessage());
				ioe.printStackTrace();
			}
		}
		// To support environment variable, we will add all environment variables to the Properties
		installProperties.putAll(System.getenv());
	}

	private String replaceProp(String propValue, Properties prop) throws ValidationException  {
			
		StringBuilder tokensb = new StringBuilder();
		StringBuilder retsb = new StringBuilder();
		boolean isToken = false;

		for(char c : propValue.toCharArray()) {
			if (c == '%') {
				if (isToken) {
					String token = tokensb.toString();
					String tokenValue = (token.length() == 0 ? "%" : prop.getProperty(token) );
					if (tokenValue == null  || tokenValue.trim().isEmpty()) {
						throw new ValidationException("ERROR: configuration token [" + token + "] is not defined in the file: [" + (propFile != null ? propFile.getAbsolutePath() : "{no install.properties file specified using -p option}") + "]");
					}
					else {
						if (EMPTY_TOKEN.equals(tokenValue)) {
							retsb.append(EMPTY_TOKEN_VALUE);
						}
						else {
							retsb.append(tokenValue);
						}
					}
					isToken = false;
				}
				else {
					isToken = true;
					tokensb.setLength(0);
				}
			}
			else if (isToken) {
				tokensb.append(String.valueOf(c));
			}
			else {
				retsb.append(String.valueOf(c));
			}
		}
		
		if (isToken) {
			throw new ValidationException("ERROR: configuration has a token defined without end-token [" + propValue + "] in the file: [" + (propFile != null ? propFile.getAbsolutePath() : "{no install.properties file specified using -p option}") + "]");
		}
		
		return retsb.toString();
	}
	
	
	@SuppressWarnings("serial")
	class ValidationException extends Exception {

		public ValidationException(String msg) {
			super(msg);
		}

		public ValidationException(Throwable cause) {
			super(cause);
		}
		
	}
	

}
