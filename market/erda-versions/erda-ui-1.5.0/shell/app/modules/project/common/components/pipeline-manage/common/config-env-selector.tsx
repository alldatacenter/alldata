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
import { Button, Tooltip } from 'antd';
import { map, isEmpty, get, find } from 'lodash';
import autoTestStore from 'project/stores/auto-test-case';
import { useUpdate } from 'common/use-hooks';
import { insertWhen, notify } from 'common/utils';
import { useEffectOnce } from 'react-use';
import routeInfoStore from 'core/stores/route';
import { WORKSPACE_LIST } from 'common/constants';
import projectStore from 'project/stores/project';
import { FormModal } from 'app/configForm/nusi-form/form-modal';
import { createPipelineAndRun, updateCasePipeline } from 'project/services/auto-test-case';
import orgStore from 'app/org-home/stores/org';
import { ymlDataToFormData } from 'app/yml-chart/common/in-params-drawer';
import { parsePipelineYmlStructure } from 'application/services/repo';
import { scopeConfig } from '../scope-config';
import i18n from 'i18n';

interface IProps {
  onTest: (arg: any) => void;
  scope: string;
  canRunTest: boolean;
}

const workSpaceMap = {
  DEV: i18n.t('dev environment'),
  TEST: i18n.t('test environment'),
  STAGING: i18n.t('staging environment'),
  PROD: i18n.t('prod environment'),
};

let inParamsKey = 1;

const getInParamsValue = (_pipelineParams: PIPELINE.IPipelineInParams[]) => {
  const _values = {} as Obj;
  map(_pipelineParams, (item: PIPELINE.IPipelineInParams) => {
    if (item.value !== undefined && item.value !== null) _values[item.name] = item.value;
  });
  return _values;
};

