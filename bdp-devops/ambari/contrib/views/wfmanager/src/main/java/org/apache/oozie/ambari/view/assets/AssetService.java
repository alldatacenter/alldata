/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view.assets;

import java.util.Collection;

import org.apache.ambari.view.ViewContext;
import org.apache.oozie.ambari.view.assets.model.ActionAsset;
import org.apache.oozie.ambari.view.assets.model.ActionAssetDefinition;
import org.apache.oozie.ambari.view.assets.model.AssetDefintion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetService {
  private final static Logger LOGGER = LoggerFactory
    .getLogger(AssetService.class);
  private final AssetRepo assetRepo;
  private final AssetDefinitionRepo assetDefinitionRepo;

  private final ViewContext viewContext;


  public AssetService(ViewContext viewContext) {
    super();
    this.viewContext = viewContext;

    this.assetDefinitionRepo = new AssetDefinitionRepo(
      viewContext.getDataStore());
    this.assetRepo = new AssetRepo(viewContext.getDataStore());

  }

  public Collection<ActionAsset> getAssets() {
    return assetRepo.findAll();
  }

  public Collection<ActionAsset> getPrioritizedAssets() {
    Collection<ActionAsset> assets = getAssets();
    return assets;
  }

  public void saveAsset(String assetId, String userName,
                        AssetDefintion assetDefinition) {
    if (assetId != null) {
      ActionAsset actionAsset = assetRepo.findById(assetId);
      if (actionAsset == null) {
        throw new RuntimeException("could not find asset with id :"
          + assetId);
      }
      actionAsset.setDescription(assetDefinition.getDescription());
      actionAsset.setName(assetDefinition.getName());
      actionAsset.setType(assetDefinition.getType());
      ActionAssetDefinition assetDefinintion = assetDefinitionRepo
        .findById(actionAsset.getDefinitionRef());
      assetDefinintion.setData(assetDefinintion.getData());
      assetDefinitionRepo.update(assetDefinintion);
      assetRepo.update(actionAsset);
    } else {
      ActionAsset actionAsset = new ActionAsset();
      actionAsset.setOwner(userName);
      ActionAssetDefinition definition = new ActionAssetDefinition();
      definition.setData(assetDefinition.getDefinition());
      ActionAssetDefinition createdDefinition = assetDefinitionRepo
        .create(definition);
      actionAsset.setDefinitionRef(createdDefinition.getId());
      actionAsset.setDescription(assetDefinition.getDescription());
      actionAsset.setName(assetDefinition.getName());
      actionAsset.setType(assetDefinition.getType());
      assetRepo.create(actionAsset);
    }
  }


  public void deleteAsset(String id) {
    assetRepo.deleteById(id);
  }

  public AssetDefintion getAssetDetail(String assetId) {
    AssetDefintion ad = new AssetDefintion();
    ActionAsset actionAsset = assetRepo.findById(assetId);
    ActionAssetDefinition actionDefinition = assetDefinitionRepo
      .findById(actionAsset.getDefinitionRef());
    ad.setDefinition(actionDefinition.getData());
    ad.setDescription(actionAsset.getDescription());
    ad.setName(actionAsset.getName());
    return ad;
  }

  public ActionAssetDefinition getAssetDefinition(String assetDefintionId) {
    return assetDefinitionRepo.findById(assetDefintionId);
  }

  public ActionAsset getAsset(String id) {
    return assetRepo.findById(id);
  }

  public Collection<ActionAsset> getMyAssets() {
    return assetRepo.getMyAsets(viewContext.getUsername());
  }

  public boolean isAssetNameAvailable(String name) {
    return assetRepo.assetNameAvailable(name);
  }
}
