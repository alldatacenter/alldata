/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.graphdb.janus;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasJanusGraphIndexClientTest {

    @Test
    public void testGetTopTermsAsendingInput() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 10, 12, 15);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        assertOrder(top5Terms, 2,1,0);
    }

    @Test
    public void testGetTopTermsAsendingInput2() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 10, 12, 15, 20, 25, 26, 30, 40);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        assertOrder(top5Terms, 7, 6, 5, 4, 3);
    }

    @Test
    public void testGetTopTermsDescendingInput() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 10, 9, 8);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        assertOrder(top5Terms, 0, 1, 2);
    }

    @Test
    public void testGetTopTermsDescendingInput2() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 10, 9, 8, 7, 6, 5, 4, 3, 2);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        assertOrder(top5Terms, 0, 1, 2, 3, 4);
    }

    @Test
    public void testGetTopTermsRandom() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 10, 19, 28, 27, 16, 1, 30, 3, 36);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        //10, 19, 28, 27, 16, 1, 30, 3, 36
        //0,  1,  2,   3,  4, 5, 6,  7,  8
        assertOrder(top5Terms, 8, 6, 2, 3, 1);
    }

    @Test
    public void testGetTopTermsRandom2() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 36, 19, 28, 27, 16, 1, 30, 3, 10);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        //36, 19, 28, 27, 16, 1, 30, 3, 10
        //0,  1,  2,   3,  4, 5, 6,  7,  8
        assertOrder(top5Terms, 0, 6, 2, 3, 1);
    }

    @Test
    public void testGetTopTermsRandom3() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 36, 36, 28, 27, 16, 1, 30, 3, 10);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        //36, 36, 28, 27, 16, 1, 30, 3, 10
        //0,  1,  2,   3,  4, 5, 6,  7,  8
        assertOrder(top5Terms, 0, 1, 6, 2, 3);
    }

    @Test
    public void testGetTopTermsRandom4() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 10, 10, 28, 27, 16, 1, 30, 36, 36);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        //10, 10, 28, 27, 16, 1, 30, 36, 36
        //0,  1,  2,   3,  4, 5, 6,  7,  8
        assertOrder(top5Terms, 7, 8, 6, 2, 3);
    }

    @Test
    public void testGetTopTermsRandom5() {
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = generateTerms( 36, 10, 28, 27, 16, 1, 30, 36, 36);
        List<String> top5Terms = AtlasJanusGraphIndexClient.getTopTerms(terms);
        //36, 10, 28, 27, 16, 1, 30, 36, 36
        //0,  1,  2,   3,  4, 5, 6,  7,  8
        assertOrder(top5Terms, 0, 7, 8, 6, 2);
    }

    @Test
    public void testGenerateSuggestionString() {
        List<String> fields = new ArrayList<>();
        fields.add("one");
        fields.add("two");
        fields.add("three");
        String generatedString = AtlasJanusGraphIndexClient.generateSuggestionsString(fields);
        Assert.assertEquals(generatedString, "'one', 'two', 'three'");
    }

    @Test
    public void testGenerateSearchWeightString() {
        Map<String, Integer> fields = new HashMap<>();
        fields.put("one", 10);
        fields.put("two", 1);
        fields.put("three", 15);
        String generatedString = AtlasJanusGraphIndexClient.generateSearchWeightString(fields);
        Assert.assertEquals(generatedString, " one^10 two^1 three^15");
    }

    private void assertOrder(List<String> topTerms, int ... indices) {
        Assert.assertEquals(topTerms.size(), indices.length);
        int i = 0;
        for(String term: topTerms) {
            Assert.assertEquals(Integer.toString(indices[i++]), term);
        }
        Assert.assertEquals(topTerms.size(), indices.length);
    }

    private Map<String, AtlasJanusGraphIndexClient.TermFreq>  generateTerms(int ... termFreqs) {
        int i =0;
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = new HashMap<>();
        for(int count: termFreqs) {
            AtlasJanusGraphIndexClient.TermFreq termFreq1 = new AtlasJanusGraphIndexClient.TermFreq(Integer.toString(i++), count);
            terms.put(termFreq1.getTerm(), termFreq1);
        }
        return terms;
    }
}