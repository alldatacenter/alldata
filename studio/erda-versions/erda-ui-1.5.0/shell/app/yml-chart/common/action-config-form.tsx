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
import DiceConfigPage from 'app/config-page/index';
import routeInfoStore from 'core/stores/route';

interface IProps {
  nodeData: null | IStageTask;
  editing: boolean;
  isCreate?: boolean;
  otherTaskAlias?: string[];
  chosenActionName: string;
  chosenAction: any;
  onSubmit?: (options: any) => void;
}

const ActionConfigForm = (props: IProps) => {
  const { chosenActionName } = props;
  const { projectId } = routeInfoStore.useStore((s) => s.params);

  const inParams = {
    actionData: props.nodeData,
    projectId,
  };
  return (
    <DiceConfigPage
      inParams={inParams}
      showLoading={false}
      scenarioType="action"
      scenarioKey={chosenActionName}
      customProps={{ actionForm: { op: props } }}
    />
  );
};

export default ActionConfigForm;
