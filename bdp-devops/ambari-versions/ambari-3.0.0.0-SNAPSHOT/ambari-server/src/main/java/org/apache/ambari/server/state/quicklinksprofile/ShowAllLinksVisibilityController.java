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


import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.ambari.server.state.quicklinks.Link;

/**
 * A visibility controller that shows all quicklinks. This is used when no quicklinks profile is set or the profile is
 * invalid. This mimics legacy behavior when all quicklinks were shown.
 *
 */
public class ShowAllLinksVisibilityController implements QuickLinkVisibilityController {

  @Override
  public boolean isVisible(@Nonnull String service, @Nonnull Link quickLink) {
    return true;
  }

  @Override
  public Optional<String> getUrlOverride(@Nonnull String service, @Nonnull Link quickLink) {
    return Optional.empty();
  }
}
