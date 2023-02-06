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
import { Button, Popconfirm } from 'antd';
import { get } from 'lodash';
import { EmptyHolder, IF } from 'common';
import { useUpdate } from 'common/use-hooks';
import { WithAuth, usePerm } from 'user/common';
import { useEffectOnce } from 'react-use';
import routeInfoStore from 'core/stores/route';
import ArtifactsFormModal from './artifacts-form-modal';
import { ArtifactsTypeMap } from 'publisher/pages/artifacts/config';
import ArtifactsDetail from './artifacts-detail';
import publisherStore from '../../stores/publisher';

import './index.scss';
import { goTo } from 'common/utils';

const { ELSE } = IF;
const Artifacts = () => {
  const { publisherItemId: id } = routeInfoStore.getState((s) => s.params);
  const [artifactsDetail] = publisherStore.useStore((s) => [s.artifactsDetail]);
  const { deleteArtifacts, updateArtifacts, getArtifactDetail } = publisherStore.effects;
  const { clearArtifactsList, clearPublisherDetail } = publisherStore.reducers;
  const publishOperationAuth = usePerm((s) => s.org.publisher.operation.pass);
  const [{ chosenArtifactsId, formModalVis, editData }, updater] = useUpdate({
    chosenArtifactsId: '',
    formModalVis: false,
    editData: undefined as PUBLISHER.IArtifacts | undefined,
  });

  useEffectOnce(() => {
    return () => {
      clearArtifactsList();
      clearPublisherDetail();
    };
  });

  const openFormModal = () => {
    const geofenceLon = get(artifactsDetail, 'geofenceLon');
    updater.editData({ ...artifactsDetail, isGeofence: !!geofenceLon });
    updater.formModalVis(true);
  };

  const closeFormModal = () => {
    updater.editData(undefined);
    updater.formModalVis(false);
  };

  const onDelete = () => {
    artifactsDetail &&
      deleteArtifacts({ artifactsId: artifactsDetail.id }).then(() => {
        goTo('../');
      });
  };

  const setPublic = (isPublic: boolean) => {
    artifactsDetail &&
      updateArtifacts({ ...artifactsDetail, artifactsId: artifactsDetail.id, public: isPublic }).then(() => {
        getArtifactDetail(id);
      });
  };

  return (
    <div className="artifacts-list-container">
      <div className="top-button-group">
        <WithAuth pass={publishOperationAuth} tipProps={{ placement: 'bottom' }}>
          <Button type="primary" className="mr-2" ghost onClick={openFormModal}>
            {i18n.t('edit')}
          </Button>
        </WithAuth>
        {artifactsDetail && artifactsDetail.public ? (
          <Popconfirm
            title={i18n.t('is it confirmed?')}
            placement="bottomRight"
            onConfirm={() => {
              setPublic(false);
            }}
          >
            <WithAuth pass={publishOperationAuth} tipProps={{ placement: 'bottom' }}>
              <Button type="primary" className="mr-2" ghost>
                {i18n.t('publisher:withdraw')}
              </Button>
            </WithAuth>
          </Popconfirm>
        ) : (
          <>
            <Popconfirm title={i18n.t('is it confirmed?')} placement="bottomRight" onConfirm={() => setPublic(true)}>
              <WithAuth pass={publishOperationAuth} tipProps={{ placement: 'bottom' }}>
                <Button type="primary" className="mr-2" ghost>
                  {i18n.t('publisher:release')}
                </Button>
              </WithAuth>
            </Popconfirm>
            <Popconfirm title={i18n.t('is it confirmed?')} placement="bottomRight" onConfirm={onDelete}>
              <WithAuth pass={publishOperationAuth} tipProps={{ placement: 'bottom' }}>
                <Button type="primary" className="mr-2" ghost>
                  {i18n.t('delete')}
                </Button>
              </WithAuth>
            </Popconfirm>
          </>
        )}
        {artifactsDetail.public && artifactsDetail.type === ArtifactsTypeMap.MOBILE.value && (
          <Button
            type="primary"
            className="mr-2"
            ghost
            onClick={() => {
              window.open(goTo.resolve.market({ publishItemId: id }));
            }}
          >
            {i18n.t('preview')}
          </Button>
        )}
      </div>
      <div className="artifacts-detail-container">
        <IF check={artifactsDetail}>
          <ArtifactsDetail artifactsId={chosenArtifactsId} data={artifactsDetail as PUBLISHER.IArtifacts} />
          <ELSE />
          <EmptyHolder relative style={{ justifyContent: 'start' }} />
        </IF>
      </div>
      <ArtifactsFormModal
        visible={formModalVis}
        onCancel={closeFormModal}
        formData={editData}
        afterSubmit={() => {
          getArtifactDetail(id);
        }}
      />
    </div>
  );
};

export default Artifacts;
