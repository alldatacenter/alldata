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

import { cutStr, qs, reorder } from 'common/utils';
import classnames from 'classnames';
import { Copy, ErdaIcon, EmptyListHolder, FileEditor } from 'common';
import { useListDnD } from 'common/use-hooks';
import { isArray, isEmpty, isString, map, reduce, set, cloneDeep, find, reject, get } from 'lodash';
import { Badge, Button, Input, Popconfirm, Popover, Radio, Select, Table, Tabs, Spin, message } from 'antd';
import testEnvStore from 'project/stores/test-env';
import React from 'react';
import { produce } from 'immer';
import i18n from 'i18n';
import './case-api.scss';
import SelectEnv from 'project/pages/test-manage/case/case-drawer/select-env';

interface IProps {
  value: IApi[];
  mode?: string;
  onChange: (list: IApi[], autoSave?: boolean, adjustData?: Function) => any;
  executeApi?: (api: object, i: number, extra?: { envId: number }) => any;
}

const formatJSON = (str: string) => {
  let res = str;
  try {
    res = JSON.stringify(JSON.parse(str), null, 2);
  } catch (e) {
    message.error(i18n.t('dop:the current input content is invalid JSON'));
  }
  return typeof res === 'string' ? res : '';
};
export interface IApi {
  rest?: {
    apiID: number;
  };
  headers: Array<{
    key: string;
    value: string;
    desc: string;
  }>;
  method: string;
  url: string;
  name: string;
  params: Array<{
    key: string;
    value: string;
    desc: string;
  }>;
  body: {
    type: string;
    content: string;
  };
  outParams: Array<{
    key: string;
    source: string;
    expression: string;
    matchIndex: number;
  }>;
  asserts: Array<
    Array<{
      arg: string;
      operator: string;
      value: string;
    }>
  >;
  status: string;
  apiResponse: string;
  apiRequest: string;
  assertResult: string;
  attemptTest?: {
    asserts: null | { success: false; result: any[] };
    response: null | { status: number; headers: object; body: object };
    request: null | { method: string; url: string; params: object[]; headers: object[] };
  };
}

const { Option } = Select;
const { TabPane } = Tabs;
const { TextArea } = Input;

const HTTP_METHOD_LIST = ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'PATCH', 'COPY', 'HEAD'];
const BODY_RAW_OPTION = ['Text', 'Text(text/plain)', 'JSON(application/json)'];

export const getEmptyApi = () => {
  return {
    headers: [],
    method: 'GET',
    url: '',
    name: '',
    params: [],
    body: {
      type: 'none',
      content: '',
    },
    outParams: [],
    asserts: [[]],
  };
};

export const CaseAPI = (props: IProps) => {
  const { value, mode, onChange, executeApi } = props;
  const inPlan = mode === 'plan';
  const [apiList, setApiList] = React.useState(value);
  const [showArr, setShowArr] = React.useState({});
  const [executingMap, setExecutingMap] = React.useState({});
  React.useEffect(() => {
    setApiList(value);
  }, [value]);

  const updateApi = (i: number, k: string, v: string, autoSave = false, adjustData?: Function) => {
    const newApiList = produce(apiList, (draft) => {
      const target = draft[i];
      set(target, k, v);
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
          set(target, 'params', paramList);
          break;
        }
        case 'params': {
          const { url } = qs.parseUrl(target.url);
          const queryStr: string[] = [];
          map(v, (item: any) => {
            if (item.key && item.value) {
              queryStr.push(`${item.key}=${item.value}`);
            }
          });
          set(target, 'url', `${url}?${queryStr.join('&')}`);
          break;
        }
        default:
      }
      if (adjustData) {
        adjustData(draft, i, k);
      }
    });

    setApiList(newApiList);
    onChange(newApiList, autoSave);
  };

  const handleDelete = (num: number) => {
    const newApi = apiList.filter((_item, i) => i !== num);
    // 同步删除对应位置的执行结果
    setApiList(newApi);
    onChange(newApi, true);
  };

  const setCurShow = (idx: number) => {
    const curShow = showArr[`${idx}`] === undefined ? false : showArr[`${idx}`];
    const newShowArr = { ...showArr, [`${idx}`]: !curShow };
    setShowArr(newShowArr);
  };

  const handleExecute = (api: any, i: number, extra?: TEST_ENV.Item) => {
    if (!executeApi) {
      return;
    }
    executeApi(api, i, { envId: extra?.id || 0 })
      .then((result: any) => {
        updateApi(i, 'attemptTest', result[0]);
        !showArr[i] && setCurShow(i);
      })
      .finally(() => {
        setExecutingMap({
          ...executingMap,
          [i]: false,
        });
      });
    setExecutingMap({
      ...executingMap,
      [i]: true,
    });
  };

  const onMove = React.useCallback(
    (dragIndex: number, hoverIndex: number) => {
      const newApiList = reorder(apiList, dragIndex, hoverIndex);
      onChange(newApiList, true);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [apiList],
  );

  const onCopyApi = (api: IApi, index: number) => {
    const newApis = cloneDeep(apiList);
    newApis.splice(index + 1, 0, {
      ...api,
      apiResponse: '',
      assertResult: '',
      attemptTest: {},
      rest: {},
      status: undefined,
    } as any);
    setApiList(newApis);
    onChange(newApis, true);
  };

  if (!apiList.length) {
    return <span className="text-holder">{i18n.t('dop:no content yet')}</span>;
  }

  return (
    <div className="case-api-list">
      {map(apiList, (api, i) => (
        <ApiItem
          key={i}
          index={i}
          {...{
            api,
            setCurShow,
            updateApi,
            handleDelete,
            executingMap,
            inPlan,
            showArr,
            handleExecute,
            onMove,
            onCopyApi,
          }}
        />
      ))}
    </div>
  );
};

