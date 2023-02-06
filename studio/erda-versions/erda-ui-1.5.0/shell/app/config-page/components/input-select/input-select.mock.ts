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

const mockData = {
  type: 'InputSelect',
  props: {
    options: [
      {
        label: '前置场景出参',
        key: 'type1',
        isLeaf: false,
      },
      {
        label: 'Mock',
        key: 'type2',
        isLeaf: false,
      },
    ],
  },
  state: {
    // eslint-disable-next-line no-template-curly-in-string
    value: '${params.id}',
  },
  operations: {
    onSelectOption: {
      key: 'onSelectOption',
      reload: true,
      fillMeta: 'selectedOptions',
      meta: { selectedOptions: '' },
    },
  },
};

export default mockData;
