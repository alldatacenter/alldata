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
import DiceConfigPage from 'app/config-page';
import { ErrorBoundary, FileEditor, ErdaIcon } from 'common';
import { Button, message, Input, Checkbox, Tooltip } from 'antd';
import routeInfoStore from 'core/stores/route';
import { statusColorMap } from 'app/config-page/utils';
import { useUpdateEffect, useThrottleFn } from 'react-use';
import { get } from 'lodash';
import { Form as PureForm } from 'dop/pages/form-editor/index';
import agent from 'agent';
import moment from 'moment';

import './debug.scss';

const stateIconMap = {
  success: <ErdaIcon type="check-one" fill={statusColorMap.success} />,
  error: <ErdaIcon type="close-one" fill={statusColorMap.error} />,
};

const DebugConfigPage = () => {
  const pageRef = React.useRef(null);
  const cacheData = window.localStorage.getItem('config-page-debug');
  const [text, setText] = React.useState(cacheData || defaultJson);
  const [logs, setLogs] = React.useState<ILog[]>([]);
  const [showCode, setShowCode] = React.useState(true);
  const [showLog, setShowLog] = React.useState(false);
  const [importValue, setImportValue] = React.useState('');
  const [activeLog, setActiveLog] = React.useState(0);
  const { url, scenario, debug, ...restQuery } = routeInfoStore.useStore((s) => s.query);
  const [proxyApi, setProxyApi] = React.useState(url);
  const _defaultData = scenario
    ? {
        scenario: {
          scenarioKey: scenario,
          scenarioType: scenario,
        },
        inParams: restQuery,
      }
    : defaultData;
  const [config, setConfig] = React.useState(_defaultData);

  useThrottleFn<string, any>(
    (newText) => {
      window.localStorage.setItem('config-page-debug', newText);
      return newText;
    },
    5000,
    [text],
  );

  useUpdateEffect(() => {
    if (activeLog) {
      setConfig(logs?.[activeLog - 1]?.pageData);
    }
  }, [activeLog]);

  const getMock = React.useCallback(
    (payload: any) => {
      return agent
        .post(proxyApi)
        .send(payload)
        .then((response: any) => {
          return response.body.protocol ? response.body : response.body.data;
        });
    },
    [proxyApi],
  );

  if (!debug) {
    return <DiceConfigPage scenarioType={scenario} scenarioKey={scenario} inParams={restQuery} />;
  }

  const updateMock = (_text?: string) => {
    try {
      const obj = new Function(`return ${_text || text}`)();
      setConfig(obj);
    } catch (error) {
      message.error('内容有错误');
    }
  };

  const onExecOp = ({ cId, op, reload, updateInfo, pageData }: any) => {
    setLogs((prev) => {
      const reLogs = prev.concat({
        time: moment().format('HH:mm:ss'),
        type: '操作',
        cId,
        opKey: op.text || op.key,
        command: JSON.stringify(op.command, null, 2),
        reload,
        data: JSON.stringify(updateInfo, null, 2),
        pageData,
      });
      setActiveLog(reLogs.length);
      return reLogs;
    });
  };

  const exportLog = () => {
    const reLogs = logs.map((item) => {
      const { assertList, ...rest } = item;
      const _asserts = assertList?.filter((aItem) => {
        return aItem.key && aItem.operator && aItem.value;
      });
      return { ...rest, assertList: _asserts };
    });
    const blob = new Blob([JSON.stringify(reLogs, null, 2)], { type: 'application/vnd.ms-excel;charset=utf-8' });
    const fileName = `assert-log.txt`;
    const objectUrl = URL.createObjectURL(blob);
    const downloadLink = document.createElement('a');
    downloadLink.href = objectUrl;
    downloadLink.setAttribute('download', fileName);
    document.body.appendChild(downloadLink);
    downloadLink.click();
    window.URL.revokeObjectURL(downloadLink.href);
  };

  const importLog = () => {
    try {
      const obj = new Function(`return ${importValue}`)();
      setLogs(obj || []);
      setActiveLog((obj || []).length);
    } catch (error) {
      message.error('内容有错误');
    }
  };

  return (
    <div className="h-full debug-page-container flex flex-col">
      <div className="flex justify-between mb-1 item-center">
        <div className="w-52">
          <Checkbox checked={showCode} onChange={(e) => setShowCode(e.target.checked)}>
            代码
          </Checkbox>
          <Checkbox className="ml-2" checked={showLog} onChange={(e) => setShowLog(e.target.checked)}>
            日志
          </Checkbox>
        </div>
        <Input value={proxyApi} size="small" onChange={(e) => setProxyApi(e.target.value)} />
      </div>
      <div className="debug-page flex-1 h-0 flex justify-between items-center">
        <div className={`flex flex-col left h-full  ${showCode || showLog ? '' : 'hide-left'}`}>
          {showCode ? (
            <div className="flex-1">
              <FileEditor
                autoHeight
                fileExtension="json"
                valueLimit={false}
                value={text}
                onChange={(_text) => {
                  setText(_text);
                  updateMock(_text);
                }}
              />
              <Button type="primary" className="update-button" onClick={() => updateMock()}>
                更新
              </Button>
              <Button
                type="primary"
                className="request-button"
                onClick={() => {
                  pageRef.current.reload(config);
                }}
              >
                请求
              </Button>
            </div>
          ) : null}
          {showLog ? (
            <div className={`log-panel mt-2`}>
              <h3>
                操作日志
                <span
                  className="ml-2 fake-link"
                  onClick={() => {
                    setLogs([]);
                    setActiveLog(0);
                  }}
                >
                  清空
                </span>
                <span className="ml-2 fake-link" onClick={exportLog}>
                  导出
                </span>
                <Tooltip
                  overlayStyle={{ width: 400, maxWidth: 400 }}
                  title={
                    <div>
                      <Input.TextArea value={importValue} onChange={(e) => setImportValue(e.target.value)} />
                      <Button size="small" type="primary" onClick={importLog}>
                        导入
                      </Button>
                    </div>
                  }
                >
                  <span className="ml-2 fake-link" onClick={importLog}>
                    导入
                  </span>
                </Tooltip>
              </h3>
              {logs.map((log, i) => {
                return (
                  <LogItem
                    key={i}
                    index={i + 1}
                    activeLog={activeLog}
                    setActiveLog={(l) => setActiveLog(l)}
                    log={log}
                    setLog={(_log) => setLogs((prev) => prev.map((item, idx) => (idx === i ? _log : item)))}
                  />
                );
              })}
            </div>
          ) : null}
        </div>
        <div className={`right overflow-auto h-full ${showCode || showLog ? '' : 'full-right'}`}>
          <ErrorBoundary>
            <DiceConfigPage
              ref={pageRef}
              showLoading
              scenarioType={scenario || config?.scenario?.scenarioType}
              scenarioKey={scenario || config?.scenario?.scenarioKey}
              inParams={config?.inParams}
              debugConfig={config}
              onExecOp={onExecOp}
              useMock={getMock}
              forceMock={!!proxyApi}
              updateConfig={(v) => {
                setConfig(v);
                setText(JSON.stringify(v, null, 2));
              }}
            />
          </ErrorBoundary>
        </div>
      </div>
    </div>
  );
};

