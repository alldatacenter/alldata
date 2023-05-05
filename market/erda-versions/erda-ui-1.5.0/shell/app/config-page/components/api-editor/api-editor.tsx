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

import { cutStr, qs } from 'common/utils';
import classnames from 'classnames';
import { Copy, EditList, EmptyListHolder, FileEditor, Title, ErdaIcon } from 'common';
import { validateValue } from 'common/components/edit-list';
import { isArray, isEmpty, isString, map, reduce, set, cloneDeep, find, reject, last, get } from 'lodash';
import {
  Badge,
  Button,
  Input,
  Popconfirm,
  Popover,
  Radio,
  Select,
  Table,
  Tabs,
  Spin,
  Modal,
  message,
  Dropdown,
  Menu,
  Tooltip,
} from 'antd';
import { Form as ConfigForm } from 'dop/pages/form-editor/index';
import React from 'react';
import { produce } from 'immer';
import i18n from 'i18n';
import './api-editor.scss';

const formatJSON = (str: string) => {
  let res = str;
  try {
    res = JSON.stringify(JSON.parse(str), null, 2);
  } catch (e) {
    message.error(i18n.t('dop:the current input content is invalid JSON'));
  }
  return typeof res === 'string' ? res : '';
};

const formatStrToKeyValue = (str: string) => {
  const obj = qs.parse(str);
  return Object.keys(obj).map((key) => ({
    key,
    value: obj[key],
  }));
};

const getConf = (data: Record<string, any>, key: string | string[]) => {
  return isArray(key)
    ? reduce(
        key,
        (obj, k) => {
          return { ...obj, [k]: data[k] };
        },
        {},
      )
    : data[key];
};

const processTemp =
  (execOperation: Function) =>
  (temp = []) => {
    return map(temp, (item) => {
      const { render } = item;
      if (render?.type === 'inputSelect') {
        const p = {} as Obj;
        const { operations: rOps, valueConvertType, props: rProps, ...renderRest } = render || {};
        if (rOps?.onSelectOptionParams) {
          p.onLoadData = (_selectOpt: any) => {
            execOperation(rOps.onSelectOptionParams, _selectOpt);
          };
        }
        p.valueConvert = (str: string[]) => {
          let v = str.join('');
          switch (valueConvertType) {
            case 'last':
              v = last(str) as string;
              break;
            default:
              break;
          }
          return v;
        };

        return {
          ...item,
          render: { ...renderRest, props: { ...rProps, ...p } },
        };
      }
      return { ...item };
    });
  };

const { Option } = Select;
const { TabPane } = Tabs;
const { TextArea } = Input;

const BODY_RAW_OPTION = ['text/plain', 'application/json', 'application/x-www-form-urlencoded'];

const formatTip = i18n.t('dop:four parameter references').replace(/</g, '{').replace(/>/g, '}');

const tip = () => (
  <div className="json-format-tip">
    <p className="json-format-tip-title">*{i18n.t('Instructions')}</p>
    <p className="json-format-tip-title">{i18n.t('dop:Method of parameter refers to variable')}</p>
    {map(formatTip.split(';'), (item) => (
      <div>
        <p className="json-format-tip-type">{item.split('/')[0]}</p>
        <p className="json-format-tip-content">{item.split('/')[1]}</p>
      </div>
    ))}
  </div>
);

