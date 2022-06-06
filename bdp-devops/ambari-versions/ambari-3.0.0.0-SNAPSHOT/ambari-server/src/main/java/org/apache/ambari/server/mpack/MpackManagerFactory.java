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
package org.apache.ambari.server.mpack;

import java.io.File;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * The {@link MpackManagerFactory} is used along with {@link AssistedInject} to
 * build instances of {@link MpackManager}.
 */
public interface MpackManagerFactory {

  /**
   * @param mpackStaging
   *        the folder location where mpack is downloaded. (not {@code null}).
   * @param stackRoot
   * @return a mpack manager instance.
   */
  MpackManager create(@Assisted("mpacksv2Staging") File mpackStaging, @Assisted("stackRoot") File stackRoot);
}

