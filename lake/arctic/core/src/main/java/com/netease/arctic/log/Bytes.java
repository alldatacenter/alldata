/*
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

package com.netease.arctic.log;

/**
 * Utils to handle byte array.
 */
public class Bytes {

  /**
   * Return a new byte array with one byte to append to the end of byte array
   *
   * @param src byte array appended to
   * @param append one byte to append
   * @return new byte array with appended byte
   */
  public static byte[] mergeByte(byte[] src, byte append) {
    return mergeByte(src, new byte[]{append});
  }

  /**
   * Return a new byte array with another byte array to append to the end of byte array
   *
   * @param src byte array appended to
   * @param append byte array to append
   * @return new merged byte array
   */
  public static byte[] mergeByte(byte[] src, byte[] append) {
    byte[] dist = new byte[src.length + append.length];
    System.arraycopy(src, 0, dist, 0, src.length);
    System.arraycopy(append, 0, dist, src.length, append.length);
    return dist;
  }

  /**
   * Return a new sub array from byte array
   *
   * @param src    original array
   * @param off    offset（index）
   * @param length length
   * @return intercepted array
   */
  public static byte[] subByte(byte[] src, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(src, off, b1, 0, length);
    return b1;
  }
}
