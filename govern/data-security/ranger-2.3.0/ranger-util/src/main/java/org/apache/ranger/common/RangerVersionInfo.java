/*
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

package org.apache.ranger.common;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * RangerVersionInfo.
 *
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class RangerVersionInfo {

	private static Package myPackage;
	private static RangerVersionAnnotation version;

	static {
		myPackage = RangerVersionAnnotation.class.getPackage();
		version = myPackage.getAnnotation(RangerVersionAnnotation.class);
	}

	/**
	 * Get the meta-data for the Ranger package.
	 *
	 * @return
	 */
	static Package getPackage() {
		return myPackage;
	}

	/**
	 * Get the Ranger version.
	 *
	 * @return the Ranger version string, eg. "0.6.3-dev"
	 */
	public static String getVersion() {
		return version != null ? version.version() : "Unknown";
	}

	/**
	 * Get the Ranger short version, with major/minor/change version numbers.
	 *
	 * @return short version string, eg. "0.6.3"
	 */
	public static String getShortVersion() {
		return version != null ? version.shortVersion() : "Unknown";
	}

	/**
	 * Get the subversion revision number for the root directory
	 *
	 * @return the revision number, eg. "451451"
	 */
	public static String getRevision() {
		return version != null ? version.revision() : "Unknown";
	}

	/**
	 * Get the branch on which this originated.
	 *
	 * @return The branch name, e.g. "trunk" or "branches/branch-0.20"
	 */
	public static String getBranch() {
		return version != null ? version.branch() : "Unknown";
	}

	/**
	 * The date that Ranger was compiled.
	 *
	 * @return the compilation date in unix date format
	 */
	public static String getDate() {
		return version != null ? version.date() : "Unknown";
	}

	/**
	 * The user that compiled Ranger.
	 *
	 * @return the username of the user
	 */
	public static String getUser() {
		return version != null ? version.user() : "Unknown";
	}

	/**
	 * Get the subversion URL for the root Ranger directory.
	 */
	public static String getUrl() {
		return version != null ? version.url() : "Unknown";
	}

	/**
	 * Get the checksum of the source files from which Ranger was built.
	 **/
	public static String getSrcChecksum() {
		return version != null ? version.srcChecksum() : "Unknown";
	}

	/**
	 * Returns the buildVersion which includes version, revision, user and date.
	 */
	public static String getBuildVersion() {
		return RangerVersionInfo.getVersion() + " from "
				+ RangerVersionInfo.getRevision() + " by "
				+ RangerVersionInfo.getUser() + " source checksum "
				+ RangerVersionInfo.getSrcChecksum();
	}

	public static void main(String[] args) {
		System.out.println("");
		System.out.println("Ranger " + getVersion());
		System.out.println("Subversion " + getUrl() + " -r " + getRevision());
		System.out.println("Compiled by " + getUser() + " on " + getDate());
		System.out.println("From source with checksum " + getSrcChecksum());
	}

}
