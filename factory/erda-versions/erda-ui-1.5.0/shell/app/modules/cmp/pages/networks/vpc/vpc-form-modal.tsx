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
import { Modal, Button } from 'antd';
import { RenderForm } from 'common';
import { useUpdate } from 'common/use-hooks';
import { get, map } from 'lodash';
import networksStore from 'cmp/stores/networks';
import { useEffectOnce } from 'react-use';
import { VswCIDRField, VpcCIDRField } from '../common/components/cidr-input';
import { getSubnetCount, validateIsSubnet } from '../common/util';
import { formConfig } from '../common/config';
import { FormInstance } from 'core/common/interface';
import { useLoading } from 'core/stores/loading';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import i18n from 'i18n';

interface IProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (arg?: any) => void;
}

interface IFormProps {
  visible: boolean;
}

const VpcForm = React.forwardRef((props: IFormProps, ref: any) => {
  const { visible } = props;
  const [chosenCIDRType, setChosenCIDRType] = React.useState('default');
  const { getCloudRegion } = cloudCommonStore;
  const regions = cloudCommonStore.useStore((s) => s.regions);

  useEffectOnce(() => {
    getCloudRegion();
  });

  const fieldsList = [
    {
      getComp: () => <div>{i18n.t('cmp:VPC')}</div>,
    },
    {
      label: i18n.t('name'),
      name: 'vpcName',
      rules: [formConfig.rule.name],
      itemProps: {
        maxLength: 128,
      },
    },
    {
      name: 'vendor',
      initialValue: 'aliyun',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('region'),
      name: 'region',
      type: 'select',
      options: map(regions, ({ regionID, localName }) => ({ value: regionID, name: `${localName} (${regionID})` })),
    },
    {
      label: `IPv4 ${i18n.t('cmp:CIDR')}`,
      extraProps: {
        extra: formConfig.extra.CIDR[chosenCIDRType],
      },
      name: 'cidrBlock',
      initialValue: formConfig.options.defaultCIDR[0],
      rules: [{ validator: validateIsSubnet(formConfig.options.defaultCIDR) }],
      getComp: () => {
        return <VpcCIDRField onChangeCIDRType={setChosenCIDRType} cidrType={chosenCIDRType} />;
      },
    },
    {
      label: i18n.t('description'),
      name: 'description',
      required: false,
      type: 'textArea',
      rules: [formConfig.rule.description],
      itemProps: {
        maxLength: 256,
      },
    },
  ];
  return (
    <div className={`${visible ? '' : 'hidden'}`}>
      <RenderForm layout="vertical" list={fieldsList} ref={ref} />
    </div>
  );
});

interface IVswFormProps extends IFormProps {
  vpc: NETWORKS.ICloudVpc | undefined;
}

const VswForm = React.forwardRef((props: IVswFormProps, ref: any) => {
  const { visible, vpc = {} } = props;
  const { cidrBlock: vpcCidrBlock, vendor, region } = vpc as NETWORKS.IVpcCreateBody;
  const [{ zones, subnetCount }, updater] = useUpdate({
    zones: [] as NETWORKS.ICloudZone[],
    subnetCount: 0,
  });
  const { getCloudZone } = networksStore.effects;
  React.useEffect(() => {
    if (vendor && region) {
      const curForm = get(ref, 'current');
      if (curForm) {
        curForm.setFieldsValue({ zoneID: undefined, vswRegion: region, vswVendor: vendor });
      }
      getCloudZone({ vendor, region }).then((res) => {
        updater.zones(res || []);
      });
    }
  }, [vendor, region, getCloudZone, updater, ref]);

  const fieldsList = [
    {
      getComp: () => <div>{i18n.t('cmp:VSwitches')}</div>,
    },
    {
      name: 'vswRegion',
      initialValue: region,
      itemProps: {
        type: 'hidden',
      },
    },
    {
      name: 'vswVendor',
      initialValue: vendor,
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('name'),
      name: 'vswName',
      rules: [formConfig.rule.name],
      itemProps: {
        maxLength: 128,
      },
    },
    {
      label: formConfig.label.Zone,
      name: 'zoneID',
      type: 'select',
      options: map(zones, (zone) => ({ name: `${zone.localName}(${zone.zoneID})`, value: zone.zoneID })),
    },
    {
      label: `IPv4 ${i18n.t('cmp:CIDR')}`,
      getComp: ({ form }: { form: FormInstance }) => {
        return (
          <VswCIDRField
            formKey="vswCidrBlock"
            form={form}
            vpcCidrBlock={vpcCidrBlock}
            onChangeMask={(mask: number) => updater.subnetCount(getSubnetCount(mask))}
          />
        );
      },
    },
    {
      label: i18n.t('cmp:number of available IP'),
      getComp: () => `${subnetCount || 0}`,
    },
    {
      label: i18n.t('description'),
      name: 'vswDescription',
      required: false,
      type: 'textArea',
      rules: [formConfig.rule.description],
      itemProps: {
        maxLength: 256,
      },
    },
  ];
  return (
    <div className={`${visible ? '' : 'hidden'}`}>
      <RenderForm layout="vertical" list={fieldsList} ref={ref} />
    </div>
  );
});

