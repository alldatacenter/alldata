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
package org.apache.drill.exec.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Verifier for execution plans. A handy tool to ensure that the
 * planner produces the expected plan given some set of conditions.
 * <p>
 * The test works by comparing the actual {@code EXPLAIN} output
 * to a "golden" file with the expected ("golden") plan.
 * <p>
 * To create a test, just write it and let it fail due to a missing file.
 * The output will go to the console. Inspect it. If it looks good,
 * copy the plan to the golden file and run again.
 * <p>
 * If comparison fails, you can optionally ask the verifier to write the
 * output to {@code /tmp/drill/test} so you can compare the golden and actual
 * outputs using your favorite diff tool to understand changes. If the changes
 * are expected, use that same IDE to copy changes from the actual
 * to the golden file.
 * <p>
 * The JSON properties of the serialized classes are all controlled
 * to have a fixed order to ensure that files compare across test
 * runs. If you see spurious failures do to changed JSON order, consider
 * adding a {@code @JsonPropertyOrder} tag to enforce a consistent order.
 * <p>
 * Lines can be prefixed with "!" to indicate that they are a regex.
 * The pattern "!!" is treated as a non-regex line that starts with a bang.
 * The pattern "!\!" is a regex that starts with a bang.
 */
public class FileVerifier {

  private final String basePath;

  public FileVerifier(String goldenFileDir) {
    if (!goldenFileDir.startsWith("/")) {
      goldenFileDir = "/" + goldenFileDir;
    }
    if (!goldenFileDir.endsWith("/")) {
      goldenFileDir += "/";
    }
    this.basePath = goldenFileDir;
  }

  public void verifyFileWithResource(File actual, String relativePath) {
    URL url = getClass().getResource(basePath + relativePath);
    assertNotNull("Golden file is missing: " + relativePath, url);
    File resource = new File(url.getPath());
    verifyFiles(actual, resource);
  }

  public void verifyFiles(File actual, File resource) {
    try (Reader expected = new FileReader(resource);
         Reader aReader = new FileReader(actual)) {
      verify(aReader, expected);
    } catch (FileNotFoundException e) {
      fail("Missing resource file: " + resource.getAbsolutePath());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void verify(String actual, File resource) {
    try (Reader expected = new FileReader(resource)) {
      verify(new StringReader(actual), expected);
    } catch (FileNotFoundException e) {
      fail("Missing resource file: " + resource.getAbsolutePath());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private void verify(Reader actualReader, Reader expectedReader) throws IOException {
    try (BufferedReader actual = new BufferedReader(actualReader);
         BufferedReader expected = new BufferedReader(expectedReader);) {
      for (int lineNo = 1; ;lineNo++ ) {
        String aLine = actual.readLine();
        String eLine = expected.readLine();
        if (aLine == null && eLine == null) {
          break;
        }
        if (eLine == null) {
          fail("Too many actual lines");
        }
        if (aLine == null) {
          fail("Missing actual lines");
        }
        compareLines(lineNo, eLine, aLine);
      }
    }
  }

  private void compareLines(int lineNo, String eLine, String aLine) {
    if (eLine.startsWith("!!")) {
      assertEquals("Line: " + lineNo, eLine.substring(1), aLine);
    } else if (eLine.startsWith("!")) {
      compareRegex(lineNo, eLine, aLine);
    } else {
      assertEquals("Line: " + lineNo, eLine, aLine);
    }
  }

  private void compareRegex(int lineNo, String eLine, String aLine) {
    if (eLine.startsWith("!\\!")) {
      eLine = eLine.substring(2);
    } else {
      eLine = eLine.substring(1);
    }
    try {
      Pattern p = Pattern.compile(eLine);
      Matcher m = p.matcher(aLine);
      assertTrue("Pattern match failed at line: " + lineNo, m.matches());
    } catch (PatternSyntaxException e) {
      fail("Bad regex pattern at line " + lineNo + ": " + e.getMessage());
    }
  }
}