export const APIEditor = (props: CP_API_EDITOR.Props) => {
  const { execOperation, operations, state } = props;
  const configProps = produce(props.props, (draft) => {
    const { commonTemp } = draft;
    map(commonTemp.target || [], (item) => {
      set(draft, `${item}.temp`, commonTemp.temp);
    });
  });
  const {
    index,
    executingMap = {},
    methodList,
    visible = true,
    apiExecute,
    showSave = true,
    loopFormField,
  } = configProps;
  const { data = {}, attemptTest } = state;
  const [api, setAPI] = React.useState(data?.apiSpec || {});
  const [loop, setLoop] = React.useState({ loop: data?.loop });

  const processTempFun = processTemp(execOperation);

  React.useEffect(() => {
    const apiSpec = produce(data?.apiSpec || {}, (draft) => {
      const headers = draft.headers || [];
      const updateHeader = (type: string) => {
        const exist = find(headers, { key: 'Content-Type' });
        if (exist) {
          exist.value = type;
        } else {
          headers.push({ key: 'Content-Type', value: type, desc: '' });
        }
        return headers;
      };
      // compatible with old data
      if (draft.body?.type.includes('Text')) {
        set(draft, 'body.type', BODY_RAW_OPTION[0]);
        set(draft, 'headers', updateHeader(BODY_RAW_OPTION[0]));
      } else if (draft.body?.type.includes('JSON')) {
        set(draft, 'body.type', BODY_RAW_OPTION[1]);
        set(draft, 'headers', updateHeader(BODY_RAW_OPTION[1]));
      } else if (draft.body?.type === BODY_RAW_OPTION[2]) {
        set(draft, 'body.content', formatStrToKeyValue(draft.body.content));
      }
    });
    setAPI(apiSpec);
    setLoop({ loop: data?.loop });
  }, [data]);

  const handleClose = () => {
    execOperation(operations.close);
  };

  const validateSpec = React.useCallback(
    (saveData: { apiSpec: CP_API_EDITOR.API }) => {
      let errMsg = '';
      const { name, url, out_params = [], method, asserts = [], headers, params, body } = saveData.apiSpec;
      if (!name) {
        errMsg = i18n.t('{name} can not empty', { name: i18n.t('interface name') });
      }
      if (!method && !errMsg) {
        errMsg = i18n.t('{name} can not empty', { name: i18n.t('request method') });
      }
      if (!url && !errMsg) {
        errMsg = i18n.t('{name} can not empty', { name: i18n.t('interface path') });
      }
      if (!isEmpty(params) && !errMsg) {
        errMsg = validateValue(get(configProps, 'params.temp', []), params);
      }
      if (!isEmpty(headers) && !errMsg) {
        errMsg = validateValue(get(configProps, 'headers.temp', []), headers);
      }
      if (body.type === BasicForm && !isEmpty(body.content) && !errMsg) {
        errMsg = validateValue(get(configProps, 'body.form.temp', []), body.content);
      }
      if (!isEmpty(out_params) && !errMsg) {
        const temp = {};
        out_params.forEach((item) => {
          if (temp[item.key]) {
            errMsg = i18n.t('out params exist the same {key}', { key: item.key });
          } else {
            temp[item.key] = true;
          }
          if (!errMsg && item.key && !item.expression) {
            errMsg = i18n.t('out params {name} setting is abnormal', { name: item.key });
          }
        });
      }
      const allowEmpty = configProps.asserts.comparisonOperators.filter((t) => t.allowEmpty).map((t) => t.value);
      if (!isEmpty(asserts) && !errMsg) {
        asserts.forEach((item) => {
          if (!errMsg && item.arg && !allowEmpty.includes(item.operator) && !item.value) {
            errMsg = i18n.t('assert {name} setting is abnormal', { name: item.arg });
          }
        });
      }
      return {
        errMsg,
        value: {
          ...saveData,
          apiSpec: {
            ...saveData.apiSpec,
            out_params: (out_params || []).filter((item: any) => item.key && item.expression),
            asserts: (asserts || []).filter((item: any) => {
              if (allowEmpty.includes(item.operator)) {
                return !!item.arg;
              } else {
                return Object.values(item).every((t) => t);
              }
            }),
          },
        },
      };
    },
    [configProps],
  );

  const handleSave = React.useCallback(
    (payload?: any) => {
      if (operations?.onChange) {
        const saveData = { ...data, apiSpec: payload || api };
        const { errMsg, value } = validateSpec(saveData);
        if (errMsg) {
          message.error(errMsg);
          return;
        }
        execOperation(operations.onChange, { data: { ...value, ...loop } });
      }
    },
    [api, loop, data, execOperation, operations, validateSpec],
  );

  if (!visible) return null;

  const updateApi = (k: string, v: any, autoSave = false, adjustData?: Function) => {
    setAPI((prev) => {
      const newAPI = produce(prev, (draft) => {
        set(draft, k, v);
        switch (k) {
          // 同步url和params
          case 'url': {
            const { query } = qs.parseUrl(v, { arrayFormat: undefined }); // 使用a=b&a=c格式解析
            const paramList: any = [];
            map(query, (qv: string | string[], qk: string) => {
              if (Array.isArray(qv)) {
                qv.forEach((vs: string) => {
                  paramList.push({
                    key: qk,
                    value: vs,
                    desc: '',
                  });
                });
              } else {
                paramList.push({
                  key: qk,
                  value: qv,
                  desc: '',
                });
              }
            });
            set(draft, 'params', paramList);
            break;
          }
          case 'params': {
            const { url } = qs.parseUrl(draft.url || '');
            const queryStr: string[] = [];
            map(v, (item: any) => {
              if (item.key && item.value) {
                queryStr.push(`${item.key}=${item.value}`);
              }
            });
            set(draft, 'url', `${url}?${queryStr.join('&')}`);
            break;
          }
          default:
        }
        if (adjustData) {
          adjustData(draft, k);
        }
      });
      if (autoSave) {
        handleSave(newAPI);
      }
      return newAPI;
    });
  };

  const handleExecute = (e: any, allowSave: boolean) => {
    const curOp = find(apiExecute.menu, { key: e.key });
    if (curOp?.operations?.click) {
      if (allowSave) {
        const saveData = { ...data, apiSpec: api };
        const { errMsg, value } = validateSpec(saveData);
        if (errMsg) {
          message.error(errMsg);
          return;
        }
        execOperation(curOp.operations.click, { data: { ...value, ...loop } });
      } else {
        execOperation(curOp.operations.click);
      }
    }
  };

  let apiExecuteButton = null;
  if (!isEmpty(apiExecute)) {
    const { menu, text, allowSave, ...rest } = apiExecute;
    const dropdownMenu = (
      <Menu
        onClick={(e) => {
          handleExecute(e, allowSave);
        }}
      >
        {menu.map((mItem) => {
          return <Menu.Item key={mItem.key}>{mItem.text}</Menu.Item>;
        })}
      </Menu>
    );
    apiExecuteButton = (
      <Dropdown overlay={dropdownMenu}>
        <Button {...rest} className="inline-flex items-center ml-3">
          {text}
          <ErdaIcon type="caret-down" className="ml-1" />
        </Button>
      </Dropdown>
    );
  }

  const isShow = true;
  const curExecuteResult = attemptTest?.data || {};
  let assertResult: any[] = [];
  let assertSuccess: boolean | undefined;
  const responseHeaders: object[] = [];
  const requestHeaders: object[] = [];
  const requestParams: object[] = [];
  const columns = [
    {
      title: 'Key',
      dataIndex: 'name',
      key: 'name',
      width: '50%',
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      width: '50%',
      render: (text: string) => <Copy>{text}</Copy>,
    },
  ];
  let resultTabs = null;
  const showPlanExecuteResult = ['Passed', 'Failed'].includes(attemptTest?.status);
  if (!isEmpty(curExecuteResult) || showPlanExecuteResult) {
    let response: any = {};
    let request: any = {};
    response = curExecuteResult?.response || {};
    request = curExecuteResult?.request || {};
    assertResult = curExecuteResult?.asserts ? curExecuteResult.asserts.result : [];
    assertSuccess = curExecuteResult?.asserts ? curExecuteResult.asserts.success : undefined;
    const { body, headers, status } = response;
    const isSuccess = status < 400 && status >= 200;
    const statusColor = classnames({
      'ml-1': true,
      'text-success': isSuccess,
      'text-danger': !isSuccess,
    });
    let responseBody = <pre className="response-body">{JSON.stringify(body, null, 2)}</pre>;
    let isRequestJson = false;
    map(headers, (v: string[], k: string) => {
      map(v, (item: string) => {
        responseHeaders.push({
          name: k,
          value: item,
        });
        if (k === 'Content-Type' && item.includes('application/json')) {
          responseBody = <FileEditor fileExtension="json" value={formatJSON(body)} readOnly />;
        }
        if (k === 'Content-Type' && item.includes('text/html')) {
          responseBody = <FileEditor fileExtension="html" value={body} readOnly style={{ maxHeight: '400px' }} />;
        }
      });
    });
    map(get(request, 'headers') || {}, (v: string[], k: string) => {
      map(v, (item: string) => {
        requestHeaders.push({
          name: k,
          value: item,
        });
        if (k === 'Content-Type' && item === 'application/json') {
          isRequestJson = true;
        }
      });
    });
    map(get(request, 'params') || {}, (v: string[], k: string) => {
      map(v, (item: string) => {
        requestParams.push({
          name: k,
          value: item,
        });
      });
    });
    resultTabs = (
      <div className="api-tabs mt-5">
        <Tabs defaultActiveKey="Response">
          <TabPane key="Request" tab="Request">
            {isEmpty(request) ? (
              <EmptyListHolder />
            ) : (
              <>
                <div className="request-info text-desc p-3">
                  <span className="method mr-3">{get(request, 'method', '')}</span>
                  <span className="url">{get(request, 'url', '')}</span>
                </div>
                <Tabs>
                  <TabPane key="Params" tab="Params">
                    <Table
                      rowKey="name"
                      size="small"
                      pagination={false}
                      columns={columns}
                      dataSource={requestParams}
                      scroll={{ x: '100%' }}
                    />
                  </TabPane>
                  <TabPane key="Headers" tab="Headers">
                    <Table
                      rowKey="name"
                      size="small"
                      pagination={false}
                      columns={columns}
                      dataSource={requestHeaders}
                      scroll={{ x: '100%' }}
                    />
                  </TabPane>
                  <TabPane key="Body" tab="Body" className="body-tab">
                    {isEmpty(get(request, 'body.content')) ? (
                      <EmptyListHolder />
                    ) : (
                      <>
                        <div className="body-type p-3 border-bottom">Type: {get(request, 'body.type', '')}</div>
                        {get(request, 'body.content', '') ? (
                          <>
                            <Button
                              disabled={!get(request, 'body.content')}
                              className="copy-btn cursor-copy copy-request"
                              data-clipboard-text={get(request, 'body.content', '')}
                              shape="circle"
                              icon={<ErdaIcon type="copy" />}
                            />
                            <Copy selector=".copy-request" />
                          </>
                        ) : null}
                        <pre className="response-body">
                          {isRequestJson ? (
                            <FileEditor
                              fileExtension="json"
                              value={formatJSON(get(request, 'body.content', ''))}
                              readOnly
                            />
                          ) : (
                            get(request, 'body.content', '')
                          )}
                        </pre>
                      </>
                    )}
                  </TabPane>
                </Tabs>
              </>
            )}
          </TabPane>
          <TabPane key="Response" tab="Response">
            <Tabs
              defaultActiveKey="Body"
              tabBarExtraContent={
                <span className="mr-3 text-desc">
                  Status:<span className={statusColor}>{status}</span>
                </span>
              }
            >
              <TabPane key="Headers" tab="Headers">
                <Table
                  scroll={{ x: '100%' }}
                  size="small"
                  pagination={false}
                  columns={columns}
                  dataSource={responseHeaders}
                />
              </TabPane>
              <TabPane key="Body" tab="Body">
                {body ? (
                  <>
                    <Button
                      disabled={!body}
                      className="copy-btn cursor-copy copy-response"
                      data-clipboard-text={body}
                      shape="circle"
                      icon={<ErdaIcon type="copy" />}
                    />
                    <Copy selector=".copy-response" />
                  </>
                ) : null}
                {responseBody}
              </TabPane>
            </Tabs>
          </TabPane>
        </Tabs>
      </div>
    );
  }

  return (
    <div className="api-item-editor">
      <Spin size="small" spinning={executingMap[index] || false}>
        <div className="api-title case-index-hover">
          <Title title={i18n.t('interface name')} level={3} />
          <Input
            className="flex-1 mb-6 mt-2"
            placeholder={i18n.t('please enter {name}', { name: i18n.t('interface name') })}
            value={api.name}
            onChange={(e) => updateApi('name', e.target.value)}
            maxLength={50}
            // onBlur={handleBlurCapture}
          />
        </div>
        <div className={`api-content ${isShow ? 'block' : 'hidden'}`}>
          <div className="api-url">
            <Input
              addonBefore={
                <Select
                  style={{ width: 110 }}
                  value={api.method}
                  onChange={(val) => updateApi('method', val, false)}
                  placeholder={i18n.t('dop:please choose')}
                >
                  {map(methodList, (method) => (
                    <Option value={method} key={method}>
                      {method}
                    </Option>
                  ))}
                </Select>
              }
              className="url"
              placeholder={i18n.t('please enter')}
              value={api.url}
              onChange={(e) => updateApi('url', e.target.value.trim())}
              // onBlur={handleBlurCapture}
            />
          </div>
          <div className="api-tabs">
            <Tabs defaultActiveKey="Params">
              {map(ApiTabComps, ({ Comp, dataKey }: any, tab) => {
                let _tab: any = tab;
                if (assertSuccess !== undefined && tab === 'Tests') {
                  // 这里直接使用color属性不行，应该是Badge组件有bug
                  _tab = (
                    <Badge dot className={assertSuccess ? 'test-assert-success' : 'test-assert-error'}>
                      {tab}
                    </Badge>
                  );
                }
                // const renderProps = get(configProps, dataKey, {});
                const renderProps = getConf(configProps, dataKey);
                return (
                  <TabPane tab={_tab} key={tab}>
                    <Comp
                      renderProps={renderProps}
                      processDataTemp={processTempFun}
                      apiObj={api}
                      data={getConf(api, dataKey)}
                      assertResult={assertResult}
                      onChange={(key: string, val: any, autoSave?: boolean, adjustData?: Function) =>
                        updateApi(key, val, autoSave, adjustData)
                      }
                    />
                  </TabPane>
                );
              })}
            </Tabs>
          </div>
          {resultTabs}
        </div>
        <div className="api-editor-footer">
          <Button onClick={handleClose}>{i18n.t('cancel')}</Button>
          {showSave ? (
            <Button
              className="ml-3"
              type="primary"
              onClick={() => {
                handleSave();
              }}
            >
              {i18n.t('save')}
            </Button>
          ) : null}
          {apiExecuteButton}
        </div>
        {loopFormField?.length ? <ConfigForm fields={loopFormField} value={loop} onChange={(v) => setLoop(v)} /> : null}
      </Spin>
    </div>
  );
};

