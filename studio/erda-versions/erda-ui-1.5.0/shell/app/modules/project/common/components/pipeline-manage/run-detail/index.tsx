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
import { Spin, Button, Menu, Modal } from 'antd';
import BaseInfo from './base-info';
import RecordList from './record-list';
import { BuildLog } from 'application/pages/build-detail/build-log';
import { IF, DeleteConfirm, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { isEmpty, get, map, isNumber, flatten } from 'lodash';
import { useUnmount } from 'react-use';
import { ciBuildStatusSet } from './config';
import autoTestStore from 'project/stores/auto-test-case';
import CasePipelineChart from './pipeline-chart';
import { FormModal } from 'app/configForm/nusi-form/form-modal';
import { ymlDataToFormData } from 'app/yml-chart/common/in-params-drawer';
import SnippetDetail from './snippet-detail';
import { ResultViewDrawer } from './result-view';
import i18n from 'i18n';
import './index.scss';

const { confirm } = Modal;
let inParamsKey = 1;
interface IProps {
  runKey: number;
  scope: string;
}
const { runningStatus } = ciBuildStatusSet;

const noop = () => {};
let tc = -1 as any;
const requestInterval = 10000; // 前端自动轮询（snippet的状态暂时无法通过socket推送）
const RunDetail = (props: IProps) => {
  const { runKey, scope } = props;
  const [pipelineDetail] = autoTestStore.useStore((s) => [s.pipelineDetail]);

  const recordRef = React.useRef(null as any);

  const {
    getPipelineDetail,
    cancelBuild: cancelBuildCall,
    runBuild: runBuildCall,
    reRunFailed,
    reRunEntire,
    updateTaskEnv,
    clearPipelineDetail,
  } = autoTestStore;
  const inParamsFormRef = React.useRef(null as any);
  const [
    {
      isBlocked,
      inParamsFormVis,
      logVisible,
      logProps,
      isFetching,
      snippetDetailVis,
      snippetDetailProps,
      resultVis,
      chosenTask,
    },
    updater,
    update,
  ] = useUpdate({
    isFetching: false, // 自动刷新的时候不loading
    startStatus: 'unstart', // unstart-未开始，ready-准备开始，start-已开始,end:执行完成或取消
    logVisible: false,
    logProps: {},
    isBlocked: false,
    inParamsFormVis: false,
    snippetDetailVis: false,
    snippetDetailProps: {},
    resultVis: false,
    chosenTask: {},
  });

  useUnmount(() => {
    clearTimeout(tc);
    clearPipelineDetail();
  });
  const { id: pipelineID, pipelineButton, extra, runParams, pipelineSnippetStages = [] } = pipelineDetail || {};

  const onSelectPipeline = (p: PIPELINE.IPipeline) => {
    if (p && p.id) {
      updater.isFetching(true);
      getPipelineDetail({ pipelineID: p.id }).then(() => {
        updater.isFetching(false);
      });
    }
  };

  React.useEffect(() => {
    clearTimeout(tc);
    if (pipelineDetail && runningStatus.includes(pipelineDetail.status)) {
      tc = setTimeout(() => {
        getPipelineDetail({ pipelineID: pipelineDetail.id }).then(() => {
          if (recordRef.current && recordRef.current.reload) {
            recordRef.current.reload();
          }
        });
      }, requestInterval);
    }
  }, [getPipelineDetail, pipelineDetail]);

  const getSnippetNode = (node: PIPELINE.ITask, pos: number[] = []) => {
    const [xPos] = pos;
    let taskList = [];

    if (isNumber(xPos)) {
      taskList = get(pipelineSnippetStages, `[${xPos}].pipelineTasks`);
    } else {
      taskList = flatten(map(pipelineSnippetStages, (item) => item.pipelineTasks));
    }
    const nodeList = [] as PIPELINE.ITask[];
    map(taskList, (subItem) => {
      if (subItem.name.startsWith(`${node.name}_`)) {
        nodeList.push(subItem);
      }
    });
    return nodeList;
  };

  const onClickNode = (node: PIPELINE.ITask, clickTarget: string) => {
    const { type } = node;
    const xIndex = get(node, '_external_.xIndex');
    const yIndex = get(node, '_external_.yIndex');
    switch (clickTarget) {
      case 'log':
        if (type === 'snippet') {
          const _nodeList = getSnippetNode(node, [xIndex, yIndex]);
          _nodeList.length &&
            update({
              snippetDetailVis: true,
              snippetDetailProps: {
                detailType: 'log',
                dataList: _nodeList,
              },
            });
        } else {
          update({
            logVisible: true,
            logProps: {
              logId: node.extra.uuid,
              title: i18n.t('msp:log details'),
              customFetchAPIPrefix: `/api/apitests/pipeline/${pipelineID}/task/${node.id}/logs`,
              pipelineID,
              taskID: node.id,
              downloadAPI: '/api/apitests/logs/actions/download',
            },
          });
        }
        break;
      case 'result':
        if (type === 'snippet') {
          const _nodeList = getSnippetNode(node, [xIndex, yIndex]);
          _nodeList.length &&
            update({
              snippetDetailVis: true,
              snippetDetailProps: {
                detailType: 'result',
                dataList: _nodeList,
              },
            });
        } else {
          update({ resultVis: true, chosenTask: node });
        }
        break;
      case 'node':
        // const hasStarted = startStatus !== 'unstart';
        // if (!hasStarted && pipelineDetail && pipelineDetail.status === 'Analyzed') {
        //   nodeClickConfirm(node);
        // }

        break;
      default:
        break;
    }
  };

  const nodeClickConfirm = (node: PIPELINE.ITask) => {
    const disabled = node.status === 'Disabled';
    confirm({
      title: i18n.t('ok'),
      className: 'node-click-confirm',
      content: i18n.t('dop:whether {action} task {name}', {
        action: disabled ? i18n.t('open') : i18n.t('close'),
        name: node.name,
      }),
      onOk: () => updateEnv({ id: node.id, disabled: !disabled }),
      onCancel: noop,
    });
  };

  const updateEnv = (info: { id: number; disabled: boolean }) => {
    const { id, disabled } = info;
    updateTaskEnv({ taskID: id, disabled, pipelineID: pipelineDetail.id }).then(() => {
      getPipelineDetail({ pipelineID });
    });
  };

  const beforeRunBuild = () => {
    if (isEmpty(runParams)) {
      // 没有入参
      runBuild();
    } else {
      updater.inParamsFormVis(true);
    }
  };
  const runBuild = (runPipelineParams?: any) => {
    updater.startStatus('ready');
    runBuildCall({ pipelineID, runPipelineParams })
      .then((result: any) => {
        if (result.success) {
          updater.startStatus('start');
        } else {
          updater.startStatus('unstart');
        }
      })
      .catch(() => updater.startStatus('unstart'));
  };

  const cancelBuild = () => {
    cancelBuildCall({ pipelineID }).then(() => {
      clearTimeout(tc);
      getPipelineDetail({ pipelineID: pipelineDetail.id });
    });
  };

  const getInParamsValue = (_pipelineParams: PIPELINE.IPipelineInParams[]) => {
    const _values = {} as Obj;
    map(_pipelineParams, (item: PIPELINE.IPipelineInParams) => {
      if (item.value !== undefined && item.value !== null) _values[item.name] = item.value;
    });
    return _values;
  };

  const reRunPipeline = (isEntire: boolean) => {
    updater.startStatus('padding');
    const runPipelineParams = getInParamsValue(runParams); // 重跑：获取当前的入参值，给到新建的那个流水线；
    const reRunFunc = !isEntire ? reRunFailed : reRunEntire;
    reRunFunc({ pipelineID, runPipelineParams })
      .then(() => {
        updater.startStatus('start');
      })
      .catch(() => updater.startStatus('unstart'));
  };

  const renderReRunMenu = () => {
    const { canRerunFailed, canRerun } = pipelineButton || {};
    return (
      <Menu>
        {canRerunFailed && (
          <Menu.Item>
            <span
              className={isBlocked ? 'disabled' : ''}
              onClick={() => {
                reRunPipeline(false);
              }}
            >{`${i18n.t('dop:rerun failed node')}(${i18n.t('dop:commit unchanged')})`}</span>
          </Menu.Item>
        )}
        {canRerun && (
          <Menu.Item>
            <span
              className={isBlocked ? 'disabled' : ''}
              onClick={() => {
                reRunPipeline(true);
              }}
            >{`${i18n.t('dop:rerun whole pipeline')}(${i18n.t('dop:commit unchanged')})`}</span>
          </Menu.Item>
        )}
      </Menu>
    );
  };

  const renderRunBtn = () => {
    const { canCancel } = pipelineButton || {};
    // 自动化测试此处无新建流水线概念，新建即执行，故暂时留一个cancel
    return (
      <IF check={canCancel}>
        <DeleteConfirm
          title={`${i18n.t('dop:confirm to cancel the current build')}?`}
          secondTitle=""
          onConfirm={() => {
            cancelBuild();
          }}
        >
          <div className="build-operator">
            <Button className="mr-2">{i18n.t('dop:cancel build')}</Button>
          </div>
        </DeleteConfirm>
      </IF>
    );
  };

  const inParamsProps = React.useMemo(() => {
    // yml数据转为表单fields
    const _fields = ymlDataToFormData(runParams);
    inParamsKey += 1;
    return { fieldList: _fields, formData: getInParamsValue(runParams), key: inParamsKey };
  }, [runParams]);

  const { showMessage } = extra || {};
  const hideLog = () => {
    update({
      logVisible: false,
      logProps: {},
    });
  };

  const closeSnippetDetail = () => {
    update({
      snippetDetailProps: {},
      snippetDetailVis: false,
    });
  };

  const closeResult = () => {
    update({ resultVis: false, chosenTask: {} });
  };

  return (
    <div className="pipeline-detail">
      <Spin spinning={isFetching}>
        <div className="info-header mb-2">
          <div>
            <span className="font-medium title">{i18n.t('dop:build detail')}</span>
          </div>
          <div className="info-header-right">
            {renderRunBtn()}
            <RecordList
              ref={recordRef}
              key={runKey}
              curPipelineDetail={pipelineDetail}
              onSelectPipeline={onSelectPipeline}
              scope={scope}
            />
          </div>
        </div>
        <BaseInfo data={pipelineDetail} />
        {showMessage && showMessage.msg ? (
          <div className="auto-test-detail-err-msg mb-2">
            <div className="auto-test-err-header">
              <ErdaIcon type="tishi" size="18px" className="auto-test-err-icon" />
              <pre>{showMessage.msg}</pre>
            </div>
            <div className="auto-test-err-stack">
              <ul style={{ listStyle: 'disc' }}>
                {showMessage.stacks.map((stack, i) => (
                  <li key={`${stack}-${String(i)}`}>
                    <pre style={{ overflow: 'hidden', whiteSpace: 'pre-wrap' }}>{stack}</pre>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ) : null}
        <CasePipelineChart scope={scope} data={pipelineDetail} onClickNode={onClickNode} />
      </Spin>
      <FormModal
        title={i18n.t('dop:please enter params')}
        onCancel={() => updater.inParamsFormVis(false)}
        onOk={(inParams: any) => {
          runBuild(inParams);
        }}
        formRef={inParamsFormRef}
        visible={inParamsFormVis}
        {...inParamsProps}
        // formData={editData}
      />
      <BuildLog visible={logVisible} hideLog={hideLog} {...logProps} />
      <SnippetDetail
        pipelineDetail={pipelineDetail}
        visible={snippetDetailVis}
        onClose={closeSnippetDetail}
        {...snippetDetailProps}
      />
      <ResultViewDrawer visible={resultVis} onClose={closeResult} data={chosenTask} />
    </div>
  );
};

export default RunDetail;
