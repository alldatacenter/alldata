/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
// */
package com.qlangtech.tis.fullbuild.indexbuild;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public enum LuceneVersion {

    LUCENE_6, LUCENE_5, LUCENE_7;

    private LuceneVersion() {
    }

    public String getKey() {
        return this.getTemplateIndexName();
    }

    public String getTemplateIndexName() {
        return "search4template";
    }
}
//
// import java.text.ParseException;
// import java.util.Locale;
//
// /* *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
// public enum LuceneVersion {
//
// //
// LUCENE_6("lucene6", "search4template6", Version.LUCENE_6_0_0),
// LUCENE_5("lucene5.3", "search4template", Version.LUCENE_5_3_0);
//
// private final String templateIndexName;
//
// private final String key;
//
// private final Version version;
//
// // org.apache.lucene.util.Version
// private LuceneVersion(String key, String templateIndexName, Version version) {
// this.key = key;
// this.templateIndexName = templateIndexName;
// this.version = version;
// }
//
// public String getTemplateIndexName() {
// return templateIndexName;
// }
//
// public Version getVersion() {
// return this.version;
// }
//
// public String getKey() {
// return this.key;
// }
//
// public static LuceneVersion parse(String key) {
// if (LUCENE_6.key.equalsIgnoreCase(key)) {
// return LuceneVersion.LUCENE_6;
// }
// if (LUCENE_5.key.equalsIgnoreCase(key)) {
// return LuceneVersion.LUCENE_5;
// }
// throw new IllegalStateException("key:" + key + " is not illegal");
// }
//
// public static LuceneVersion parseByVersionNumber(String version) {
// try {
// Version targetVer = Version.parse(version);
// for (LuceneVersion v : LuceneVersion.values()) {
// if (v.version.equals(targetVer)) {
// return v;
// }
// }
// } catch (ParseException e) {
// throw new RuntimeException(e);
// }
// throw new IllegalStateException("target version:" + version + " is not illegal");
// }
//
// public static final class Version {
//
// /**
// * Match settings and bugs in Lucene's 5.0 release.
// *
// * @deprecated (5.0) Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_0_0 = new Version(5, 0, 0);
//
// /**
// * Match settings and bugs in Lucene's 5.1.0 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_1_0 = new Version(5, 1, 0);
//
// /**
// * Match settings and bugs in Lucene's 5.2.0 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_2_0 = new Version(5, 2, 0);
//
// /**
// * Match settings and bugs in Lucene's 5.2.1 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_2_1 = new Version(5, 2, 1);
//
// /**
// * Match settings and bugs in Lucene's 5.3.0 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_3_0 = new Version(5, 3, 0);
//
// /**
// * Match settings and bugs in Lucene's 5.3.1 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_3_1 = new Version(5, 3, 1);
//
// /**
// * Match settings and bugs in Lucene's 5.3.2 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_3_2 = new Version(5, 3, 2);
//
// /**
// * Match settings and bugs in Lucene's 5.4.0 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_4_0 = new Version(5, 4, 0);
//
// /**
// * Match settings and bugs in Lucene's 5.4.1 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_4_1 = new Version(5, 4, 1);
//
// /**
// * Match settings and bugs in Lucene's 5.5.0 release.
// *
// * @deprecated Use latest
// */
// @Deprecated
// public static final Version LUCENE_5_5_0 = new Version(5, 5, 0);
//
// /**
// * Match settings and bugs in Lucene's 6.0 release.
// * <p>
// * Use this to get the latest &amp; greatest settings, bug
// * fixes, etc, for Lucene.
// */
// public static final Version LUCENE_6_0_0 = new Version(6, 0, 0);
//
// // To add a new version:
// // * Only add above this comment
// // * If the new version is the newest, change LATEST below and deprecate the previous LATEST
// /**
// * <p><b>WARNING</b>: if you use this setting, and then
// * upgrade to a newer release of Lucene, sizable changes
// * may happen.  If backwards compatibility is important
// * then you should instead explicitly specify an actual
// * version.
// * <p>
// * If you use this constant then you  may need to
// * <b>re-index all of your documents</b> when upgrading
// * Lucene, as the way text is indexed may have changed.
// * Additionally, you may need to <b>re-test your entire
// * application</b> to ensure it behaves as expected, as
// * some defaults may have changed and may break functionality
// * in your application.
// */
// public static final Version LATEST = LUCENE_6_0_0;
//
// /**
// * Constant for backwards compatibility.
// *
// * @deprecated Use {@link #LATEST}
// */
// @Deprecated
// public static final Version LUCENE_CURRENT = LATEST;
//
// /**
// * Parse a version number of the form {@code "major.minor.bugfix.prerelease"}.
// * <p>
// * Part {@code ".bugfix"} and part {@code ".prerelease"} are optional.
// * Note that this is forwards compatible: the parsed version does not have to exist as
// * a constant.
// *
// * @lucene.internal
// */
// public static Version parse(String version) throws ParseException {
// StrictStringTokenizer tokens = new StrictStringTokenizer(version, '.');
// if (tokens.hasMoreTokens() == false) {
// throw new ParseException("Version is not in form major.minor.bugfix(.prerelease) (got: " + version + ")", 0);
// }
// int major;
// String token = tokens.nextToken();
// try {
// major = Integer.parseInt(token);
// } catch (NumberFormatException nfe) {
// ParseException p = new ParseException("Failed to parse major version from \"" + token + "\" (got: " + version + ")", 0);
// p.initCause(nfe);
// throw p;
// }
// if (tokens.hasMoreTokens() == false) {
// throw new ParseException("Version is not in form major.minor.bugfix(.prerelease) (got: " + version + ")", 0);
// }
// int minor;
// token = tokens.nextToken();
// try {
// minor = Integer.parseInt(token);
// } catch (NumberFormatException nfe) {
// ParseException p = new ParseException("Failed to parse minor version from \"" + token + "\" (got: " + version + ")", 0);
// p.initCause(nfe);
// throw p;
// }
// int bugfix = 0;
// int prerelease = 0;
// if (tokens.hasMoreTokens()) {
// token = tokens.nextToken();
// try {
// bugfix = Integer.parseInt(token);
// } catch (NumberFormatException nfe) {
// ParseException p = new ParseException("Failed to parse bugfix version from \"" + token + "\" (got: " + version + ")", 0);
// p.initCause(nfe);
// throw p;
// }
// if (tokens.hasMoreTokens()) {
// token = tokens.nextToken();
// try {
// prerelease = Integer.parseInt(token);
// } catch (NumberFormatException nfe) {
// ParseException p = new ParseException("Failed to parse prerelease version from \"" + token + "\" (got: " + version + ")", 0);
// p.initCause(nfe);
// throw p;
// }
// if (prerelease == 0) {
// throw new ParseException("Invalid value " + prerelease + " for prerelease; should be 1 or 2 (got: " + version + ")", 0);
// }
// if (tokens.hasMoreTokens()) {
// // Too many tokens!
// throw new ParseException("Version is not in form major.minor.bugfix(.prerelease) (got: " + version + ")", 0);
// }
// }
// }
// try {
// return new Version(major, minor, bugfix, prerelease);
// } catch (IllegalArgumentException iae) {
// ParseException pe = new ParseException("failed to parse version string \"" + version + "\": " + iae.getMessage(), 0);
// pe.initCause(iae);
// throw pe;
// }
// }
//
// /**
// * Parse the given version number as a constant or dot based version.
// * <p>This method allows to use {@code "LUCENE_X_Y"} constant names,
// * or version numbers in the format {@code "x.y.z"}.
// *
// * @lucene.internal
// */
// public static Version parseLeniently(String version) throws ParseException {
// String versionOrig = version;
// version = version.toUpperCase(Locale.ROOT);
// switch(version) {
// case "LATEST":
// case "LUCENE_CURRENT":
// return LATEST;
// default:
// version = version.replaceFirst("^LUCENE_(\\d+)_(\\d+)_(\\d+)$", "$1.$2.$3").replaceFirst("^LUCENE_(\\d+)_(\\d+)$", "$1.$2.0").replaceFirst("^LUCENE_(\\d)(\\d)$", "$1.$2.0");
// try {
// return parse(version);
// } catch (ParseException pe) {
// ParseException pe2 = new ParseException("failed to parse lenient version string \"" + versionOrig + "\": " + pe.getMessage(), 0);
// pe2.initCause(pe);
// throw pe2;
// }
// }
// }
//
// /**
// * Returns a new version based on raw numbers
// *
// * @lucene.internal
// */
// public static Version fromBits(int major, int minor, int bugfix) {
// return new Version(major, minor, bugfix);
// }
//
// /**
// * Major version, the difference between stable and trunk
// */
// public final int major;
//
// /**
// * Minor version, incremented within the stable branch
// */
// public final int minor;
//
// /**
// * Bugfix number, incremented on release branches
// */
// public final int bugfix;
//
// /**
// * Prerelease version, currently 0 (alpha), 1 (beta), or 2 (final)
// */
// public final int prerelease;
//
// // stores the version pieces, with most significant pieces in high bits
// // ie:  | 1 byte | 1 byte | 1 byte |   2 bits   |
// // major   minor    bugfix   prerelease
// private final int encodedValue;
//
// private Version(int major, int minor, int bugfix) {
// this(major, minor, bugfix, 0);
// }
//
// private Version(int major, int minor, int bugfix, int prerelease) {
// this.major = major;
// this.minor = minor;
// this.bugfix = bugfix;
// this.prerelease = prerelease;
// // make sure it fits in the 8 bits we encode it into:
// if (major > 255 || major < 0) {
// throw new IllegalArgumentException("Illegal major version: " + major);
// }
// if (minor > 255 || minor < 0) {
// throw new IllegalArgumentException("Illegal minor version: " + minor);
// }
// if (bugfix > 255 || bugfix < 0) {
// throw new IllegalArgumentException("Illegal bugfix version: " + bugfix);
// }
// if (prerelease > 2 || prerelease < 0) {
// throw new IllegalArgumentException("Illegal prerelease version: " + prerelease);
// }
// if (prerelease != 0 && (minor != 0 || bugfix != 0)) {
// throw new IllegalArgumentException("Prerelease version only supported with major release (got prerelease: " + prerelease + ", minor: " + minor + ", bugfix: " + bugfix + ")");
// }
// encodedValue = major << 18 | minor << 10 | bugfix << 2 | prerelease;
// assert encodedIsValid();
// }
//
// /**
// * Returns true if this version is the same or after the version from the argument.
// */
// public boolean onOrAfter(Version other) {
// return encodedValue >= other.encodedValue;
// }
//
// @Override
// public String toString() {
// if (prerelease == 0) {
// return "" + major + "." + minor + "." + bugfix;
// }
// return "" + major + "." + minor + "." + bugfix + "." + prerelease;
// }
//
// @Override
// public boolean equals(Object o) {
// return o != null && o instanceof Version && ((Version) o).encodedValue == encodedValue;
// }
//
// // Used only by assert:
// private boolean encodedIsValid() {
// assert major == ((encodedValue >>> 18) & 0xFF);
// assert minor == ((encodedValue >>> 10) & 0xFF);
// assert bugfix == ((encodedValue >>> 2) & 0xFF);
// assert prerelease == (encodedValue & 0x03);
// return true;
// }
//
// @Override
// public int hashCode() {
// return encodedValue;
// }
// }
//
// static final class StrictStringTokenizer {
//
// public StrictStringTokenizer(String s, char delimiter) {
// this.s = s;
// this.delimiter = delimiter;
// }
//
// public final String nextToken() {
// if (pos < 0) {
// throw new IllegalStateException("no more tokens");
// }
// int pos1 = s.indexOf(delimiter, pos);
// String s1;
// if (pos1 >= 0) {
// s1 = s.substring(pos, pos1);
// pos = pos1 + 1;
// } else {
// s1 = s.substring(pos);
// pos = -1;
// }
// return s1;
// }
//
// public final boolean hasMoreTokens() {
// return pos >= 0;
// }
//
// private final String s;
//
// private final char delimiter;
//
// private int pos;
// }
// }