const Empty = () => null;

const AssertTips = () => {
  const format = (tips: string) => {
    return tips.replaceAll('<', '{').replaceAll('>', '}');
  };
  const tips = (
    <ul className="contents ml-4">
      <li className="level1">
        <span className="font-medium">
          {i18n.t('dop:greater than, greater than or equal to, less than, less than or equal to')}:{' '}
        </span>
        {format(i18n.t('dop:supports integers and decimals'))}
      </li>
      <li className="level1">
        <span className="font-medium">{i18n.t('dop:equal to, not equal to')}: </span>
        {format(i18n.t('dop:support integers, decimals, strings, objects (arrays, Map)'))}
      </li>
      <li className="level1">
        <span className="font-medium">{i18n.t('dop:contain, not contain')}: </span>
        {format(i18n.t('dop:support string and regular matching'))}
      </li>
      <li className="level1">
        <span className="font-medium">{i18n.t('dop:empty, not empty')}: </span>
        {format(i18n.t('dop:support judging if arrays, maps and strings are empty'))}
      </li>
      <li className="level1">
        <span className="font-medium">{i18n.t('dop:exist, not exist')}: </span>
        {format(
          i18n.t('dop:Please fill in the json expression of obtained target key to determine if the key exists.'),
        )}
      </li>
      <li className="level1">
        <span className="font-medium">{i18n.t('dop:belong to, not belong to')}: </span>
        {format(i18n.t('dop:support positive and negative integers, 0, and character strings'))}
        <ul className="ml-4">
          <li className="level2">
            {format(
              i18n.t(
                'dop|Numeric value: Please fill in according to mathematical expression standards. It supports both open and closed intervals. For examples: [-20,20] is a set, <[-200,200],-1,2>.',
                { nsSeparator: '|' },
              ),
            )}
          </li>
          <li className="level2">
            {format(
              i18n.t('dop|only supports collections for strings, for example: <"abc","bcd","200","-200">', {
                nsSeparator: '|',
              }),
            )}
          </li>
        </ul>
      </li>
    </ul>
  );

  return (
    <Tooltip placement="topLeft" title={tips} overlayClassName="api-editor-asserts-tips">
      <ErdaIcon className="ml-1" type="help" />
    </Tooltip>
  );
};

