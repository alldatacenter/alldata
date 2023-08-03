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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.nestedstructure.authorizer;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TestRecordFilterJavaScript {

    @Test(expectedExceptions = {MaskingException.class})
    public void testArbitraryCommand(){
        RecordFilterJavaScript.filterRow("user","this.engine.factory.scriptEngine.eval('java.lang.Runtime.getRuntime().exec(\"/Applications/Spotify.app/Contents/MacOS/Spotify\")')",
                TestJsonManipulator.bigTester);
    }

    @Test
    public void testAccessJava() {
        try {
            RecordFilterJavaScript.filterRow("user", "bufferedWriter = new java.io.BufferedWriter(new java.io.FileWriter('omg.txt'));\n" +
                    "            bufferedWriter.write(\"Writing line one to file\"); bufferedWriter.close;", TestJsonManipulator.bigTester);

        } catch (MaskingException e) {
            Assert.assertTrue(e.getCause() instanceof RuntimeException);
            Assert.assertTrue(e.getCause().getCause() instanceof ClassNotFoundException);
        }
        Assert.assertFalse(Files.exists(Paths.get("omg.txt")));
    }

    @AfterTest
    public void deleteTestFile() throws IOException {
        Files.deleteIfExists(Paths.get("omg.txt"));
    }

    @Test
    public void testBasicFilters(){
        Assert.assertEquals(RecordFilterJavaScript.filterRow("user", "jsonAttr.partner.equals('dance')", TestJsonManipulator.testString1), true);
        Assert.assertEquals(RecordFilterJavaScript.filterRow("user", "jsonAttr.address.zipCode.equals('19019')", TestJsonManipulator.testString1), true);
        Assert.assertEquals(RecordFilterJavaScript.filterRow("user", "jsonAttr.aMap.mapNumber > 5", TestJsonManipulator.bigTester), true);

        Assert.assertEquals(RecordFilterJavaScript.filterRow("user", "jsonAttr.partner.equals('cox')", TestJsonManipulator.testString1), false);
    }
}

