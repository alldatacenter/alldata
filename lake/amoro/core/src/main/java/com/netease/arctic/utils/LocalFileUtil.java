package com.netease.arctic.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;

public class LocalFileUtil {

  public static void deleteDirectory(File directory) throws IOException {
    if (directory.exists()) {
      Files.walk(directory.toPath()).sorted(Comparator.reverseOrder())
              .map(java.nio.file.Path::toFile).forEach(File::delete);
      directory.delete();
      if (directory.exists()) {
        throw new IOException("Unable to delete directory " + directory);
      }
    }
  }

  public static void mkdir(File directory) throws IOException {
    if (!directory.exists()) {
      directory.mkdirs();
    }

    if (!directory.isDirectory()) {
      throw new IOException("Unable to create :" + directory);
    }
  }
}
