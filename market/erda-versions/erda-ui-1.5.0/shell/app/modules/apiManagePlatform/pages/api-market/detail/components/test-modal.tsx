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
import { Modal, Tooltip, Tabs, Button, Table, Input, Select, message } from 'antd';
import i18n from 'i18n';
import { qs } from 'common/utils';
import { Icon as CustomIcon, FileEditor } from 'common';
import { useUpdate } from 'common/use-hooks';
import screenFull, { Screenfull } from 'screenfull';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import { useLoading } from 'core/stores/loading';
import { APITabs, commonColumn, ResponseTabs, formatData, fillingUrl } from './test-modal-utils';
import { set, cloneDeep, map } from 'lodash';
import './test-modal.scss';

const defaultSchema = 'https';

interface AutoInfo {
  clientID: string;
  clientSecret: string;
}
interface IProps {
  visible: boolean;
  dataSource: {
    autoInfo: AutoInfo;
    host: string;
    basePath: string;
    protocol: string;
    url: string;
    method: string;
    requestScheme: Record<string, any>;
  };

  onCancel: () => void;
}

interface IApis {
  params: any[];
  path: any[];
  body: {
    isAdd?: boolean;
    type: 'none' | 'application/x-www-form-urlencoded' | 'Text' | 'Text(text/plain)' | 'application/json';
    content: any;
  };
  header: any[];
}

interface IState {
  fullscreen: boolean;
  apis: IApis;
  resTab: API_MARKET.ResponseTabs;
  resBody: string;
  resHeader: any[];
  schema: API_MARKET.Schema;
}

const screenController = screenFull as Screenfull;
const { TabPane } = Tabs;

