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

package org.apache.ambari.view.filebrowser;

import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.utils.ambari.ValidatorUtils;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyValidator implements Validator {
  protected static final Logger LOG = LoggerFactory.getLogger(PropertyValidator.class);

  public static final String WEBHDFS_URL = "webhdfs.url";

  @Override
  public ValidationResult validateInstance(ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    return null;
  }

  @Override
  public ValidationResult validateProperty(String property, ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {

    // if associated with cluster(local or remote), no need to validate associated properties
    ClusterType clusterType = viewInstanceDefinition.getClusterType();
    if (clusterType == ClusterType.LOCAL_AMBARI || clusterType == ClusterType.REMOTE_AMBARI) {
      return ValidationResult.SUCCESS;
    }

    if (property.equals(WEBHDFS_URL)) {
      String webhdfsUrl = viewInstanceDefinition.getPropertyMap().get(WEBHDFS_URL);
      if (!ValidatorUtils.validateHdfsURL(webhdfsUrl)) {
        LOG.error("Invalid webhdfs.url = {}", webhdfsUrl);
        return new InvalidPropertyValidationResult(false, "Must be valid URL");
      }
    }
    return ValidationResult.SUCCESS;
  }

  public static class InvalidPropertyValidationResult implements ValidationResult {
    private boolean valid;
    private String detail;

    public InvalidPropertyValidationResult(boolean valid, String detail) {
      this.valid = valid;
      this.detail = detail;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public String getDetail() {
      return detail;
    }
  }

}