const ApiTabComps = {
  Params: {
    dataKey: 'params',
    Comp: (props: any) => {
      const { data, onChange, renderProps, processDataTemp } = props;
      const { temp, ...rest } = renderProps;
      const useTemp = processDataTemp(temp);
      return (
        <EditList
          {...rest}
          value={data || []}
          dataTemp={useTemp}
          onChange={(v) => {
            onChange('params', v, false);
          }}
        />
      );
    },
  },
  Headers: {
    dataKey: 'headers',
    Comp: (props: any) => {
      const { data, onChange, renderProps, processDataTemp } = props;
      const { temp, ...rest } = renderProps;
      const useTemp = processDataTemp(temp);
      return (
        <EditList
          {...rest}
          value={data || []}
          dataTemp={useTemp}
          onChange={(v) => {
            onChange('headers', v, false);
          }}
        />
      );
    },
  },
  Body: {
    dataKey: 'body',
    Comp: (props: any) => <APIBody {...props} />,
  },
  Tests: {
    dataKey: ['out_params', 'asserts', 0],
    Comp: (props: any) => {
      const { data = {}, onChange, assertResult, renderProps } = props;
      const { comparisonOperators } = renderProps.asserts;
      const getOpList = (assertList: object[]) => {
        const opList = map(assertList, (_assert, i) => {
          const res = assertResult[i];
          if (!res) {
            return null;
          }
          return (
            <Popover
              content={<pre className="text-xs">{cutStr(res.actualValue, 200)}</pre>}
              title={i18n.t('dop:actual value')}
              trigger="hover"
            >
              {res.success === true ? (
                <ErdaIcon size="16" className="assert-status success" type="tg" />
              ) : res.success === false ? (
                <ErdaIcon size="16" className="assert-status error" type="wtg" />
              ) : null}
            </Popover>
          );
        });
        return opList;
      };
      return (
        <div className="case-api-tables">
          <div className="table-title">{i18n.t('dop:output parameter')}</div>
          <div className="table-body">
            <KeyValEdit
              type="out_params"
              data={data.out_params}
              order={['key', 'source', 'expression']}
              dataModel={{
                key: '',
                source: 'body:json',
                expression: '',
              }}
              onChange={(val: any, autoSave?: boolean, adjustData?: Function) => {
                onChange('out_params', val, autoSave, adjustData);
              }}
              itemMap={{
                key: {
                  props: {
                    placeholder: i18n.t('dop:output parameter name'),
                  },
                },
                source: {
                  Comp: (p: any) => {
                    const { value, onChange: onCurChange, className = '' } = p;
                    return (
                      <Select
                        value={value || undefined} // 没有值时显示placeholder
                        className={`${className} case-api-test-select`}
                        placeholder={i18n.t('dop:source')}
                        onChange={(val) => {
                          onCurChange(val, false);
                        }}
                      >
                        <Option value="status">status</Option>
                        <Option value="header">Header:K/V</Option>
                        {/* <Option value="cookie">Cookie:K/V</Option> */}
                        <Option value="body:json">Body:JSON(body)</Option>
                      </Select>
                    );
                  },
                },
                expression: {
                  props: {
                    placeholder: 'example: .data.id',
                  },
                  getProps(record: any) {
                    if (record.source === 'status') {
                      return { disabled: true, value: 'status' };
                    }
                    return {};
                  },
                },
              }}
            />
          </div>
          <div className="table-title flex items-center">
            {i18n.t('dop:assertion')} <AssertTips />
          </div>
          <div className="table-body">
            <KeyValEdit
              type="asserts"
              data={data.asserts || []}
              opList={getOpList(data.asserts || [])}
              order={['arg', 'operator', 'value']}
              dataModel={{
                arg: '',
                operator: '',
                value: '',
              }}
              onChange={(val: any, autoSave?: boolean, adjustData?: Function) => {
                onChange('asserts', val, autoSave, adjustData);
              }}
              itemMap={{
                arg: {
                  Comp: (p: any) => {
                    const { value, onChange: onCurChange, className = '' } = p;
                    return (
                      <Select
                        value={value || undefined} // 没有值时显示placeholder
                        className={`${className} case-api-test-select`}
                        placeholder={i18n.t('dop:parameter name')}
                        onChange={(v) => onCurChange(v, false)}
                      >
                        {data.out_params?.map((option: any) => {
                          return option.key === '' ? null : (
                            <Option key={option.key} value={option.key}>
                              {option.key}
                            </Option>
                          );
                        })}
                      </Select>
                    );
                  },
                },
                operator: {
                  Comp: (p: any) => {
                    const { value, onChange: onCurChange, className = '' } = p;
                    return (
                      <Select
                        value={value || undefined} // 没有值时显示placeholder
                        className={`${className} case-api-test-select`}
                        placeholder={i18n.t('dop:compare')}
                        onChange={(v) => onCurChange(v, false)}
                      >
                        {map(comparisonOperators || [], (item) => {
                          return <Option value={item.value}>{item.label}</Option>;
                        })}
                      </Select>
                    );
                  },
                },
                value: {
                  props: {
                    placeholder: i18n.t('value'),
                  },
                },
              }}
            />
          </div>
        </div>
      );
    },
  },
};