const ConfigEnvSelector = (props: IProps) => {
  const { onTest, scope, canRunTest } = props;
  const scopeConfigData = scopeConfig[scope];
  const [caseDetail, configEnvs] = autoTestStore.useStore((s) => [s.caseDetail, s.configEnvs]);
  const { id: orgId, name: orgName } = orgStore.useStore((s) => s.currentOrg);
  const projectId = routeInfoStore.useStore((s) => s.params.projectId);
  const { getAutoTestConfigEnv, clearConfigEnvs, getCaseDetail } = autoTestStore;
  const info = projectStore.useStore((s) => s.info);
  const { clusterConfig, name: projectName } = info;

  const [{ formVis, fields, inParamsForm, canDoTest, needModal, clusterList }, updater, update] = useUpdate({
    formVis: false,
    fields: [] as any[],
    inParamsForm: [] as any[],
    canDoTest: false,
    needModal: true,
    clusterList: [],
  });

  useEffectOnce(() => {
    scopeConfigData.executeEnvChosen && getAutoTestConfigEnv({ scopeID: projectId, scope: scopeConfigData.scope });
    return () => clearConfigEnvs();
  });

  const setFields = (_inParamsForm: any[]) => {
    const _f = [
      ...insertWhen(scopeConfigData.executeClusterChosen, [
        {
          label: i18n.t('choose cluster'),
          component: 'select',
          required: true,
          key: 'clusterName',
          defaultValue: getLastRunParams().clusterName || 'TEST',
          type: 'select',
          dataSource: {
            type: 'static',
            static: map(clusterList, (item) => ({ name: item.workspace, value: item.key })),
          },
        },
      ]),
      ...insertWhen(scopeConfigData.executeEnvChosen, [
        {
          label: i18n.t('choose global configuration'),
          component: 'select',
          required: true,
          key: 'configManageNamespaces',
          defaultValue: '0',
          type: 'select',
          dataSource: {
            type: 'static',
            static: map([{ ns: '0', displayName: i18n.t('none') }, ...configEnvs], (item) => ({
              name: item.displayName,
              value: item.ns,
            })),
          },
        },
      ]),
      ...insertWhen(!isEmpty(_inParamsForm), [
        {
          component: 'custom',
          getComp: () => {
            return <div className="font-medium border-bottom">{i18n.t('dop:params configuration')}</div>;
          },
        },
        ..._inParamsForm,
      ]),
    ];
    return _f;
  };

  React.useEffect(() => {
    updater.fields(setFields(inParamsForm));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inParamsForm, clusterConfig]);

  const inFormProps = React.useMemo(() => {
    inParamsKey += 1;
    return { fieldList: fields, key: inParamsKey };
  }, [fields]);

  const getLastRunParams = () => {
    const runParams = get(caseDetail, 'meta.runParams');
    const val: Obj = {};
    map(runParams, (item) => {
      val[item.name] = item.value;
    });
    return val;
  };

  const onClickTest = () => {
    if (!needModal) {
      // 无需弹框，直接执行
      execute();
      return;
    }
    if (canDoTest) {
      updater.formVis(true);
    } else {
      const ymlStr = get(caseDetail, 'meta.pipelineYml');
      updater.canDoTest(false);
      if (ymlStr) {
        (parsePipelineYmlStructure({ pipelineYmlContent: ymlStr }) as unknown as Promise<any>)
          .then((res: any) => {
            const updateObj = {} as any;
            if (!isEmpty(res.data.stages)) {
              updateObj.canDoTest = true;
              updateObj.formVis = true;
            } else {
              notify('warning', i18n.t('dop:please add valid tasks to the pipeline below before operating'));
            }
            const inP = ymlDataToFormData(get(res, 'data.params') || [], getLastRunParams());
            if (isEmpty(inP) && !scopeConfigData.executeEnvChosen && !scopeConfigData.executeClusterChosen) {
              // 无入参，无环境，不需要弹框
              updateObj.needModal = false;
              updateObj.formVis = false;
            } else {
              updateObj.inParamsForm = inP;
            }
            update(updateObj);
            if (updateObj.needModal === false) execute();
          })
          .catch(() => {
            notify('warning', i18n.t('dop:please add valid tasks to the pipeline below before operating'));
          });
      } else {
        notify('warning', i18n.t('dop:please add valid tasks to the pipeline below before operating'));
      }
    }
  };

  React.useEffect(() => {
    updater.canDoTest(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseDetail]);

  React.useEffect(() => {
    if (!isEmpty(clusterConfig)) {
      const sortBy = WORKSPACE_LIST;
      const clusterData = [] as any[];
      sortBy.forEach((workspace) => {
        if (clusterConfig[workspace]) {
          clusterData.push({
            workspace: workSpaceMap[workspace],
            clusterName: clusterConfig[workspace],
            key: workspace,
          });
        }
      });
      updater.clusterList(clusterData);
    }
  }, [clusterConfig, updater]);

  const execute = (p: Obj = {}) => {
    let curClusterName = clusterConfig?.TEST;
    const chosenCluster = find(p?.runParams, { name: 'clusterName' });
    if (chosenCluster) {
      curClusterName = clusterConfig[chosenCluster.value];
    }

    createPipelineAndRun({
      pipelineYml: get(caseDetail, 'meta.pipelineYml'),
      pipelineSource: scopeConfigData.runPipelineSource,
      pipelineYmlName: caseDetail.inode,
      clusterName: curClusterName,
      autoRunAtOnce: true,
      autoStartCron: true,
      labels: { orgID: `${orgId}`, projectID: projectId, projectName, orgName },
      ...p,
    }).then((res: any) => {
      onTest(res.data);
    });
  };

  const saveRunParams = (runParams: any[]) => {
    updateCasePipeline({ nodeId: caseDetail.inode, runParams }).then(() => {
      getCaseDetail({ id: caseDetail.inode });
    });
  };

  return (
    <div>
      <Tooltip title={canRunTest ? '' : i18n.t('dop:pipeline-run-tip')}>
        <Button
          type="primary"
          disabled={!canRunTest}
          onClick={(e) => {
            e.stopPropagation();
            onClickTest();
          }}
        >
          {scopeConfigData.text.executeButton}
        </Button>
      </Tooltip>

      <FormModal
        title={i18n.t('execute')}
        onCancel={() => updater.formVis(false)}
        onOk={(val: any) => {
          const { configManageNamespaces, ...rest } = val;
          const runParams = isEmpty(rest) ? [] : map(rest, (v, k) => ({ name: k, value: v }));
          saveRunParams(runParams); // 保存此次的值
          execute({
            configManageNamespaces: configManageNamespaces === '0' ? [] : [configManageNamespaces],
            runParams,
          });
        }}
        visible={formVis}
        {...inFormProps}
      />
    </div>
  );
};

export default ConfigEnvSelector;
