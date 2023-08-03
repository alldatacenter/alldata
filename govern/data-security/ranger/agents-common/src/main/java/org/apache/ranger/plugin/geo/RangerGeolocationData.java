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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class RangerGeolocationData implements Comparable<RangerGeolocationData>, RangeChecker<Long> {
	private static final Logger LOG = LoggerFactory.getLogger(RangerGeolocationData.class);

	private static final Character IPSegmentsSeparator = '.';

	private final long fromIPAddress;
	private final long toIPAddress;
	private final String[] locationData;
	private int hash;

	public static RangerGeolocationData create(String fields[], int index, boolean useDotFormat) {

		RangerGeolocationData data = null;

		if (fields.length > 2) {
			String startAddress = fields[0];
			String endAddress = fields[1];

			if (RangerGeolocationData.validateAsIP(startAddress, useDotFormat) && RangerGeolocationData.validateAsIP(endAddress, useDotFormat)) {

				long startIP, endIP;
				if (!useDotFormat) {
					startAddress = RangerGeolocationData.unsignedIntToIPAddress(Long.valueOf(startAddress));
					endAddress = RangerGeolocationData.unsignedIntToIPAddress(Long.valueOf(endAddress));
				}
				startIP = RangerGeolocationData.ipAddressToLong(startAddress);
				endIP = RangerGeolocationData.ipAddressToLong(endAddress);

				if ((endIP - startIP) >= 0) {

					String[] locationData = new String[fields.length-2];
					for (int i = 2; i < fields.length; i++) {
						locationData[i-2] = fields[i];
					}
					data = new RangerGeolocationData(startIP, endIP, locationData);
				}
			}

		} else {
			LOG.error("GeolocationMetadata.createMetadata() - Not enough fields specified, need {start, end, location} at " + index);
		}
		return data;
	}

	private RangerGeolocationData(final long fromIPAddress, final long toIPAddress, final String[] locationData) {
		this.fromIPAddress = fromIPAddress;
		this.toIPAddress = toIPAddress;
		this.locationData = locationData;
	}

	public String[] getLocationData() {
		return locationData;
	}

	@Override
	public int compareTo(final RangerGeolocationData other) {
		int ret = (other == null) ? 1 : 0;
		if (ret == 0) {
			ret = Long.compare(fromIPAddress, other.fromIPAddress);
			if (ret == 0) {
				ret = Long.compare(toIPAddress, other.toIPAddress);
				if (ret == 0) {
					ret = Integer.compare(locationData.length, other.locationData.length);
					for (int i = 0; ret == 0 && i < locationData.length; i++) {
						ret = stringCompareTo(locationData[i], other.locationData[i]);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public boolean equals(Object other) {
		boolean ret = false;
		if (other != null && (other instanceof RangerGeolocationData)) {
			ret = this == other || compareTo((RangerGeolocationData) other) == 0;
		}
		return ret;
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = Objects.hash(fromIPAddress, toIPAddress, locationData);
		}
		return hash;
	}

	@Override
	public int compareToRange(final Long ip) {
		int ret = Long.compare(fromIPAddress, ip.longValue());

		if (ret < 0) {
			ret = Long.compare(toIPAddress, ip.longValue());
			if (ret > 0) {
				ret = 0;
			}
		}

		return ret;
	}

	public static long ipAddressToLong(final String ipAddress) {

		long ret = 0L;

		try {
			byte[] bytes = InetAddress.getByName(ipAddress).getAddress();

			if (bytes != null && bytes.length <= 4) {
				for (int i = 0; i < bytes.length; i++) {
					int val = bytes[i] < 0 ? (256 + bytes[i]) : bytes[i];
					ret += (val << (8 * (3 - i)));
				}
			}
		}
		catch (UnknownHostException exception) {
			LOG.error("RangerGeolocationData.ipAddressToLong() - Invalid IP address " + ipAddress);
		}

		return ret;
	}

	public static String unsignedIntToIPAddress(final long val) {
		if (val <= 0) {
			return "";
		}
		long remaining = val;
		String segments[] = new String[4];
		for (int i = 3; i >= 0; i--) {
			long segment = remaining % 0x100;
			remaining = remaining / 0x100;
			segments[i] = String.valueOf(segment);
		}
		return StringUtils.join(segments, IPSegmentsSeparator);
	}

	public static boolean validateAsIP(String ipAddress, boolean ipInDotNotation) {
		if (!ipInDotNotation) {
			return StringUtils.isNumeric(ipAddress);
		}

		boolean ret = false;

		try {
			// Only to validate to see if ipAddress is in correct format
			InetAddress.getByName(ipAddress).getAddress();
			ret = true;
		}
		catch(UnknownHostException exception) {
			LOG.error("RangerGeolocationData.validateAsIP() - Invalid address " + ipAddress);
		}

		return ret;
	}

	private static int stringCompareTo(String str1, String str2) {
		if(str1 == str2) {
			return 0;
		} else if(str1 == null) {
			return -1;
		} else if(str2 == null) {
			return 1;
		} else {
			return str1.compareTo(str2);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	private StringBuilder toString(StringBuilder sb) {
		sb.append("{")
				.append("from=")
				.append(RangerGeolocationData.unsignedIntToIPAddress(fromIPAddress))
				.append(", to=")
				.append(RangerGeolocationData.unsignedIntToIPAddress(toIPAddress))
				.append(", location={");
			for (String data : locationData) {
				sb.append(data).append(", ");
			}
				sb.append("}");
		sb.append("}");
		return sb;
	}
}
