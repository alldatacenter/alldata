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

import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;

import java.util.Optional;

public class NestedStructureResource extends RangerAccessResourceImpl {
  public static final String KEY_SCHEMA = "schema";
  public static final String KEY_FIELD  = "field";

  public NestedStructureResource(Optional<String> schema, Optional<String> field) {
        if (schema.isPresent()) {
            setValue(KEY_SCHEMA, schema.get());
        }

        if (field.isPresent()) {
            setValue(KEY_FIELD, field.get());
        }
    }

    public NestedStructureResource(Optional<String> schema) {
        if (schema.isPresent()) {
            setValue(KEY_SCHEMA, schema.get());
        }
    }
}


