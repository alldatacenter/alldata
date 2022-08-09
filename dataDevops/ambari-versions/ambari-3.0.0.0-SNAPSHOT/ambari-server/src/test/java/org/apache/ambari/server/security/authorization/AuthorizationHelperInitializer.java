/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security.authorization;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.apache.ambari.server.orm.dao.ViewInstanceDAO;

import com.google.inject.Provider;

/**
 * Test helper to set DAO object to static variables and set proper expectation in AuthenticationHelper
 */
public class AuthorizationHelperInitializer {

  public static void viewInstanceDAOReturningNull() {
    Provider viewInstanceDAOProvider = createNiceMock(Provider.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    expect(viewInstanceDAOProvider.get()).andReturn(viewInstanceDAO).anyTimes();
    expect(viewInstanceDAO.findByResourceId(anyLong())).andReturn(null).anyTimes();

    replay(viewInstanceDAOProvider, viewInstanceDAO);

    AuthorizationHelper.viewInstanceDAOProvider = viewInstanceDAOProvider;
  }



}