const VpcFormModal = (props: IProps) => {
  const { visible, onCancel, onOk } = props;
  const { addVpc, addVsw } = networksStore.effects;
  const [{ stepKey }, updater] = useUpdate({
    stepKey: 'vpc',
  });
  const vpcRef = React.useRef(null as any);
  const vswRef = React.useRef(null as any);
  const [addVpcLoading, addVswLoading] = useLoading(networksStore, ['addVpc', 'addVsw']);
  React.useEffect(() => {
    if (!visible) {
      updater.stepKey('vpc');
    }
  }, [updater, visible]);

  const handleStepChange = (step: string) => {
    if (step === 'vsw') {
      const vpcFormRef = get(vpcRef, 'current');
      if (vpcFormRef) {
        vpcFormRef
          .validateFields()
          .then(() => {
            updater.stepKey(step);
          })
          .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
            vpcFormRef.scrollToField(errorFields[0].name);
          });
      }
    } else {
      updater.stepKey(step);
    }
  };

  const handelSubmit = () => {
    const vpcFormRef = get(vpcRef, 'current');
    const vswFormRef = get(vswRef, 'current');
    if (vswFormRef) {
      vpcFormRef
        .validateFields()
        .then((vpc: NETWORKS.IVpcCreateBody) => {
          vswFormRef
            .validateFields()
            .then((vsw: any) => {
              const { vswRegion: region, vswVendor: vendor, vswCidrBlock, vswDescription: description, ...rest } = vsw;
              addVpc(vpc).then((res) => {
                // 先保存vpc，获取vpcID后保存vsw
                const vswData = {
                  ...rest,
                  region,
                  vendor,
                  cidrBlock: `${vswCidrBlock.slice(0, 4).join('.')}/${vswCidrBlock.slice(4)}`,
                  description,
                  vpcID: res.vpcID,
                };
                addVsw(vswData).then((vswRes: any) => {
                  if (vswRes.success) {
                    onOk();
                  }
                });
              });
            })
            .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
              vswFormRef.scrollToField(errorFields[0].name);
            });
        })
        .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
          vpcFormRef.scrollToField(errorFields[0].name);
        });
    }
  };

  const formFootMap = {
    vpc: [
      <Button key="back" onClick={onCancel}>
        {i18n.t('cancel')}
      </Button>,
      <Button key="next" type="primary" onClick={() => handleStepChange('vsw')}>
        {i18n.t('msp:next')}
      </Button>,
    ],
    vsw: [
      <Button key="back" onClick={onCancel}>
        {i18n.t('cancel')}
      </Button>,
      <Button key="prev" type="primary" onClick={() => handleStepChange('vpc')}>
        {i18n.t('msp:prev')}
      </Button>,
      <Button key="ok" type="primary" loading={addVpcLoading || addVswLoading} onClick={handelSubmit}>
        {i18n.t('ok')}
      </Button>,
    ],
  };
  const vpcFormRef = get(vpcRef, 'current');
  return (
    <Modal
      title={i18n.t('add {name}', { name: i18n.t('cmp:VPC') })}
      visible={visible}
      maskClosable={false}
      destroyOnClose
      onCancel={onCancel}
      footer={formFootMap[stepKey]}
    >
      <VpcForm visible={stepKey === 'vpc'} ref={vpcRef} />
      <VswForm visible={stepKey === 'vsw'} ref={vswRef} vpc={(vpcFormRef && vpcFormRef.getFieldsValue()) || {}} />
    </Modal>
  );
};

export default VpcFormModal;
