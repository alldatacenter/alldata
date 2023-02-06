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
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import { Form, Button } from 'antd';
import { isEmpty, map, debounce, pick, get, filter, uniq } from 'lodash';
import { JumpBoardForm } from './deploy-cluster-forms/jump-board-form';
import { ClusterConfigForm } from './deploy-cluster-forms/cluster-config-form';
import { ClusterFPSForm } from './deploy-cluster-forms/cluster-fps-form';
import { ClusterSSHForm } from './deploy-cluster-forms/cluster-ssh-form';
import { DockerForm } from './deploy-cluster-forms/docker-form';
// import { MainPlatformForm } from './deploy-cluster-forms/main-platform-form';
import { NodesForm } from './deploy-cluster-forms/nodes-form';
import { PlatformForm } from './deploy-cluster-forms/platform-form';
import { StorageForm } from './deploy-cluster-forms/storage-form';

import './deploy-cluster-form.scss';

interface IProps {
  [pro: string]: any;
  orgId: number;
  orgName: string;
  form: FormInstance;
  onSubmit: (arg: any) => void;
  formRef?: any;
  data?: any;
}

const formMenu = [
  { key: 'jump-board', name: i18n.t('jump server') },
  { key: 'cluster-config', name: i18n.t('cluster infos') },
  { key: 'cluster-ssh', name: i18n.t('cmp:cluster ssh infos') },
  { key: 'cluster-fps', name: i18n.t('cmp:file proxy service') },
  { key: 'storage', name: i18n.t('cmp:storage configs') },
  { key: 'nodes', name: i18n.t('cmp:node list') },
  { key: 'platform', name: i18n.t('platform configs') },
  { key: 'docker', name: i18n.t('cmp:docker configs') },
];

