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
package org.apache.ranger.credentialapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Testbuildks {

    private String keystoreFile;

    @Before
    public void setup() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        keystoreFile = basedir + File.separator + "target" + File.separator + "testkeystore.jceks";
    }

    @After
    public void cleanup() throws Exception {
        FileUtils.deleteQuietly(new File(keystoreFile));
    }

    @Test
    public void testBuildKSsuccess() throws Exception {
        buildks buildksOBJ = new buildks();
        String[] argsCreateCommand = {"create", "TestCredential1", "-value", "PassworD123", "-provider", "jceks://file@/" + keystoreFile, "","jceks"};
        int rc1 = buildksOBJ.createCredential(argsCreateCommand);
        assertEquals(0, rc1);

        String[] argsListCommand = {"list", "-provider","jceks://file@/" + keystoreFile};
        int rc2=buildksOBJ.listCredential(argsListCommand);
        assertEquals(0, rc2);

        String[] argsGetCommand = {"get", "TestCredential1", "-provider", "jceks://file@/" +keystoreFile };
        System.out.println("Get command = " + Arrays.toString(argsGetCommand));
        String pw = buildksOBJ.getCredential(argsGetCommand);
        assertEquals("PassworD123", pw);
        assertTrue(pw.equals("PassworD123"));
        boolean getCredentialPassed = pw.equals("PassworD123");
        assertTrue(getCredentialPassed);

        String[] argsDeleteCommand = new String[] {"delete", "TestCredential1", "-provider", "jceks://file@/" +keystoreFile };
        boolean isSilentMode = true;

        int rc3 = buildksOBJ.deleteCredential(argsDeleteCommand, isSilentMode);
        assertEquals(0, rc3);
    }

    @Test
    public void testInvalidProvider() throws Exception {
        buildks buildksOBJ = new buildks();
        String[] argsCreateCommand = {"create", "TestCredential1", "-value", "PassworD123", "-provider", "jksp://file@/"+keystoreFile};
        int rc1 = buildksOBJ.createCredential(argsCreateCommand);
        assertEquals(-1, rc1);
    }

    @Test
    public void testInvalidCommand() throws Exception {
        buildks buildksOBJ = new buildks();
        String[] argsCreateCommand = {"creat", "TestCredential1", "-value", "PassworD123", "-provider", "jksp://file@/"+keystoreFile};
        int rc1 = buildksOBJ.createCredential(argsCreateCommand);
        assertEquals(-1, rc1);
    }
}
