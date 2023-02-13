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
import { Modal, Radio, Tooltip } from 'antd';
import { find, get, map, set, filter, cloneDeep, remove } from 'lodash';
import { Form } from 'dop/pages/form-editor/index';
import clusterStore from 'cmp/stores/cluster';
import networkStore from 'cmp/stores/networks';
import { getOptions, chargeTypeMap, clusterSpecMap } from '../config';
import { regRulesMap } from '../form-utils';
import i18n from 'i18n';
import { useUpdateEffect } from 'react-use';
import { insertWhen, regRules } from 'common/utils';
import { useUpdate } from 'common/use-hooks';
import orgStore from 'app/org-home/stores/org';
import { AliCloudFormPreview } from './index';

const cloudVendorMap = {
  name: i18n.t('cmp:Alibaba Cloud container service cluster'),
  initValue: {
    vpcCIDR: '192.168.0.0/16',
    vSwitchCIDR: '192.168.0.0/18',
    podCIDR: '10.112.0.0/14',
    serviceCIDR: '10.96.0.0/16',
  },
};

const subNetRule = [
  {
    pattern: String(regRulesMap.subnet.pattern),
    msg: regRulesMap.subnet.message,
  },
];

interface IProps {
  onClose: () => void;
  onSubmit: ({ recordID }: { recordID: string }) => void;
  visible: boolean;
  cloudVendor: string;
}

