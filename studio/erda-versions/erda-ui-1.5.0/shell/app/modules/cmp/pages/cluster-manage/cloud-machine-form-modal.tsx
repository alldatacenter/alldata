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

import { FormModal, RenderPureForm, ErdaIcon } from 'common';
import { uniq, get } from 'lodash';
import i18n from 'i18n';
import { diskTypeMap, getOptions } from './config';
import LabelSelector from 'dcos/common/label-selector';
import { CustomLabel, checkCustomLabels } from 'dcos/common/custom-label';
import { FormInstance } from 'core/common/interface';
import orgStore from 'app/org-home/stores/org';
import React from 'react';

interface IProps {
  visible: boolean;
  formData?: any;
  cluster: ORG_CLUSTER.ICluster | null;
  orgId: number;
  onCancel: () => void;
  onSubmit: (resp: any) => any;
}

const BasicForm = ({ form }: { form: FormInstance }) => {
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const defaultOrgTag = `org-${currentOrg.name}`; // 取企业名打默认的tag:org-{orgName}

  const fieldsList = [
    {
      label: i18n.t('machines'),
      name: 'instanceNum',
      type: 'inputNumber',
      initialValue: 1,
      itemProps: {
        min: 1,
        max: 20,
        className: 'w-full',
      },
      extraProps: {
        extra: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 20 }),
      },
    },
    {
      label: i18n.t('cmp:machine label'),
      name: 'labels',
      getComp: () => <LabelSelector />,
    },
    {
      label: i18n.t('custom labels'),
      name: 'customLabels',
      required: false,
      initialValue: defaultOrgTag,
      getComp: () => <CustomLabel />,
      rules: [{ validator: checkCustomLabels }],
    },
    {
      label: i18n.t('cmp:machine type'),
      name: 'instanceType',
      initialValue: 'ecs.sn2ne.2xlarge',
      itemProps: { type: 'hidden' },
    },
    {
      label: 'cloudVendor',
      name: 'cloudVendor',
      initialValue: 'alicloud-ecs',
      itemProps: { type: 'hidden' },
    },
    {
      label: 'cloudResource',
      name: 'cloudResource',
      initialValue: 'ecs',
      itemProps: { type: 'hidden' },
    },
  ];
  return <RenderPureForm list={fieldsList} form={form} />;
};

const MoreForm = ({ form }: { form: FormInstance }) => {
  const fieldsList = [
    {
      label: i18n.t('cmp:disk size'),
      name: 'diskSize',
      type: 'inputNumber',
      initialValue: 200,
      itemProps: {
        min: 1,
        max: 1024,
        className: 'w-full',
        formatter: (v: string) => `${v}G`,
        parser: (v: string) => v.replace('G', ''),
      },
      extraProps: {
        extra: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 1024 }),
      },
    },
    {
      label: i18n.t('cmp:disk type'),
      name: 'diskType',
      type: 'select',
      initialValue: diskTypeMap.cloud_ssd.value,
      options: getOptions('diskType'),
      itemProps: {
        className: 'w-full',
      },
    },
  ];
  return <RenderPureForm list={fieldsList} form={form} />;
};

const CloudMachineAddForm = (props: any) => {
  const { form, cluster } = props;
  const [showMore, setShowMore] = React.useState(false);
  const cloudVendor = get(cluster, 'cloudVendor');

  return (
    <div className="cluster-form">
      <BasicForm form={form} />
      {['alicloud-cs', 'alicloud-cs-managed'].includes(cloudVendor) ? null : (
        <div className="more">
          <a className="more-btn" onClick={() => setShowMore(!showMore)}>
            {i18n.t('advanced settings')}
            {showMore ? (
              <ErdaIcon className="align-middle" type="up" size="16" />
            ) : (
              <ErdaIcon className="align-middle" type="down" size="16" />
            )}
          </a>
          <div className={`more-form ${showMore ? '' : 'hidden'}`}>
            <MoreForm form={form} />
          </div>
        </div>
      )}
    </div>
  );
};

const CloudMachineFormModal = (props: IProps) => {
  const { formData, onCancel, visible, onSubmit, orgId, cluster } = props;

  const handelSubmit = (data: any) => {
    const { customLabels = [], instanceNum, diskSize, cloudVendor: defaultVendor, ...rest } = data;
    // k8s(alicloud) 云集群(alicloud-ecs)  容器集群(alicloud-ack)
    const cloudVendor = cluster?.cloudVendor || defaultVendor;

    rest.labels = uniq((data.labels || []).concat(customLabels));
    const postData = {
      instanceNum: Number(instanceNum),
      clusterName: cluster?.name,
      cloudVendor,
      orgID: orgId,
      ...rest,
    };
    if (!['alicloud-cs', 'alicloud-cs-managed'].includes(cloudVendor)) {
      postData.diskSize = Number(diskSize);
    }
    onSubmit(postData);
    onCancel();
  };

  return (
    <FormModal
      width={800}
      title={i18n.t('cmp:add alibaba cloud machine')}
      visible={visible}
      onOk={handelSubmit}
      onCancel={onCancel}
      PureForm={CloudMachineAddForm}
      formData={formData}
      cluster={cluster}
      modalProps={{
        destroyOnClose: true,
      }}
    />
  );
};

export default CloudMachineFormModal;
