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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;

/**
 * Masks the 3 primitive types of json: strings, numbers, and booleans
 * Not all mask types are supported on each type.  For example hashing a boolean doesn't make sense.
 * When masking dates, a subset of the date formats defined in {@link DateTimeFormatter} are allowed.
 * See DataMasker.SUPPORTED_DATE_FORMATS for the list of supported formats.
 */
public class DataMasker {
    /**
     * The default numberic value when masking and no other value is defined.
     */
    static final Number DEFAULT_NUMBER_MASK = new Long(-11111);

    /**
     * The default boolean value when masking and no other value is defined.
     */
    static final Boolean DEFAULT_BOOLEAN_MASK = false;

    /**
     * A list of the supported date formats that can be masked.
     */
    static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATS;

    static {
        SUPPORTED_DATE_FORMATS = Arrays.asList(
                DateTimeFormatter.BASIC_ISO_DATE,
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ISO_OFFSET_DATE,
                DateTimeFormatter.ISO_DATE,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ISO_ORDINAL_DATE,
                DateTimeFormatter.ISO_WEEK_DATE,
                DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.RFC_1123_DATE_TIME
        );
    }

    /**
     * Masks a boolean value if applicable
     * @param value input value
     * @param maskType type of masking
     * @param customMaskValueStr string representation of a boolean if applicable
     * @return the masked value
     */
    public static Boolean maskBoolean(Boolean value, String maskType, String customMaskValueStr) {
        if (maskType == null){
            throw new MaskingException("boolean doesn't support mask type: " + maskType);
        }

        final Boolean ret;

        switch (maskType) {
            case MaskTypes.MASK:
                ret = DEFAULT_BOOLEAN_MASK;
                break;

            case MaskTypes.MASK_NULL:
                // replace with NULL
                ret = null;
                break;

            case MaskTypes.MASK_NONE:
                // noop, same as no mask
                ret = value;
                break;

            case MaskTypes.CUSTOM: {
                Boolean customMaskValue = DEFAULT_BOOLEAN_MASK;

                try {
                    customMaskValue = Boolean.parseBoolean(customMaskValueStr);
                } catch (Exception e) {
                    // ignore
                }

                // already done by the policy
                ret = customMaskValue;
            }
                break;

            default:
                // raise error, error message "unknown mask type"
                throw new MaskingException("boolean doesn't support mask type: " + maskType);
        }

        return ret;
    }

    /**
     * Masks a number value if applicable
     * @param value input value of the number
     * @param maskType type of masking
     * @param customMaskValueStr string representation of a number if application
     * @return the masked value
     */
    public static Number maskNumber(Number value, String maskType, String customMaskValueStr) {
        if (maskType == null){
            throw new MaskingException("number doesn't support mask type: " + maskType);
        }

        final Number ret;

        switch (maskType) {
            case MaskTypes.MASK:
                ret= DEFAULT_NUMBER_MASK;
                break;

            case MaskTypes.MASK_NULL:
                // replace with NULL
                ret = null;
                break;

            case MaskTypes.MASK_NONE:
                // noop, same as no mask
                ret = value;
                break;

            case MaskTypes.CUSTOM: {
                try {
                    ret = Long.parseLong(customMaskValueStr);
                } catch (Exception e) {
                    throw new MaskingException("unable to extract number from custom mask value: " + customMaskValueStr, e);
                }
            }
                break;

            default:
                // raise error, error message "unknown mask type"
                throw new MaskingException("number doesn't support mask type: " + maskType);
        }

        return ret;
    }

    /**
     * Masks a string value if applicable
     * @param value the input value of the string
     * @param maskType type of masking
     * @param customMaskValue string value if using custom masking
     * @return the masked value
     */
    public static String maskString(String value, String maskType, String customMaskValue) {
        if (maskType == null){
            throw new MaskingException("string doesn't support mask type: " + maskType);
        }

        final String ret;

        switch (maskType) {
            case MaskTypes.MASK:
                ret = generateMask(value);
                break;

            case MaskTypes.MASK_SHOW_LAST_4:
                // "Show last 4 characters; replace rest with 'x'"
                ret = showLastFour(value);
                break;

            case MaskTypes.MASK_SHOW_FIRST_4:
                // "Show first 4 characters; replace rest with 'x'",
                ret = showFirstFour(value);
                break;

            case MaskTypes.MASK_HASH:
                // "Hash the value"
                //can't hash null, so give it a consistent null string to hash
                ret = DigestUtils.sha256Hex(value == null ? "null" : value);
                break;

            case MaskTypes.MASK_NULL:
                // replace with NULL
                ret = null;
                break;

            case MaskTypes.MASK_NONE:
                // noop, same as no mask
                ret = value;
                break;

            case MaskTypes.MASK_DATE_SHOW_YEAR:
                //"Date: show only year",
                ret = maskYear(value);
                break;

            case MaskTypes.CUSTOM:
                // already done by the policy
                ret = customMaskValue;
                break;

            default:
                // raise error, error message "unknown mask type"
                throw new MaskingException("string doesn't support mask type: " + maskType);
        }

        return ret;
    }

    private static String generateMask(String value) {
        // to do : take an object as param, identify whether it's an array, string or number;
        // number return -1111111111; array : mask each element
        //number of **** will be between MIN and MAX MASK LENGTH
        int maskedValueLen = StringUtils.length(value);

        if (maskedValueLen < MaskTypes.MIN_MASK_LENGTH) {
            maskedValueLen = MaskTypes.MIN_MASK_LENGTH;
        } else if (maskedValueLen > MaskTypes.MAX_MASK_LENGTH) {
            maskedValueLen = MaskTypes.MAX_MASK_LENGTH;
        }

        return StringUtils.repeat("*", maskedValueLen);
    }

    private static String showLastFour(String value){
        int length = StringUtils.length(value);

        return length <= 4 ? value : StringUtils.repeat("x", length - 4) + value.substring(length - 4);
    }

    private static String showFirstFour(String value){
        int length = StringUtils.length(value);

        return length <= 4 ? value : value.substring(0, 4) + StringUtils.repeat("x", length - 4);
    }

    private static String maskYear(String value){
        String ret = null;

        if (StringUtils.isEmpty(value)) {
            ret = value;
        } else {
            for (DateTimeFormatter dateFormat : SUPPORTED_DATE_FORMATS) {
                try {
                    TemporalAccessor temporalAccessor = dateFormat.parse(value);
                    LocalDate        localDateTime    = LocalDate.from(temporalAccessor);

                    ret = localDateTime.format(DateTimeFormatter.ofPattern("yyyy"));

                    break;
                } catch (Exception e) {
                    // ignore
                }
            }

            if (ret == null) {
                throw new MaskingException("Unable to mask year, unsupported date format: " + value +
                        ". See documentation for supported date formats.");
            }
        }

        return ret;
    }
}
