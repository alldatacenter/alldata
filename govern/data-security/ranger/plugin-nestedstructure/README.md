<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# ranger-api-plugin

## License
Licensed under the Apache License, Version 2.0.   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

## Usage

Simply call `NestedStructureAuthorizer.authorize()` to determine if the user has access to the JSON record and fields.
The result will indicate if the user is authorized. The result also contains the JSON string the user is authorized view, for example by masking fields as specified in Apache Ranger policies.
```java
String      schema     = "json_object.cxt.cmt.product.vnull3";
String      userName   = "beckma200";
Set<String> userGroups = new HashSet<>();
String      jsonString = ...;

AccessResult result = NestedStructureAuthorizer.getInstance().authorize(schema, userName, userGroups, jsonString, NestedStructureAccessType.READ);

String authorizedJsonString = result.hasAccess() ? result.getJson() : null;
```

An example client is included at `org.apache.ranger.authorization.nestedstructure.authorizer.ExampleClient`

## Configuration Items
The classpath needs to contain 3 files, `ranger-nestedstructure-audit.xml`,
`ranger-nestedstructure-policymgr-ssl.xml`, and `ranger-nestedstructure-security.xml`.
Each of these files need to edited in each deployment.
Other required files do not need edits and are included in the jar file.

**ranger-nestedstructure-security.xml**
- `ranger.plugin.nestedstructure.policy.rest.url` should be set to the correct audit location (prod vs integration).


**ranger-nestedstructure-audit.xml**
- `xasecure.audit.destination.solr.urls` should be set to the correct audit location (prod vs integration).

**ranger-nestedstructure-policymgr-ssl.xml**
- `xasecure.policymgr.clientssl.keystore` should be set to location of the `ranger-plugin-keystore.p12` file.
- `xasecure.policymgr.clientssl.keystore.credential.file` should be set to the location of `ranger.jceks` file.
- `xasecure.policymgr.clientssl.truststore` should be set to location of the `global-truststore.p12` file.
- `xasecure.policymgr.clientssl.truststore.credential.file` should be set to the location of the `ranger.jceks` file.


## Example of Permissions of Masking Different Fields

```json
{
  "store": {
    "book": [
      {
        "category": "reference",
        "author": "Nigel Rees",
        "title": "Sayings of the Century",
        "price": 8.95
      },
      {
        "category": "fiction",
        "author": "Evelyn Waugh",
        "title": "Sword of Honour",
        "price": 12.99
      },
      {
        "category": "fiction",
        "author": "Herman Melville",
        "title": "Moby Dick",
        "isbn": "0-553-21311-3",
        "price": 8.99
        },
      {
        "category": "fiction",
        "author": "J. R. R. Tolkien",
        "title": "The Lord of the Rings",
        "isbn": "0-395-19395-8",
        "price": 22.99
      }
    ],
    "bicycle": {
      "color": "red",
      "price": 19.95
    }
  },
  "expensive": 10
}
```

##### Arrays
Arrays require the user to specify that all elements of the array should be considered. The addition of an asterisk `*` is required.
To restrict by book price, specify one of the following values in Apache Ranger Policy for resource `field`:

- `store.book[*]price < 100`
- `store.book.*.price < 100`

##### Maps
Simple dot `.` syntax is all that is required.
To restrict the color of the bicycle use in Ranger
`store.bicycle.color`

### Masking
Only primitive types (numbers, booleans, and strings) can be masked.
Elements inside arrays and maps will be masked at a field level.

Note that at this time, masking a container is NOT possible.  Each element has to be individually masked.

If the mask type is not applicable to the datatype, a default mask of `NULL` will be used.

#### Mask Types
* MASK
  * Replaces entire String with `*`.
  * Replaces Number with `-11111`
  * Ensures resulting String has length of 5 of more
  * Replaces Booleans with false
  * Supported types: String, Boolean, Number
* MASK_SHOW_LAST_4
  * Replaces all but the last four characters of a string with `x`
  * Supported types: String
* MASK_SHOW_FIRST_4
  * Replaces all except the first four characters of a string with `x`
  * Supported types: String
* MASK_HASH
  * Replaces string with a SHA256 hash of the string
  * Supported types: String
* CUSTOM
  * Replaces value with a custom specified value of the same type
  * Supported types: String, Boolean, Number
* MASK_NULL
  * Replaces value with `null`
  * Supported types: String, Boolean, Number
* MASK_NONE
  * Returns the value without changing it
  * Supported types: String, Boolean, Number
* MASK_DATE_SHOW_YEAR
  * Replaces a parsable date with only the year parsed from the date.
  * The table below lists the supported date formats.
  * For more information on date formats see [DateFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) documentation.
  * <table>
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

