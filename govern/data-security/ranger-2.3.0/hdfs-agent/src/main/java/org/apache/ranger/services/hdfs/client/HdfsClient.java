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

 package org.apache.ranger.services.hdfs.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import javax.security.auth.Subject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecureClientLogin;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsClient extends BaseClient {

	private static final Logger LOG = LoggerFactory.getLogger(HdfsClient.class);
  private Configuration conf;

	public HdfsClient(String serviceName, Map<String,String> connectionProperties) {
		super(serviceName,connectionProperties, "hdfs-client");
    conf = new Configuration();
    Set<String> rangerInternalPropertyKeys = getConfigHolder().getRangerInternalPropertyKeys();
    for (Map.Entry<String, String> entry: connectionProperties.entrySet())  {
      String key = entry.getKey();
      String value = entry.getValue();
      if (!rangerInternalPropertyKeys.contains(key) && value != null) {
        conf.set(key, value);
      }
    }

	}
	
	private List<String> listFilesInternal(String baseDir, String fileMatching, final List<String> pathList) throws  HadoopException {
		List<String> fileList = new ArrayList<String>();
		String errMsg = " You can still save the repository and start creating "
				+ "policies, but you would not be able to use autocomplete for "
				+ "resource names. Check ranger_admin.log for more info.";
		try {
			String dirPrefix = (baseDir.endsWith("/") ? baseDir : (baseDir + "/"));
			String filterRegEx = null;
			if (fileMatching != null && fileMatching.trim().length() > 0) {
				filterRegEx = fileMatching.trim();
			}
			

			UserGroupInformation.setConfiguration(conf);

			FileSystem fs = null;
			try {
				fs = FileSystem.get(conf);

				Path basePath = new Path(baseDir);
				FileStatus[] fileStatus = fs.listStatus(basePath);

				if(LOG.isDebugEnabled()) {
					LOG.debug("<== HdfsClient fileStatus : " + fileStatus.length + " PathList :" + pathList);
				}

				if (fileStatus != null) {
					if (fs.exists(basePath) && ArrayUtils.isEmpty(fileStatus))  {
						fileList.add(basePath.toString());
					} else {
						for(FileStatus stat : fileStatus) {
								Path path = stat.getPath();
								String pathComponent = path.getName();
								String prefixedPath = dirPrefix + pathComponent;
						        if ( pathList != null && pathList.contains(prefixedPath)) {
                                   continue;
						        }
								if (filterRegEx == null) {
									fileList.add(prefixedPath);
								}
								else if (FilenameUtils.wildcardMatch(pathComponent, fileMatching)) {
									fileList.add(prefixedPath);
								}
							}
					}
				}
			} catch (UnknownHostException uhe) {
				String msgDesc = "listFilesInternal: Unable to connect using given config parameters"
						+ " of Hadoop environment [" + getSerivceName() + "].";
				HadoopException hdpException = new HadoopException(msgDesc, uhe);
				hdpException.generateResponseDataMap(false, getMessage(uhe),
						msgDesc + errMsg, null, null);
				if(LOG.isDebugEnabled()) {
					LOG.debug("<== HdfsClient listFilesInternal Error : " + uhe);
				}
				throw hdpException;
			} catch (FileNotFoundException fne) {
				String msgDesc = "listFilesInternal: Unable to locate files using given config parameters "
						+ "of Hadoop environment [" + getSerivceName() + "].";
				HadoopException hdpException = new HadoopException(msgDesc, fne);
				hdpException.generateResponseDataMap(false, getMessage(fne),
						msgDesc + errMsg, null, null);
				
				if(LOG.isDebugEnabled()) {
					LOG.debug("<== HdfsClient listFilesInternal Error : " + fne);
				}
				
				throw hdpException;
			}
		} catch (IOException ioe) {
			String msgDesc = "listFilesInternal: Unable to get listing of files for directory "
					+ baseDir + fileMatching
					+ "] from Hadoop environment ["
					+ getSerivceName()
					+ "].";
			HadoopException hdpException = new HadoopException(msgDesc, ioe);
			hdpException.generateResponseDataMap(false, getMessage(ioe),
					msgDesc + errMsg, null, null);
			if(LOG.isDebugEnabled()) {
				LOG.debug("<== HdfsClient listFilesInternal Error : " + ioe);
			}
			throw hdpException;

		} catch (IllegalArgumentException iae) {
			String msgDesc = "Unable to get listing of files for directory ["
					+ baseDir + "] from Hadoop environment [" + getSerivceName()
					+ "].";
			HadoopException hdpException = new HadoopException(msgDesc, iae);
			hdpException.generateResponseDataMap(false, getMessage(iae),
					msgDesc + errMsg, null, null);
			if(LOG.isDebugEnabled()) {
				LOG.debug("<== HdfsClient listFilesInternal Error : " + iae);
			}
			throw hdpException;
		}
		return fileList;
	}


	public List<String> listFiles(final String baseDir, final String fileMatching, final List<String> pathList) throws Exception {

		PrivilegedExceptionAction<List<String>> action = new PrivilegedExceptionAction<List<String>>() {
			@Override
			public List<String> run() throws Exception {
				return listFilesInternal(baseDir, fileMatching, pathList);
			}
		};
		return Subject.doAs(getLoginSubject(),action);
	}
	
	public static final void main(String[] args) {
		
		if (args.length < 2) {
			System.err.println("USAGE: java " + HdfsClient.class.getName() + " repositoryName  basedirectory  [filenameToMatch]");
			System.exit(1);
		}
		
		String repositoryName = args[0];
		String baseDir = args[1];
		String fileNameToMatch = (args.length == 2 ? null : args[2]);
		
		HdfsClient fs = new HdfsClient(repositoryName, new HashMap<String,String>());
		List<String> fsList = null;
		try {
			fsList = fs.listFiles(baseDir, fileNameToMatch,null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fsList != null && fsList.size() > 0) {
			for(String s : fsList) {
				System.out.println(s);
			}
		}
		else {
			System.err.println("Unable to get file listing for [" + baseDir + (baseDir.endsWith("/") ? "" : "/") + fileNameToMatch + "]  in repository [" + repositoryName + "]");
		}
	}

	public static Map<String, Object> connectionTest(String serviceName,
			Map<String, String> configs) throws Exception {

	LOG.info("===> HdfsClient.connectionTest()" );
    Map<String, Object> responseData = new HashMap<String, Object>();
    boolean connectivityStatus = false;

    String validateConfigsMsg = null;
    try {
      validateConnectionConfigs(configs);
    } catch (IllegalArgumentException e)  {
      validateConfigsMsg = e.getMessage();
    }

    if (validateConfigsMsg == null) {

		  HdfsClient connectionObj = new HdfsClient(serviceName, configs);
		  if (connectionObj != null) {
			List<String> testResult = null;
			try {
				 testResult = connectionObj.listFiles("/", null,null);
			} catch (HadoopException e) {
				LOG.error("<== HdfsClient.connectionTest() error " + e.getMessage(),e );
					throw e;
			}

			if (testResult != null && testResult.size() != 0) {
			  	connectivityStatus = true;
			}
		}
    }
        String testconnMsg = null;
		if (connectivityStatus) {
			testconnMsg = "ConnectionTest Successful";
			generateResponseDataMap(connectivityStatus, testconnMsg, testconnMsg,
					null, null, responseData);
		} else {
			testconnMsg = "Unable to retrieve any files using given parameters, "
				+ "You can still save the repository and start creating policies, "
				+ "but you would not be able to use autocomplete for resource names. "
				+ "Check ranger_admin.log for more info. ";
      String additionalMsg = (validateConfigsMsg != null)  ?
        validateConfigsMsg : testconnMsg;
			generateResponseDataMap(connectivityStatus, testconnMsg, additionalMsg,
					null, null, responseData);
		}
		LOG.info("<== HdfsClient.connectionTest(): Status " + testconnMsg );
		return responseData;
	}

  public static void validateConnectionConfigs(Map<String, String> configs)
      throws IllegalArgumentException {
	  String lookupPrincipal=null;
	  try{
		  lookupPrincipal = SecureClientLogin.getPrincipal(configs.get("lookupprincipal"), java.net.InetAddress.getLocalHost().getCanonicalHostName());
	  }catch(Exception e){
		  //do nothing
	  }
	  String lookupKeytab = configs.get("lookupkeytab");
	  if(StringUtils.isEmpty(lookupPrincipal) || StringUtils.isEmpty(lookupKeytab)){
		  // username
		  String username = configs.get("username");
		  if ((username == null || username.isEmpty()))  {
			  throw new IllegalArgumentException("Value for username not specified");
		  }

		  // password
		  String password = configs.get("password");
		  if ((password == null || password.isEmpty()))  {
			  throw new IllegalArgumentException("Value for password not specified");
		  }
	  }
	// hadoop.security.authentication
	String authentication = configs.get("hadoop.security.authentication");
	if ((authentication == null || authentication.isEmpty())) {
		throw new IllegalArgumentException("Value for hadoop.security.authentication not specified");
	}

	String fsDefaultName = configs.get("fs.default.name");
	fsDefaultName = (fsDefaultName == null) ? "" : fsDefaultName.trim();
	if (fsDefaultName.isEmpty()) {
		throw new IllegalArgumentException("Value for fs.default.name not specified");
	} else {
		String[] fsDefaultNameElements = fsDefaultName.split(",");
		for (String fsDefaultNameElement : fsDefaultNameElements) {
			if (fsDefaultNameElement.isEmpty()) {
				throw new IllegalArgumentException(
						"Value for " + "fs.default.name element" + fsDefaultNameElement + " not specified");
			}
		}
		if (fsDefaultNameElements != null && fsDefaultNameElements.length >= 2) {
			String cluster = "";
			StringBuilder clusters = new StringBuilder();
			configs.put("dfs.nameservices", "hdfscluster");
			configs.put("fs.default.name", "hdfs://" + configs.get("dfs.nameservices"));
			configs.put("dfs.client.failover.proxy.provider." + configs.get("dfs.nameservices"),
					"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
			for (int i = 0; i < fsDefaultNameElements.length; i++) {
				cluster = "namenode" + (i + 1);
				configs.put("dfs.namenode.rpc-address." + configs.get("dfs.nameservices") + "." + cluster,
						fsDefaultNameElements[i]);
				if (i == (fsDefaultNameElements.length - 1)) {
					clusters.append(cluster);
				} else {
					clusters.append(cluster).append(",");
				}
			}
			configs.put("dfs.ha.namenodes." + configs.get("dfs.nameservices"), clusters.toString());
		}
	}

    String dfsNameservices = configs.get("dfs.nameservices");
    dfsNameservices = (dfsNameservices == null) ? "" : dfsNameservices.trim();
    if (!dfsNameservices.isEmpty()) {
      String[] dfsNameserviceElements = dfsNameservices.split(",");
      for (String dfsNameserviceElement : dfsNameserviceElements)  {
        String proxyProvider = configs.get("dfs.client.failover.proxy.provider." + dfsNameserviceElement);
        proxyProvider =   (proxyProvider == null) ? "" : proxyProvider.trim();
        if (proxyProvider.isEmpty())  {
          throw new IllegalArgumentException("Value for " + "dfs.client.failover.proxy.provider." + dfsNameserviceElement + " not specified");
        }

        String dfsNameNodes = configs.get("dfs.ha.namenodes." + dfsNameserviceElement);
        dfsNameNodes = (dfsNameNodes == null) ? "" : dfsNameNodes.trim();
        if (dfsNameNodes.isEmpty())  {
          throw new IllegalArgumentException("Value for " + "dfs.ha.namenodes." + dfsNameserviceElement + " not specified");
        }
        String[] dfsNameNodeElements = dfsNameNodes.split(",");
        for (String dfsNameNodeElement : dfsNameNodeElements)  {
          String nameNodeUrlKey = "dfs.namenode.rpc-address." +
              dfsNameserviceElement + "." + dfsNameNodeElement.trim();
          String nameNodeUrl =  configs.get(nameNodeUrlKey);
          nameNodeUrl = (nameNodeUrl == null) ? "" : nameNodeUrl.trim();
          if (nameNodeUrl.isEmpty())  {
            throw new IllegalArgumentException("Value for " + nameNodeUrlKey + " not specified");
          }
        }
      }
    }
  }

}
