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
import routeInfoStore from 'core/stores/route';
import mspStore from 'msp/stores/micro-service';
import CmpStrategyForm from 'cmp/common/alarm-strategy/strategy-form';

const StrategyForm = () => {
  const { projectId, terminusKey } = routeInfoStore.useStore((s) => s.params);
  const { type } = mspStore.useStore((s) => s.currentProject);

  return (
    <CmpStrategyForm
      scopeType="msp"
      scopeId={projectId}
      commonPayload={{ scopeType: `msp_env`, scopeId: terminusKey, projectType: type }}
    />
  );
};

export default StrategyForm;