const eleInView = (ref: any) => {
  const curRef = ref && ref.current;
  if (curRef) {
    const { offsetHeight } = curRef;
    const pos = curRef.getBoundingClientRect();
    if (pos.top < 180 && pos.top + offsetHeight >= 180) {
      return true;
    }
  }
  return false;
};
const DeployClusterForm = (props: IProps) => {
  const { orgId, orgName, onSubmit, data, form, formRef } = props;

  const formDivRef = React.useRef(null);
  const [scrollTop, setScrollTop] = React.useState(0);
  const [activeKey, setActiveKey] = React.useState('jump-board');
  const categoryRefsMap = React.useRef({} as any);
  const [formData, setFormData] = React.useState(data || {});

  React.useEffect(() => {
    if (!isEmpty(data)) {
      setFormData(data);
      setTimeout(() => {
        // 后面的表单有些需要根据data来展示过滤某些表单，所以先初始化表单后再setFiedlsValue
        // 如jump中私钥和密码二选一，需要先让jump中先初始化后再set
        data && formRef && form.setFieldsValue(pick(data, Object.keys(formRef.fieldsStore.fieldsMeta)));
      }, 0);
    }
  }, [data, form, formRef]);

  const debounceCheck = React.useCallback(
    debounce(() => {
      if (formDivRef && formDivRef.current) {
        const curEle = formDivRef.current as any;
        setScrollTop(curEle.scrollTop);
      }
    }, 100),
    [formDivRef],
  );

  React.useEffect(() => {
    const refMap = {};
    if (formMenu) {
      formMenu.forEach((item: any) => {
        refMap[`${item.key}`] = React.createRef();
      });
    }
    categoryRefsMap.current = refMap;

    const operateScrollEvent = (isRemove?: boolean) => {
      if (!formDivRef) {
        return;
      }
      const curRef = formDivRef.current as any;
      if (curRef) {
        !isRemove
          ? curRef.addEventListener('scroll', debounceCheck)
          : curRef.removeEventListener('scroll', debounceCheck);
      }
    };

    operateScrollEvent();
    return () => {
      operateScrollEvent(true);
    };
  }, [debounceCheck]);

  React.useEffect(() => {
    if (!isEmpty(categoryRefsMap.current)) {
      map(categoryRefsMap.current, (ref: any, key) => {
        if (eleInView(ref)) {
          setActiveKey(key);
        }
      });
    }
  }, [scrollTop]);

  React.useEffect(() => {
    if (categoryRefsMap && categoryRefsMap.current) {
      const cur = categoryRefsMap.current[activeKey] as any;
      if (cur && cur.current && !eleInView(cur)) {
        cur.current.scrollIntoView();
      }
    }
  }, [activeKey]);

  const scrollToTarget = (key: string) => {
    if (categoryRefsMap && categoryRefsMap.current) {
      const cur = categoryRefsMap.current[key] as any;
      if (cur && cur.current) {
        cur.current.scrollIntoView();
      }
    }
    setActiveKey(key);
  };

  const handleSubmit = () => {
    props.form
      .validateFields()
      .then((values: any) => {
        const postData = { ...values } as any;

        const nodes = get(values, 'config.nodes');
        if (nodes) {
          postData.config.nodes = filter(
            map(nodes, (item: any) => {
              if (item) {
                const { ip, type, tag } = item;
                const reTag = `org-${orgName}${tag ? `,${tag}` : ''}`;
                // tag：添加org标签后，去重
                return { ip, type, tag: uniq(reTag.split(',')).join(',') };
              }
              return item;
            }),
            (n: any) => !!n.ip,
          );
        }
        postData.orgID = Number(orgId);
        postData.saas = true; // 隐藏、后续可能展现
        const jumpPort = get(postData, 'jump.port');
        if (jumpPort !== undefined) {
          postData.jump.port = Number(jumpPort);
        }
        const sshPort = get(postData, 'config.ssh.port');
        if (sshPort !== undefined) {
          postData.config.ssh.port = Number(sshPort);
        }
        const fpsPort = get(postData, 'config.fps.port');
        if (fpsPort !== undefined) {
          postData.config.fps.port = Number(fpsPort);
        }
        const replica = get(postData, 'config.storage.gluster.replica');
        if (replica !== undefined) {
          postData.config.storage.gluster.replica = Number(replica);
        }
        const mysqlPort = get(postData, 'config.platform.mysql.port');
        if (mysqlPort !== undefined) {
          postData.config.platform.mysql.port = Number(mysqlPort);
        }
        const platformPort = get(postData, 'config.platform.port');
        if (platformPort !== undefined) {
          postData.config.platform.port = Number(platformPort);
        }
        const mainPlatformPort = get(postData, 'config.mainPlatform.port');
        if (mainPlatformPort !== undefined) {
          postData.config.mainPlatformPort.port = Number(mainPlatformPort);
        }

        onSubmit && onSubmit(postData);
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        props.form.scrollToField(errorFields[0].name);
      });
  };

  const reset = () => {
    props.form.resetFields();
    setFormData({});
    scrollToTarget('jump-board'); // 重置表单后回到页头
  };

  return (
    <div className="deploy-cluster-form">
      <div className="left" ref={formDivRef}>
        <JumpBoardForm {...props} data={formData} curRef={categoryRefsMap.current['jump-board']} />
        <ClusterConfigForm {...props} data={formData} curRef={categoryRefsMap.current['cluster-config']} />
        <ClusterSSHForm {...props} data={formData} curRef={categoryRefsMap.current['cluster-ssh']} />
        <ClusterFPSForm {...props} data={formData} curRef={categoryRefsMap.current['cluster-fps']} />
        <StorageForm {...props} data={formData} curRef={categoryRefsMap.current.storage} />
        <NodesForm {...props} data={formData} curRef={categoryRefsMap.current.nodes} />
        <PlatformForm {...props} data={formData} curRef={categoryRefsMap.current.platform} />
        <DockerForm {...props} data={formData} curRef={categoryRefsMap.current.docker} />
        {/* <MainPlatformForm {...props} data={formData} /> */}

        <div className="op-row">
          <Button type="primary" onClick={handleSubmit}>
            {i18n.t('done')}
          </Button>
          <Button className="ml-3" onClick={reset}>
            {i18n.t('reset')}
          </Button>
        </div>
      </div>
      <div className="right">
        {formMenu.map(({ key, name }: { key: string; name: string }) => {
          return (
            <div
              key={key}
              className={`form-menu-item ${activeKey === key ? 'active' : ''}`}
              onClick={() => scrollToTarget(key)}
            >
              {name}
            </div>
          );
        })}
      </div>
    </div>
  );
};
const Forms = (props) => {
  const [form] = Form.useForm();
  return <DeployClusterForm form={form} {...props} />;
};

const EnhancedForm = (props: any) => {
  const formRef = React.useRef(null);
  return <Forms {...props} ref={formRef} formRef={formRef.current} />;
};
export default EnhancedForm;
