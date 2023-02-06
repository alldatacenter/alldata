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
import { Modal, Radio } from 'antd';
import { find, get, map, set, filter, cloneDeep, remove } from 'lodash';
import { Form } from 'dop/pages/form-editor/index';
import clusterStore from 'cmp/stores/cluster';
import { clusterSpecMap } from '../config';
import { regRulesMap } from '../form-utils';
import { insertWhen, regRules } from 'common/utils';
import i18n from 'i18n';
import { useUpdate } from 'common/use-hooks';
import orgStore from 'app/org-home/stores/org';

interface IProps {
  onClose: () => void;
  onSubmit: ({ recordID }: { recordID: string }) => void;
  visible: boolean;
}

const AliCloudErdcForm = ({ visible, onClose, onSubmit }: IProps) => {
  const formRef = React.useRef(null as any);
  const clusterList = clusterStore.useStore((s) => s.list);
  const { addCloudCluster } = clusterStore.effects;

  const [{ storage }, updater] = useUpdate({
    storage: 'nas',
  });

  const basicFields = [
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
    },
    {
      label: i18n.t('cmp:cluster specifications'),
      component: 'radio',
      key: 'clusterSize',
      componentProps: {
        radioType: 'radio',
      },
      required: true,
      defaultValue: get(clusterSpecMap.erdc, 'prod.value'),
      dataSource: {
        static: () =>
          map(clusterSpecMap.erdc, ({ name, value }) => (
            <Radio.Button key={value} value={value}>
              {name}
            </Radio.Button>
          )),
      },
    },
    {
      label: i18n.t('cmp:whether to enable https'),
      component: 'switch',
      key: 'enableHttps',
      required: true,
      defaultValue: false,
    },
  ];

  const springboardFields = [
    {
      label: i18n.t('cmp:machine IP'),
      component: 'input',
      key: 'installerIp',
      required: true,
    },
    {
      label: i18n.t('user name'),
      component: 'input',
      key: 'user',
      required: true,
    },
    {
      label: i18n.t('password'),
      component: 'input',
      key: 'password',
      required: true,
      isPassword: true,
    },
    {
      label: i18n.t('port'),
      component: 'input',
      key: 'port',
      required: true,
      defaultValue: '22',
      rules: [
        {
          pattern: String(/^\d+$/),
          msg: i18n.t('msp:please key in numbers'),
        },
      ],
    },
  ];

  const storageFields = [
    {
      label: i18n.t('cmp:shared storage'),
      component: 'select',
      key: 'storage',
      required: true,
      componentProps: {
        onChange: (e: string) => {
          updater.storage(e);
        },
      },
      dataSource: {
        type: 'static',
        static: [
          {
            name: 'NAS',
            value: 'nas',
          },
          {
            name: 'Glusterfs',
            value: 'glusterfs',
          },
        ],
      },
      defaultValue: 'nas',
    },
    ...insertWhen(storage === 'nas', [
      {
        label: i18n.t('cmp:NAS address'),
        component: 'input',
        key: 'nasDomain',
        required: true,
      },
    ]),
    ...insertWhen(storage !== 'nas', [
      {
        label: i18n.t('cmp:Glusterfs server IP address list'),
        component: 'input',
        key: 'glusterfsIps',
        required: true,
        componentProps: {
          placeholder: i18n.t('cmp:3 or 1 is recommended, and separate them by comma'),
        },
      },
    ]),
  ];

  const networkFields = [
    {
      label: i18n.t('cmp:container segment'),
      component: 'input',
      key: 'dockerCIDR',
      rules: [
        {
          pattern: String(regRulesMap.subnet.pattern),
          msg: regRulesMap.subnet.message,
        },
      ],
      required: true,
    },
    {
      label: i18n.t('cmp:Pod network segment'),
      component: 'input',
      key: 'podCIDR',
      required: true,
      rules: [
        {
          pattern: String(regRulesMap.subnet.pattern),
          msg: regRulesMap.subnet.message,
        },
      ],
    },
    {
      label: i18n.t('cmp:Service network segment'),
      component: 'input',
      key: 'serviceCIDR',
      required: true,
      rules: [
        {
          pattern: String(regRulesMap.subnet.pattern),
          msg: regRulesMap.subnet.message,
        },
      ],
    },
  ];

  const serverFields = [
    {
      label: i18n.t('cmp:domain name server address list'),
      component: 'input',
      key: 'nameservers',
      componentProps: {
        placeholder: i18n.t('cmp:separate by comma'),
      },
      required: true,
      rules: [
        {
          pattern: String(regRulesMap.ipWithComma.pattern),
          msg: regRulesMap.ipWithComma.message,
        },
      ],
    },
  ];

  const machineFields = [
    {
      label: i18n.t('cmp:machine IP list'),
      component: 'input',
      key: 'hostIps',
      required: true,
      componentProps: {
        placeholder: i18n.t('cmp:separate by comma'),
      },
      rules: [
        {
          pattern: String(regRulesMap.ipWithComma.pattern),
          msg: regRulesMap.ipWithComma.message,
        },
      ],
    },
    {
      label: i18n.t('cmp:data disk device'),
      component: 'input',
      key: 'device',
      componentProps: {
        placeholder: i18n.t('cmp:such as vdb, which does not support multiple data disks, can be empty.'),
      },
    },
  ];

  const fields = [
    ...map(basicFields, (field) => ({ ...field, category: 'basic' })),
    ...map(springboardFields, (field) => ({ ...field, category: 'springboard' })),
    ...map(storageFields, (field) => ({ ...field, category: 'storage' })),
    ...map(networkFields, (field) => ({ ...field, category: 'network' })),
    ...map(serverFields, (field) => ({ ...field, category: 'server' })),
    ...map(machineFields, (field) => ({ ...field, category: 'machine' })),
  ];

  React.useEffect(() => {
    formRef.current && formRef.current.setFields(cloneDeep(fields));
  }, [fields, storage]);

  const onOk = () => {
    formRef.current
      .validateFields()
      .then((values: any) => {
        const currentOrg = orgStore.getState((s) => s.currentOrg);
        const { id: orgId, name: orgName } = currentOrg;
        remove(values, 'storage');
        remove(values, 'isNewVpc');
        remove(values, 'isNewVsw');
        set(values, 'clusterType', 'Edge');
        set(values, 'cloudVendor', 'alicloud-ecs');

        const _postData = {
          ...values,
          orgId,
          orgName,
        };

        addCloudCluster(_postData).then((res) => {
          const { recordID } = res;
          onSubmit({ recordID });
        });
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        formRef.current.scrollToField(errorFields[0].name);
      });
  };

  return (
    <>
      <Modal
        title={i18n.t('cmp:add existing resources to build a cluster')}
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
            const bFields = filter(totalFields, { category: 'basic' });
            const sFields = filter(totalFields, { category: 'springboard' });
            const stFields = filter(totalFields, { category: 'storage' });
            const dFields = filter(totalFields, { category: 'network' });
            const seFields = filter(totalFields, { category: 'server' });
            const mFields = filter(totalFields, { category: 'machine' });
            return (
              <>
                <div className="font-bold mb-1">{i18n.t('cmp:cluster configuration')}</div>
                <RenderFields form={form} fields={bFields} />
                <div className="font-bold mb-1">{i18n.t('cmp:jump server configuration')}</div>
                <RenderFields form={form} fields={sFields} />
                <div className="font-bold mb-1">{i18n.t('cmp:shared storage')}</div>
                <RenderFields form={form} fields={stFields} />
                <div className="font-bold mb-1">{i18n.t('cmp:network configuration')}</div>
                <RenderFields form={form} fields={dFields} />
                <div className="font-bold mb-1">{i18n.t('cmp:domain name server')}</div>
                <RenderFields form={form} fields={seFields} />
                <div className="font-bold mb-1">{i18n.t('cmp:machine information configuration')}</div>
                <RenderFields form={form} fields={mFields} />
              </>
            );
          }}
        />
      </Modal>
    </>
  );
};

export default AliCloudErdcForm;
