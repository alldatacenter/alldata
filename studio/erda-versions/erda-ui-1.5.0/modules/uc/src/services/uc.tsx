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

import axios from 'axios';

const apiPrefix = '/api/uc';

const flowApiMap = {
  login: `${apiPrefix}/self-service/login/browser`,
  registration: `${apiPrefix}/self-service/registration/browser`,
  settings: `${apiPrefix}/self-service/settings/browser`,
};

const initFlow = (type: keyof typeof flowApiMap): Promise<{ flow: string; csrf_token: string }> => {
  const flowApi = flowApiMap[type];
  return axios.get(flowApi).then((res: UC.IKratosRes) => {
    const flow = res?.data?.id;
    const nodes = res?.data?.ui?.nodes;
    const ctItem = nodes.find((item) => item.attributes.name === 'csrf_token') as UC.IKratosDataNode;
    return { flow, csrf_token: ctItem?.attributes?.value };
  });
};

export const login = (payload: UC.ILoginPayload) => {
  const { identifier, password } = payload;
  return initFlow('login').then(({ flow, csrf_token }) => {
    return axios
      .post(`${apiPrefix}/self-service/login?flow=${flow}`, {
        method: 'password',
        csrf_token,
        password,
        password_identifier: identifier,
      })
      .then((res: UC.IKratosRes) => res.data);
  });
};

export const registration = (payload: UC.IRegistrationPayload) => {
  const { password, ...rest } = payload;
  return initFlow('registration').then(({ flow, csrf_token }) => {
    return axios
      .post(`${apiPrefix}/self-service/registration?flow=${flow}`, {
        method: 'password',
        password,
        csrf_token,
        traits: {
          ...rest,
        },
      })
      .then((res: UC.IKratosRes) => res.data);
  });
};

export const logout = () => {
  return axios.get(`${apiPrefix}/self-service/logout/browser`).then((res) => {
    const token = res?.data?.logout_url?.split('token=')?.[1];
    return axios.get(`${apiPrefix}/self-service/logout?token=${token}`).then((res: UC.IKratosRes) => res.data);
  });
};

export const whoAmI = () => {
  return axios.get(`${apiPrefix}/sessions/whoami`).then((res: UC.IWhoAmIRes) => res.data);
};

export const updateUser = (payload: Omit<UC.IUser, 'id'>) => {
  return initFlow('settings').then(({ flow, csrf_token }) => {
    return axios
      .post(`${apiPrefix}/self-service/settings?flow=${flow}`, {
        method: 'profile',
        csrf_token,
        traits: payload,
      })
      .then((res: UC.IKratosRes) => res.data);
  });
};

export const updatePassword = (payload: string) => {
  return initFlow('settings').then(({ flow, csrf_token }) => {
    return axios
      .post(`${apiPrefix}/self-service/settings?flow=${flow}`, {
        method: 'password',
        csrf_token,
        password: payload,
      })
      .then((res: UC.IKratosRes) => res.data);
  });
};
