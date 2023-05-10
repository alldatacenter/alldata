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
import { getMemberLabels } from '../services';

interface IState {
  memberLabels: Array<{ label: string; name: string }>;
}

const initState: IState = {
  memberLabels: [],
};

const memberLabel = createStore({
  name: 'memberLabel',
  state: initState,
  effects: {
    async getMemberLabels({ call, update }) {
      const { list } = await call(getMemberLabels);
      update({ memberLabels: list });
      return list;
    },
    async updateMemberLabels() {
      // await call(updateMemberLabels, payload, { successMsg: i18n.t('updated successfully') });
      await memberLabel.effects.getMemberLabels();
    },
  },
  reducers: {
    clearMemberLabels(state) {
      state.memberLabels = [];
    },
  },
});
export default memberLabel;