const ApiItem = ({
  api,
  index,
  showArr,
  inPlan,
  executingMap,
  setCurShow,
  handleExecute,
  updateApi,
  handleDelete,
  onMove,
  onCopyApi,
}: any) => {
  const envList = testEnvStore.useStore((s) => s.envList);
  const [dragRef, previewRef] = useListDnD({
    type: 'case-api',
    index,
    onMove,
  });

  const isShow = showArr[`${index}`] === undefined ? false : showArr[`${index}`];
  const numCls = classnames(
    'index-num',
    inPlan
      ? {
          success: api.status === 'Passed',
          error: api.status === 'Failed',
        }
      : api.attemptTest &&
          api.attemptTest.asserts && {
            success: api.attemptTest.asserts.success === true,
            error: api.attemptTest.asserts.success === false,
          },
  );
  const curExecuteResult = api.attemptTest;
  let assertResult: any[] = [];
  let assertSuccess: boolean;
  const responseHeaders: object[] = [];
  const requestHeaders: object[] = [];
  const requestParams: object[] = [];
  const columns = [
    {
      title: 'Key',
      dataIndex: 'name',
      key: 'name',
      // XXX 2020/8/21 nusi中定宽表计算逻辑有问题，会导致先设置pk-table-blank-column的宽度一直增加，先设置百分比width，nusi 830版本可能会移除定宽表计算逻辑
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
  const showPlanExecuteResult = inPlan && ['Passed', 'Failed'].includes(api.status);
  if (curExecuteResult || showPlanExecuteResult) {
    let response: any = {};
    let request: any = {};
    if (inPlan) {
      try {
        response = JSON.parse(api.apiResponse);
      } catch (error) {
        response = { body: api.apiResponse || {}, headers: {}, status: '' };
      }
      try {
        request = JSON.parse(api.apiRequest);
      } catch (error) {
        request = { body: api.apiRequest || {}, headers: {}, status: '' };
      }
      try {
        const result = JSON.parse(api.assertResult);
        assertResult = result.result;
        assertSuccess = result.success;
      } catch (error) {
        // do nothing
      }
    } else {
      response = curExecuteResult.response || {};
      request = curExecuteResult.request || {};
      assertResult = curExecuteResult.asserts ? curExecuteResult.asserts.result : [];
      assertSuccess = curExecuteResult.asserts ? curExecuteResult.asserts.success : undefined;
    }
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
      <div className="api-tabs">
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
                  <TabPane key="Body" tab="Body">
                    {isEmpty(get(request, 'body.content')) ? (
                      <EmptyListHolder />
                    ) : (
                      <>
                        <div className="body-type p-3 border-bottom">Type: {get(request, 'body.type', '')}</div>
                        <Button
                          disabled={!get(request, 'body.content')}
                          className="copy-btn cursor-copy copy-request"
                          data-clipboard-text={get(request, 'body.content', '')}
                          shape="circle"
                          icon={<ErdaIcon type="copy" />}
                        />
                        <Copy selector=".copy-request" />
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
                  size="small"
                  pagination={false}
                  columns={columns}
                  dataSource={responseHeaders}
                  scroll={{ x: '100%' }}
                />
              </TabPane>
              <TabPane key="Body" tab="Body">
                <Button
                  disabled={!body}
                  className="copy-btn cursor-copy copy-response"
                  data-clipboard-text={body}
                  shape="circle"
                  icon={<ErdaIcon type="copy" />}
                />
                <Copy selector=".copy-response" />
                {responseBody}
              </TabPane>
            </Tabs>
          </TabPane>
        </Tabs>
      </div>
    );
  }
  return (
    <div ref={previewRef} key={index} className={'api-item'}>
      <Spin size="small" spinning={executingMap[index] || false}>
        <div className="api-title case-index-hover">
          <span ref={dragRef} className="case-index-block">
            <span className={numCls}>{index + 1}</span>
            <ErdaIcon size="16" className="drag-icon" type="px" />
          </span>
          <span>
            <ErdaIcon size="16" className="copy-icon" type="copy" onClick={() => onCopyApi(api, index)} />
          </span>
          <Input
            className="flex-1"
            placeholder={i18n.t('dop:input interface name')}
            value={api.name}
            onChange={(e) => updateApi(index, 'name', e.target.value)}
            maxLength={50}
          />
          <ErdaIcon
            fill="black-400"
            className={`${isShow ? 'arrow-down' : 'arrow-up'} api-op hover-active`}
            type="chevron-down"
            onClick={() => setCurShow(index)}
          />
          {inPlan ? null : (
            <SelectEnv envList={envList} onClick={(extra: TEST_ENV.Item) => handleExecute(api, index, extra)}>
              <ErdaIcon
                fill="black-400"
                className="ml-3 mt-1 api-op hover-active"
                type="play"
                onClick={() => handleExecute(api, index)}
              />
            </SelectEnv>
          )}
          <Popconfirm title={`${i18n.t('common:confirm deletion')}？`} onConfirm={() => handleDelete(index)}>
            <ErdaIcon fill="black-400" size="18" className="ml-3 delete-icon api-op hover-active" type="sc1" />
          </Popconfirm>
        </div>
        <div className={`api-content ${isShow ? 'block' : 'hidden'}`}>
          <div className="api-url">
            <Input
              addonBefore={
                <Select
                  style={{ width: 110 }}
                  value={api.method}
                  onChange={(val: string) => updateApi(index, 'method', val, true)}
                  placeholder={i18n.t('dop:please choose')}
                >
                  {map(HTTP_METHOD_LIST, (method) => (
                    <Option value={method} key={method}>
                      {method}
                    </Option>
                  ))}
                </Select>
              }
              className="url"
              placeholder={i18n.t('please enter')}
              value={api.url}
              onChange={(e) => updateApi(index, 'url', e.target.value.trim())}
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
                return (
                  <TabPane tab={_tab} key={tab}>
                    <Comp
                      data={
                        isArray(dataKey)
                          ? reduce(
                              dataKey,
                              (obj, k) => {
                                return { ...obj, [k]: api[k] };
                              },
                              {},
                            )
                          : api[dataKey]
                      }
                      assertResult={assertResult}
                      onChange={(key: string, val: any, autoSave?: boolean, adjustData?: Function) =>
                        updateApi(index, key, val, autoSave, adjustData)
                      }
                    />
                  </TabPane>
                );
              })}
            </Tabs>
          </div>
          {resultTabs}
        </div>
      </Spin>
    </div>
  );
};

