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

package org.apache.ambari.server.state.quicklinksprofile;

import java.io.IOException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory returns the quicklinks visibility controller. If the received string is a parseable and valid quicklinks
 * profile, a {@link DefaultQuickLinkVisibilityController} will be returned based on the profile. If the string is
 * {@code null} or invalid {@link ShowAllLinksVisibilityController} will be returned that displays all links which
 * corresponds to the legacy behavior.
 */
public class QuickLinkVisibilityControllerFactory {
  private static final Logger LOG = LoggerFactory.getLogger(QuickLinkVisibilityControllerFactory.class);

  /**
   * @param quickLinkProfileJson the quick links profile or {@code null} if no profile is set
   * @return a {@link DefaultQuickLinkVisibilityController} if the profile exists and is valid, {@link ShowAllLinksVisibilityController}
   *   otherwise.
   */
  public static QuickLinkVisibilityController get(@Nullable String quickLinkProfileJson) {
    if (null == quickLinkProfileJson) {
      LOG.info("No quick link profile is set, will display all quicklinks.");
      return new ShowAllLinksVisibilityController();
    }
    try {
      QuickLinksProfile profile = new QuickLinksProfileParser().parse(quickLinkProfileJson.getBytes());
      return new DefaultQuickLinkVisibilityController(profile);
    }
    catch (IOException | QuickLinksProfileEvaluationException ex) {
      LOG.error("Unable to parse quick link profile json: " + quickLinkProfileJson, ex);
      return new ShowAllLinksVisibilityController();
    }
  }

}
