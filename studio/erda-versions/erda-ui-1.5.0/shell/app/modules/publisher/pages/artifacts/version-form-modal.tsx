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

import { FormModal, LoadMoreSelector } from 'common';
import i18n from 'i18n';
import React from 'react';
import { map, get } from 'lodash';
import { FormInstance } from 'core/common/interface';
import publisherStore from 'app/modules/publisher/stores/publisher';
import { getJoinedApps } from 'user/services/user';
import { getReleaseList } from 'application/services/release';

interface IProps {
  visible: boolean;
  artifacts: PUBLISHER.IArtifacts;
  onCancel: () => void;
  afterSubmit?: () => any;
}

const VersionFormModal = ({ visible, onCancel, artifacts, afterSubmit = () => {} }: IProps) => {
  const { addVersion } = publisherStore.effects;
  const { type } = artifacts;
  const [chosenApp, setChosenApp] = React.useState('');
  const [chosenVersion, setChosenVersion] = React.useState('');
  React.useEffect(() => {
    if (!visible) {
      setChosenApp('');
      setChosenVersion('');
    }
  }, [visible]);

  const handelSubmit = (data: any) => {
    onCancel();
    const { releaseId } = data;
    addVersion({ artifactsId: artifacts.id, releaseId, version: chosenVersion }).then(() => {
      afterSubmit();
    });
  };

  const fieldsList = (form: FormInstance) => [
    {
      label: i18n.t('application'),
      name: 'app',
      itemProps: {
        onChange: (v: string) => {
          setChosenApp(v);
          setChosenVersion('');
          form.setFieldsValue({ releaseId: undefined });
        },
      },
      getComp: () => {
        const getData = (q: any) => {
          return getJoinedApps({ ...q, mode: type }).then((res: any) => res.data);
        };
        return (
          <LoadMoreSelector
            getData={getData}
            dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
              total,
              list: map(list, (app) => {
                const { name, id, projectName, projectId } = app;
                return {
                  ...app,
                  label: `${projectName || projectId} / ${name}`,
                  value: id,
                };
              }),
            })}
          />
        );
      },
    },
    {
      label: i18n.t('version'),
      name: 'releaseId',
      itemProps: {
        onChange: (v: string, opt: any) => {
          setChosenVersion(get(opt, 'version'));
        },
      },
      getComp: () => {
        const getArtifacts = (q: any) => {
          if (!chosenApp) return;
          return getReleaseList({ ...q, applicationId: chosenApp }).then((res: any) => res.data);
        };
        return (
          <LoadMoreSelector
            getData={getArtifacts}
            extraQuery={{ applicationId: chosenApp }}
            dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
              total,
              list: map(list, (release) => {
                const { releaseId, version } = release;
                return {
                  ...release,
                  label: version,
                  value: releaseId,
                };
              }),
            })}
          />
        );
      },
    },
  ];

  return (
    <FormModal
      name={i18n.t('version')}
      fieldsList={fieldsList}
      visible={visible}
      onOk={handelSubmit}
      onCancel={onCancel}
      modalProps={{
        maskClosable: false,
        destroyOnClose: true,
      }}
    />
  );
};

export default VersionFormModal;
