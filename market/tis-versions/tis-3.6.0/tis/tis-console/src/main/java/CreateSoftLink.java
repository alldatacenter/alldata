/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;

/**
 * 将tis-plugin下的所有插件软连接到/opt/data/tis/plugins下
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-26 08:42
 **/
public class CreateSoftLink {

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("args length must be 1");
    }
    File pluginModuleDir = new File(args[0]);
    if (!pluginModuleDir.exists() || !pluginModuleDir.isDirectory()) {
      throw new IllegalArgumentException("pluginModuleDir is not illegal:" + pluginModuleDir.getAbsolutePath());
    }

    File targetPluginDir = TIS.pluginDirRoot;
    FileUtils.forceMkdir(targetPluginDir);
    FileUtils.cleanDirectory(targetPluginDir);
    Iterator<File> fileIt = FileUtils.iterateFiles(pluginModuleDir, new String[]{PluginClassifier.PACAKGE_TPI_EXTENSION_NAME}, true);
    File tpiFile = null;
    int fileCount = 0;
    while (fileIt.hasNext()) {
      tpiFile = fileIt.next();
      fileCount++;
      System.out.println("mk link:" + tpiFile.getAbsolutePath());
      Files.createSymbolicLink((new File(targetPluginDir, tpiFile.getName())).toPath(), tpiFile.toPath());
    }

    if (fileCount < 1) {
      throw new IllegalStateException("fileCount must more than 0");
    }
  }


}
