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

package org.apache.ranger.policyengine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class PerfTesterTest {

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testMain() {

        String[] args = readCommandLine();

        if (args != null) {
            RangerPolicyenginePerfTester.main(args);
        }
    }

    @Test
    public void testArgParsing() {
        String[] args = readCommandLine();

        if (args != null) {
            CommandLineParser commandLineParser = new CommandLineParser();
            PerfTestOptions parseResult = commandLineParser.parse(args);
            Assert.assertNotNull(parseResult);
        }
    }

    String[] readCommandLine() {
        // Read arguments from a file - with hardcoded name 'commandline'

        String[] ret = null;

        URL commandLineFileURL = CommandLineParser.getInputFileURL("/commandline");
        if (commandLineFileURL != null) {
            try (
                    InputStream inStream = commandLineFileURL.openStream();
                    InputStreamReader reader = new InputStreamReader(inStream, Charset.forName("UTF-8"));
                    BufferedReader br = new BufferedReader(reader);
            ) {


                String line;

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        ret = line.split(" ");
                        break;
                    }
                }

            } catch (Exception exception) {
                System.out.println("Error reading arguments:" + exception);
            }
        }
        return ret;
    }

}
