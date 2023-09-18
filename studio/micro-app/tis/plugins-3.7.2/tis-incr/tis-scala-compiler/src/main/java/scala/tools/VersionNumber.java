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
package scala.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class VersionNumber implements Comparable<VersionNumber> {

    private static final Pattern _regexp = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?([-\\.].+)?");

    int major;

    int minor;

    int bugfix;

    String modifier;

    public VersionNumber() {
        major = 0;
        minor = 0;
        bugfix = 0;
    }

    public VersionNumber(String s) {
        Matcher match = _regexp.matcher(s);
        if (!match.find()) {
            throw new IllegalArgumentException("invalid versionNumber : major.minor(.bugfix)(modifier) :" + s);
        }
        major = Integer.parseInt(match.group(1));
        minor = Integer.parseInt(match.group(2));
        if ((match.group(3) != null) && (match.group(3).length() > 1)) {
            bugfix = Integer.parseInt(match.group(3).substring(1));
        }
        if ((match.group(4) != null) && (match.group(4).length() > 1)) {
            modifier = match.group(4);
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(major).append('.').append(minor).append('.').append(bugfix);
        if ((modifier != null) && (modifier.length() > 0)) {
            str.append(modifier);
        }
        return str.toString();
    }

    /**
     * Not a commutative compareTo !! Can return 0 for any VersionNumber o that
     * match this version (same defined major, minor, bugfix) undefined part are
     * ignored.
     */
    @Override
    public int compareTo(VersionNumber o) {
        int back = 0;
        if ((back == 0) && (major > o.major)) {
            // lgtm [java/constant-comparison]
            back = 1;
        }
        if ((back == 0) && (major < o.major)) {
            back = -1;
        }
        if ((back == 0) && (minor > o.minor)) {
            back = 1;
        }
        if ((back == 0) && (minor < o.minor)) {
            back = -1;
        }
        if ((back == 0) && (bugfix > o.bugfix)) {
            back = 1;
        }
        if ((back == 0) && (bugfix < o.bugfix)) {
            back = -1;
        }
        return back;
    }

    public boolean isZero() {
        return (major == 0) && (minor == 0) && (bugfix == 0);
    }

    String applyScalaArtifactVersioningScheme(String name) {
        return name + '_' + (modifier == null ? (major + "." + minor) : toString());
    }
}

class VersionNumberMask extends VersionNumber {

    private static final Pattern _regexp = Pattern.compile("(\\d+)(\\.\\d+)?(\\.\\d+)?([-\\.].+)?");

    public VersionNumberMask(String s) {
        Matcher match = _regexp.matcher(s);
        if (!match.find()) {
            throw new IllegalArgumentException("invalid versionNumber : major.minor(.bugfix)(modifier) :" + s);
        }
        major = Integer.parseInt(match.group(1));
        minor = -1;
        if ((match.group(2) != null) && (match.group(2).length() > 1)) {
            minor = Integer.parseInt(match.group(2).substring(1));
        }
        bugfix = -1;
        if ((match.group(3) != null) && (match.group(3).length() > 1)) {
            bugfix = Integer.parseInt(match.group(3).substring(1));
        }
        modifier = null;
        if ((match.group(4) != null) && (match.group(4).length() > 1)) {
            modifier = match.group(4);
        }
    }

    /**
     * Doesn't compare modifier
     */
    @Override
    public int compareTo(VersionNumber o) {
        int back = 0;
        if ((back == 0) && (major > o.major)) {
            // lgtm [java/constant-comparison]
            back = 1;
        }
        if ((back == 0) && (major < o.major)) {
            back = -1;
        }
        if ((back == 0) && (minor > -1) && (minor > o.minor)) {
            back = 1;
        }
        if ((back == 0) && (minor > -1) && (minor < o.minor)) {
            back = -1;
        }
        if ((back == 0) && (bugfix > -1) && (bugfix > o.bugfix)) {
            back = 1;
        }
        if ((back == 0) && (bugfix > -1) && (bugfix < o.bugfix)) {
            back = -1;
        }
        return back;
    }
}
