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

/**
 * An internal structure.  It notes if a specific field is authorized, is masked and what type of masking it is.
 * If written in scala, this would have been a case class.
 **/
class FieldLevelAccess {
    final String  field;
    final boolean hasAccess;
    final Long    maskPolicyId;
    final boolean isMasked;
    final String  customMaskedValue;
    final String  maskType;

    public FieldLevelAccess(String field, boolean hasAccess, Long maskPolicyId, boolean isMasked,
                            String maskType, String customMaskedValue) {
        this.field             = field;
        this.hasAccess         = hasAccess;
        this.maskPolicyId      = maskPolicyId;
        this.isMasked          = isMasked;
        this.maskType          = maskType;
        this.customMaskedValue = customMaskedValue;
    }

    public String getCustomMaskedValue() {
        return customMaskedValue;
    }

    public String getField() {
        return field;
    }

    public boolean isHasAccess() {
        return hasAccess;
    }

    public Long getMaskPolicyId() {
        return maskPolicyId;
    }

    public boolean isMasked() {
        return isMasked;
    }
}
