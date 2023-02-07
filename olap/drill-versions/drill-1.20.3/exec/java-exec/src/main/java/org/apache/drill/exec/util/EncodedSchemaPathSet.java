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


import org.apache.drill.shaded.guava.com.google.common.io.BaseEncoding;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.physical.PlannerSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This class provided utility methods to encode and decode a set of user specified
 * SchemaPaths to a set of encoded SchemaPaths with the following properties.
 * <ol>
 * <li>Valid Drill identifier as per its grammar with only one, root name segment.
 * <li>A single identifier can not exceed 1024 characters in length.
 * </ol>
 * <p>
 * Format of the encoded SchemaPath:
 * <blockquote><pre>$$ENC\d\dlt;base32 encoded input paths&gt;</pre></blockquote>
 * <p>
 * We use Base-32 over Base-64 because the later's charset includes '\' and '+'.
 */
public class EncodedSchemaPathSet {

  private static final int ESTIMATED_ENCODED_SIZE = 1024;

  private static final String ENC_PREFIX = "$$ENC";

  private static final String ENC_FORMAT_STRING = ENC_PREFIX + "%02d%s";
  private static final int ENC_PREFIX_SIZE = ENC_PREFIX.length() + "00".length();
  private static final int MAX_ENC_IDENTIFIER_SIZE = (PlannerSettings.DEFAULT_IDENTIFIER_MAX_LENGTH - ENC_PREFIX_SIZE);
  private static final int MAX_ENC_IDENTIFIER_COUNT = 100; // "$$ENC00*...$$ENC99*"

  private static final BaseEncoding CODEC = BaseEncoding.base32().omitPadding(); // no-padding version

  public static final String ENCODED_STAR_COLUMN = encode("*")[0];

  /*
   * Performance of various methods of encoding a Java String to UTF-8 keeps changing
   * between releases, hence we'll encapsulate the actual methods within these functions
   * and use them everywhere in Drill
   */
  private static final String UTF_8 = "utf-8";


