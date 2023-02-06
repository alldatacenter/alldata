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
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';
import { Spin, Button } from 'antd';
import { isEmpty } from 'lodash';
import { Holder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo, setSearch } from 'common/utils';
import ClusterList from './cluster-list';
import { AddClusterModal, ClusterTypeModal, AliCloudContainerForm, AliCloudErdcForm } from './add-cluster-forms';
import { useLoading } from 'core/stores/loading';
import clusterStore from '../../stores/cluster';
import { ClusterLog } from './cluster-log';
import routeStore from 'core/stores/route';
import { encode } from 'js-base64';

const ClusterManage = () => {
  const list = clusterStore.useStore((s) => s.list);
  const { addCluster, updateCluster, getClusterList } = clusterStore.effects;
  const [loading] = useLoading(clusterStore, ['getClusterList']);

  const [
    {
      addModalVis,
      typeSelectorVis,
      addModalFormData,
      addClusterType,
      logRecordId,
      cloudVendor,
      aliCloudContainerFormVisible,
      aliCloudErdcFormVisible,
    },
    updater,
  ] = useUpdate({
    addModalVis: false,
    typeSelectorVis: false,
    addModalFormData: null,
    addClusterType: '',
    logRecordId: '',
    cloudVendor: '',
    aliCloudContainerFormVisible: false,
    aliCloudErdcFormVisible: false,
  });

  useEffectOnce(() => {
    const query = routeStore.getState((s) => s.query);
    getClusterList();
    if (query?.autoOpen) {
      updater.typeSelectorVis(true);
      setSearch({}, [], true);
    }
    return () => {
      clusterStore.reducers.clearClusterList();
    };
  });

  const toggleAddModalVis = (isCancel = false) => {
    if (addModalVis) {
      isCancel && !addClusterType && toggleTypeModalVis();
      updater.addClusterType('');
    }
    updater.addModalVis(!addModalVis);
  };

  const toggleTypeModalVis = () => {
    updater.typeSelectorVis(!typeSelectorVis);
  };

  const handleSelectType = (addType: string) => {
    if (['k8s', 'edas', 'dcos'].includes(addType)) {
      // 导入集群
      updater.addClusterType(addType);
      handleShowAddClusterModal();
    } else if (['alicloud-cs', 'alicloud-cs-managed'].includes(addType)) {
      updater.cloudVendor(addType);
      updater.aliCloudContainerFormVisible(true);
    } else if (addType === 'erdc') {
      updater.aliCloudErdcFormVisible(true);
    }
  };

  const handleShowAddClusterModal = (record?: any) => {
    if (record) {
      updater.addClusterType(record.type);
      updater.addModalFormData(record);
    } else {
      updater.addModalFormData(null);
    }
    toggleAddModalVis();
  };

  const handleAddCluster = (values: any) => {
    const { id, credential: credentialData, ...restData } = values;
    const credential =
      credentialData?.content === '********'
        ? { address: credentialData?.address }
        : credentialData?.content
        ? { ...credentialData, content: encode(credentialData.content) }
        : credentialData;
    if (id) {
      // urls 中仍有其他配置，后面可能会加入
      updateCluster({ ...values, credential });
    } else {
      addCluster({ ...restData, credential });
      if (restData.credentialType === 'proxy') {
        setSearch({ autoOpenCmd: true, clusterName: restData.name }, [], true);
      }
    }
  };

  const handleAddAliCloudContainer = ({ recordID }: { recordID: string }) => {
    updater.logRecordId(recordID);
    updater.aliCloudContainerFormVisible(false);
  };

  const handleAddAliCloudErdc = ({ recordID }: { recordID: string }) => {
    updater.logRecordId(recordID);
    updater.aliCloudErdcFormVisible(false);
  };

  return (
    <div className="cluster-manage-ct">
      <Spin spinning={loading}>
        <ClusterList onEdit={handleShowAddClusterModal} />
      </Spin>
      <div className="top-button-group">
        <Button onClick={() => goTo('./history')}>{i18n.t('cmp:operation history')}</Button>
        <Button type="primary" onClick={() => updater.typeSelectorVis(true)}>
          {i18n.t('cmp:add cluster')}
        </Button>
      </div>
      <ClusterTypeModal visible={typeSelectorVis} toggleModal={toggleTypeModalVis} onSubmit={handleSelectType} />
      <AddClusterModal
        visible={addModalVis}
        initData={addModalFormData}
        toggleModal={toggleAddModalVis}
        onSubmit={handleAddCluster}
        clusterType={addClusterType}
        clusterList={list}
      />
      <ClusterLog recordID={logRecordId} onClose={() => updater.logRecordId('')} />
      <AliCloudContainerForm
        cloudVendor={cloudVendor}
        visible={aliCloudContainerFormVisible}
        onSubmit={handleAddAliCloudContainer}
        onClose={() => {
          toggleTypeModalVis();
          updater.aliCloudContainerFormVisible(false);
        }}
      />
      <AliCloudErdcForm
        visible={aliCloudErdcFormVisible}
        onSubmit={handleAddAliCloudErdc}
        onClose={() => {
          toggleTypeModalVis();
          updater.aliCloudErdcFormVisible(false);
        }}
      />
    </div>
  );
};

export default ClusterManage;
