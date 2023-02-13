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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { Icon as CustomIcon, LogRoller } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { get, map, find, isEmpty } from 'lodash';
import { useEffectOnce } from 'react-use';
import { ciStatusMap } from 'application/pages/build-detail/config';
import clusterStore from '../../stores/cluster';
import './cluster-log.scss';
import { Drawer, Switch } from 'antd';

let intervalObj = null as any;
const StepListComp = ({
  stepData,
  onClickStep,
  activeStep,
}: {
  stepData: any;
  onClickStep: any;
  activeStep: string;
}) => {
  const steps = stepData;
  const clickItem = (id: string) => {
    onClickStep(id);
  };
  return (
    <div className="cluster-log-step-wrap">
      {steps.map((step: any) => {
        const { status = '', id, name, statusText } = step;
        const curStatus = status.toLowerCase();
        const disabled = curStatus === 'default';

        return (
          <div
            key={id}
            className={`one-step ${curStatus} ${activeStep === id ? 'on-active' : ''}`}
            onClick={() => !disabled && clickItem(id)}
          >
            <div className={`${disabled ? 'not-allowed' : 'cursor-pointer'} step-task-item`}>
              <CustomIcon type={status === 'default' ? 'circle' : 'circle-fill'} />
              <span className="step-title">
                <span className="name">{name}</span>(<span className={'status-text'}>{statusText}</span>)
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
};

const getStepFromTasks = (taskData: any) => {
  const stepList = [] as any[];
  const list = taskData.list || [];
  map(list, (listItem) => {
    const pipelineStages = get(listItem, 'pipelineDetail.pipelineStages', []);
    map(pipelineStages, (item) => {
      const pipelineTasks = get(item, 'pipelineTasks', []);
      map(pipelineTasks, (task) => {
        const { name, id, status } = task;
        const curStatus = ciStatusMap[status] || {};
        stepList.push({
          name,
          id,
          status: curStatus.status || 'warning',
          statusText: curStatus.text || i18n.t('unknown'),
        });
      });
    });
  });

  return stepList;
};

const getPureLog = (taskData: any) => {
  const pureLog = [] as string[];
  const list = taskData.list || [];
  map(list, (listItem) => {
    pureLog.push(listItem.detail);
  });
  return pureLog;
};

const getCurTaskId = (state: any) => {
  const { userChosenTaskId, stepList } = state;
  if (userChosenTaskId) {
    // 当前用户选择，不自动切换step
    return userChosenTaskId;
  } else {
    // 用户未选择，自动定位到processing的taskId
    let taskId = get(
      find(stepList, (item) => item.status === 'processing'),
      'id',
    );
    if (!taskId) {
      // 当前无processing，定位到第一个
      taskId = get(stepList, '[0].id');
    }
    return taskId;
  }
};

export const ClusterLog = ({ recordID, onClose }: { recordID?: string; onClose: () => void }) => {
  const [state, updater] = useUpdate({
    isStdErr: false,
    stepList: [],
    userChosenTaskId: undefined as string | undefined,
    pureLog: [],
  });

  const { getClusterLogTasks } = clusterStore.effects;

  useEffectOnce(() => {
    return () => {
      clearGetLogStep();
    };
  });

  React.useEffect(() => {
    if (recordID) {
      getLogStep();
    } else {
      updater.stepList([]);
      updater.isStdErr(false);
      updater.userChosenTaskId(undefined);
      updater.pureLog([]);
      clearGetLogStep();
    }
  }, [recordID]);

  const clearGetLogStep = () => {
    clearTimeout(intervalObj);
  };

  const getLogStep = () => {
    clearGetLogStep();
    recordID &&
      getClusterLogTasks({ recordIDs: recordID }).then((res: any) => {
        if (get(res, 'list[0].pipelineDetail')) {
          updater.stepList(getStepFromTasks(res));
        } else if (get(res, 'list[0].detail')) {
          updater.pureLog(getPureLog(res));
        }
      });
    intervalObj = setTimeout(() => {
      getLogStep();
    }, 5000);
  };

  const switchLog = (
    <Switch
      checkedChildren={i18n.t('error')}
      unCheckedChildren={i18n.t('standard')}
      checked={state.isStdErr}
      onChange={() => updater.isStdErr(!state.isStdErr)}
    />
  );
  const stream = state.isStdErr ? 'stderr' : 'stdout';
  const curTaskId = state.userChosenTaskId || getCurTaskId(state);
  return (
    <Drawer title={i18n.t('operation log')} visible={!!recordID} onClose={onClose} width="60%" destroyOnClose>
      {isEmpty(state.pureLog) || (curTaskId && recordID) ? null : (
        <div className="cluster-log-pure-log">
          {map(state.pureLog, (item, idx) => {
            return <div key={`${idx}`}>{item}</div>;
          })}
        </div>
      )}
      <StepListComp
        stepData={state.stepList}
        activeStep={curTaskId}
        onClickStep={(taskId: string) => {
          updater.userChosenTaskId(taskId);
          getLogStep();
        }}
      />
      {curTaskId && recordID ? (
        <LogRoller
          key={`${stream}_${curTaskId}_${recordID}`}
          query={{
            fetchApi: '/api/node-logs',
            recordID,
            taskID: curTaskId,
            stream,
          }}
          extraButton={switchLog}
          logKey={`cluster-op-${recordID}`}
        />
      ) : null}
    </Drawer>
  );
};
