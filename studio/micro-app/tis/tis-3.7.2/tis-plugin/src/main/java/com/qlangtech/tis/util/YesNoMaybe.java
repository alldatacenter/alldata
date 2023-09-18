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

package com.qlangtech.tis.util;

/**
 * Enum that represents {@link Boolean} state (including null for the absence.)
 *
 * <p>
 * This is for situations where we can't use {@link Boolean}, such as annotation elements.
 *
 * @author Kohsuke Kawaguchi
 */
public enum YesNoMaybe {
    YES,
    NO,
    MAYBE;

    public static Boolean toBoolean(YesNoMaybe v) {
        if (v==null)    return null;
        return v.toBool();
    }

    public Boolean toBool() {
        switch (this) {
            case YES:
                return true;
            case NO:
                return false;
            default:
                return null;
        }
    }
}
