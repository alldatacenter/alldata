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

package org.apache.ranger.plugin.store.file;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.geo.GeolocationMetadata;
import org.apache.ranger.plugin.store.GeolocationStore;
import org.apache.ranger.plugin.geo.RangerGeolocationDatabase;
import org.apache.ranger.plugin.geo.RangerGeolocationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class GeolocationFileStore implements GeolocationStore {
	private static final Logger LOG = LoggerFactory.getLogger(GeolocationFileStore.class);

	public static final String GeoLineCommentIdentifier = "#";
	public static final Character GeoFieldsSeparator = ',';

	public static final String PROP_GEOLOCATION_FILE_LOCATION = "FilePath";
	public static final String PROP_GEOLOCATION_FILE_REINIT = "ForceRead";
	public static final String PROP_GEOLOCATION_IP_IN_DOT_FORMAT = "IPInDotFormat";

	private static Map<String, RangerGeolocationDatabase> geolocationDBMap = new HashMap<>();

	private RangerGeolocationDatabase geolocationDatabase;

	private boolean isMetalineProcessed;
	private boolean useDotFormat;

	@Override
	public void init(final Map<String, String> context) {

		String filePathToGeolocationFile = context.get(PROP_GEOLOCATION_FILE_LOCATION);

		if (StringUtils.isBlank(filePathToGeolocationFile)) {
			filePathToGeolocationFile = "/etc/ranger/data/geo.txt";
		}

		String reinit = context.get(PROP_GEOLOCATION_FILE_REINIT);
		boolean reinitialize = reinit == null || Boolean.parseBoolean(reinit);

		String ipInDotFormat = context.get(PROP_GEOLOCATION_IP_IN_DOT_FORMAT);
		useDotFormat = ipInDotFormat == null || Boolean.parseBoolean(ipInDotFormat);


		if (LOG.isDebugEnabled()) {
			LOG.debug("GeolocationFileStore.init() - Geolocation file location=" + filePathToGeolocationFile);
			LOG.debug("GeolocationFileStore.init() - Reinitialize flag =" + reinitialize);
			LOG.debug("GeolocationFileStore.init() - UseDotFormat flag =" + useDotFormat);
		}

		RangerGeolocationDatabase database = geolocationDBMap.get(filePathToGeolocationFile);

		if (database == null || reinitialize) {
			RangerGeolocationDatabase newDatabase = build(filePathToGeolocationFile);
			if (newDatabase != null) {
				geolocationDBMap.put(filePathToGeolocationFile, newDatabase);
				database = newDatabase;
			} else {
				LOG.error("GeolocationFileStore.init() - Could not build database. Using old database if present.");
			}
		}
		geolocationDatabase = database;

		if (geolocationDatabase == null) {
			LOG.error("GeolocationFileStore.init() - Cannot build Geolocation database from file " + filePathToGeolocationFile);
		}

	}

	@Override
	public RangerGeolocationDatabase getGeoDatabase() {
		return geolocationDatabase;
	}

	@Override
	public final RangerGeolocationData getGeoLocation(final String ipAddress) {
		RangerGeolocationData ret = null;

		RangerGeolocationDatabase database = geolocationDatabase;		// init() may get called when getGeolocation is half-executed

		if (database != null) {

			long start = 0L, end = 0L;

			start = System.currentTimeMillis();
			ret = database.find(ipAddress);
			end = System.currentTimeMillis();

			if (LOG.isDebugEnabled()) {
				if (ret == null) {
					LOG.debug("GeolocationFileStore.getGeolocation() - " + ipAddress + " not found. Search time = " + (end - start) + " milliseconds");
				} else {
					LOG.debug("GeolocationFileStore.getGeolocation() - " + ipAddress + " found. Search time = " + (end - start) + " milliseconds");

					for (String attrName : database.getMetadata().getLocationDataItemNames()) {
						LOG.debug("GeolocationFileStore.getGeolocation() - IPAddress[" + attrName + "]=" + database.getValue(ret, attrName) + ", ");
					}

				}
			}
		} else {
			LOG.error("GeolocationFileStore.getGeolocation() - GeoLocationDatabase is not initialized correctly.");
		}

		return ret;
	}

	private Reader getReader(String dataFileName) throws IOException {
		Reader ret = null;

		File f = new File(dataFileName);

		if(f.exists() && f.canRead()) {
			LOG.info("GeolocationFileStore: reading location data from file '" + dataFileName + "'");

			ret = new FileReader(dataFileName);
		} else {
			InputStream inStr = this.getClass().getResourceAsStream(dataFileName);

			if(inStr != null) {
				LOG.info("GeolocationFileStore: reading location data from resource '" + dataFileName + "'");

				ret = new InputStreamReader(inStr);
			}
		}

		if(ret == null) {
			throw new FileNotFoundException(dataFileName);
		}

		return ret;
	}

	RangerGeolocationDatabase build(String dataFileName) {

		RangerGeolocationDatabase database = null;

		BufferedReader bufferedReader = null;
		long start = 0L, end = 0L;

		start = System.currentTimeMillis();

		try {
			bufferedReader = new BufferedReader(getReader(dataFileName));

			database  = new RangerGeolocationDatabase();

			String line;
			int lineNumber = 0;
			isMetalineProcessed = false;

			while(( line = bufferedReader.readLine()) != null) {
				lineNumber++;
				if (!processLine(lineNumber, line, database)) {
					LOG.error("RangerGeolocationDatabaseBuilder.build() - Invalid geo-specification - " + lineNumber + ":" + line);
					database = null;
					break;
				}
			}

			bufferedReader.close();
			bufferedReader = null;
		}
		catch(FileNotFoundException ex) {
			LOG.error("RangerGeolocationDatabaseBuilder.build() - Unable to open file '" + dataFileName + "'");
		}
		catch(IOException ex) {
			LOG.error("RangerGeolocationDatabaseBuilder.build() - Error reading file '" + dataFileName + "', " + ex);
		}
		finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				}
				catch (Exception exception) {
					// Ignore
				}
			}
		}

		end = System.currentTimeMillis();

		if (LOG.isDebugEnabled()) {
			LOG.debug("RangerGeolocationDatabaseBuilder.build() - Time taken for reading file = " + (end - start) + " milliseconds");
		}

		if (database != null) {
			database.optimize();
		}

		return database;
	}

	private boolean processLine(int lineNumber, String line, RangerGeolocationDatabase database) {

		boolean ret = true;

		line = line.trim();

		if (!line.startsWith(GeoLineCommentIdentifier)) {
			String fields[] = StringUtils.split(line, GeoFieldsSeparator);
			if (fields != null) {
				if (!isMetalineProcessed) {
					GeolocationMetadata metadata = GeolocationMetadata.create(fields, lineNumber);
					if (metadata != null) {
						database.setMetadata(metadata);
						isMetalineProcessed = true;
					} else {
						LOG.error("GeolocationFileStore.processLine() - Invalid metadata specification " + lineNumber + ":" + line);
						ret = false;
					}
				} else {
					RangerGeolocationData data = RangerGeolocationData.create(fields, lineNumber, useDotFormat);
					if (data != null) {
						database.getData().insert(data);
					} else {
						LOG.error("GeolocationFileStore.processLine() - Invalid data specification " + lineNumber + ":" + line);
					}
				}
			} else {
				LOG.error("GeolocationFileStore.processLine() - Invalid line, skipping.." + lineNumber + ":" + line);
			}
		}
		return ret;
	}

}
