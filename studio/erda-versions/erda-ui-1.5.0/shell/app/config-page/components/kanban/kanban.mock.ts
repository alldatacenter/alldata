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

const mockData: CP_KANBAN.Spec = {
  type: 'Kanban',
  props: {},
  data: {
    operations: {
      boardCreate: {},
    },
    boards: [
      {
        id: '1',
        pageNo: 1,
        total: 2,
        pageSize: 20,
        title: '已完成',
        operations: {
          boardDelete: {},
          boardUpdate: {},
          boardLoadMore: {},
        },
        cards: [
          {
            id: '1-1',
            title:
              'ttt1ttt1ttt1ttt1ttt1ttt1ttt1ttt1ttt1,ttt1ttt1ttt1,ttt1ttt1ttt1,ttt1ttt1ttt1ttt1ttt1,ttt1ttt1ttt1,ttt1,ttt1ttt1ttt1ttt1ttt1ttt1ttt1ttt1ttt1',
            extra: {
              priority: 'HIGH',
              type: 'TASK',
            },
            operations: {
              cardMoveTo: {
                async: true,
                serverData: {
                  extra: {
                    allowedMoveToTargetBoardIDs: ['2', '3'],
                  },
                },
              },
            },
          },
        ],
      },
    ],
  },
};

export default mockData;
