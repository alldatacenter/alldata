/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.store.http;

import okhttp3.HttpUrl;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.store.http.util.SimpleHttp;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestURLParameters {

  @Test
  public void testUrlParameters() {
    // Http client setup
    HttpUrl githubSingleParam = HttpUrl.parse("https://github.com/orgs/{org}/repos");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    filters.put("org", "apache");
    filters.put("param1", "value1");
    filters.put("param2", "value2");
    assertEquals("https://github.com/orgs/apache/repos", SimpleHttp.mapURLParameters(githubSingleParam, filters));


    HttpUrl githubMultiParam = HttpUrl.parse("https://github.com/orgs/{org}/{repos}");
    CaseInsensitiveMap<String> filters2 = CaseInsensitiveMap.newHashMap();
    filters2.put("org", "apache");
    filters2.put("param1", "value1");
    filters2.put("repos", "drill");
    assertEquals("https://github.com/orgs/apache/drill", SimpleHttp.mapURLParameters(githubMultiParam, filters2));

    HttpUrl githubNoParam = HttpUrl.parse("https://github.com/orgs/org/repos");
    CaseInsensitiveMap<String> filters3 = CaseInsensitiveMap.newHashMap();

    filters3.put("org", "apache");
    filters3.put("param1", "value1");
    filters3.put("repos", "drill");
    assertEquals("https://github.com/orgs/org/repos", SimpleHttp.mapURLParameters(githubNoParam, filters3));
  }

  @Test
  public void testParamAtEnd() {
    HttpUrl pokemonUrl = HttpUrl.parse("https://pokeapi.co/api/v2/pokemon/{pokemon_name}");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    filters.put("pokemon_name", "Misty");
    filters.put("param1", "value1");
    filters.put("repos", "drill");
    assertEquals("https://pokeapi.co/api/v2/pokemon/Misty", SimpleHttp.mapURLParameters(pokemonUrl, filters));
  }

  @Test
  public void testUpperCase() {
    HttpUrl githubSingleParam = HttpUrl.parse("https://github.com/orgs/{ORG}/repos");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    filters.put("org", "apache");
    filters.put("param1", "value1");
    filters.put("param2", "value2");
    assertEquals("https://github.com/orgs/apache/repos", SimpleHttp.mapURLParameters(githubSingleParam, filters));
  }

  @Test
  public void testURLDefaultParameters() {
    HttpUrl githubSingleParam = HttpUrl.parse("https://github.com/orgs/{org=apache}/repos");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    filters.put("param1", "value1");
    filters.put("param2", "value2");
    assertEquals("https://github.com/orgs/apache/repos", SimpleHttp.mapURLParameters(githubSingleParam, filters));
  }

  @Test
  public void testMixedCase() {
    // Since SQL is case-insensitive,
    HttpUrl githubSingleParam = HttpUrl.parse("https://github.com/orgs/{ORG}/{org}/repos");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    filters.put("org", "apache");
    filters.put("ORG", "linux");
    filters.put("param1", "value1");
    filters.put("param2", "value2");
    assertEquals("https://github.com/orgs/linux/linux/repos", SimpleHttp.mapURLParameters(githubSingleParam, filters));
  }

  @Test
  public void testDuplicateParameters() {
    HttpUrl pokemonUrl = HttpUrl.parse("https://pokeapi.co/api/{pokemon_name}/pokemon/{pokemon_name}");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    filters.put("pokemon_name", "Misty");
    filters.put("param1", "value1");
    filters.put("repos", "drill");
    assertEquals("https://pokeapi.co/api/Misty/pokemon/Misty", SimpleHttp.mapURLParameters(pokemonUrl, filters));
  }

  @Test
  public void testDefaultParametersWithDifferentDatatypes() {
    HttpUrl pokemonUrl = HttpUrl.parse("https://pokeapi.co/api/{boolean=true}/{int=1234}");
    CaseInsensitiveMap<String> filters = CaseInsensitiveMap.newHashMap();
    assertEquals("https://pokeapi.co/api/true/1234", SimpleHttp.mapURLParameters(pokemonUrl,filters));
  }

  @Test
  public void testDefaultParameterExtractor() {
    HttpUrl pokemonUrl = HttpUrl.parse("https://pokeapi.co/api/{pokemon_name=Misty}");
    String defaultValue = SimpleHttp.getDefaultParameterValue(pokemonUrl, "pokemon_name");
    assertEquals("Misty", defaultValue);
  }

  @Test
  public void testDefaultParameterExtractorWithBlankDefault() {
    HttpUrl pokemonUrl = HttpUrl.parse("https://pokeapi.co/api/{pokemon_name=}");
    try {
      SimpleHttp.getDefaultParameterValue(pokemonUrl, "pokemon_name");
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("Default URL parameters must have a value."));
    }
  }
}
