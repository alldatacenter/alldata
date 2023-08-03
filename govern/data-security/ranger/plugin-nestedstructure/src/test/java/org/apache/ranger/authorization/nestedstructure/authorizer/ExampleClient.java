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


import java.util.Set;

public class ExampleClient {
    public static void main( String[] args) {
        String      schema     = "json_object.foo.v1";
        String      userName   = "someuser";
        Set<String> userGroups = null;
        String      jsonString = "{\n" +
                "    \"foo\": \"12345678\",\n" +
                "    \"address\": {\n" +
                "      \"city\": \"philadelphia\" \n" +
                "    },\n" +
                "    \"bar\": \"snafu\" \n" +
                "}\n";

        AccessResult result = NestedStructureAuthorizer.getInstance().authorize(schema, userName, userGroups, jsonString, NestedStructureAccessType.READ);

        System.out.println("has errors: "+ result.hasErrors());
        System.out.println("hasAccess: "+ result.hasAccess());
        System.out.println("authorizedJson: "+ result.getJson());

        result.getErrors().stream().forEach(e-> e.printStackTrace());

        System.out.println("done");
    }
}
