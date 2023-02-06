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

export const getIssueTime = (params: ISSUE_FIELD.IProjectIssueQuery): ISSUE_FIELD.IIssueTime => {
  return agent
    .get('/api/issues/actions/get-properties-time')
    .query(params)
    .then((response: any) => response.body);
};

export const addFieldItem = (params: Omit<ISSUE_FIELD.IFiledItem, 'propertyID' | 'index'>) => {
  return agent
    .post('/api/issues/actions/create-property')
    .send(params)
    .then((response: any) => response.body);
};

export const updateFieldItem = (params: Omit<ISSUE_FIELD.IFiledItem, 'index'>) => {
  return agent
    .put('/api/issues/actions/update-property')
    .send(params)
    .then((response: any) => response.body);
};

export const deleteFieldItem = (params: { propertyID: number }) => {
  return agent
    .delete('/api/issues/actions/delete-property')
    .query(params)
    .then((response: any) => response.body);
};

export const getFieldsByIssue = (params: ISSUE_FIELD.IFieldsByIssueQuery): ISSUE_FIELD.IFiledItem[] => {
  return agent
    .get('/api/issues/actions/get-properties')
    .query(params)
    .then((response: any) => response.body);
};

export const batchUpdateFieldsOrder = (params: ISSUE_FIELD.IFiledItem[]): ISSUE_FIELD.IFiledItem[] => {
  return agent
    .put('/api/issues/actions/update-properties-index')
    .send({ data: params })
    .then((response: any) => response.body);
};

export const getSpecialFieldOptions = (params: ISSUE_FIELD.ISpecialFieldQuery): ISSUE_FIELD.ISpecialOption[] => {
  return agent
    .get('/api/issues/action/get-stage')
    .query(params)
    .then((response: any) => response.body);
};

export const updateSpecialFieldOptions = (params: ISSUE_FIELD.ISpecialFieldQuery): ISSUE_FIELD.ISpecialOption[] => {
  return agent
    .put('/api/issues/action/update-stage')
    .send(params)
    .then((response: any) => response.body);
};
