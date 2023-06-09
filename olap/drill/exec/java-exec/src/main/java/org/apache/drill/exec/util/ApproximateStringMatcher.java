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
package org.apache.drill.exec.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApproximateStringMatcher {
    // From https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance.
    // This function is not performant and should only be used for small lists. It is useful to
    // detect typos in queries entered by the user but not appropriate to do approximate string matching
    // on large data sets
    private static int LevenshteinDistance(final String s0, final String s1) {
        final int len0 = s0.length() + 1;
        final int len1 = s1.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) {
            cost[i] = i;
        }

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {
                // matching current letters in both strings
                final int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            final int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    public static String getBestMatch(final List<String> namesToSearch, final String nameToMatch) {
        final List<Integer> editDistances = new ArrayList<>();
        for (final String name : namesToSearch) {
            final int dist = ApproximateStringMatcher.LevenshteinDistance(nameToMatch, name);
            editDistances.add(dist);
        }
        final int minIndex = editDistances.indexOf(Collections.min(editDistances));
        final String bestMatch = namesToSearch.get(minIndex);
        return bestMatch;
    }
}
