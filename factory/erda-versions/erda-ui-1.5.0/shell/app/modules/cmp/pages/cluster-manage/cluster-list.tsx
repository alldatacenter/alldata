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
import { Modal, Table, Button, Drawer, Input, Spin } from 'antd';
import { goTo, insertWhen, notify, setSearch } from 'common/utils';
import { map, get, find } from 'lodash';
import AddMachineModal from 'app/modules/cmp/common/components/machine-form-modal';
import AddCloudMachineModal from './cloud-machine-form-modal';
import TokenManageModal from './token-manage-modal';
import { Icon as CustomIcon, Copy, ConfirmDelete } from 'common';
import { useUpdate } from 'common/use-hooks';
import machineStore from 'app/modules/cmp/stores/machine';
import clusterStore from 'app/modules/cmp/stores/cluster';
import i18n from 'i18n';
import { ClusterLog } from './cluster-log';
import { getClusterOperationHistory } from 'app/modules/cmp/services/machine';
import { ColumnProps, IActions } from 'core/common/interface';
import orgStore from 'app/org-home/stores/org';
import { bgColorClsMap } from 'common/utils/style-constants';
import { useLoading } from 'core/stores/loading';
import DiceConfigPage from 'config-page/index';

import routeStore from 'core/stores/route';
import { getToken } from 'cmp/services/token-manage';
import './cluster-list.scss';

interface IProps {
  onEdit: (record: any) => void;
}

interface ICluster {
  title: string;
}

export const statusMap = {
  online: ['green', i18n.t('cmp:online')],
  offline: ['red', i18n.t('cmp:offline')],
  initializing: ['yellow', i18n.t('runtime:initializing')],
  'initialize error': ['red', i18n.t('cmp:initialization failed')],
  pending: ['gray', i18n.t('dop:pending')],
  unknown: ['red', i18n.t('unknown')],
};

export const manageTypeMap = {
  agent: i18n.t('cmp:agent registration'),
  create: i18n.t('establish'),
  import: i18n.t('import'),
};

