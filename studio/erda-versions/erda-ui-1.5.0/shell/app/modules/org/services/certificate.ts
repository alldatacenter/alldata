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

export const getCertificateList = (payload: Certificate.ListQuery): IPagingResp<Certificate.Detail> => {
  return agent
    .get('/api/certificates/actions/list-certificates')
    .query(payload)
    .then((response: any) => response.body);
};

export const addCertificate = (payload: Certificate.Detail) => {
  return agent
    .post('/api/certificates')
    .send(payload)
    .then((response: any) => response.body);
};

export const getCertificateDetail = ({ id }: { id: string }): Certificate.Detail => {
  return agent.get(`/api/certificates/${id}`).then((response: any) => response.body);
};

export const updateCertificate = ({ id, desc, uuid, filename }: Certificate.UpdateBody) => {
  return agent
    .put(`/api/certificates/${id}`)
    .send({ desc, uuid, filename })
    .then((response: any) => response.body);
};

export const deleteCertificate = ({ id }: Certificate.Detail) => {
  return agent.delete(`/api/certificates/${id}`).then((response: any) => response.body);
};
