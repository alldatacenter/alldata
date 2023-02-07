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
package org.apache.drill.exec.udfs;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import javax.inject.Inject;

public class CryptoFunctions {

  /**
   * This class returns the md2 digest of a given input string.
   *  Usage is SELECT md2( <input string> ) FROM ...
   */
  @FunctionTemplate(name = "md2", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class MD2Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = org.apache.commons.codec.digest.DigestUtils.md2Hex(input).toLowerCase();

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * This function returns the MD5 digest of a given input string.
   *  Usage is shown below:
   *  select md5( 'testing' ) from (VALUES(1));
   */
  @FunctionTemplate(name = "md5", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class MD5Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String outputString = org.apache.commons.codec.digest.DigestUtils.md5Hex(input).toLowerCase();

      out.buffer = buffer;
      out.start = 0;
      out.end = outputString.getBytes().length;
      buffer.setBytes(0, outputString.getBytes());
    }

  }

  /**
   * sha(<text>) / sha1(<text>): Calculates an SHA-1 160-bit checksum for the string, as described in RFC 3174 (Secure Hash Algorithm).
   * (https://en.wikipedia.org/wiki/SHA-1) The value is returned as a string of 40 hexadecimal digits, or NULL if the argument was NULL.
   * Note that sha() and sha1() are aliases for the same function.
   *
   * > select sha1( 'testing' ) from (VALUES(1));
   */
  @FunctionTemplate(names = {"sha", "sha1"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class SHA1Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {

    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);

      String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = sha1.getBytes().length;
      buffer.setBytes(0, sha1.getBytes());
    }

  }

  /**
   * sha2(<text>) / sha256(<text>): Calculates an SHA-2 256-bit checksum for the string.  The value is returned as a string of hexadecimal digits,
   * or NULL if the argument was NULL. Note that sha2() and sha256() are aliases for the same function.
   * > select sha2( 'testing' ) from (VALUES(1));
   */
  @FunctionTemplate(names = {"sha256", "sha2"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class SHA256Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;


    @Override
    public void setup() {

    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);

      String sha2 = org.apache.commons.codec.digest.DigestUtils.sha256Hex(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = sha2.getBytes().length;
      buffer.setBytes(0, sha2.getBytes());
    }

  }

  /**
   * This function returns the SHA384 digest of a given input string.
   *  Usage is shown below:
   *  select sha384( 'testing' ) from (VALUES(1));
   */
  @FunctionTemplate(name = "sha384", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class SHA384Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {

    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);

      String sha384 = org.apache.commons.codec.digest.DigestUtils.sha384Hex(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = sha384.getBytes().length;
      buffer.setBytes(0, sha384.getBytes());
    }

  }

  /**
   * This function returns the SHA512 digest of a given input string.
   *  Usage is shown below:
   *  select sha512( 'testing' ) from (VALUES(1));
   */
  @FunctionTemplate(name = "sha512", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class SHA512Function implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {

    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);

      String sha512 = org.apache.commons.codec.digest.DigestUtils.sha512Hex(input);

      out.buffer = buffer;
      out.start = 0;
      out.end = sha512.getBytes().length;
      buffer.setBytes(0, sha512.getBytes());
    }

  }

  /**
   * aes_encrypt()/ aes_decrypt(): implement encryption and decryption of data using the official AES (Advanced Encryption Standard) algorithm,
   * previously known as "Rijndael." AES_ENCRYPT() encrypts the string str using the key string key_str and returns a
   * binary string containing the encrypted output.
   * Usage:  SELECT aes_encrypt( 'encrypted_text', 'my_secret_key' ) AS aes FROM (VALUES(1));
   */
  @FunctionTemplate(name = "aes_encrypt", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class AESEncryptFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Param
    VarCharHolder rawKey;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      String key = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawKey.start, rawKey.end, rawKey.buffer);
      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String encryptedText = "";
      try {
        byte[] keyByteArray = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-1");
        keyByteArray = sha.digest(keyByteArray);
        keyByteArray = java.util.Arrays.copyOf(keyByteArray, 16);
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyByteArray, "AES");

        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
        encryptedText = javax.xml.bind.DatatypeConverter.printBase64Binary(cipher.doFinal(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      } catch (Exception e) {
        //Exceptions are ignored
      }

      out.buffer = buffer;
      out.start = 0;
      out.end = encryptedText.getBytes().length;
      buffer.setBytes(0, encryptedText.getBytes());
    }

  }

  /**
   *  AES_DECRYPT() decrypts the encrypted string crypt_str using the key string key_str and returns the original cleartext string.
   *  If either function argument is NULL, the function returns NULL.
   *  Usage:  SELECT aes_decrypt( <encrypted_text>, <key> ) FROM ...
   */
  @FunctionTemplate(name = "aes_decrypt", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class AESDecryptFunction implements DrillSimpleFunc {

    @Param
    VarCharHolder rawInput;

    @Param
    VarCharHolder rawKey;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf buffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      String key = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawKey.start, rawKey.end, rawKey.buffer);
      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(rawInput.start, rawInput.end, rawInput.buffer);
      String decryptedText = "";
      try {
        byte[] keyByteArray = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-1");
        keyByteArray = sha.digest(keyByteArray);
        keyByteArray = java.util.Arrays.copyOf(keyByteArray, 16);
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyByteArray, "AES");

        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey);
        decryptedText = new String(cipher.doFinal(javax.xml.bind.DatatypeConverter.parseBase64Binary(input)));
      } catch (Exception e) {
        //Exceptions are ignored
      }

      out.buffer = buffer;
      out.start = 0;
      out.end = decryptedText.getBytes().length;
      buffer.setBytes(0, decryptedText.getBytes());
    }

  }

}
