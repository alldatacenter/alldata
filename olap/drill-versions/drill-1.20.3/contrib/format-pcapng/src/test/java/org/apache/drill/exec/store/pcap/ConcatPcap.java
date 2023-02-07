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
package org.apache.drill.exec.store.pcap;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Concatenates PCAP files. The only trickiness is that we have to skip the
 * 24 byte header on all but the first file.
 */
public class ConcatPcap {
  public static void main(String[] args) throws IOException {
    try (DataOutputStream out = new DataOutputStream(System.out)) {
      if (args.length > 0) {
        boolean first = true;
        for (String arg : args) {
          try (FileInputStream in = new FileInputStream(arg)) {
            copy(first, in, out);
            first = false;
          }
        }
      } else {
        copy(true, System.in, out);
      }
    }
  }

  /**
   * Concatenates a stream onto the output.
   *
   * @param first Is this the beginning of the output?
   * @param in    The data to copy to the output
   * @param out   Where the output should go
   * @throws IOException If there is an error reading or writing.
   */
  public static void copy(boolean first, InputStream in, DataOutputStream out) throws IOException {
    byte[] buffer = new byte[1024 * 1024];
    int n;
    if (!first) {
      //noinspection UnusedAssignment
      n = (int) in.skip(6 * 4L);
    }
    n = in.read(buffer);
    while (n > 0) {
      out.write(buffer, 0, n);
      n = in.read(buffer);
    }
  }
}