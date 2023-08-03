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

package org.apache.ranger.tagsync.model;

import com.google.gson.Gson;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract  class AbstractTagSource implements TagSource {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractTagSource.class);
	private TagSink tagSink;
	private String name;

	@Override
	public void setTagSink(TagSink sink) {
		if (sink == null) {
			LOG.error("Sink is null!!!");
		} else {
			this.tagSink = sink;
		}
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString( ) {
		return this.name;
	}

	protected void updateSink(final ServiceTags toUpload) throws Exception {
		if (toUpload == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No ServiceTags to upload");
			}
		} else {
			if (LOG.isDebugEnabled()) {
				String toUploadJSON = new Gson().toJson(toUpload);
				LOG.debug("Uploading serviceTags=" + toUploadJSON);
			}

			try {
				ServiceTags uploaded = tagSink.upload(toUpload);

				if (LOG.isDebugEnabled()) {
					String uploadedJSON = new Gson().toJson(uploaded);
					LOG.debug("Uploaded serviceTags=" + uploadedJSON);
				}
			} catch (Exception exception) {
				String toUploadJSON = new Gson().toJson(toUpload);
				LOG.error("Failed to upload serviceTags: " + toUploadJSON);
				LOG.error("Exception : ", exception);
				throw exception;
			}
		}
	}

}
