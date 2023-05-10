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
import { map as _map, pickBy } from 'lodash';
import { Row, Col, Input, Select, Button, Tabs, Form, Popconfirm, Tooltip } from 'antd';
import { Copy, KeyValueEditor, IF, ErdaIcon } from 'common';
import { regRules, notify, qs } from 'common/utils';
import CommonPanel from './trace-common-panel';
import TraceHistoryList from './trace-history-list';
import RequestStatusViewer from './trace-status-viewer';
import constants from './constants';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import monitorCommonStore from 'common/stores/monitorCommon';
import traceQuerierStore from 'trace-insight/stores/trace-querier';
import { useEffectOnce } from 'react-use';
import TraceSearchDetail from './trace-search-detail';
import i18n from 'i18n';
import './trace-querier.scss';

const { HTTP_METHOD_LIST, MAX_BODY_LENGTH, MAX_URL_LENGTH } = constants;
const { TabPane } = Tabs;
const { Option } = Select;
const { TextArea } = Input;
const { Item: FormItem } = Form;
const { banFullWidthPunctuation, url: urlRule } = regRules;

const TraceInsightQuerier = () => {
  const [form] = Form.useForm();
  const [
    requestTraceParams,
    traceHistoryList,
    currentTraceRequestId,
    traceStatusDetail,
    traceDetailContent,
    spanDetailContent,
  ] = traceQuerierStore.useStore((s) => [
    s.requestTraceParams,
    s.traceHistoryList,
    s.currentTraceRequestId,
    s.traceStatusDetail,
    s.traceDetailContent,
    s.spanDetailContent,
  ]);
  const urlQuery = routeInfoStore.useStore((s) => s.query);
  const isShowTraceDetail = monitorCommonStore.useStore((s) => s.isShowTraceDetail);
  const { setIsShowTraceDetail } = monitorCommonStore.reducers;
  const [isTraceDetailContentFetching, isTraceHistoryFetching, isRequestTraceFetching] = useLoading(traceQuerierStore, [
    'getTraceDetailContent',
    'getTraceHistoryList',
    'requestTrace',
  ]);

  const {
    requestTrace,
    getSpanDetailContent,
    getTraceHistoryList,
    getTraceDetail,
    getTraceDetailContent,
    getTraceStatusDetail,
    cancelTraceStatus,
  } = traceQuerierStore.effects;

  const {
    setRequestTraceParams,
    setCurrentTraceRequestId,
    clearTraceStatusDetail,
    clearCurrentTraceRequestId,
    clearRequestTraceParams,
  } = traceQuerierStore.reducers;

  useEffectOnce(() => {
    getTraceHistoryList();
    return () => {
      clearRequestTraceParams();
      clearCurrentTraceRequestId();
      clearTraceStatusDetail();
    };
  });

  const { method, url, body, query, header, updateTime } = requestTraceParams;
  const queryStr = qs.stringify(query);
  const { validateFields } = form;
  const { requestId } = urlQuery;

  const [startTime, setStartTime] = React.useState(0);
  const [activeTab, setActiveTab] = React.useState('1');
  const [traceRecords, setTraceRecords] = React.useState({});
  const [inputUrl, setInputUrl] = React.useState('');
  let paramsEditor: any;
  let headersEditor: any;

  React.useEffect(() => {
    form.setFieldsValue({ body });
  }, [body]);

  React.useEffect(() => {
    requestId && setActiveTab('2');
  }, [requestId]);

  React.useEffect(() => {
    requestId &&
      getTraceDetailContent({ traceId: requestId, needReturn: true }).then((content: any) => {
        setTraceRecords(content);
      });
  }, [getTraceDetailContent, requestId]);

  React.useEffect(() => {
    form.setFieldsValue({ method });
  }, [method, form]);

  React.useEffect(() => {
    if (updateTime) {
      setStartTime(new Date(updateTime).getTime());
    }
  }, [updateTime]);

  const resetRequestTrace = () => {
    form.resetFields();
    clearRequestTraceParams();
    clearCurrentTraceRequestId();
    clearTraceStatusDetail();
  };

  const handleSetRequestTraceParams = (payload: any) => {
    return validateFields().then(() => {
      const params = { ...payload };
      const { query: preQuery, url: preUrl } = params;
      if (preUrl) {
        const queryMap = qs.parseUrl(preUrl).query;
        params.query = { ...preQuery, ...pickBy(queryMap, (v, k) => v && k) };
      }
      setRequestTraceParams({ ...requestTraceParams, ...params });
    });
  };

  const handleRequestTrace = () => {
    validateFields()
      .then(async () => {
        const payload: any = {};
        // 适应 AntD Tabs 组件 Tab Content 惰性加载取不到 ref 的问题
        if (paramsEditor) {
          payload.query = paramsEditor.getEditData();
        }

        if (headersEditor) {
          payload.header = headersEditor.getEditData();
        }
        await handleSetRequestTraceParams(payload);
        requestTrace({ startTime: new Date().getTime() });
      })
      .catch(() => {
        notify('warning', i18n.t('msp:param-error-check'));
      });
  };

  const renderMetaViewer = () => {
    return (
      <div className="meta-viewer">
        {!url ? (
          <p>{i18n.t('msp:currently no request')}</p>
        ) : (
          <p className="meta-viewer-copy">
            <Copy>{url}</Copy>
          </p>
        )}
      </div>
    );
  };

  const renderUrlEditor = React.useCallback(() => {
    const selectBefore = (
      <FormItem
        className="mb-0 -mt-0.5 h-7"
        name="method"
        initialValue={method}
        rules={[{ required: true, message: i18n.t('msp:this item is required') }]}
      >
        <Select
          bordered={false}
          style={{ width: 110 }}
          onSelect={(value) => {
            handleSetRequestTraceParams({ method: value });
          }}
        >
          {_map(HTTP_METHOD_LIST, (item) => (
            <Option value={item} key={item}>
              {item}
            </Option>
          ))}
        </Select>
      </FormItem>
    );

    return (
      <div className="url-editor">
        <Row gutter={10}>
          <Col span={18}>
            <FormItem
              className="m-0 h-8"
              name="url"
              rules={[{ required: true, message: i18n.t('msp:this item is required') }, urlRule]}
            >
              <Input
                addonBefore={selectBefore}
                placeholder={
                  i18n.t('msp|please enter a legal url, length limit: ', { nsSeparator: '|' }) +
                  MAX_URL_LENGTH.toString()
                }
                maxLength={MAX_URL_LENGTH}
                onChange={(e) => setInputUrl(e.target.value)}
                onBlur={(e) => {
                  handleSetRequestTraceParams({ url: e.target.value });
                }}
              />
            </FormItem>
          </Col>
          <Col span={6}>
            <Button
              type="primary"
              disabled={!inputUrl || !urlRule.pattern.test(inputUrl)}
              loading={!!url && isRequestTraceFetching}
              onClick={handleRequestTrace}
            >
              {i18n.t('msp:request')}
            </Button>
            <Popconfirm
              title={i18n.t('confirm to reset?')}
              placement="bottom"
              onConfirm={() => {
                resetRequestTrace();
              }}
            >
              <Button className="ml-4">{i18n.t('reset')}</Button>
            </Popconfirm>
          </Col>
        </Row>
      </div>
    );
  }, [inputUrl, url, requestTraceParams, isRequestTraceFetching]);

  const renderRequestEditor = () => {
    return (
      <Tabs className="request-editor" defaultActiveKey="1">
        <TabPane tab="Params" key="1">
          <div className="request-edit-params-form">
            <KeyValueEditor
              isNeedTextArea={false}
              tableProps={{
                size: 'default',
              }}
              form={form}
              dataSource={query}
              ref={(ref) => {
                paramsEditor = ref;
              }}
              onChange={(data: any) => {
                setRequestTraceParams({
                  ...requestTraceParams,
                  query: data,
                  url: `${qs.parseUrl(url).url}`,
                });
              }}
            />
          </div>
        </TabPane>
        <TabPane tab="Headers" key="2">
          <div className="request-edit-params-form">
            <KeyValueEditor
              tableProps={{
                size: 'default',
              }}
              form={form}
              dataSource={header}
              ref={(ref) => {
                headersEditor = ref;
              }}
            />
          </div>
        </TabPane>
        <TabPane tab="Body" key="3">
          <FormItem name="body" initialValue={body}>
            <TextArea
              className="request-edit-body-form"
              autoSize={{ minRows: 8, maxRows: 12 }}
              maxLength={MAX_BODY_LENGTH}
              placeholder={
                i18n.t('msp|please enter body, length limit:', { nsSeparator: '|' }) + MAX_BODY_LENGTH.toString()
              }
              onChange={(e) => {
                handleSetRequestTraceParams({ body: e.target.value });
              }}
            />
          </FormItem>
        </TabPane>
      </Tabs>
    );
  };

  const renderStatusList = () => {
    return (
      <CommonPanel
        title={
          <div className="flex justify-between items-center">
            <div className="flex">
              <h3 className="trace-common-panel-title font-medium mr-2">{i18n.t('msp:tracing information')}</h3>
              {traceStatusDetail?.status === 1 && (
                <Tooltip title={i18n.t('full screen')}>
                  <ErdaIcon
                    type="full-screen-one"
                    size="14"
                    className="cursor-pointer hover:text-primary mb-2"
                    onClick={() => setIsShowTraceDetail(true)}
                  />
                </Tooltip>
              )}
            </div>
            <IF check={requestTraceParams.responseCode}>
              <div className="response-code">{`${i18n.t('msp:request response status')}：${
                requestTraceParams.responseCode
              }`}</div>
            </IF>
          </div>
        }
        className="trace-status-list-ct"
      >
        <RequestStatusViewer
          traceStatusDetail={traceStatusDetail}
          cancelTraceStatus={cancelTraceStatus}
          spanDetailContent={spanDetailContent}
          traceDetailContent={traceDetailContent}
          isTraceDetailContentFetching={isTraceDetailContentFetching}
          getSpanDetailContent={getSpanDetailContent}
        />
      </CommonPanel>
    );
  };

  return (
    <div>
      <Row className="trace-querier" gutter={20}>
        <Col span={6}>
          <CommonPanel title={i18n.t('msp:query records')} className="history-status-list-ct">
            <TraceHistoryList
              dataSource={traceHistoryList}
              isFetching={isTraceHistoryFetching}
              currentTraceRequestId={currentTraceRequestId}
              setInputUrl={setInputUrl}
              getTraceHistoryList={getTraceHistoryList}
              getTraceDetail={getTraceDetail}
              getTraceStatusDetail={getTraceStatusDetail}
              setCurrentTraceRequestId={setCurrentTraceRequestId}
              setRequestTraceParams={setRequestTraceParams}
              clearTraceStatusDetail={clearTraceStatusDetail}
              clearCurrentTraceRequestId={clearCurrentTraceRequestId}
              clearRequestTraceParams={clearRequestTraceParams}
              form={form}
            />
          </CommonPanel>
        </Col>
        <Col span={18}>
          <CommonPanel>
            <React.Fragment>
              {renderMetaViewer()}
              <Form form={form}>
                {renderUrlEditor()}
                {renderRequestEditor()}
              </Form>
            </React.Fragment>
          </CommonPanel>
          {renderStatusList()}
        </Col>
      </Row>
      {isShowTraceDetail && <TraceSearchDetail traceId={traceStatusDetail?.requestId} startTime={startTime} />}
    </div>
  );
};

export default TraceInsightQuerier;
