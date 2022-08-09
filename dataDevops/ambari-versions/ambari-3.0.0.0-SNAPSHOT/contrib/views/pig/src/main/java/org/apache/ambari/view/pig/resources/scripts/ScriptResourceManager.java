/**
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

package org.apache.ambari.view.pig.resources.scripts;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.scripts.models.PigScript;
import org.apache.ambari.view.pig.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.ambari.view.pig.utils.UserLocalObjects;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Object that provides CRUD operations for script objects
 */
public class ScriptResourceManager extends PersonalCRUDResourceManager<PigScript> {
  private final static Logger LOG =
      LoggerFactory.getLogger(ScriptResourceManager.class);

  /**
   * Constructor
   * @param context View Context instance
   */
  public ScriptResourceManager(ViewContext context) {
    super(PigScript.class, context);
  }

  @Override
  public PigScript create(PigScript object) {
    super.create(object);
    if (object.getPigScript() == null || object.getPigScript().isEmpty()) {
      createDefaultScriptFile(object);
    }
    return object;
  }

  private void createDefaultScriptFile(PigScript object) {
    String userScriptsPath = context.getProperties().get("scripts.dir");
    if (userScriptsPath == null) {
      String msg = "scripts.dir is not configured!";
      LOG.error(msg);
      throw new MisconfigurationFormattedException("scripts.dir");
    }
    int checkId = 0;

    boolean fileCreated;
    String newFilePath;
    do {
      String normalizedName = object.getTitle().replaceAll("[^a-zA-Z0-9 ]+", "").replaceAll(" ", "_").toLowerCase();
      String timestamp = new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
      newFilePath = String.format(userScriptsPath +
              "/%s-%s%s.pig", normalizedName, timestamp, (checkId == 0)?"":"_"+checkId);
      LOG.debug("Trying to create new file " + newFilePath);

      try {
        FSDataOutputStream stream = UserLocalObjects.getHdfsApi(context).create(newFilePath, false);
        stream.close();
        fileCreated = true;
        LOG.debug("File created successfully!");
      } catch (FileAlreadyExistsException e) {
        fileCreated = false;
        LOG.debug("File already exists. Trying next id");
      } catch (IOException e) {
        try {
          delete(object.getId());
        } catch (ItemNotFound itemNotFound) {
          throw new ServiceFormattedException("Error in creation, during clean up: " + itemNotFound.toString(), itemNotFound);
        }
        throw new ServiceFormattedException("Error in creation: " + e.toString(), e);
      } catch (InterruptedException e) {
        try {
          delete(object.getId());
        } catch (ItemNotFound itemNotFound) {
          throw new ServiceFormattedException("Error in creation, during clean up: " + itemNotFound.toString(), itemNotFound);
        }
        throw new ServiceFormattedException("Error in creation: " + e.toString(), e);
      }
      checkId += 1;
    } while (!fileCreated);

    object.setPigScript(newFilePath);
    getPigStorage().store(object);
  }
}
