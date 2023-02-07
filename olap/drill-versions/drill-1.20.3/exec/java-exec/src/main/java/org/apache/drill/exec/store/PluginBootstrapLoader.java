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
package org.apache.drill.exec.store;

import java.io.IOException;

import org.apache.drill.exec.planner.logical.StoragePlugins;

/**
 * Generalized interface for bootstraping or upgrading the plugin persistent
 * store. Provides the list of bootstrap plugins, along with those to be
 * upgraded. Provides confirmation that the upgrade was performed to avoid
 * doing it again.
 * <p>
 * <b>Caveats:</b>: The upgrade mechanism is rather ad-hoc; it actually needs
 * a version number to be solid, but the persistent store does not currently
 * provide a version number. See DRILL-7613.
 */
public interface PluginBootstrapLoader {

  /**
   * Drill has detected that this is a new installation. Provide
   * the list of storage plugins (that is, names and configs)
   * to use to populate the storage plugin store.
   */
  StoragePlugins bootstrapPlugins() throws IOException;

  /**
   * Drill has detected that on restart, the persistent storage
   * plugin exists. Return any changes that should be applied.
   * <p>
   * Note: this functionality is crude, there is no version, all
   * Drillbits in a cluster will race to do the upgrade.
   * Caveat emptor. See DRILL-7613.
   */
  StoragePlugins updatedPlugins();

  /**
   * Drill successfully applied the plugin upgrades from
   * {@link #updatedPlugins()}. Use this event to mark this
   * version as having been upgraded. (Again, this is crude and
   * may not actually work. See DRILL-7613.)
   */
  void onUpgrade();
}
