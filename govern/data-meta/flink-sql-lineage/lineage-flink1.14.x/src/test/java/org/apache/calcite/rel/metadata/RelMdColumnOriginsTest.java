package org.apache.calcite.rel.metadata;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelMdColumnOriginsTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(RelMdColumnOriginsTest.class);

    /**
     * Test snippet code
     */
    public void testReplaceSourceColumn() {
        Set<String> sourceColumnSet = new LinkedHashSet<>();
        sourceColumnSet.add("name");
        sourceColumnSet.add("age");

        String input = "CONCAT($1, $1, $5)";
        Pattern pattern = Pattern.compile("\\$\\d+");
        Matcher matcher = pattern.matcher(input);

        Set<String> operandSet = new LinkedHashSet<>();
        while (matcher.find()) {
            operandSet.add(matcher.group());
        }

        Map<String, String> sourceColumnMap = new HashMap<>();
        Iterator<String> iterator = sourceColumnSet.iterator();
        operandSet.forEach(e -> sourceColumnMap.put(e, iterator.next()));
        LOG.debug("sourceColumnMap: {}", sourceColumnMap);

        matcher = pattern.matcher(input);
        String temp;
        while (matcher.find()) {
            temp = matcher.group();
            input = input.replace(temp, sourceColumnMap.get(temp));
        }

        assertEquals("CONCAT(name, name, age)", input);
    }
}