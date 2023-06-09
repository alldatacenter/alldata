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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines possible actions on the file and performs the necessary action
 */
public enum ActionOnFile {

  /**
   * No action will be performed
   */
  NONE {
    @Override
    public void action(URL url) { }
  },

  /**
   * Renames the file by adding current timestamp value with "yyyyMMdd_HHmmss"
   * format before last dot of original file name<p>
   * Example:<br>
   * Original file name: "storage-plugins-override.conf"<br>
   * New file name: "storage-plugins-override-20180703_033354.conf"
   */
  RENAME {
    @Override
    public void action(URL url) {
      String fileName = url.getFile();
      File file = new File(url.getPath());
      String currentDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      String newFileName = new StringBuilder(fileName)
          .insert(fileName.lastIndexOf("."), "-" + currentDateTime)
          .toString();
      Path filePath = file.toPath();
      try {
        Files.move(filePath, filePath.resolveSibling(newFileName));
      } catch (IOException e) {
        logger.error("There was an error during file {} rename.", fileName, e);
      }
    }
  },

  /**
   * Removes the file
   */
  REMOVE {
    @Override
    public void action(URL url) {
      File file = new File(url.getPath());
      try {
        Files.delete(file.toPath());
      } catch (IOException e) {
        logger.error("There was an error during file {} removing.", url.getFile(), e);
      }
    }
  };

  private static final Logger logger = LoggerFactory.getLogger(ActionOnFile.class);

  /**
   * Action which should be performed on the file
   * @param url the file URL
   */
  public abstract void action(URL url);
}
