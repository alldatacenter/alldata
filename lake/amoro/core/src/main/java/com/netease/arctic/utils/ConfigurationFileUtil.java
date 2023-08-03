package com.netease.arctic.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Util class to encode configuration files with base64
 */
public class ConfigurationFileUtil {
  /**
   * encode target file with base64, such as krb5.conf、**.keytab files
   * @param filePath target file path
   * @return file content with base64 encode
   * @throws IOException if an error occurs while reading
   */
  public static String encodeConfigurationFileWithBase64(String filePath) throws IOException {
    if (filePath == null || "".equals(filePath.trim())) {
      return null;
    } else {
      return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(filePath)));
    }
  }

  /**
   * encode target xml file with base64, such as core-site.xml、hdfs-site.xml files
   * @param filePath target file path
   * @return file content with base64 encode
   * @throws IOException if an error occurs while reading
   */
  public static String encodeXmlConfigurationFileWithBase64(String filePath) throws IOException {
    if (filePath == null || "".equals(filePath.trim())) {
      return Base64.getEncoder().encodeToString("<configuration></configuration>".getBytes(StandardCharsets.UTF_8));
    } else {
      return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(filePath)));
    }
  }
}
