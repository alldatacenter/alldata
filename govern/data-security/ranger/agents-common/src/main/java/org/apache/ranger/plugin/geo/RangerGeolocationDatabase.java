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

public class RangerGeolocationDatabase {
	private static final Logger LOG = LoggerFactory.getLogger(RangerGeolocationDatabase.class);

	private BinarySearchTree<RangerGeolocationData, Long> data = new BinarySearchTree<>();

	private GeolocationMetadata metadata = new GeolocationMetadata();

	public String getValue(final RangerGeolocationData geolocationData, final String attributeName) {
		String value = null;
		int index = -1;

		if (geolocationData != null && StringUtils.isNotBlank(attributeName)) {
			if ((index = getMetadata().getDataItemNameIndex(attributeName)) != -1) {
				String[] attrValues = geolocationData.getLocationData();
				if (index < attrValues.length) {
					value = attrValues[index];
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("RangerGeolocationDatabase.getValue() - No value specified attribute-name:" + attributeName);
					}
				}
			} else {
				LOG.error("RangerGeolocationDatabase.getValue() - RangerGeolocationDatabase not initialized or Invalid attribute-name:" + attributeName);
			}
		}

		return value;
	}

	public RangerGeolocationData find(final String ipAddressStr) {
		RangerGeolocationData ret = null;

		if (StringUtils.isNotBlank(ipAddressStr) && RangerGeolocationData.validateAsIP(ipAddressStr, true)) {
			ret = data.find(RangerGeolocationData.ipAddressToLong(ipAddressStr));
		}
		return ret;
	}

	public void optimize() {
		long start = 0L, end = 0L;

		start = System.currentTimeMillis();
		data.rebalance();
		end = System.currentTimeMillis();

		if (LOG.isDebugEnabled()) {
			LOG.debug("RangerGeolocationDatabase.optimize() - Time taken for optimizing database = " + (end - start) + " milliseconds");
		}
	}

	public void setData(final BinarySearchTree<RangerGeolocationData, Long> dataArg) { data = dataArg != null ? dataArg : new BinarySearchTree<RangerGeolocationData, Long>();}

	public void setMetadata(final GeolocationMetadata metadataArg) { metadata = metadataArg != null ? metadataArg : new GeolocationMetadata();}

	public GeolocationMetadata getMetadata() { return metadata; }

	public BinarySearchTree<RangerGeolocationData, Long> getData() { return data; }

	public void dump(ValuePrinter<RangerGeolocationData> processor) {

		BinarySearchTree<RangerGeolocationData, Long> geoDatabase = getData();
		GeolocationMetadata metadata = getMetadata();
		processor.build();

		processor.print("#================== Geolocation metadata ==================");
		processor.print(metadata.toString());

		processor.print("#================== Dump of geoDatabase - START ==================");
		geoDatabase.preOrderTraverseTree(processor);
		processor.print("#================== Dump of geoDatabase - END   ==================");

		processor.close();
	}
}
