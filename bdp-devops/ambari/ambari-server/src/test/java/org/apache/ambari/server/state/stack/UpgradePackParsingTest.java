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
package org.apache.ambari.server.state.stack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.stack.ModuleFileUnmarshaller;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests parsing upgrade packs in the entire codebase.  This does not use Guice
 * initialization, which will parse upgrade packs itself.
 */
@Category({ category.StackUpgradeTest.class})
public class UpgradePackParsingTest {

  @Test
  @SuppressWarnings("unchecked")
  public void findAndValidateUpgradePacks() throws Exception {

    IOFileFilter filter = new IOFileFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return false;
      }

      @Override
      public boolean accept(File file) {
        // file has the folder named 'upgrades', ends with '.xml' and is NOT 'config-upgrade.xml'
        if (file.getAbsolutePath().contains("upgrades") &&
            file.getAbsolutePath().endsWith(".xml") &&
            !file.getAbsolutePath().contains("config-upgrade.xml")) {

          return true;
        }

        return false;
      }
    };

    List<File> files = new ArrayList<>();

    files.addAll(FileUtils.listFiles(new File("src/main/resources/stacks"), filter,
      FileFilterUtils.directoryFileFilter()));

    files.addAll(FileUtils.listFiles(new File("src/test/resources/stacks"), filter,
        FileFilterUtils.directoryFileFilter()));

    files.addAll(FileUtils.listFiles(new File("src/test/resources/stacks_with_upgrade_cycle"), filter,
        FileFilterUtils.directoryFileFilter()));

    ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

    for (File file : files) {
      String fileContent = FileUtils.readFileToString(file, "UTF-8");

      // these things must be in upgrade packs for them to work anyway
      if (fileContent.contains("<upgrade") && fileContent.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
        if (!fileContent.contains("xsi:noNamespaceSchemaLocation=\"upgrade-pack.xsd\"")) {
          String msg = String.format("File %s appears to be an upgrade pack, but does not define 'upgrade-pack.xsd' as its schema",
              file.getAbsolutePath());
          Assert.fail(msg);
        } else {
          unmarshaller.unmarshal(UpgradePack.class, file, true);
        }
      }
    }
  }

}
