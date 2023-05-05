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

import React, { MutableRefObject } from 'react';
import { Modal, Button } from 'antd';
import { Copy, FormModal, IFormItem, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import apiClientStore from 'apiManagePlatform/stores/api-client';
import { getClientList } from 'apiManagePlatform/services/api-client';
import { getVersionTree } from 'apiManagePlatform/services/api-market';
import { getSlaList } from 'apiManagePlatform/services/api-access';
import { goTo } from 'common/utils';
import { isObject } from 'lodash';
import SelectPro from 'apiManagePlatform/components/select-pro';
import SLASelect from 'apiManagePlatform/components/sla-select';

interface IState {
  selectVersion: string | undefined;
  selectClient: string | number | undefined;
  selectSla: string | number | undefined;
  versions: API_MARKET.VersionTreeItem[];
  clients: API_CLIENT.ClientItem[];
  applyModal: boolean;
  createAppModal: boolean;
  infoModal: boolean;
  clientSk: API_CLIENT.ClientSk;
  slaList: API_ACCESS.SlaItem[];
}

interface IProps {
  visible: boolean;
  dataSource: API_MARKET.Asset;
  onCancel: () => void;
}

const identifierReg = /^[a-zA-Z0-9_-]+$/;

const createNewApp = {
  value: 'create App',
  name: (
    <div className="hover-active text-link">
      <ErdaIcon type="add-one" className="mr-2" />
      {i18n.t('create a new client')}
    </div>
  ),
} as any;

const ApplyModal = ({ visible, onCancel, dataSource }: IProps) => {
  const formRef = React.useRef({}) as MutableRefObject<FormInstance>;
  const clientFormRef = React.useRef({}) as MutableRefObject<FormInstance>;
  const { createContract } = apiClientStore.effects;
  const { createClient } = apiClientStore.effects;
  const [state, updater, update] = useUpdate<IState>({
    selectVersion: undefined,
    selectClient: undefined,
    selectSla: undefined,
    versions: [],
    clients: [],
    applyModal: false,
    createAppModal: false,
    infoModal: false,
    clientSk: {},
    slaList: [],
  });
  const getClients = React.useCallback(() => {
    getClientList<Promise<API_MARKET.CommonResList<API_CLIENT.ClientItem[]>>>({ pageNo: 1, paging: false }).then(
      (res) => {
        if (res.success) {
          updater.clients(res.data.list || []);
        } else {
          updater.clients([]);
        }
      },
    );
  }, [updater]);
  const getSla = React.useCallback(
    (swaggerVersion: string) => {
      getSlaList<Promise<API_MARKET.CommonResList<API_ACCESS.SlaItem[]>>>({
        swaggerVersion,
        assetID: dataSource.assetID,
      }).then((res) => {
        let selectSla: number | undefined;
        if (res.success) {
          const slaList = (res.data.list || []).filter((sla) => sla.source !== 'system');
          const defaultSla = slaList.find((sla) => sla.default);
          selectSla = slaList.length ? defaultSla?.id : undefined;
          update({
            slaList,
            selectSla,
          });
        } else {
          selectSla = undefined;
          update({
            slaList: [],
            selectSla,
          });
        }
        formRef.current.setFieldsValue({ slaID: selectSla });
      });
    },
    [dataSource.assetID, update],
  );
  React.useEffect(() => {
    update({
      applyModal: visible,
      createAppModal: false,
    });
    if (visible) {
      getVersionTree<Promise<API_MARKET.CommonResList<API_MARKET.VersionTreeItem[]>>>({
        assetID: dataSource.assetID,
        patch: false,
        instantiation: false,
        access: true,
      }).then((res) => {
        if (res.success) {
          updater.versions(res.data.list);
        } else {
          updater.versions([]);
        }
      });
      getClients();
    } else {
      update({
        selectClient: undefined,
        selectVersion: undefined,
        selectSla: undefined,
        slaList: [],
      });
    }
  }, [dataSource.assetID, getClients, update, updater, visible]);
  const handleCreateAppCancel = () => {
    update({
      applyModal: true,
      createAppModal: false,
    });
  };
  const handleSelectVersion = (value: string) => {
    update({
      selectVersion: value,
      selectSla: undefined,
    });
    getSla(value);
  };
  const handleSelectApp = (value: string) => {
    let temp: Partial<IState> = {
      selectClient: value,
    };
    if (value === createNewApp.value) {
      temp = {
        applyModal: false,
        createAppModal: true,
        selectClient: undefined,
      };
    }
    update(temp);
  };
  const handleSelectSla = (value: number) => {
    updater.selectSla(value);
  };
  const handleApply = (data: Omit<API_CLIENT.CreteContract, 'assetID'>) => {
    createContract({ ...data, assetID: dataSource.assetID, slaID: +data.slaID }).then(({ sk }) => {
      onCancel();
      update({
        applyModal: false,
        infoModal: true,
        clientSk: sk,
      });
    });
  };
  const beforeSubmitCreateApp = (data: API_CLIENT.CreateClient) => {
    return createClient(data);
  };
  const handleCreateApp = ({ client }: { client: API_CLIENT.Client }) => {
    getClients();
    updater.selectClient(client.id);
    handleCreateAppCancel();
  };
  const handleCloseInfo = () => {
    updater.infoModal(false);
  };
  const filterOption = (name: string, option: React.ReactElement<any>) => {
    const child = option.props.children;
    if (isObject(child)) {
      return true;
    } else {
      return (child as string).toLowerCase().includes(name);
    }
  };
  const renderModalChild = (data: API_ACCESS.SlaItem[], selectKey: number, handleChange: (data: number) => void) => {
    return <SLASelect dataSource={data} defaultSelectKey={selectKey} onChange={handleChange} />;
  };
  const nameToId = (e: React.FocusEvent<HTMLInputElement>) => {
    const name = e.target.value;
    const identifier = clientFormRef.current.getFieldValue('name');
    if (!identifier && identifierReg.test(name)) {
      clientFormRef.current.setFieldsValue({ name });
    }
  };
  const fieldsList: IFormItem[] = [
    {
      label: i18n.t('API version'),
      name: 'swaggerVersion',
      type: 'select',
      initialValue: state.selectVersion,
      options: state.versions.map(({ swaggerVersion }) => ({ name: swaggerVersion, value: swaggerVersion })),
      itemProps: {
        placeholder: i18n.t('please select'),
        onSelect: handleSelectVersion,
      },
    },
    {
      label: i18n.t('related Client'),
      name: 'clientID',
      type: 'select',
      initialValue: state.selectClient,
      options: state.clients
        .map(({ client }) => ({ value: client.id, name: client.displayName || client.name }))
        .concat(createNewApp),
      itemProps: {
        placeholder: i18n.t('please select'),
        onSelect: handleSelectApp,
        filterOption,
        showSearch: true,
      },
    },
    {
      label: 'SLA',
      name: 'slaID',
      initialValue: state.selectSla,
      required: !!state.slaList.length,
      itemProps: {
        placeholder: i18n.t('please select'),
        onSelect: handleSelectSla,
        filterOption,
        showSearch: true,
      },
      getComp(): React.ReactElement<any> | string {
        return (
          <SelectPro<API_ACCESS.SlaItem, any>
            dataSource={state.slaList}
            allowClear
            modalProps={{
              title: 'SLA',
              width: 600,
              children: renderModalChild,
            }}
          >
            {state.slaList.map((item) => {
              return (
                <SelectPro.Option key={item.id} value={item.id}>
                  {item.name}
                </SelectPro.Option>
              );
            })}
          </SelectPro>
        );
      },
    },
  ];
  const creteAppFieldsList: IFormItem[] = [
    {
      label: i18n.t('client name'),
      name: 'displayName',
      itemProps: {
        placeholder: i18n.t('please enter'),
        autoComplete: 'off',
        maxLength: 50,
        onBlur: nameToId,
      },
    },
    {
      label: i18n.t('client identifier'),
      name: 'name',
      pattern: identifierReg,
      itemProps: {
        placeholder: i18n.t('dop:letters, numbers, underscores and hyphens'),
        autoComplete: 'off',
        maxLength: 50,
      },
    },
    {
      label: i18n.t('description'),
      type: 'textArea',
      name: 'desc',
      itemProps: {
        placeholder: i18n.t('please enter'),
        autoComplete: 'off',
        maxLength: 1024,
      },
    },
  ];
  return (
    <>
      <FormModal
        title={i18n.t('apply to use')}
        visible={state.applyModal}
        ref={formRef}
        onCancel={onCancel}
        onOk={handleApply}
        fieldsList={fieldsList}
        modalProps={{
          destroyOnClose: true,
        }}
      />
      <FormModal
        title={i18n.t('create client')}
        visible={state.createAppModal}
        onCancel={handleCreateAppCancel}
        onOk={handleCreateApp}
        beforeSubmit={beforeSubmitCreateApp}
        fieldsList={creteAppFieldsList}
        ref={clientFormRef}
      />
      <Modal
        visible={state.infoModal}
        title={i18n.t('API request')}
        onCancel={handleCloseInfo}
        footer={<Button onClick={handleCloseInfo}>{i18n.t('close')}</Button>}
      >
        <p className="mb-2">
          {i18n.t('The administrator has received your request. Please go to')}
          <span
            onClick={() => {
              goTo(goTo.pages.apiMyVisit);
            }}
            className="text-link"
          >
            {i18n.t('my visit')}
          </span>
          {i18n.t('check whether your request is approved')}
        </p>
        <p className="mb-2">
          {i18n.t('You can use ClientID and ClientSecret below to access API instance after approval.')}
        </p>
        <p className="mb-1">
          <span className="font-medium">ClientID: </span>
          <span className="cursor-copy" data-clipboard-text={state.clientSk.clientID}>
            {state.clientSk.clientID}
          </span>
        </p>
        <p className="mb-1">
          <span className="font-medium">ClientSecret: </span>
          <span className="cursor-copy" data-clipboard-text={state.clientSk.clientSecret}>
            {state.clientSk.clientSecret}
          </span>
        </p>
        <Copy selector=".cursor-copy" />
      </Modal>
    </>
  );
};

export default ApplyModal;
