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

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.hdfs.UserService;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;
import org.apache.ambari.view.utils.hdfs.ConfigurationBuilder;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

public class HDFSFileUtils {
	public static final String VIEW_CONF_KEYVALUES = "view.conf.keyvalues";
	private static final String DEFAULT_FS = "fs.defaultFS";
	private static final String AMBARI_SKIP_HOME_DIRECTORY_CHECK_PROTOCOL_LIST = "views.skip.home-directory-check.file-system.list";
	private final static Logger LOGGER = LoggerFactory
			.getLogger(HDFSFileUtils.class);
	private ViewContext viewContext;

	public HDFSFileUtils(ViewContext viewContext) {
		super();
		this.viewContext = viewContext;
	}

	public boolean fileExists(String path) {
		try {
			return getHdfsgetApi().exists(path);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public FSDataInputStream read(String filePath) throws IOException {
		FSDataInputStream is;
		try {
			is = getHdfsgetApi().open(filePath);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return is;
	}

	public String writeToFile(String filePath, String content, boolean overwrite)
			throws IOException {
		FSDataOutputStream fsOut;
		try {
			fsOut = getHdfsgetApi().create(filePath, overwrite);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		fsOut.write(content.getBytes());
		fsOut.close();
		return filePath;
	}

	public void deleteFile(String filePath) throws IOException {
		try {
			getHdfsgetApi().delete(filePath, false);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private HdfsApi getHdfsgetApi() {
		try {
			Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(viewContext, VIEW_CONF_KEYVALUES);
			HdfsApi api;
			if(props.isPresent()){
				api = HdfsUtil.connectToHDFSApi(viewContext, props.get());
			}else{
				api = HdfsUtil.connectToHDFSApi(viewContext);
			}

			return api;
		} catch (Exception ex) {
			LOGGER.error("Error in getting HDFS Api", ex);
			throw new RuntimeException(
					"HdfsApi connection failed. Check \"webhdfs.url\" property",
					ex);
		}
	}

	public Boolean shouldCheckForHomeDir(){
		Optional<Map<String, String>> viewConfigs = ViewPropertyHelper.getViewConfigs(viewContext, VIEW_CONF_KEYVALUES);
		ConfigurationBuilder configBuilder;
		if(viewConfigs.isPresent()) {
			configBuilder = new ConfigurationBuilder(this.viewContext, viewConfigs.get());
		}else{
			configBuilder = new ConfigurationBuilder(this.viewContext);
		}
		Configuration configurations = null;
		try {
			configurations = configBuilder.buildConfig();
		} catch (HdfsApiException e) {
			throw	 new RuntimeException(e);
		}
		String defaultFS = configurations.get(DEFAULT_FS);
		try {
			URI fsUri = new URI(defaultFS);
			String protocol = fsUri.getScheme();
			String ambariSkipCheckValues = viewContext.getAmbariProperty(AMBARI_SKIP_HOME_DIRECTORY_CHECK_PROTOCOL_LIST);
			List<String> protocolSkipList = (ambariSkipCheckValues == null? new LinkedList<String>() : Arrays.asList(ambariSkipCheckValues.split(",")));
			if(null != protocol && protocolSkipList.contains(protocol)){
				return Boolean.FALSE;
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Error occurred while parsing the defaultFS URI.", e);
			return Boolean.TRUE;
		}
		return Boolean.TRUE;
	}

	public FileStatus getFileStatus(String filePath) {
		try {
			return getHdfsgetApi().getFileStatus(filePath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}
	public boolean hdfsCheck()  {
		try {
			getHdfsgetApi().getStatus();
			return true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean homeDirCheck(){
		UserService userservice = new UserService(viewContext, getViewConfigs(viewContext));
		userservice.homeDir();
		return true;
	}
	private Map<String,String> getViewConfigs(ViewContext context) {
		Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
		return props.isPresent()? props.get() : new HashMap<String, String>();
	}
}