const AliCloudContainerClusterForm = ({ visible, onClose, onSubmit, cloudVendor }: IProps) => {
  const formRef = React.useRef(null as any);
  const clusterList = clusterStore.useStore((s) => s.list);
  const { addCloudCluster, getCloudPreview } = clusterStore.effects;
  const { getVpcList, getVswList } = networkStore.effects;
  const [vpcList, vswList] = networkStore.useStore((s) => [s.vpcList, s.vswList]);

  const [{ count, previewModalVis, previewData, postData }, updater] = useUpdate({
    previewData: [],
    previewModalVis: false,
    postData: {} as any,
    count: 1, // TODO forceUpdate 由于表单内的联动时，使用setFields是无法触发父组件的重新渲染的，导致setFields(xxx)内的参数还是上次闭包中的值，使结果不生效，这里先hack一下
  });

  const getFormData = (key: string, defaultValue?: any) => {
    return formRef.current ? get(formRef.current.getData(), key) : defaultValue || undefined;
  };

  const reloadFields = () => {
    updater.count(count + 1);
  };

  const onVpcCidrChange = (value: string) => {
    const currentPodValue = getFormData('podCIDR', '');
    const currentServiceValue = getFormData('serviceCIDR', '');
    if (value.startsWith('10.')) {
      currentPodValue.startsWith('10.') && formRef.current.setFieldValue('podCIDR', '172.16.0.0/14');
      currentServiceValue.startsWith('10.') && formRef.current.setFieldValue('serviceCIDR', '172.20.0.0/16');
    } else {
      formRef.current.setFieldValue('podCIDR', '10.16.0.0/14');
      formRef.current.setFieldValue('serviceCIDR', '10.20.0.0/16');
    }
  };

  const vpcCIDRField = {
    label: i18n.t('cmp:VPC network segment'),
    component: 'input',
    key: 'vpcCIDR',
    disabled: getFormData('isNewVpc') === 'exist',
    rules: subNetRule,
    required: true,
    defaultValue: get(cloudVendorMap, 'initValue.vpcCIDR'),
    category: 'more',
    componentProps: {
      onChange: (e: any) => {
        onVpcCidrChange(e.target.value);
      },
    },
  };

  const vSwitchCIDRField = {
    label: i18n.t('cmp:switch network segment'),
    component: 'input',
    key: 'vSwitchCIDR',
    required: true,
    disabled: getFormData('isNewVsw') === 'exist',
    defaultValue: get(cloudVendorMap, 'initValue.vSwitchCIDR'),
    rules: subNetRule,
    category: 'more',
  };

  const fields = [
    {
      label: i18n.t('{name} identifier', { name: i18n.t('cluster') }),
      component: 'input',
      key: 'clusterName',
      rules: [
        regRules.clusterName,
        {
          validator: (v: any) => {
            const curCluster = find(clusterList, { name: v });
            return [!curCluster, i18n.t('cmp:cluster already existed')];
          },
        },
      ],
      componentProps: {
        placeholder: i18n.t('cmp:letters and numbers, separated by hyphens, cannot be modified if confirmed'),
      },
      required: true,
      category: 'basic',
    },
    {
      label: i18n.t('cluster name'),
      component: 'input',
      key: 'displayName',
      rules: [
        {
          min: '1',
        },
        {
          max: '30',
        },
      ],
      category: 'basic',
    },
    {
      label: i18n.t('cmp:extensive domain'),
      component: 'input',
      key: 'rootDomain',
      rules: [
        {
          pattern: '/^(?=^.{3,255}$)[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+$/',
          msg: i18n.t('please enter the correct format'),
        },
      ],
      required: true,
      category: 'basic',
    },
    {
      label: i18n.t('region'),
      component: 'select',
      key: 'region',
      disabled: false,
      componentProps: {
        onChange: () => {
          formRef.current.setFieldValue('isNewVpc', 'new');
          formRef.current.setFieldValue('vpcID', undefined);
          formRef.current.setFieldValue('vpcCIDR', get(cloudVendorMap, 'initValue.vpcCIDR'));
          formRef.current.setFieldValue('isNewVsw', 'new');
          formRef.current.setFieldValue('vSwitchID', undefined);
          formRef.current.setFieldValue('vSwitchCIDR', get(cloudVendorMap, 'initValue.vSwitchCIDR'));
          reloadFields();
        },
      },
      required: true,
      dataSource: {
        type: 'static',
        static: getOptions('region'),
      },
      category: 'basic',
    },
    {
      label: i18n.t('cmp:cluster specifications'),
      component: 'radio',
      key: 'clusterSpec',
      componentProps: {
        radioType: 'radio',
      },
      required: true,
      defaultValue: get(clusterSpecMap[cloudVendor], 'Standard.value'),
      dataSource: {
        static: () =>
          map(clusterSpecMap[cloudVendor], ({ name, value, tip }) => (
            <Radio.Button key={value} value={value}>
              <Tooltip title={tip}>{name}</Tooltip>
            </Radio.Button>
          )),
      },
      category: 'basic',
    },
    {
      label: i18n.t('cmp:billing method'),
      component: 'radio',
      key: 'chargeType',
      componentProps: {
        radioType: 'button',
      },
      required: true,
      defaultValue: chargeTypeMap.PostPaid.value,
      dataSource: {
        static: getOptions('chargeType'),
      },
      category: 'basic',
    },
    {
      label: i18n.t('cmp:purchase time'),
      component: 'select',
      key: 'chargePeriod',
      required: true,
      dataSource: {
        type: 'static',
        static: getOptions('chargePeriod'),
      },
      removeWhen: [
        [
          {
            field: 'chargeType',
            operator: '!=',
            value: 'PrePaid',
          },
        ],
      ],
      category: 'basic',
    },
    {
      label: i18n.t('cmp:whether to enable https'),
      component: 'switch',
      key: 'enableHttps',
      required: true,
      defaultValue: false,
      category: 'basic',
    },
    {
      label: i18n.t('cmp:VPC network segment addition method'),
      component: 'select',
      key: 'isNewVpc',
      required: true,
      componentProps: {
        onChange: (e: string) => {
          if (e === 'exist') {
            formRef.current.setFieldValue('vpcCIDR', '');
            formRef.current.setFieldValue('vpcID', undefined);
            getVpcList({ vendor: 'aliyun', region: getFormData('region'), pageNo: 1, pageSize: 30 });
          } else {
            formRef.current.setFieldValue('isNewVsw', 'new');
            formRef.current.setFieldValue('vSwitchCIDR', get(cloudVendorMap, 'initValue.vSwitchCIDR'));
          }
          reloadFields();
        },
      },
      dataSource: {
        type: 'static',
        static: [
          {
            name: i18n.t('add'),
            value: 'new',
          },
          {
            name: i18n.t('select the existing'),
            value: 'exist',
          },
        ],
      },
      defaultValue: 'new',
      category: 'more',
    },
    {
      label: 'VPC',
      component: 'select',
      key: 'vpcID',
      required: true,
      componentProps: {
        onChange: (e: string) => {
          const selectedVpc = find(vpcList, { vpcID: e });
          const vpcCidrValue = selectedVpc!.cidrBlock;
          formRef.current.setFieldValue('vpcCIDR', vpcCidrValue);
          formRef.current.setFieldValue('isNewVsw', 'new');
          formRef.current.setFieldValue('vSwitchCIDR', '');
          onVpcCidrChange(vpcCidrValue);
          reloadFields();
        },
      },
      dataSource: {
        type: 'static',
        static: map(vpcList, (item) => ({ name: item.vpcName, value: item.vpcID })),
      },
      removeWhen: [
        [
          {
            field: 'isNewVpc',
            operator: '=',
            value: 'new',
          },
        ],
      ],
      category: 'more',
    },
    vpcCIDRField,
    {
      label: i18n.t('cmp:switch network segment addition method'),
      component: 'select',
      key: 'isNewVsw',
      required: true,
      defaultValue: 'new',
      componentProps: {
        onChange: (e: string) => {
          if (e === 'exist') {
            formRef.current.setFieldValue('vSwitchCIDR', '');
            formRef.current.setFieldValue('vSwitchID', undefined);
            getVswList({
              vendor: 'aliyun',
              region: getFormData('region'),
              vpcID: getFormData('vpcID', ''),
              pageNo: 1,
              pageSize: 30,
            });
          } else {
            formRef.current.setFieldValue('vSwitchCIDR', get(cloudVendorMap, 'initValue.vSwitchCIDR'));
          }
          reloadFields();
        },
      },
      dataSource: {
        type: 'static',
        static: [
          {
            name: i18n.t('add (recommended)'),
            value: 'new',
          },
          ...insertWhen(getFormData('isNewVpc') === 'exist' && !!getFormData('vpcID', ''), [
            {
              name: i18n.t('select the existing'),
              value: 'exist',
            },
          ]),
        ],
      },
      category: 'more',
    },
    {
      label: 'VSwitch',
      component: 'select',
      key: 'vSwitchID',
      required: true,
      componentProps: {
        onChange: (e: string) => {
          const selectedVsw = find(vswList, { vSwitchID: e });
          formRef.current.setFieldValue('vSwitchCIDR', selectedVsw!.cidrBlock);
        },
      },
      dataSource: {
        // TODO 这里不使用dynamic方式书写api，是因为一旦调用setFields这个api就会被重新调用，导致重复call
        type: 'static',
        static: map(vswList, (item) => ({ name: `${item.vswName}(${item.cidrBlock})`, value: item.vSwitchID })),
      },
      removeWhen: [
        [
          {
            field: 'isNewVsw',
            operator: '=',
            value: 'new',
          },
        ],
      ],
      category: 'more',
    },
    vSwitchCIDRField,
    {
      label: i18n.t('cmp:Pod network segment'),
      component: 'input',
      key: 'podCIDR',
      defaultValue: getFormData('isNewVpc') === 'exist' ? undefined : get(cloudVendorMap, 'initValue.podCIDR'),
      rules: subNetRule,
      category: 'more',
    },
    {
      label: i18n.t('cmp:Service network segment'),
      component: 'input',
      key: 'serviceCIDR',
      defaultValue: get(cloudVendorMap, 'initValue.serviceCIDR'),
      rules: subNetRule,
      category: 'more',
    },
  ];

  useUpdateEffect(() => {
    formRef.current && formRef.current.setFields(cloneDeep(fields));
  }, [fields, vpcList, vswList, count]);

  const beforeAddConfirm = (values: any) => {
    const { chargePeriod, ...rest } = values;

    const currentOrg = orgStore.getState((s) => s.currentOrg);
    const { id: orgId, name: orgName } = currentOrg;
    const _postData = {
      ...rest,
      orgId,
      orgName,
      cloudVendor,
      chargePeriod: chargePeriod && Number(chargePeriod),
    };
    getCloudPreview(_postData).then((res: any) => {
      updater.postData(_postData);
      updater.previewData(res);
      updater.previewModalVis(true);
    });
  };

  const onOk = () => {
    formRef.current
      .validateFields()
      .then(async (values: any) => {
        beforeAddConfirm(values);
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        formRef.current.scrollToField(errorFields[0].name);
      });
  };

  const onClosePreview = () => {
    updater.previewModalVis(false);
    updater.previewData([]);
  };

  const onPreviewSubmit = () => {
    const _values = cloneDeep(postData);
    remove(_values, 'isNewVpc');
    remove(_values, 'isNewVsw');
    set(_values, 'clusterType', 'Edge');
    set(_values, 'cloudVendor', cloudVendor);

    addCloudCluster(_values).then((res) => {
      const { recordID } = res;
      onSubmit({ recordID });
    });
  };

  return (
    <>
      <Modal
        title={
          cloudVendor === 'alicloud-cs'
            ? i18n.t('cmp:add Alibaba Cloud Container Service Cluster (Proprietary Version)')
            : i18n.t('cmp:add Alibaba Cloud Container Service Cluster (Hosted Version)')
        }
        visible={visible}
        onCancel={onClose}
        onOk={onOk}
        destroyOnClose
        maskClosable={false}
        width={600}
      >
        <Form
          fields={fields}
          formRef={formRef}
          formRender={({ RenderFields, form, fields: totalFields }: any) => {
            const basicFields = filter(totalFields, { category: 'basic' });
            const moreFields = filter(totalFields, { category: 'more' });
            const region = getFormData('region');
            return (
              <>
                <div className="font-bold">{i18n.t('basic settings')}</div>
                <RenderFields form={form} fields={basicFields} />
                {region ? (
                  <>
                    <div className="font-bold">{i18n.t('dop:more settings')}</div>
                    <RenderFields form={form} fields={moreFields} />
                  </>
                ) : null}
              </>
            );
          }}
        />
      </Modal>
      <AliCloudFormPreview
        dataSource={previewData}
        visible={previewModalVis}
        onClose={onClosePreview}
        onOk={onPreviewSubmit}
      />
    </>
  );
};

export default AliCloudContainerClusterForm;
