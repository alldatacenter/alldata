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

const mockData: CP_DRAWER.Spec = {
  type: 'Drawer',
  props: {
    visible: true,
    title: '添加用户',
    content: 'test',
    closable: true,
    maskClosable: true,
    placement: 'right',
    size: 'm', // s:256, m: 560, l: 800, xl: 1100
  },
  operations: {
    close: {
      key: 'close-drawer',
      reload: false,
      command: { key: 'set', state: { visible: false }, target: 'Drawer1' },
    },
  },
};

export default mockData;