export default DebugConfigPage;
interface ILogItemProps {
  index: number;
  log: ILog;
  activeLog: number;
  setLog: (log: ILog) => void;
  setActiveLog: (n: number) => void;
}

interface ILog {
  time: string;
  reload: boolean;
  type: string;
  cId: string;
  opKey: string;
  data: Obj;
  command: Obj;
  assertList: IAssert[];
  pageData: Obj;
}

interface IAssert {
  key: string;
  operator: string;
  value: string;
}

const LogItem = (props: ILogItemProps) => {
  const { log, index, setLog, activeLog, setActiveLog } = props;

  const AssertForm = (
    <PureForm
      onChange={(d) => {
        setLog({ ...log, assertList: d.assertList });
      }}
      value={log}
      fields={[
        {
          key: 'assertList',
          label: '断言',
          labelTip: '请依次填写断言key、比较、value',
          component: 'arrayObj',
          required: true,
          componentProps: {
            direction: 'row',
            objItems: [
              {
                component: 'input',
                componentProps: {
                  placeholder: '断言key值',
                  size: 'small',
                },
                key: 'key',
                labelTip: '例如：memberTable.state.value',
                options: 'k1:TCP',
                required: true,
              },
              {
                component: 'select',
                options: getOperatorOptions(),
                key: 'operator',
                required: true,
                componentProps: {
                  size: 'small',
                },
              },
              {
                component: 'input',
                componentProps: {
                  placeholder: '断言值',
                  size: 'small',
                },
                key: 'value',
                required: true,
              },
              {
                key: 'state',
                getComp: (assert: IAssert) => {
                  const { key, operator, value } = assert;
                  if (key && operator && value) {
                    const curState = getAssertState(assert, log.pageData);
                    return <div className="ml-1">{curState ? stateIconMap.success : stateIconMap.error}</div>;
                  }
                  return null;
                },
              },
            ],
          },
        },
      ]}
    />
  );

  const setActive = () => {
    activeLog !== index && setActiveLog(index);
  };

  return (
    <div className={`log-item py-2 cursor-pointer ${activeLog === index ? 'active-item' : ''}`} onClick={setActive}>
      <span>
        {index}: {log.reload && <ErdaIcon type="refresh1" />} {log.type} {log.cId}.{log.opKey}
      </span>
      {(log.data || log.command) && (
        <Tooltip
          placement="top"
          overlayStyle={{ maxWidth: 600 }}
          title={
            <div>
              {log.command ? (
                <>
                  <span>command: </span>
                  <pre className="code-block overflow-auto" style={{ maxHeight: 200 }}>
                    {log.command}
                  </pre>
                </>
              ) : null}
              {log.data ? (
                <>
                  <span>data: </span>
                  <pre className="code-block overflow-auto mt-2" style={{ maxHeight: 200 }}>
                    {log.data}
                  </pre>
                </>
              ) : null}
            </div>
          }
        >
          <span className="fake-link px-1">查看数据</span>
        </Tooltip>
      )}
      <Tooltip title={AssertForm} trigger="click" placement="right" overlayStyle={{ width: 600, maxWidth: 600 }}>
        <span className="px-1 fake-link">断言</span>
      </Tooltip>
    </div>
  );
};

