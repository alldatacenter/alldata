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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;


public class QuickLinkVisibilityControllerFactoryTest {

  /**
   * If receives a {@code null} quick profile string, the factory should return an instance of {@link ShowAllLinksVisibilityController}
   * to mimic legacy behavior.
   */
  @Test
  public void nullStringPassed() throws Exception {
    assertTrue(
        "An instance of " + ShowAllLinksVisibilityController.class.getSimpleName() + " should have been returned",
        QuickLinkVisibilityControllerFactory.get(null) instanceof ShowAllLinksVisibilityController);
  }

  /**
   * If receives an invalid json as quick profile string, the factory should return an instance of {@link ShowAllLinksVisibilityController}
   * to mimic legacy behavior.
   */
  @Test
  public void invalidJsonPassed() throws Exception {
    assertTrue(
        "An instance of " + ShowAllLinksVisibilityController.class.getSimpleName() + " should have been returned",
        QuickLinkVisibilityControllerFactory.get("Hello world!") instanceof ShowAllLinksVisibilityController);
  }

  /**
   * If receives an invalid quick profile string (e.g. valid json but no filters defined or contradicting filters),
   * the factory should return an instance of {@link ShowAllLinksVisibilityController} to mimic legacy behavior.
   */
  @Test
  public void invalidProfilePassed() throws Exception {
    String json = Resources.toString(Resources.getResource("inconsistent_quicklinks_profile.json"), Charsets.UTF_8);
    assertTrue(
        "An instance of " + ShowAllLinksVisibilityController.class.getSimpleName() + " should have been returned",
        QuickLinkVisibilityControllerFactory.get(json) instanceof ShowAllLinksVisibilityController);

    json = Resources.toString(Resources.getResource("inconsistent_quicklinks_profile_2.json"), Charsets.UTF_8);
    assertTrue(
        "An instance of " + ShowAllLinksVisibilityController.class.getSimpleName() + " should have been returned",
        QuickLinkVisibilityControllerFactory.get(json) instanceof ShowAllLinksVisibilityController);
  }

  /**
   * If receives a valid profile json string, an visibility controller should be returned that behaves according to the
   * received profile.
   */
  @Test
  public void validProfilePassed() throws Exception {
    String json = Resources.toString(Resources.getResource("example_quicklinks_profile.json"), Charsets.UTF_8);
    assertTrue(
        "An instance of " + DefaultQuickLinkVisibilityController.class.getSimpleName() + " should have been returned",
        QuickLinkVisibilityControllerFactory.get(json) instanceof DefaultQuickLinkVisibilityController);
  }

}