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
import EditGlobalVariable from 'application/common/components/edit-global-variable';
import EditStage from 'application/common/components/edit-stage';
import PropertyView from 'application/common/components/property-view';
import { IDiceYamlEditorItem } from 'application/common/components/dice-yaml-editor-item';
import { forEach, isEmpty, remove, omit } from 'lodash';
import i18n from 'i18n';

interface IOption {
  task: IStageTask;
  taskGroup: any[];
  item: any;
  actions: IStageAction[];
  pipelineTaskAlias: string[];
  editConvertor: (service: any) => void;
}

const pushTask = (options: IOption) => {
  const { task, taskGroup, item, actions, editConvertor, pipelineTaskAlias } = options;
  const otherTaskAlias = remove([...pipelineTaskAlias], (alias) => alias !== task.alias);
  const propertyViewDataSource = omit(task, ['alias', 'type', 'resources']);
  taskGroup.push({
    ...item,
    lineTo: ['all'],
    icon: 'wfw',
    editView: (editing: boolean) => {
      return (
        <EditStage
          editing={editing}
          actions={actions}
          task={task}
          otherTaskAlias={otherTaskAlias}
          onSubmit={editConvertor}
        />
      );
    },
    title: task.type,
    data: task,
    name: task.alias,
    content: () => {
      return <PropertyView dataSource={propertyViewDataSource} />;
    },
  });
};

interface IProps {
  title?: string;
  actions: IStageAction[];
  editGlobalVariable: (globalVariable: any) => void;
  editConvertor: (service: any) => void;
  pipelineYmlStructure: IPipelineYmlStructure;
}

export default ({ title, editGlobalVariable, editConvertor, actions, pipelineYmlStructure }: IProps) => {
  const result: any[] = [];
  const order = ['envs', 'stages'];

  if (!isEmpty(pipelineYmlStructure)) {
    forEach(order, (key: string, index: number) => {
      const currentItem: any[] = pipelineYmlStructure[key];
      let item: IDiceYamlEditorItem | IDiceYamlEditorItem[];

      if (index === 0) {
        // @ts-ignore
        item = {
          id: 'qj-1',
          icon: 'qj',
          title: `${title || ''} ${i18n.t('dop:global variable')}`,
          lineTo: ['all'],
          data: currentItem,
          allowMove: false,
          allowDelete: false,
          content: () => {
            return <PropertyView dataSource={currentItem} />;
          },
          editView: (editing: boolean) => (
            <EditGlobalVariable editing={editing} globalVariable={currentItem} onSubmit={editGlobalVariable} />
          ),
        };
        result.push([item]);
      } else if (index === 1 && currentItem) {
        const pipelineTaskAlias = currentItem.reduce((acc: string[], stage: any[]) => {
          const stageTaskAlias = stage.map((task) => task.alias);
          return acc.concat(stageTaskAlias);
        }, []);

        currentItem.forEach((group: IStageTask[], groupIndex: number) => {
          const taskGroup: any[] = [];
          if (group) {
            group.forEach((task: IStageTask, taskIndex: number) => {
              pushTask({
                task,
                taskGroup,
                item: {
                  taskIndex,
                  groupIndex,
                },
                actions,
                editConvertor,
                pipelineTaskAlias,
              });
            });
          }
          result.push(taskGroup);
        });
      }
    });
  } else {
    result.push([
      {
        id: 'qj-1',
        icon: 'qj',
        title: i18n.t('dop:deploy global variables'),
        name: i18n.t('dop:deploy global variables'),
        lineTo: ['all'],
        allowMove: false,
        content: () => {
          return null;
        },
        editView: (editing: boolean) => (
          <EditGlobalVariable editing={editing} globalVariable={{}} onSubmit={editGlobalVariable} />
        ),
      },
    ]);
  }
  return {
    editorData: result,
  };
};
