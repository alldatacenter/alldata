/**
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
package org.apache.oozie.ambari.view;

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.hdfs.FileOperationService;
import org.apache.ambari.view.commons.hdfs.UploadService;
import org.apache.ambari.view.commons.hdfs.UserService;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;

import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.Map;

public class FileServices {
	public static final String VIEW_CONF_KEYVALUES = "view.conf.keyvalues";

	private ViewContext context;

	public FileServices(ViewContext viewContext) {
		this.context=viewContext;
	}

	/**
	 * @see UploadService
	 * @return service
	 */
	@Path("/upload")
	public UploadService upload() {
		return new UploadService(context, getViewConfigs());
	}

	/**
	 * @see org.apache.ambari.view.commons.hdfs.FileOperationService
	 * @return service
	 */
	@Path("/fileops")
	public FileOperationService fileOps() {
		return new FileOperationService(context, getViewConfigs());
	}

	/**
	 * @see org.apache.ambari.view.commons.hdfs.UserService
	 * @return service
	 */
	@Path("/user")
	public UserService userService() {
		return new UserService(context, getViewConfigs());
	}

	private Map<String,String> getViewConfigs() {
		Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
		return props.isPresent()? props.get() : new HashMap<String, String>();
	}
}
