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
package org.apache.ambari.server.checks;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.Clusters;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Used to help mock out cluster and repository queries.
 */
public class MockCheckHelper extends CheckHelper {
  public RepositoryVersionDAO m_repositoryVersionDAO = Mockito.mock(RepositoryVersionDAO.class);
  public Clusters m_clusters = Mockito.mock(Clusters.class);

  public MockCheckHelper() {
    clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {

      @Override
      public RepositoryVersionDAO get() {
        return m_repositoryVersionDAO;
      }
    };
  }

  /**
   * Helper to set the AmbariMetaInfo provider instance
   */
  public void setMetaInfoProvider(Provider<AmbariMetaInfo> provider) {
    metaInfoProvider = provider;
  }
}
