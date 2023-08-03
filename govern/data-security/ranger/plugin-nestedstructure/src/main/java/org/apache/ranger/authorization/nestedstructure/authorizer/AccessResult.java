/**
* Copyright 2022 Comcast Cable Communications Management, LLC
*
* Licensed under the Apache License, Version 2.0 (the ""License"");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an ""AS IS"" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or   implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* SPDX-License-Identifier: Apache-2.0
*/

package org.apache.ranger.authorization.nestedstructure.authorizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The response of the API when checking for authorizing and masking fields that are not authorized.
 **/
public class AccessResult {
    private final boolean         hasAccess;
    private final String          json;
    private final List<Exception> errors = new ArrayList<>();

    public AccessResult(boolean hasAccess, String json) {
        this.hasAccess = hasAccess;
        this.json      = json;
    }

    public boolean hasAccess() {
        return hasAccess;
    }

    public String getJson() {
        return json;
    }

    public boolean hasErrors() { return errors.size() > 0; }

    public AccessResult addError(Exception e){
        errors.add(e);

        return this;
    }

    public List<Exception> getErrors() { return errors; }
}