const Empty = () => null;

const ApiTabComps = {
  Params: {
    dataKey: 'params',
    Comp: (props: any) => {
      const { data, onChange } = props;
      return (
        <KeyValEdit
          type="params"
          data={data}
          order={['key', 'value', 'desc']}
          dataModel={{ key: '', value: '', desc: '' }}
          onChange={(val: any, autoSave?: boolean) => {
            onChange('params', val, autoSave);
          }}
          itemMap={{
            key: {
              props: {
                placeholder: i18n.t('dop:parameter name'),
              },
            },
            value: {
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
            },
            desc: {
              props: {
                placeholder: i18n.t('description'),
              },
            },
          }}
        />
      );
    },
  },
  Headers: {
    dataKey: 'headers',
    Comp: (props: any) => {
      const { data, onChange } = props;
      return (
        <KeyValEdit
          type="headers"
          data={data}
          order={['key', 'value', 'desc']}
          dataModel={{ key: '', value: '', desc: '' }}
          onChange={(val: any, autoSave?: boolean) => {
            onChange('headers', val, autoSave);
          }}
          itemMap={{
            key: {
              props: {
                placeholder: i18n.t('dop:parameter name'),
              },
            },
            value: {
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
            },
            desc: {
              props: {
                placeholder: i18n.t('description'),
              },
            },
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
    dataKey: ['outParams', 'asserts', 0],
    Comp: (props: any) => {
      const { data, onChange, assertResult } = props;
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
              type="outParams"
              data={data.outParams}
              order={['key', 'source', 'expression']}
              dataModel={{
                key: '',
                source: 'body:json',
                expression: '',
                matchIndex: '',
              }}
              onChange={(val: any, autoSave?: boolean, adjustData?: Function) => {
                onChange('outParams', val, autoSave, adjustData);
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
                        onChange={(val: string) => {
                          onCurChange(val, true);
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
                matchIndex: {
                  // props: {
                  //   placeholder: '第几个匹配项',
                  // },
                  Comp: Empty,
                },
              }}
            />
          </div>
          <div className="table-title">{i18n.t('dop:assertion')}</div>
          <div className="table-body">
            <KeyValEdit
              type="asserts"
              data={data.asserts[0]}
              order={['arg', 'operator', 'value']}
              opList={getOpList(data.asserts[0])}
              dataModel={{
                arg: '',
                operator: '',
                value: '',
              }}
              onChange={(val: any, autoSave?: boolean, adjustData?: Function) => {
                onChange('asserts.0', val, autoSave, adjustData);
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
                        onChange={(v: string) => onCurChange(v, true)}
                      >
                        {data.outParams.map((option: any) => {
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
                        onChange={(v: string) => onCurChange(v, true)}
                      >
                        <Option value=">">{i18n.t('dop:more than the')}</Option>
                        <Option value=">=">{i18n.t('dop:greater than or equal to')}</Option>
                        <Option value="=">{i18n.t('dop:equal to')}</Option>
                        <Option value="<=">{i18n.t('dop:less than or equal to')}</Option>
                        <Option value="<">{i18n.t('less than')}</Option>
                        <Option value="!=">{i18n.t('dop:not equal to')}</Option>
                        <Option value="contains">{i18n.t('dop:contains')}</Option>
                        <Option value="not_contains">{i18n.t('dop:does not contain')}</Option>
                        <Option value="exist">{i18n.t('dop:existence')}</Option>
                        <Option value="not_exist">{i18n.t('does not exist')}</Option>
                        <Option value="empty">{i18n.t('dop:is empty')}</Option>
                        <Option value="not_empty">{i18n.t('dop:not null')}</Option>
                        <Option value="belong">{i18n.t('dop:belongs to')}</Option>
                        <Option value="not_belong">{i18n.t('dop:does not belong to')}</Option>
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
  const [content, setContent] = React.useState('');
  React.useEffect(() => {
    setContent(val);
  }, [val]);

  return (
    <div className="test-json-editor">
      <Button
        className="json-format-btn"
        size="small"
        onClick={() => {
          setContent(formatJSON(content));
        }}
      >
        {i18n.t('format')}
      </Button>
      <FileEditor
        fileExtension="json"
        value={content}
        minLines={8}
        onChange={(value: string) => updateBody('content', value)}
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
    const { data, updateBody }: any = props;
    return (
      <KeyValEdit
        type="body"
        order={['key', 'value', 'desc']}
        data={isString(data.content) ? [] : (data.content as any)}
        dataModel={{
          key: '',
          value: '',
          desc: '',
        }}
        onChange={(val: any, autoSave?: boolean) => {
          updateBody('content', val, autoSave);
        }}
        itemMap={{
          key: {
            props: {
              placeholder: i18n.t('dop:parameter name'),
            },
          },
          value: {
            props: {
              placeholder: i18n.t('dop:parameter value'),
            },
          },
          desc: {
            props: {
              placeholder: i18n.t('description'),
            },
          },
        }}
      />
    );
  },
  raw: (props: any) => {
    const { data, updateBody }: any = props;
    const val = isString(data.content) ? data.content : '';
    return <TextArea rows={4} value={val} onChange={(e) => updateBody('content', e.target.value)} />;
  },
  'JSON(application/json)': (props: any) => <TestJsonEditor {...props} />,
};

const APIBody = (props: any) => {
  const { data, onChange } = props;
  const isRaw = !['none', BasicForm].includes(data.type);
  const realType = data.type;

  const updateBody = (key: string, val: any, autoSave?: boolean) => {
    const newBody: any = { ...data, [key]: val || '' };
    if (key === 'type') {
      switch (val) {
        case 'none':
          newBody.content = '';
          break;
        case BasicForm:
          newBody.content = [];
          break;
        default:
          break;
      }
    }
    onChange('body', newBody, autoSave, (newData: any, i: number) => {
      const { headers, body } = newData[i];
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
          // eslint-disable-next-line no-param-reassign
          newData[i].headers = reject(headers, { key: headerType.key });
        }
      };
      switch (body.type) {
        case 'JSON(application/json)':
          adjustHeader('push', { key: 'Content-Type', value: 'application/json', desc: '' });
          break;
        case 'Text(text/plain)':
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

  const changeType = (type: string, autoSave?: boolean) => {
    // 如果切换为raw类型，使用raw的第一个选项
    updateBody('type', type === 'raw' ? BODY_RAW_OPTION[0] : type, autoSave);
  };

  const CurValueComp = ValMap[realType] || ValMap.raw;
  return (
    <div className="case-api-body">
      <div className="body-type-chosen mb-2 px-3">
        <Radio.Group onChange={(e) => changeType(e.target.value)} value={isRaw ? 'raw' : realType}>
          <Radio value={'none'}>none</Radio>
          <Radio value={BasicForm}>x-www-form-urlencoded</Radio>
          <Radio value={'raw'}>raw</Radio>
        </Radio.Group>
        {isRaw ? (
          <Select
            size="small"
            style={{ minWidth: 120 }}
            onChange={(t: string) => changeType(t, true)}
            value={realType}
            dropdownMatchSelectWidth={false}
          >
            {map(BODY_RAW_OPTION, (item) => (
              <Option key={item} value={item}>
                {item}
              </Option>
            ))}
          </Select>
        ) : null}
      </div>
      <div className="body-value-container">{CurValueComp && <CurValueComp data={data} updateBody={updateBody} />}</div>
    </div>
  );
};

interface IKeyValProps {
  data: object[];
  type: string;
  dataModel: object;
  itemMap: object;
  order: string[];
  opList?: any[];
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
      // 去掉值都为空的
      newVal.filter((item: any) => !Object.values(item).every((v) => !v)),
      autoSave,
      (newData: any, i: number, k: string) => {
        const { outParams, asserts } = newData[i];
        if (k === 'outParams') {
          // 修改出参时修改对应断言
          const oldKey = oldVal[idx].key;
          asserts[0].forEach((a: any) => {
            if (a.arg === oldKey) {
              // eslint-disable-next-line no-param-reassign
              a.arg = outParams[idx].key;
            }
          });
        }
        // 更新断言时同时清除小试中对应断言的结果
        if (k.startsWith('asserts')) {
          const { attemptTest } = newData[i];
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
    const newVals = values.filter((item, i) => i !== num);
    setValues(newVals);
    onChange(newVals.slice(0, -1), true, (newData: any, i: number, k: string) => {
      const { outParams, asserts } = newData[i];
      // 删除出参时删除对应断言，data为apis全部数据
      if (k === 'outParams') {
        const outParamKeys = {};
        outParams.forEach((p: any) => {
          outParamKeys[p.key] = true;
        });
        // 只保留arg没填或者在outParams有匹配的断言
        const newAsserts = asserts[0].filter((a: any) => a.arg === '' || outParamKeys[a.arg]);
        // eslint-disable-next-line no-param-reassign
        newData[i].asserts[0] = newAsserts;
      }
      // 删除断言时同时删除小试中对应断言的结果
      if (k.startsWith('asserts')) {
        const { attemptTest } = newData[i];
        if (attemptTest) {
          attemptTest.asserts.result.splice(num, 1);
        }
      }
    });
  };

  return (
    <div className="key-val-container">
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
              {type === 'outParams' ? (
                <Popconfirm
                  title={i18n.t(
                    'dop:Deleting the output parameter will delete the corresponding parameter name assertion. Continue?',
                  )}
                  onConfirm={() => handleDelete(i)}
                >
                  <ErdaIcon
                    fill="black-800"
                    size="16"
                    type="sc1"
                    className={lastItem ? 'hidden-del hover-active' : 'show-del hover-active'}
                  />
                </Popconfirm>
              ) : (
                <ErdaIcon
                  type="sc1"
                  size="16"
                  fill="black-800"
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
