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

import React from 'react';
import DiceConfigPage, { useMock } from 'app/config-page';
import { functionalColor } from 'common/constants';

const Mock = () => {
  return (
    <DiceConfigPage
      showLoading
      scenarioType="mock"
      scenarioKey={'mock'}
      useMock={useMock}
      forceMock
      customProps={{
        dataRank: {
          op: {
            clickRow: (a: CP_DATA_RANK.IItem) => {
              // do something
            },
          },
          props: {
            theme: [
              {
                color: functionalColor.actions,
                titleIcon: 'mail',
                backgroundIcon: 'baocun',
              },
              {
                color: functionalColor.success,
                titleIcon: 'mysql',
                backgroundIcon: 'shezhi',
              },
              {
                color: functionalColor.warning,
                titleIcon: 'RocketMQ',
                backgroundIcon: 'map-draw',
              },
              {
                color: functionalColor.error,
                titleIcon: 'morenzhongjianjian',
                backgroundIcon: 'data-server',
              },
            ],
          },
        },
      }}
    />
  );
};
export default Mock;
