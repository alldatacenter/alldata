/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Route, Switch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { MemberDetailPage } from './pages/MemberDetailPage';
import { RoleDetailPage } from './pages/RoleDetailPage';
import { Sidebar } from './Sidebar';
import { useMemberSlice } from './slice';

export function MemberPage() {
  useMemberSlice();

  return (
    <Container>
      <Sidebar />
      <Switch>
        <Route
          path="/organizations/:orgId/members/:memberId"
          component={MemberDetailPage}
        />
        <Route
          path="/organizations/:orgId/roles/:roleId"
          component={RoleDetailPage}
        />
      </Switch>
    </Container>
  );
}

const Container = styled.div`
  display: flex;
  flex: 1;
`;