const TestJsonEditor = (props: any) => {
  const { data, updateBody }: any = props;
  const val = isString(data.content) ? `${data.content}` : '';

  const handleFormat = () => {
    const newContent = formatJSON(val);
    updateBody('content', newContent, false);
  };

  const handleChange = (v: string) => {
    updateBody('content', v);
  };

  return (
    <div className="test-json-editor">
      <Button className="json-format-btn" size="small" onClick={handleFormat}>
        {i18n.t('format')}
      </Button>
      <FileEditor
        fileExtension="json"
        value={val}
        minLines={10}
        maxLines={25}
        onChange={handleChange}
        onLoad={(editor) => {
          editor.getSession().setUseWorker(false);
        }}
      />
    </div>
  );
};

const BasicForm = 'application/x-www-form-urlencoded';
const ValMap = {
  none: () => <div className="body-val-none">{i18n.t('dop:the current request has no body')}</div>,
  [BasicForm]: (props: any) => {
    const { data, updateBody, renderProps, processDataTemp }: any = props;
    const { temp, ...rest } = renderProps.form || {};
    const value = Array.isArray(data.content) ? data.content : [];
    const useTemp = processDataTemp(temp);
    return (
      <EditList
        {...rest}
        dataTemp={useTemp}
        value={value}
        onChange={(val) => {
          updateBody('content', val, false);
        }}
      />
    );
  },
  raw: (props: any) => {
    const { data, updateBody }: any = props;
    const val = isString(data.content) ? data.content : '';
    return (
      <TextArea
        rows={8}
        value={val}
        autoSize
        className="body-val-raw"
        onChange={(e) => updateBody('content', e.target.value)}
        // onBlur={e => updateBody('content', e.target.value, true)}
      />
    );
  },
  'application/json': (props: any) => <TestJsonEditor {...props} />,
};

