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

import { map, findLast, isEqual, cloneDeep, filter } from 'lodash';
import React from 'react';
import { FormInstance } from 'core/common/interface';
import { useUpdate } from 'common/use-hooks';
import { ErdaIcon } from 'common';
import { setLS, goTo } from 'common/utils';
import { Row, Col, Form, Input, Popconfirm, Modal, message } from 'antd';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import runtimeStore from 'runtime/stores/runtime';
import runtimeDomainStore from 'runtime/stores/domain';

import './domain-modal.scss';

const FormItem = Form.Item;

interface IProps {
  serviceName: string;
  form: FormInstance;
  visible: boolean;
  onCancel: () => void;
}

const DomainModal = (props: IProps) => {
  const [form] = Form.useForm();
  const { visible, serviceName, onCancel } = props;
  const {
    id: runtimeId,
    releaseId,
    clusterType,
    extra: { workspace },
  } = runtimeStore.useStore((s: any) => s.runtimeDetail);
  const domainMap = runtimeDomainStore.useStore((s) => s.domainMap);
  const { projectId } = routeInfoStore.useStore((s) => s.params);

  const initDomains = cloneDeep(domainMap[serviceName]);
  const [{ domains }, updater] = useUpdate({
    domains: initDomains,
  });

  React.useEffect(() => {
    if (visible) {
      updater.domains(cloneDeep(domainMap[serviceName]));
    }
  }, [domainMap, serviceName, updater, visible]);

  const saveConfig = () => {
    const doneSaveConfig = () => {
      domainMap[serviceName] = domains;
      setLS(`${runtimeId}_domain`, domainMap);
      onCancel();
    };
    form.validateFields().then((values: any) => {
      map(values, (realValue, keyStr) => {
        const [domainType, name, index] = keyStr.split('@');
        const target = index ? domains[index] : findLast(domains, domainType);
        name && (target[name] = realValue);
      });
      if (!isEqual(domainMap[serviceName], domains)) {
        if (['k8s', 'edas'].includes(clusterType)) {
          runtimeDomainStore
            .updateK8SDomain({
              runtimeId,
              releaseId,
              serviceName,
              domains: map(
                filter(
                  domains,
                  (domain) =>
                    (domain.domainType === 'DEFAULT' && domain.customDomain) || domain.domainType !== 'DEFAULT',
                ),
                (domain) => (domain.domainType === 'DEFAULT' ? domain.customDomain + domain.rootDomain : domain.domain),
              ),
            })
            .then(() => {
              setTimeout(() => {
                // TODO: refactor
                location.reload();
              }, 1000);
            });
        } else {
          doneSaveConfig();
          runtimeStore.setHasChange(true);
        }
      } else {
        message.warning(i18n.t('dop:no change'));
      }
    });
  };

  const addCustom = () => {
    if (domains.length >= 1) {
      updater.domains([
        ...domains,
        {
          domainType: 'CUSTOM',
          packageId: '',
          tenantGroup: '',
          appName: '',
          domain: '',
          customDomain: '',
          rootDomain: '',
          useHttps: true,
        },
      ]);
    }
  };

  const deleteCustom = (index: number) => {
    const newList = [...domains];
    newList.splice(index, 1);
    updater.domains(newList);
  };

  const hrefparams = React.useMemo(() => {
    let tenantGroup = '';
    let packageId = '';
    if (Array.isArray(domains)) {
      packageId = domains[0].packageId;
      tenantGroup = domains[0].tenantGroup;
    }
    return {
      tenantGroup,
      packageId,
    };
  }, [domains]);

  const gotoGetwayDetail = () => {
    const { tenantGroup, packageId } = hrefparams;
    // /microService/{projectId}/{env}/{tenantGroup}/gateway/api-package/{packageId}/detail'
    // const href = `/microService/${projectId}/${workspace}/${tenantGroup}/gateway/api-package/${packageId}/detail`;
    goTo(goTo.pages.getwayDetail, {
      projectId,
      env: workspace,
      packageId,
      tenantGroup,
    });
  };

  return (
    <Modal
      title={i18n.t('runtime:domain settings')}
      visible={visible}
      destroyOnClose
      onOk={saveConfig}
      onCancel={onCancel}
    >
      <Form layout="vertical" form={form}>
        <div className="config-item ml-3">
          <div className="flex justify-between items-center config-item-title font-medium text-base mb-2">
            <span>{serviceName}</span>
            <span style={{ marginRight: '40px' }}>
              {hrefparams.packageId && hrefparams.tenantGroup ? (
                <span className="text-xs fake-link" onClick={gotoGetwayDetail}>
                  {i18n.t('runtime:route rule configuration')}
                </span>
              ) : null}
            </span>
          </div>
          {map(domains, ({ domainType, customDomain, rootDomain, domain }, index) => {
            return domainType === 'DEFAULT' ? (
              <div key={domainType} className="default-area">
                <Row>
                  <Col span={22}>
                    <FormItem
                      label={i18n.t('runtime:domain name')}
                      name={`${domainType}@customDomain@${index}`}
                      initialValue={customDomain}
                      rules={[
                        // { required: true, message: i18n.t('runtime:please fill in the domain name') },
                        {
                          // 公司内项目不允许包含. 不允许有4级域名
                          pattern: rootDomain.includes('terminus')
                            ? /^[a-zA-Z0-9][-a-zA-Z0-9]{0,62}$/
                            : /^[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})*$/,
                          message: i18n.t('runtime:please fill in the correct domain name'),
                        },
                      ]}
                    >
                      <Input
                        placeholder={i18n.t('runtime:please fill in the domain name')}
                        addonAfter={rootDomain}
                        autoComplete="off"
                      />
                    </FormItem>
                  </Col>
                </Row>
                <div className="custom-domain" key="custom">
                  <span>{i18n.t('runtime:custom domain name')}:</span>
                  <span className="add-domain-icon">
                    <ErdaIcon
                      type="add-one"
                      className="ml-3 hover-active cursor-pointer mt-1"
                      onClick={() => addCustom()}
                      size="18"
                    />
                  </span>
                </div>
              </div>
            ) : (
              <Row key={domainType + index} type="flex" align="middle">
                <Col span={22}>
                  <FormItem className="hidden" name={`${domainType}@@${index}`} initialValue={serviceName}>
                    <Input />
                  </FormItem>
                  <FormItem
                    name={`${domainType}@domain@${index}`}
                    initialValue={domain}
                    rules={[
                      { required: true, message: i18n.t('runtime:please fill in the custom domain name') },
                      {
                        pattern: /^[a-zA-Z0-9*][-a-zA-Z0-9]{0,62}(\.[a-zA-Z0-9*][-a-zA-Z0-9]{0,62})+\.?$/,
                        message: i18n.t('runtime:please fill in the correct domain name'),
                      },
                    ]}
                  >
                    <InputItem onDelete={() => deleteCustom(index)} />
                  </FormItem>
                </Col>
              </Row>
            );
          })}
        </div>
      </Form>
    </Modal>
  );
};

export default DomainModal as any as (p: Omit<IProps, 'form'>) => JSX.Element;

interface IInputItemProps {
  value?: string;
  onChange?: (v: string) => void;
  onDelete: () => void;
}

const InputItem = (props: IInputItemProps) => {
  const { value, onChange, onDelete } = props;
  return (
    <>
      <Input
        value={value}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange?.(e.target.value)}
        placeholder={i18n.t('runtime:Custom domain name needs to be bound at the domain name provider.')}
        autoComplete="off"
      />
      <Popconfirm title={i18n.t('runtime:confirm deletion')} onConfirm={onDelete}>
        <span className="delete-domain-icon">
          {' '}
          <ErdaIcon type="reduce-one" size="18" className="hover-active cursor-pointer mt-1.5" />{' '}
        </span>
      </Popconfirm>
    </>
  );
};
