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

import { createStore } from 'core/cube';
import i18n from 'i18n';
import { getLabels, createLabel, updateLabel, deleteLabel } from '../services/label';

interface IState {
  list: LABEL.Item[];
}

const initState: IState = {
  list: [],
};

const projectLabel = createStore({
  name: 'projectLabel',
  state: initState,
  effects: {
    async getLabels({ call, update, getParams }, { projectID, ...query }: LABEL.ListQuery) {
      const { projectId } = getParams();
      const { list } = await call(getLabels, { ...query, projectID: +projectId || projectID });
      update({ list });
      return list;
    },
    async createLabel({ call, getParams }, { projectID, ...payload }: LABEL.CreateBody) {
      const { projectId } = getParams();
      await call(
        createLabel,
        { ...payload, projectID: +projectId || projectID },
        { successMsg: i18n.t('dop:label created successfully') },
      );
    },
    async updateLabel({ call }, payload: LABEL.Item) {
      await call(updateLabel, payload, { successMsg: i18n.t('dop:label updated successfully') });
    },
    async deleteLabel({ call }, labelId: number) {
      await call(deleteLabel, labelId, { successMsg: i18n.t('dop:label deleted successfully') });
      projectLabel.reducers.deleteLabel(labelId);
    },
  },
  reducers: {
    clearList(state) {
      state.list = [];
    },
    deleteLabel(state, labelId: number) {
      state.list = state.list.filter((l) => l.id !== labelId);
    },
  },
});

export default projectLabel;