const APIBody = (props: any) => {
  const { data = {}, onChange, renderProps, processDataTemp } = props;
  const isRaw = data.type && !['none', BasicForm].includes(data.type);
  const realType = data.type || 'none';

  const updateBody = (key: string, val: any, autoSave?: boolean, resetContent?: boolean) => {
    const newBody: any = { ...data, [key]: val || '' };
    if (key === 'type' && resetContent) {
      switch (val) {
        case 'none':
          newBody.content = '';
          break;
        case BasicForm:
          newBody.content = [];
          break;
        case BODY_RAW_OPTION[0]:
          newBody.content = '';
          break;
        default:
          break;
      }
    }
    onChange('body', newBody, autoSave, (newData: any) => {
      if (!newData.headers) {
        newData.headers = [];
      }
      const { headers, body } = newData;
      const adjustHeader = (action: string, headerType: any) => {
        // 按key查找
        const exist = find(headers, { key: headerType.key });
        if (action === 'push') {
          // 有的话更新，没有就添加
          if (exist) {
            exist.value = headerType.value;
          } else {
            headers.push(headerType);
          }
        } else if (exist && action === 'remove') {
          newData.headers = reject(headers, { key: headerType.key });
        }
      };
      switch (body.type) {
        case 'application/json':
          adjustHeader('push', { key: 'Content-Type', value: 'application/json', desc: '' });
          break;
        case 'text/plain':
          adjustHeader('push', { key: 'Content-Type', value: 'text/plain', desc: '' });
          break;
        case 'Text':
          adjustHeader('remove', { key: 'Content-Type' });
          break;
        case BasicForm:
          adjustHeader('push', { key: 'Content-Type', value: body.type, desc: '' });
          break;
        default:
          break;
      }
    });
  };

  /**
   * @description 切换body类型
   * @param type {string}
   * @param autoSave {boolean}
   * @param resetContent {boolean}
   */
  const changeType = (type: string, autoSave?: boolean, resetContent?: boolean) => {
    if (!isEmpty(data.content) && resetContent) {
      Modal.confirm({
        title: i18n.t('confirm to switch Body type?'),
        onOk() {
          updateBody('type', type === 'raw' ? BODY_RAW_OPTION[0] : type, autoSave, resetContent);
        },
      });
    } else {
      // 如果切换为raw类型，使用raw的第一个选项
      updateBody('type', type === 'raw' ? BODY_RAW_OPTION[0] : type, autoSave, resetContent);
    }
  };

  const CurValueComp = ValMap[realType] || ValMap.raw;
  return (
    <div className="case-api-body">
      <div className="body-type-chosen my-2 px-3">
        <Radio.Group onChange={(e) => changeType(e.target.value, false, true)} value={isRaw ? 'raw' : realType}>
          <Radio value={'none'}>none</Radio>
          <Radio value={BasicForm}>x-www-form-urlencoded</Radio>
          <Radio value={'raw'}>raw</Radio>
        </Radio.Group>
        {isRaw ? (
          <span>
            <Select
              size="small"
              style={{ minWidth: 120 }}
              onChange={(t: string) => changeType(t, false)}
              value={realType}
              dropdownMatchSelectWidth={false}
            >
              {map(BODY_RAW_OPTION, (item) => (
                <Option key={item} value={item}>
                  {item}
                </Option>
              ))}
            </Select>
            <Tooltip title={tip} overlayStyle={{ maxWidth: 500 }}>
              <ErdaIcon className="ml-2 mt-1" type="help" />
            </Tooltip>
          </span>
        ) : null}
      </div>
      <div className="body-value-container">
        {CurValueComp && (
          <CurValueComp
            data={data}
            updateBody={updateBody}
            renderProps={renderProps}
            processDataTemp={processDataTemp}
          />
        )}
      </div>
    </div>
  );
};

