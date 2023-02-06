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

const mockData: CP_DROPDOWN_SELECT.Spec = {
  type: 'DropdownSelect',
  props: {
    visible: true,
    options: [
      {
        label: '组织B',
        value: 'organizeB',
        prefixImgSrc:
          'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQYQY0vUTJwftJ8WqXoLiLeB--2MJkpZLpYOA&usqp=CAU',
        operations: {
          click: {
            key: 'click',
            show: false,
            reload: false,
            command: {
              key: 'goto',
              target: 'orgRoot',
              jumpOut: false,
              state: {
                params: {
                  orgName: 'organizeA',
                },
              },
            },
          },
        },
      },
      {
        label: '组织A',
        value: 'organizeA',
        prefixImgSrc:
          'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTI1EaartvKCwGgDS7FTpu71EyFs1wCl1MsFQ&usqp=CAU',
        operations: {
          click: {
            key: 'click',
            show: false,
            reload: false,
            command: {
              key: 'goto',
              target: 'orgRoot',
              jumpOut: false,
              state: {
                params: {
                  orgName: 'organizeA',
                },
              },
            },
          },
        },
      },
    ],
    quickSelect: [
      {
        value: 'orgList',
        label: '浏览公开组织',
        operations: {
          click: {
            key: 'click',
            show: false,
            reload: false,
            command: {
              key: 'goto',
              target: 'orgList',
              jumpOut: false,
            },
          },
        },
      },
    ],
  },
  state: {
    value: 'organizeA',
  },
};

export default mockData;