  private static byte[] encodeUTF(String input) {
    try {
      return input.getBytes(UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new DrillRuntimeException(e); // should never come to this
    }
  }

  private static String decodeUTF(byte[] input) {
    try {
      return new String(input, UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new DrillRuntimeException(e); // should never come to this
    }
  }

  private static String decodeUTF(byte[] input, int offset, int length) {
    try {
      return new String(input, offset, length, UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new DrillRuntimeException(e); // should never come to this
    }
  }

  /**
   * Returns the encoded array of SchemaPath identifiers from the input array of SchemaPath.
   * <p>
   * The returned identifiers have the following properties:
   * <ul>
   *  <li>Each SchemaPath identifier in the array has only one single root NameSegment.</li>
   *  <li>Maximum length of each such identifier is equal to the maximum length of Drill identifier (currently 1024).</li>
   * </ul>
   * <p>
   * We take advantage of the fact that Java's modified utf-8 encoding can never contain
   * embedded null byte.
   * @see <a>http://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html#modified-utf-8</a>
   */
  public static String[] encode(final String... schemaPaths) {
    Preconditions.checkArgument(schemaPaths != null && schemaPaths.length > 0,
        "At least one schema path should be provided");

    NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream(ESTIMATED_ENCODED_SIZE);
    int bufOffset = 1; // 1st byte is NULL
    for (String schemaPath : schemaPaths) {
      out.write(0);
      out.write(encodeUTF(schemaPath));
    }
    out.close();

    final int bufLen = out.size() - 1; // not counting the first NULL byte
    String encodedStr = CODEC.encode(out.getBuffer(), bufOffset, bufLen);
    assert !encodedStr.endsWith("=") : String.format("Encoded string '%s' ends with '='", encodedStr);
    return splitIdentifiers(encodedStr);
  }

  public static boolean isEncodedSchemaPath(SchemaPath schemaPath) {
    return schemaPath != null && isEncodedSchemaPath(schemaPath.getRootSegment().getNameSegment().getPath());
  }

  public static boolean isEncodedSchemaPath(String schemaPath) {
    return schemaPath != null && schemaPath.startsWith(ENC_PREFIX);
  }

  /**
   * Returns the decoded Collection of SchemaPath from the input which
   * may contain a mix of encoded and non-encoded SchemaPaths.
   * <p>
   * The size of returned Collection is always equal to or greater than the
   * input array.
   * <p>
   * The non-encoded SchemaPaths are collated in the beginning to the returned
   * array, in the same order as that of the input array.
   */
  public static Collection<SchemaPath> decode(final Collection<SchemaPath> encodedPaths) {
    String[] schemaPathStrings = new String[encodedPaths.size()];
    Iterator<SchemaPath> encodedPathsItr = encodedPaths.iterator();
    for (int i = 0; i < schemaPathStrings.length; i++) {
      SchemaPath schemaPath = encodedPathsItr.next();
      if (schemaPath.getRootSegmentPath().startsWith(ENC_PREFIX)) {
        // encoded schema path contains only root segment
        schemaPathStrings[i] = schemaPath.getRootSegmentPath();
      } else {
        schemaPathStrings[i] = schemaPath.toExpr();
      }
    }
    String[] decodedStrings = decode(schemaPathStrings);
    if (decodedStrings == schemaPathStrings) {
      return encodedPaths; // return the original collection as no encoded SchemaPath was found
    } else {
      ImmutableList.Builder<SchemaPath> builder = new ImmutableList.Builder<>();
      for (String decodedString : decodedStrings) {
        if ("*".equals(decodedString) || "`*`".equals(decodedString)) {
          builder.add(SchemaPath.STAR_COLUMN);
        } else {
          builder.add(SchemaPath.parseFromString(decodedString));
        }
      }
      return builder.build();
    }
  }

  /**
   * Returns the decoded array of SchemaPath strings from the input which
   * may contain a mix of encoded and non-encoded SchemaPaths.
   * <p>
   * The size of returned array is always equal to or greater than the
   * input array.
   * <p>
   * The non-encoded SchemaPaths are collated in the beginning to the returned
   * array, in the same order as that of the input array.
   */
  public static String[] decode(final String... encodedPaths) {
    Preconditions.checkArgument(encodedPaths != null && encodedPaths.length > 0,
        "At least one encoded path should be provided");

    StringBuilder sb = new StringBuilder(ESTIMATED_ENCODED_SIZE);

    // As the encoded schema path move across components, they could get reordered.
    // Sorting ensures that the original order is restored before concatenating the
    // components back to the full encoded String.
    Arrays.sort(encodedPaths);

    List<String> decodedPathList = Lists.newArrayList();
    for (String encodedPath : encodedPaths) {
      if (encodedPath.startsWith(ENC_PREFIX)) {
        sb.append(encodedPath, ENC_PREFIX_SIZE, encodedPath.length());
      } else {
        decodedPathList.add(encodedPath);
      }
    }

    if (sb.length() > 0) {
      byte[] decodedBytes;
      try {
        decodedBytes = CODEC.decode(sb);
      } catch (IllegalArgumentException e) {
        throw new DrillRuntimeException(String.format(
            "Unable to decode the input strings as encoded schema paths:\n%s", Arrays.asList(encodedPaths)), e);
      }

      int start = 0, index = 0;
      for (; index < decodedBytes.length; index++) {
        if (decodedBytes[index] == 0 && index - start > 0) {
          decodedPathList.add(decodeUTF(decodedBytes, start, index-start));
          start = index + 1;
        }
      }
      if (index - start > 0) {
        String lastSchemaPath = decodeUTF(decodedBytes, start, index-start).trim();
        if (!lastSchemaPath.isEmpty()) {
          decodedPathList.add(lastSchemaPath);
        }
      }
      return decodedPathList.toArray(new String[decodedPathList.size()]);
    } else {
      // original list did not have any encoded path, return as is
      return encodedPaths;
    }
  }

  /**
   * Splits the input string so that the length of each encoded string,
   * including the signature prefix is less than or equal to MAX_DRILL_IDENTIFIER_SIZE.
   */
  private static String[] splitIdentifiers(String input) {
    if (input.length() < MAX_ENC_IDENTIFIER_SIZE) {
      return new String[] { String.format(ENC_FORMAT_STRING, 0, input) };
    }
    int splitsCount = (int) Math.ceil(input.length() / (double)MAX_ENC_IDENTIFIER_SIZE);
    if (splitsCount > MAX_ENC_IDENTIFIER_COUNT) {
      throw new DrillRuntimeException(String.format(
          "Encoded size of the SchemaPath identifier '%s' exceeded maximum value.", input));
    }
    String[] result = new String[splitsCount];
    for (int i = 0, startIdx = 0; i < result.length; i++, startIdx += MAX_ENC_IDENTIFIER_SIZE) {
      // TODO: see if we can avoid memcpy due to input.substring() call
      result[i] = String.format(ENC_FORMAT_STRING, i, input.substring(startIdx, Math.min(input.length(), startIdx + MAX_ENC_IDENTIFIER_SIZE)));
    }
    return result;
  }

  /**
   * Optimized version of Java's ByteArrayOutputStream which returns the underlying
   * byte array instead of making a copy
   */
  private static class NoCopyByteArrayOutputStream extends ByteArrayOutputStream {
    public NoCopyByteArrayOutputStream(int size) {
      super(size);
    }

    public byte[] getBuffer() {
      return buf;
    }

    public int size() {
      return count;
    }

    @Override
    public void write(int b) {
      super.write(b);
    }

    @Override
    public void write(byte[] b) {
      super.write(b, 0, b.length);
    }

    @Override
    public void close() {
      try {
        super.close();
      } catch (IOException e) {
        throw new DrillRuntimeException(e); // should never come to this
      }
    }
  }

}
