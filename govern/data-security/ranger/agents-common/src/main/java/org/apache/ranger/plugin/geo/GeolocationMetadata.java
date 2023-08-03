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

package org.apache.ranger.plugin.geo;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeolocationMetadata {
	private static final Logger LOG = LoggerFactory.getLogger(GeolocationMetadata.class);

	private String[] locationDataItemNames = new String[0];

	public static GeolocationMetadata create(String fields[], int index) {
		GeolocationMetadata ret = null;

		if (fields.length > 2) {
			String[] metadataNames = new String[fields.length-2];

			for (int i = 2; i < fields.length; i++) {
				metadataNames[i-2] = fields[i];
			}
			ret = new GeolocationMetadata(metadataNames);
		} else {
			LOG.error("GeolocationMetadata.createMetadata() - Not enough fields specified, need {start, end, location} at " + index);
		}

		return ret;
	}

	GeolocationMetadata() {}

	GeolocationMetadata(final String[] locationDataItemNames) {
		this.locationDataItemNames = locationDataItemNames;
	}

	public int getDataItemNameIndex(final String dataItemName) {
		int ret = -1;

		if (!StringUtils.isBlank(dataItemName)) {
			for (int i = 0; i < locationDataItemNames.length; i++) {
				if (locationDataItemNames[i].equals(dataItemName)) {
					ret = i;
					break;
				}
			}
		}

		return ret;
	}

	public String[] getLocationDataItemNames() {
		return locationDataItemNames;
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toStringDump(sb);

		return sb.toString();
	}

	private StringBuilder toStringDump(StringBuilder sb) {
		sb.append("FROM_IP,TO_IP,");

		for (String locationDataItemName : locationDataItemNames) {
			sb.append(locationDataItemName).append(", ");
		}

		return sb;
	}
}
