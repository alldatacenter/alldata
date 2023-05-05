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

import agent from 'agent';

export const getLibraryRefList = (payload: APPLICATION.IQuery): IPagingResp<APP_SETTING.LibRef> => {
  return agent
    .get('/api/lib-references')
    .query(payload)
    .then((response: any) => response.body);
};

export const addLibraryRef = (payload: APP_SETTING.AddLibRef) => {
  return agent
    .post('/api/lib-references')
    .send(payload)
    .then((response: any) => response.body);
};

export const deleteLibraryRef = (id: number) => {
  return agent.delete(`/api/lib-references/${id}`).then((response: any) => response.body);
};

export const getCertRefList = (payload: APP_SETTING.CertRefQuery): IPagingResp<APP_SETTING.CertRef> => {
  return agent
    .get('/api/certificates/actions/list-application-quotes')
    .query(payload)
    .then((response: any) => response.body);
};

export const pushToConfig = (payload: APP_SETTING.PushToConfigBody) => {
  return agent
    .post('/api/certificates/actions/push-configs')
    .send(payload)
    .then((response: any) => response.body);
};

export const addCertRef = (payload: APP_SETTING.AddCertRef) => {
  return agent
    .post('/api/certificates/actions/application-quote')
    .send(payload)
    .then((response: any) => response.body);
};

export const deleteCertRef = (payload: APP_SETTING.AddCertRef) => {
  return agent
    .delete('/api/certificates/actions/application-cancel-quote')
    .query(payload)
    .then((response: any) => response.body);
};

export const getVersionPushConfig = (appID: string | number): APP_SETTING.PublisherConfig => {
  return agent
    .get(`/api/applications/${appID}/actions/get-publish-item-relations`)
    .then((response: any) => response.body);
};

export const updateVersionPushConfig = ({ appID, ...config }: APP_SETTING.PublisherUpdateBody) => {
  return agent
    .post(`/api/applications/${appID}/actions/update-publish-item-relations`)
    .send(config)
    .then((response: any) => response.body);
};
