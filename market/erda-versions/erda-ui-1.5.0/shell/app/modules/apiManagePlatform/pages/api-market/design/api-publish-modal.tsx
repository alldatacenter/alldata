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
import { FormModal, Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import routeInfoStore from 'core/stores/route';
import { message, Tooltip } from 'antd';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { isEmpty } from 'lodash';
import orgStore from 'app/org-home/stores/org';

const VERSION_TIP = (
  <div>
    <div>{i18n.t('dop:tips of publish api 1')}</div>
    <div>
      <a href="https://semver.org/lang/zh-CN/" target="_blank" rel="noopener noreferrer">
        https://semver.org/lang/zh-CN/
      </a>
    </div>
    <div>{i18n.t('dop:tips of publish api 2')}</div>
  </div>
);

const idReg = /^([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]$/;
interface IProps {
  visible: boolean;
  treeNodeData: API_SETTING.ITreeNodeData;
  onSubmit: (values: any) => void;
  onClose: () => void;
}

const ApiPublishModal = (props: IProps) => {
  const { visible, onClose, onSubmit, treeNodeData } = props;
  const formRef = React.useRef({}) as MutableRefObject<FormInstance>;
  const [openApiDoc] = apiDesignStore.useStore((s) => [s.openApiDoc]);

  const { appId, projectId } = routeInfoStore.useStore((s) => s.params);
  const orgId = orgStore.getState((s) => s.currentOrg.id);

  const [{ formData }, updater] = useUpdate({
    formData: {} as Obj,
  });

  React.useEffect(() => {
    const asset = treeNodeData?.asset || {};
    const { major, minor, patch } = asset;
    if (major !== undefined || minor !== undefined || patch !== undefined) {
      const version = `${major}.${minor}.${patch}`;
      updater.formData({
        ...asset,
        assetName: asset.assetName,
        assetID: asset.assetID,
        version,
      });
    } else {
      const tempFormData = asset || {};
      updater.formData({
        ...tempFormData,
        assetName: asset.assetName,
        assetID: asset.assetID,
      });
    }
  }, [formRef, treeNodeData, updater]);

  const apiFieldList = [
    {
      label: i18n.t('API name'),
      name: 'assetName',
      type: 'input',
      required: true,
      itemProps: {
        placeholder: i18n.t('dop:example {content}', {
          content: i18n.t('dop:interface document in user center'),
        }),
      },
    },
    {
      label: 'API ID',
      type: 'input',
      name: 'assetID',
      required: true,
      itemProps: {
        placeholder: i18n.t('dop:example {content}', { content: 'user-content' }),
      },
      rules: [
        {
          pattern: idReg,
          message: i18n.t(
            'default:start with number or letter, can contain numbers, letters, dots, hyphens and underscores',
          ),
        },
      ],
    },
    {
      label: (
        <span>
          {i18n.t('dop:release version')}
          <Tooltip title={VERSION_TIP}>
            <CustomIcon type="tishi" />
          </Tooltip>
        </span>
      ),
      type: 'input',
      name: 'version',
      required: false,
      itemProps: {
        placeholder: i18n.t('dop:example {content}', { content: 'user-content' }),
        autoComplete: 'off',
      },
      rules: [
        {
          pattern: /^(?:[1-9]\d*|0)\.(?:[1-9]\d*|0)\.(?:[1-9]\d*|0)$/,
          message: i18n.t('Please enter a valid version number, such as x.y.z.'),
        },
      ],
    },
  ];

  const onPublishApi = (values: { assetName: string; assetID: string; version: string }) => {
    if (openApiDoc?.paths && !isEmpty(openApiDoc.paths)) {
      const { assetName, assetID, version } = values;
      const [major, minor, patch] = version?.split('.') || [];
      const _versions: Obj = { inode: treeNodeData.inode, specProtocol: 'oas3-yaml' };
      if (version) {
        _versions.major = +major;
        _versions.minor = +minor;
        _versions.patch = +patch;
      }
      onSubmit({
        assetID,
        assetName,
        source: 'design_center',
        projectID: +projectId,
        appID: +appId,
        orgId: +orgId,
        versions: [_versions],
      });
    } else {
      message.warning(i18n.t('dop:api can not be empty'));
    }
    onClose();
  };

  return (
    <FormModal
      title={i18n.t('dop:publish documents')}
      fieldsList={apiFieldList}
      visible={visible}
      ref={formRef}
      onOk={onPublishApi}
      onCancel={onClose}
      formData={formData}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    />
  );
};

export default ApiPublishModal;
