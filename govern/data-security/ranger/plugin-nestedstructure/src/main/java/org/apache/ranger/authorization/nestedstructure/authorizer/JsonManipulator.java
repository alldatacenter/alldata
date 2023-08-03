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

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* Accepts a json string, parses it into a {@link DocumentContext}.
 * Individual fields can be updated in the {@link DocumentContext}.
 * And a new json string can be obtained.
**/
public class JsonManipulator {
    /**
    the overall document
     **/
    private final DocumentContext documentContext;

    /**
    a {@link DocumentContext} that specializes in returning field names (not values)
     **/
    private final DocumentContext fieldContextDocument;

    private final Set<String> fields;


    /**
     *
     * @param jsonString json to be parsed and masked
     */
    public JsonManipulator(String jsonString) {
        checkIsValidJson(jsonString);

        documentContext = JsonPath.parse(jsonString);

        //"$..*" - give me everything
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();

        fieldContextDocument = JsonPath.using(conf).parse(jsonString);

        List<String> leafPathList = fieldContextDocument.read("$..*");

        //remove non-leaf nodes from the list
        //if element n starts with element n-1, then n-1 is a leaf node and should be removed
        Collections.sort(leafPathList);

        List<String> filteredList = new ArrayList<>();

        for (int i = 0; i < leafPathList.size(); i++) {
            String current = leafPathList.get(i);

            if ((i + 1) < leafPathList.size()) {
                String next = leafPathList.get(i + 1);

                if (!next.startsWith(current)) {
                    filteredList.add(current);
                }
            } else {
                filteredList.add(current);
            }
        }

        leafPathList = filteredList;

        Stream<String> newList = leafPathList.stream().map(path -> {
            return path.replaceAll("\\[[0-9]+\\]", ".*") //removes "[0]" replaces with .*
                    .replaceAll("\\$\\['", "") //removes "$['"
                    .replaceAll("'\\]\\['", ".") //removes "']['"
                    .replaceAll("\\*\\['", "*.") //removes *['
                    .replaceAll("'\\]", ""); //removes trailing "']"
        });

        fields = newList.collect(Collectors.toSet());
    }

    /**
     *
      * @return The names of all the edge fields in the {@link DocumentContext}.
     * Note that is a value is nested (ie it is of type map) that it is not returned.
     * For example if the full field set was Set(address, address.city, address.street, address.state),
     * only Set(address.city, address.street, address.state) would be returned
     */
    public Set<String> getFields() { return fields; }

    /**
     * Does the actual masking of values.
     * @param fieldAccess
     */
    public void maskFields(List<FieldLevelAccess> fieldAccess){
        Stream<FieldLevelAccess> maskedFields = fieldAccess.stream().filter(fa -> fa.hasAccess && fa.isMasked);

        maskedFields.forEach(fa -> {
            //System.out.println( " attribute " + fa.field + " masked ? " + (fa.isMasked? "yes":"no"));

            getMatchingFields(fa.field).forEach(fullFieldPath -> {
                final Object realValue = documentContext.read(fullFieldPath);
                final Object maskedValue;

                //I know I could use polymorphism to not have different methods
                //but I prefer the readability and the clarity of different method names
                if (realValue instanceof String) {
                    maskedValue = DataMasker.maskString((String)realValue, fa.maskType, fa.customMaskedValue);
                } else if (realValue instanceof Number) {
                    maskedValue = DataMasker.maskNumber((Number)realValue, fa.maskType, fa.customMaskedValue);
                } else if (realValue instanceof Boolean) {
                    maskedValue = DataMasker.maskBoolean((Boolean)realValue, fa.maskType, fa.customMaskedValue);
                } else {
                    throw new MaskingException("unable to determine field type: " + realValue);
                }

                documentContext.set(fullFieldPath, maskedValue);
            });
        });
    }

    /**
     * @return the current/updated json string of the {@link DocumentContext} that is being worked on
     */
    public String getJsonString(){return documentContext.jsonString();}


    /**
     * Used for testing
     * @param fullPath
     * @return the value at a specific path
     */
    String readString(String fullPath){
        return documentContext.read(fullPath).toString();
    }

    private void checkIsValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString); // throws JsonSyntaxException
        } catch (JsonSyntaxException e) {
            throw new MaskingException("invalid input json; unable to mask", e);
        }
    }

    private List<String> getMatchingFields(String fieldPath){
        return fieldContextDocument.read("$."+fieldPath);
    }
}

