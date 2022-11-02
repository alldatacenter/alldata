/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.fast_hdfs_resource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;

import com.google.gson.Gson;

public class Runner {
  public static void main(String[] args)
      throws IOException, URISyntaxException {
    // 1 - Check arguments
    if (args.length != 1) {
      System.err.println("Incorrect number of arguments. Please provide:\n"
          + "1) Path to json file\n"
          + "Exiting...");
      System.exit(1);
    }

    // 2 - Check if json-file exists
    final String jsonFilePath = args[0];
    File file = new File(jsonFilePath);

    if (!file.isFile()) {
      System.err
          .println("File " + jsonFilePath + " doesn't exist.\nExiting...");
      System.exit(1);
    }

    Gson gson = new Gson();
    Resource[] resources = null;
    Map<String, FileSystem> fileSystemNameToInstance = new HashMap<String, FileSystem>();
    Map<String, List<Resource>> fileSystemToResource = new HashMap<String, List<Resource>>();

    boolean failed = false;
    try {
      // 3 - Load data from JSON
      resources = (Resource[]) gson.fromJson(new FileReader(jsonFilePath),
          Resource[].class);
      
      Configuration conf = new Configuration();
      FileSystem dfs = null;

      String defaultFsSchema = FileSystem.getDefaultUri(conf).getScheme();

      // Creating connections
      for (Resource resource : resources) {
        String fsName = null;
        URI targetURI = new URI(resource.getTarget());
        String targetSchema = targetURI.getScheme();

        if(targetSchema != null && !targetSchema.equals(defaultFsSchema)) {
          String authority = targetURI.getAuthority();
          if(authority == null) {
            authority = "";
          }
          fsName = String.format("%s://%s/", targetSchema, authority);
        } else if(resource.getNameservice() != null) {
          fsName = resource.getNameservice();
        }

        if(!fileSystemNameToInstance.containsKey(fsName)) {
          URI fileSystemUrl;
          if(fsName == null) {
            fileSystemUrl = FileSystem.getDefaultUri(conf);
          } else {
            fileSystemUrl = new URI(fsName);
          }

          dfs = FileSystem.get(fileSystemUrl, conf);

          // 4 - Connect to DFS
          System.out.println("Initializing filesystem uri: " + fileSystemUrl);
          dfs.initialize(fileSystemUrl, conf);

          fileSystemNameToInstance.put(fsName, dfs);
        }

        if(!fileSystemToResource.containsKey(fsName)) {
          fileSystemToResource.put(fsName, new ArrayList<Resource>());
        }
        fileSystemToResource.get(fsName).add(resource);
      }

      //for (Resource resource : resources) {
      for (Map.Entry<String, List<Resource>> entry : fileSystemToResource.entrySet()) {
        String nameservice = entry.getKey();
        List<Resource> resourcesNameservice = entry.getValue();

        for(Resource resource: resourcesNameservice) {
          if (nameservice != null) {
            System.out.println("Creating: " + resource + " in " + nameservice);
          } else {
            System.out.println("Creating: " + resource + " in default filesystem");
          }

          dfs = fileSystemNameToInstance.get(nameservice);

          Resource.checkResourceParameters(resource, dfs);

          Path pathHadoop = null;

          if (resource.getAction().equals("download")) {
            pathHadoop = new Path(resource.getSource());
          } else {
            String path = resource.getTarget();
            pathHadoop = new Path(path);
            if (!resource.isManageIfExists() && dfs.exists(pathHadoop)) {
              System.out.println(
                  String.format("Skipping the operation for not managed DFS directory %s  since immutable_paths contains it.", path)
              );
              continue;
            }
          }

          if (resource.getAction().equals("create")) {
            // 5 - Create
            Resource.createResource(resource, dfs, pathHadoop);
            Resource.setMode(resource, dfs, pathHadoop);
            Resource.setOwner(resource, dfs, pathHadoop);
          } else if (resource.getAction().equals("delete")) {
            // 6 - Delete
            dfs.delete(pathHadoop, true);
          } else if (resource.getAction().equals("download")) {
            // 7 - Download
            dfs.copyToLocalFile(pathHadoop, new Path(resource.getTarget()));
          }
        }
      }
    } 
    catch(Exception e) {
       System.out.println("Exception occurred, Reason: " + e.getMessage());
       e.printStackTrace();
       failed = true;
    }
    finally {
      for(FileSystem dfs:fileSystemNameToInstance.values()) {
        dfs.close();
      }
    }
    if(!failed) {
      System.out.println("All resources created.");
    } else {
      System.exit(1);
    }
  }

}
