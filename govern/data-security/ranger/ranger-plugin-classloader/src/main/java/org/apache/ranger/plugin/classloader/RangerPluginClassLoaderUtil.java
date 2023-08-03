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

package org.apache.ranger.plugin.classloader;


import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerPluginClassLoaderUtil {

	private static final Logger LOG = LoggerFactory.getLogger(RangerPluginClassLoaderUtil.class);

	private static volatile RangerPluginClassLoaderUtil config   = null;
	private static String rangerPluginLibDir			= "ranger-%-plugin-impl";
	
	public static RangerPluginClassLoaderUtil getInstance() {
		RangerPluginClassLoaderUtil result = config;
		if (result == null) {
			synchronized (RangerPluginClassLoaderUtil.class) {
				result = config;
				if (result == null) {
					config = result = new RangerPluginClassLoaderUtil();
				}
			}
		}
		return result;
	}

	
	public URL[]  getPluginFilesForServiceTypeAndPluginclass( String serviceType, Class<?> pluginClass) throws Exception {
		
		URL[] ret = null;
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPluginClassLoaderUtil.getPluginFilesForServiceTypeAndPluginclass(" + serviceType + ")" + " Pluging Class :" +  pluginClass.getName());
		}

		String[] libDirs = new String[] { getPluginImplLibPath(serviceType, pluginClass) };
		
		ret = getPluginFiles(libDirs);
		
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPluginClassLoaderUtil.getPluginFilesForServiceTypeAndPluginclass(" + serviceType + ")" + " Pluging Class :" +  pluginClass.getName());
		}

		return ret;
		
	}

	private  URL[] getPluginFiles(String[] libDirs) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPluginClassLoaderUtil.getPluginFiles()");
		}

		List<URL> ret = new ArrayList<URL>();
		for ( String libDir : libDirs) {
			getFilesInDirectory(libDir,ret);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPluginClassLoaderUtil.getPluginFilesForServiceType(): " + ret.size() + " files");
		}

		return ret.toArray(new URL[] { });
	}

	private  void getFilesInDirectory(String dirPath, List<URL> files) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPluginClassLoaderUtil.getPluginFiles()");
		}
		
		if ( dirPath != null) {
			try {
				
				File[] dirFiles = new File(dirPath).listFiles();

				if(dirFiles != null) {
					for(File dirFile : dirFiles) {
						try {
							if (!dirFile.canRead()) {
								LOG.error("getFilesInDirectory('" + dirPath + "'): " + dirFile.getAbsolutePath() + " is not readable!");
							}

							URL jarPath = dirFile.toURI().toURL();

							LOG.info("getFilesInDirectory('" + dirPath + "'): adding " + dirFile.getAbsolutePath());

							files.add(jarPath);
						} catch(Exception excp) {
							LOG.warn("getFilesInDirectory('" + dirPath + "'): failed to get URI for file " + dirFile.getAbsolutePath(), excp);
						}
					}
				}
			} catch(Exception excp) {
				LOG.warn("getFilesInDirectory('" + dirPath + "'): error", excp);
			}
		} else {
				LOG.warn("getFilesInDirectory('" + dirPath + "'): could not find directory in path " + dirPath);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPluginClassLoaderUtil.getFilesInDirectory(" + dirPath + ")");
		}
	}
	
    private String getPluginImplLibPath(String serviceType, Class<?> pluginClass) throws Exception {

       String ret = null;

       if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPluginClassLoaderUtil.getPluginImplLibPath for Class (" + pluginClass.getName() + ")");
		}

	   URI uri = pluginClass.getProtectionDomain().getCodeSource().getLocation().toURI();
	
	   Path  path = Paths.get(URI.create(uri.toString()));

	   ret = path.getParent().toString() + File.separatorChar + rangerPluginLibDir.replaceAll("%", serviceType);
	
	   if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPluginClassLoaderUtil.getPluginImplLibPath for Class (" + pluginClass.getName() + " PATH :" + ret + ")");
		}
	
	   return ret;
    }
}