interface IKeyValProps {
  data: object[];
  type: string;
  dataModel: object;
  itemMap: object;
  opList?: any[];
  order: string[];
  onChange: (...args: any) => any;
}
const KeyValEdit = (props: IKeyValProps) => {
  const { data, type, dataModel, itemMap, opList = [], onChange, order } = props;
  const [values, setValues] = React.useState(data || []);

  React.useEffect(() => {
    let newVal: any = [];
    if (isEmpty(data)) {
      newVal = [{ ...dataModel }];
    } else if (find(data, dataModel)) {
      newVal = data;
    } else {
      newVal = [...data, { ...dataModel }];
    }
    setValues(newVal);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  const updateValue = (idx: number, key: string, val: string, autoSave = false) => {
    const oldVal = cloneDeep(values);
    const newVal: any = cloneDeep(values);
    newVal[idx][key] = val.trim();
    // source选了status时，把这一行expression设为status
    if (key === 'source') {
      switch (val) {
        case 'status':
          newVal[idx].expression = 'status';
          break;
        case 'body:json':
          newVal[idx].expression = '';
          break;
        default:
          break;
      }
    }
    setValues(newVal);
    onChange(
      newVal.filter((item: any) => !Object.values(item).every((v) => !v)),
      autoSave,
      (newData: any, k: string) => {
        const { out_params, asserts } = newData;
        if (k === 'out_params') {
          // 修改出参时修改对应断言
          const oldKey = oldVal[idx].key;
          asserts?.forEach((a: any) => {
            if (a.arg === oldKey) {
              a.arg = out_params[idx].key;
            }
          });
        }
        // 更新断言时同时清除小试中对应断言的结果
        if (k.startsWith('asserts')) {
          const { attemptTest } = newData;
          if (attemptTest && attemptTest.asserts && attemptTest.asserts.result.length >= asserts.length) {
            const match = attemptTest.asserts.result[idx];
            if (match) {
              match.success = undefined;
            }
          }
        }
      },
    );
  };

  const handleDelete = (num: number) => {
    const newVals = values.filter((_, i) => i !== num);
    setValues(newVals);
    onChange(newVals.slice(0, -1), false, (newData: any, k: string) => {
      const { out_params, asserts } = newData;
      // 删除出参时删除对应断言，data为apis全部数据
      if (k === 'out_params') {
        const outParamKeys = {};
        out_params.forEach((p: any) => {
          outParamKeys[p.key] = true;
        });
        // 只保留arg没填或者在outParams有匹配的断言
        const newAsserts = asserts.filter((a: any) => a.arg === '' || outParamKeys[a.arg]);
        newData.asserts = newAsserts;
      }
      // 删除断言时同时删除小试中对应断言的结果
      if (k.startsWith('asserts')) {
        const { attemptTest } = newData;
        if (attemptTest) {
          attemptTest.asserts.result.splice(num, 1);
        }
      }
    });
  };

  return (
    <div className="api-key-val-container">
      {map(values, (item, i) => {
        const lastItem = i === values.length - 1;
        return (
          <div className="key-val-item" key={i}>
            {map(order, (key: string) => {
              const val = item[key];
              const { Comp, props: compProps, getProps } = itemMap[key];
              const extraProps = getProps ? getProps(item) : {};
              return (
                <React.Fragment key={key}>
                  {Comp ? (
                    <Comp
                      className="flex-1"
                      value={val}
                      record={item}
                      onChange={(curVal: any, autoSave: boolean) => updateValue(i, key, curVal, autoSave)}
                    />
                  ) : (
                    <Input
                      className="flex-1"
                      placeholder={i18n.t('please enter')}
                      value={val}
                      onChange={(e) => updateValue(i, key, e.target.value)}
                      // onBlur={e => updateValue(i, key, e.target.value, true)}
                      {...compProps}
                      {...extraProps}
                    />
                  )}
                  {Comp === Empty ? null : <div className="item-separate" />}
                </React.Fragment>
              );
            })}
            <div className="key-val-operation">
              {opList[i] || null}
              {type === 'out_params' ? (
                <Popconfirm
                  title={i18n.t(
                    'dop:Deleting the output parameter will delete the corresponding parameter name assertion. Continue?',
                  )}
                  onConfirm={() => handleDelete(i)}
                >
                  <ErdaIcon
                    type="delete1"
                    size="14"
                    className={lastItem ? 'hidden-del hover-active' : 'show-del hover-active'}
                  />
                </Popconfirm>
              ) : (
                <ErdaIcon
                  type="delete1"
                  size="14"
                  onClick={() => {
                    handleDelete(i);
                  }}
                  className={lastItem ? 'hidden-del hover-active' : 'show-del hover-active'}
                />
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
