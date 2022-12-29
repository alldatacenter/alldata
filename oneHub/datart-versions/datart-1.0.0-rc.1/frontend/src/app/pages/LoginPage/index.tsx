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

import { LayoutWithBrand } from 'app/components';
import { Version } from 'app/components/Version';
import {
  selectLoggedInUser,
  selectLoginLoading,
  selectOauth2Clients,
  selectSystemInfo,
} from 'app/slice/selectors';
import { getOauth2Clients, login } from 'app/slice/thunks';
import React, { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import { LoginForm } from './LoginForm';

export function LoginPage() {
  const dispatch = useDispatch();
  const history = useHistory();
  const systemInfo = useSelector(selectSystemInfo);
  const loading = useSelector(selectLoginLoading);
  const loggedInUser = useSelector(selectLoggedInUser);
  const oauth2Clients = useSelector(selectOauth2Clients);

  useEffect(() => {
    dispatch(getOauth2Clients());
  }, [dispatch]);

  const onLogin = useCallback(
    values => {
      dispatch(
        login({
          params: values,
          resolve: () => {
            history.replace('/');
          },
        }),
      );
    },
    [dispatch, history],
  );
  return (
    <LayoutWithBrand>
      <LoginForm
        loading={loading}
        loggedInUser={loggedInUser}
        oauth2Clients={oauth2Clients}
        registerEnable={systemInfo?.registerEnable}
        onLogin={onLogin}
      />
      <Version version={systemInfo?.version} />
    </LayoutWithBrand>
  );
}