const ClusterList = ({ onEdit }: IProps) => {
  const { addCloudMachine } = machineStore.effects;
  const { upgradeCluster, deleteCluster, getRegisterCommand, clusterInitRetry } = clusterStore.effects;
  const [curCluster, setCurCluster] = React.useState<ORG_CLUSTER.ICluster | null>(null);
  const [registerCommand, setRegisterCommand] = React.useState('');
  const [loading] = useLoading(clusterStore, ['getRegisterCommand']);
  const token = getToken.useData();
  const orgId = orgStore.getState((s) => s.currentOrg.id);
  const [state, updater] = useUpdate({
    tokenManageVisible: false,
    clusterName: '',
    modalVisibleRow: null,
    popoverVisible: false,
    popoverVisibleRow: null,
    afterAdd: null,
    cloudModalVis: false,
    deleteModalVis: false,
    curDeleteCluster: null as null | ORG_CLUSTER.ICluster,
    deleteClusterName: '',
    registerCommandVisible: false,
  });

  const toggleAddCloudMachine = (cluster?: ORG_CLUSTER.ICluster) => {
    if (cluster) {
      setCurCluster(cluster);
      updater.cloudModalVis(true);
    } else {
      setCurCluster(null);
      updater.cloudModalVis(false);
    }
  };
  const onAddCloudMachine = (postData: ORG_MACHINE.IAddCloudMachineBody) => {
    toggleAddCloudMachine();
    addCloudMachine(postData).then((res) => {
      updater.afterAdd({ ...res });
    });
  };

  const checkClusterUpdate = (cluster: ORG_CLUSTER.ICluster) => {
    upgradeCluster({ clusterName: cluster.name, precheck: true }).then(
      ({ status, precheckHint }: { status: number; precheckHint: string }) => {
        switch (status) {
          case 3:
            Modal.warning({
              title: i18n.t('warning'),
              content: precheckHint,
            });
            break;
          case 2:
            Modal.confirm({
              title: i18n.t('warning'),
              content: precheckHint,
              onOk() {
                upgradeCluster({ clusterName: cluster.name, precheck: false });
                goTo(`./history?clusterName=${cluster.name}`);
              },
            });
            break;
          case 1:
            Modal.confirm({
              title: i18n.t('warning'),
              content: precheckHint,
              onOk() {
                goTo(`./history?clusterName=${cluster.name}`);
              },
            });
            break;
          default:
            break;
        }
      },
    );
  };

  const toggleDeleteModal = (item?: ORG_CLUSTER.ICluster) => {
    if (item) {
      updater.curDeleteCluster(item);
      updater.deleteModalVis(true);
    } else {
      updater.curDeleteCluster(null);
      updater.deleteModalVis(false);
      updater.deleteClusterName('');
    }
  };

  const submitDelete = ({ clusterName }: { clusterName: string }) => {
    // 删除后根据recordID查看操作纪录接口中的详情作为提示
    deleteCluster({ clusterName }).then((deleteRes: any) => {
      const { recordID: deleteRecordID } = deleteRes || {};
      deleteRecordID &&
        getClusterOperationHistory({ recordIDs: deleteRecordID } as any).then((listRes: any) => {
          const curRecord = get(listRes, 'data.list[0]') || ({} as any);
          if (curRecord && curRecord.status === 'failed') {
            notify('error', curRecord.detail);
          }
        });
    });
    toggleDeleteModal();
  };

  const showCommand = React.useCallback(
    async (clusterName: string) => {
      updater.registerCommandVisible(true);
      const command = await getRegisterCommand({ clusterName });
      const { orgName } = routeStore.getState((s) => s.params);
      setRegisterCommand(
        `${command.replace('$REQUEST_PREFIX', `${window.location.origin}/api/${orgName}/cluster/init-command`)}`,
      );
    },
    [getRegisterCommand, updater],
  );

  const query = routeStore.useStore((s) => s.query);
  React.useEffect(() => {
    if (query.autoOpenCmd) {
      showCommand(query.clusterName);
      setSearch({}, [], true);
    }
  }, [query, showCommand]);

  return (
    <>
      <TokenManageModal
        clusterName={state.clusterName}
        token={token?.accessKey}
        onCancel={() => updater.tokenManageVisible(false)}
        visible={state.tokenManageVisible}
      />
      <AddMachineModal
        visible={!!state.modalVisibleRow}
        cluster={state.modalVisibleRow as ORG_CLUSTER.ICluster}
        onCancel={() => updater.modalVisibleRow(null)}
        onSubmit={(resp: ORG_MACHINE.IClusterOperateRecord) => updater.afterAdd(resp)}
      />
      <AddCloudMachineModal
        visible={state.cloudModalVis}
        cluster={curCluster}
        orgId={orgId}
        onCancel={() => toggleAddCloudMachine()}
        onSubmit={onAddCloudMachine}
      />
      <ClusterLog recordID={state.afterAdd && state.afterAdd.recordID} onClose={() => updater.afterAdd(null)} />
      {state.deleteModalVis && (
        <ConfirmDelete
          title={i18n.t('cmp:Please enter the cluster identity to confirm to go offline.')}
          onConfirm={() => submitDelete({ clusterName: state.deleteClusterName })}
          secondTitle={i18n.t('cmp:Please enter {name}, to confirm the cluster to go offline', {
            name: state.curDeleteCluster?.name,
          })}
          onCancel={() => toggleDeleteModal()}
          disabledConfirm={state.deleteClusterName !== state.curDeleteCluster?.name}
          modalChildren={
            <Input
              value={state.deleteClusterName}
              placeholder={i18n.t('please enter {name}', { name: i18n.t('cmp:cluster identity') })}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => updater.deleteClusterName(e.target.value)}
            />
          }
          hasTriggerContent={false}
        />
      )}
      <Drawer
        visible={state.registerCommandVisible}
        destroyOnClose
        title={i18n.t('cmp:cluster registration command')}
        width="800"
        onClose={() => updater.registerCommandVisible(false)}
      >
        <Spin spinning={loading} wrapperClassName="full-spin-height">
          <div className="flex flex-col items-end h-full">
            <Input.TextArea id="command-script" readOnly value={registerCommand} className="min-h-3/5" />
            <Button
              type="ghost"
              className="btn-to-copy mt-4"
              data-clipboard-target="#command-script"
              disabled={!registerCommand.length}
            >
              {i18n.t('cmp:copy command')}
            </Button>
            <Copy selector=".btn-to-copy" />
          </div>
        </Spin>
      </Drawer>
      <div>
        <DiceConfigPage
          scenarioKey="cmp-cluster-list"
          scenarioType="cmp-cluster-list"
          customProps={{
            list: {
              props: {
                extraContent: {
                  grayBg: true,
                  option: { graphic: { type: 'text', style: { fontSize: 10 } } },
                },
              },
              op: {
                clickItem: (click: { meta: { name: string } }) => {
                  const { meta } = click;
                  goTo(goTo.pages.cmpClustersDetail, { clusterName: meta.name });
                },
                edit: (record: { meta: object }) => {
                  const { meta } = record;
                  onEdit(meta);
                },
                addMachine: (record: { meta: object }) => {
                  const { meta } = record;
                  updater.modalVisibleRow(meta);
                },
                addCloudMachines: (record: { meta: ORG_CLUSTER.ICluster }) => {
                  const { meta } = record;
                  toggleAddCloudMachine(meta);
                },
                upgrade: (record: { meta: ORG_CLUSTER.ICluster }) => {
                  const { meta } = record;
                  checkClusterUpdate(meta);
                },
                deleteCluster: (record: { meta: ORG_CLUSTER.ICluster }) => {
                  const { meta } = record;
                  toggleDeleteModal(meta);
                },
                tokenManagement: async (record: { meta: ORG_CLUSTER.ICluster }) => {
                  const { meta } = record;
                  await getToken.fetch({
                    clusterName: meta.name,
                  });
                  updater.tokenManageVisible(true);
                  updater.clusterName(meta.name);
                },
                showRegisterCommand: (record: { meta: ORG_CLUSTER.ICluster }) => {
                  const { meta } = record;
                  showCommand(meta.name);
                },
                retryInit: (record: { meta: ORG_CLUSTER.ICluster }) => {
                  const { meta } = record;
                  clusterInitRetry({ clusterName: meta.name });
                },
              },
            },
          }}
        />
      </div>
    </>
  );
};

export default ClusterList;