const TestModal = ({ visible, onCancel, dataSource }: IProps) => {
  const { runAttemptTest } = apiMarketStore.effects;
  const [assetDetail, access, assetVersion] = apiMarketStore.useStore((s) => [
    s.assetVersionDetail.asset,
    s.assetVersionDetail.access,
    s.assetVersionDetail.version,
  ]);
  const [isRunning] = useLoading(apiMarketStore, ['runAttemptTest']);
  const [{ apis, fullscreen, resTab, resBody, resHeader, schema }, updater, update] = useUpdate<IState>({
    fullscreen: false,
    apis: {},
    resTab: 'header',
    resBody: '',
    resHeader: [],
    schema: defaultSchema,
  });
  React.useEffect(() => {
    if (visible) {
      update({
        apis: formatData(dataSource.requestScheme || {}),
      });
    }
  }, [dataSource.requestScheme, update, visible]);

  const completeUrl = React.useMemo(() => {
    const url = fillingUrl(dataSource.url, apis.path || []);
    const params = (apis.params || []).reduce((prev, { key, value }) => {
      const currentPrev = { ...prev };
      if (key in prev) {
        currentPrev[key] = Array.isArray(currentPrev[key]) ? currentPrev[key].push(value) : [currentPrev[key], value];
        return currentPrev;
      }
      return {
        ...prev,
        [key]: value,
      };
    }, {});
    const queryStr = qs.stringify(params, { arrayFormat: 'none' });
    return `${access.bindDomain}${url}${queryStr ? `?${queryStr}` : ''}`;
  }, [apis.path, dataSource.url, apis.params, access.bindDomain]);

  const handleScreenControl = async () => {
    const dom = document.getElementsByClassName('api-test-modal')[0];
    if (dom && !screenController.isFullscreen) {
      await screenController.request(dom);
      updater.fullscreen(true);
    } else {
      await screenController.exit();
      updater.fullscreen(false);
    }
  };
  const handleCancel = () => {
    update({
      fullscreen: false,
      apis: {},
      resTab: 'header',
    });
    onCancel();
  };

  const changeTabs = (key: API_MARKET.ResponseTabs) => {
    updater.resTab(key);
  };

  // eslint-disable-next-line no-unused-vars
  const updateApis = (key: string, value: any, _autoSave = false, adjustData?: Function) => {
    const newApis = cloneDeep(apis);
    set(newApis, key, value);
    if (adjustData) {
      adjustData(newApis, key);
    }
    updater.apis(newApis);
  };
  const handleRunTest = () => {
    const url = fillingUrl(dataSource.url, apis.path);
    let { header } = apis;
    let bodyValue = apis.body.content;
    const bodyType = apis.body.type;
    if (bodyType && bodyType !== 'none') {
      header = [{ key: 'Content-Type', value: apis.body.type }, ...apis.header];
      if (apis.body.type === 'application/json') {
        try {
          bodyValue = JSON.parse(bodyValue);
        } catch (_) {
          message.error(i18n.t('dop:JSON format error'));
          return;
        }
      }
    }
    runAttemptTest({
      clientID: dataSource.autoInfo.clientID,
      clientSecret: dataSource.autoInfo.clientSecret,
      assetID: assetDetail.assetID,
      swaggerVersion: assetVersion.swaggerVersion,
      apis: [
        {
          schema,
          method: dataSource.method,
          url,
          header,
          params: apis.params,
          body: { type: bodyType, content: bodyValue },
        },
      ],
    }).then(({ response }) => {
      const headers = map(response.headers || {}, (value, key) => ({ key, value }));
      let bodyData = '';
      if (response.status !== 200) {
        try {
          const { message } = JSON.parse(response.body) || {};
          bodyData = `${response.status}: ${message}`;
        } catch (e) {
          bodyData = `${response.status}: ${response.body}`;
        }
      } else {
        try {
          const dataObj = JSON.parse(response.body);
          bodyData = JSON.stringify(dataObj, null, 2);
        } catch (e) {
          bodyData = response.body || '';
        }
      }
      update({
        resTab: 'body',
        resBody: bodyData,
        resHeader: headers,
      });
    });
  };
  const modalTitle = (
    <span className="flex items-center justify-start">
      <span>{i18n.t('API test')}</span>
      <span className="ml-3 hover-active" onClick={handleScreenControl}>
        <Tooltip
          placement="right"
          title={fullscreen ? i18n.t('exit full screen') : i18n.t('full screen')}
          getPopupContainer={(el) => el?.parentNode as HTMLElement}
        >
          <CustomIcon type={fullscreen ? 'shink' : 'grow'} />
        </Tooltip>
      </span>
    </span>
  );
  const modalClassName = fullscreen ? 'fullscreen' : '';
  return (
    <Modal
      className={`api-test-modal ${modalClassName}`}
      title={modalTitle}
      visible={visible}
      width={800}
      destroyOnClose
      onCancel={handleCancel}
      maskClosable={false}
      footer={<Button onClick={handleCancel}>{i18n.t('close')}</Button>}
    >
      <div className="flex justify-between items-center">
        <div className="flex-1">
          <Input.Group compact>
            <Input style={{ width: '10%' }} disabled value={dataSource.method} />
            <Select
              style={{ width: '15%' }}
              defaultValue={defaultSchema}
              onChange={(v) => {
                updater.schema(v);
              }}
            >
              <Select.Option key="https" value="https">
                HTTPS
              </Select.Option>
              <Select.Option key="http" value="http">
                HTTP
              </Select.Option>
            </Select>
            <Input disabled style={{ width: '75%' }} value={completeUrl} />
          </Input.Group>
        </div>
        <Button className="ml-2" type="primary" loading={isRunning} onClick={handleRunTest}>
          {i18n.t('execute')}
        </Button>
      </div>
      <Tabs className="mb-4">
        {APITabs.map((item) => {
          const { title, dataIndex, render } = item;
          const data = apis[dataIndex] || [];
          return (
            <TabPane tab={title} key={dataIndex}>
              {render({ onChange: updateApis, data, type: dataIndex, record: apis })}
            </TabPane>
          );
        })}
      </Tabs>
      <Tabs defaultActiveKey="header" activeKey={resTab} className="mb-3" onChange={changeTabs}>
        {map(ResponseTabs, ({ name, value }) => {
          // return <Radio.Button key={value} value={value}>{name}</Radio.Button>;
          return (
            <TabPane tab={name} key={value}>
              {value === 'body' ? (
                <div>
                  <FileEditor fileExtension="json" value={resBody} minLines={8} maxLines={20} readOnly />
                </div>
              ) : (
                <Table columns={commonColumn} pagination={false} dataSource={resHeader} scroll={{ x: '100%' }} />
              )}
            </TabPane>
          );
        })}
      </Tabs>
    </Modal>
  );
};

export default TestModal;
