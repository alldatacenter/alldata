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
 * Defines the types of masks that are supported.
 */
public interface MaskTypes {
    String MASK                = "MASK";
    String MASK_SHOW_LAST_4    = "MASK_SHOW_LAST_4";
    String MASK_SHOW_FIRST_4   = "MASK_SHOW_FIRST_4";
    String MASK_HASH           = "MASK_HASH";
    String MASK_NULL           = "MASK_NULL";
    String MASK_NONE           = "MASK_NONE";
    String MASK_DATE_SHOW_YEAR = "MASK_DATE_SHOW_YEAR";
    String CUSTOM              = "CUSTOM";
    int    MIN_MASK_LENGTH     = 5;
    int    MAX_MASK_LENGTH     = 30;

    /**
     * These formats can be masked into yyyy, '2012'
     <table>
     <tr><th>Format</th><th>Description</th><th>Example</th></tr>
     <tr><td>BASIC_ISO_DATE</td><td>Basic ISO date</td><td>'20111203'</td></tr>
     <tr><td>ISO_LOCAL_DATE	ISO</td><td>Local Date</td><td>'2011-12-03'</td></tr>
     <tr><td>ISO_OFFSET_DATE</td><td>ISO Date with offset</td><td>''2011-12-03+01:00'</td></tr>
     <tr><td>ISO_DATE</td><td>ISO Date with or without offset</td><td>'2011-12-03+01:00'; '2011-12-03'</td></tr>
     <tr><td>ISO_LOCAL_DATE_TIME</td><td>ISO Local Date and Time</td><td>'2011-12-03T10:15:30'</td></tr>
     <tr><td>ISO_OFFSET_DATE_TIME</td><td>Date Time with Offset</td><td>'2011-12-03T10:15:30+01:00'</td></tr>
     <tr><td>ISO_ZONED_DATE_TIME</td><td>Zoned Date Time</td><td>'2011-12-03T10:15:30+01:00[Europe/Paris]'</td></tr>
     <tr><td>ISO_DATE_TIME</td><td>Date and time with ZoneId</td><td>'2011-12-03T10:15:30+01:00[Europe/Paris]'</td></tr>
     <tr><td>ISO_ORDINAL_DATE</td><td>Year and day of year/td><td>'2012-337'</td></tr>
     <tr><td>ISO_WEEK_DATE</td><td>Year and Week</td><td>'2012-W48-6'</td></tr>
     <tr><td>ISO_INSTANT</td><td>Date and Time of an Instant</td><td>'2011-12-03T10:15:30Z'</td></tr>
     <tr><td>RFC_1123_DATE_TIME</td><td>RFC 1123 / RFC 822</td><td>'Tue, 3 Jun 2008 11:05:30 GMT'</td></tr>
     </table>
     */
}
