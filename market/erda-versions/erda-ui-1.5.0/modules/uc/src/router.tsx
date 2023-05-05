// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { Route, Switch, Router } from 'react-router-dom';
import Login from 'src/pages/login';
import Setting from 'src/pages/setting';
import Registration from 'src/pages/registration';
import { history } from 'src/common';

const NotFound = () => {
  return <h1>page not found</h1>;
};

export default function App() {
  return (
    <Router history={history as any}>
      <Switch>
        <Route exact path="/uc/settings" component={Setting} />
        <Route exact path="/uc/login" component={Login} />
        <Route exact path="/uc/registration" component={Registration} />
        <Route path="*" component={NotFound} />
      </Switch>
    </Router>
  );
}