const operatorMap = [
  { key: '=', name: '等于' },
  { key: '!=', name: '不等于' },
  { key: '>', name: '大于' },
  { key: '>=', name: '大等于' },
  { key: '<', name: '小于' },
  { key: '<=', name: '小等于' },
  { key: 'includesBy', name: '包含于' },
  { key: 'includes', name: '包含' },
];

const getOperatorOptions = () => {
  return operatorMap.map(({ key, name }) => `${key}:${name}`).join(';');
};

const getAssertState = (assert: IAssert, pageData: Obj) => {
  const { key, value, operator } = assert;

  let state = false;
  const curValue = JSON.stringify(get(pageData, key));
  switch (operator) {
    case '=':
      state = curValue === value;
      break;
    case '!=':
      state = curValue !== value;
      break;
    case '>':
      state = curValue > value;
      break;
    case '>=':
      state = curValue >= value;
      break;
    case '<':
      state = curValue < value;
      break;
    case '<=':
      state = curValue <= value;
      break;
    case 'includesBy':
      state = value.includes(curValue);
      break;
    case 'includes':
      state = curValue?.includes(value);
      break;
    default:
      break;
  }
  return state;
};

const defaultData = {
  scenario: {
    scenarioType: 'project-list-my',
    scenarioKey: 'project-list-my',
  },
  inParams: {},
};
const defaultJson = JSON.stringify(defaultData, null, 2);
