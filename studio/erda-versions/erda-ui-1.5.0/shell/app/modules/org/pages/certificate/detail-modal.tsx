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
import { Modal, Button, Tooltip } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { isEmpty, get, map, isString } from 'lodash';
import i18n from 'i18n';
import { typeMap, keyPrefix } from './index';
import certificateStore from '../../stores/certificate';

interface IProps {
  id?: string;
  onClose: () => void;
  detail?: {
    type: string;
    iosInfo?: any;
    androidInfo?: any;
  };
}

interface IInfoProps {
  title: string;
  value: any;
  textItem?: Array<{ label: string; key: string; type?: string }>;
}

const defaultInfoItem = [{ label: i18n.t('password'), key: 'password', type: 'password' }];

const getFileRender = (
  fileInfo: { fileName: string; uuid: string },
  textItem: Array<{ label: string; key: string; type?: string }> = defaultInfoItem,
) => {
  const { fileName, uuid, ...rest } = fileInfo;
  if (!uuid) return null;
  return (
    <>
      <div className="table-operations mb-2">
        <Tooltip title={i18n.t('download')}>
          <a className="table-operations-btn" download={uuid} href={`/api/files/${uuid}`}>
            {fileName || uuid}
          </a>
        </Tooltip>
      </div>
      {getInfoRender(rest, textItem)}
    </>
  );
};

const getInfoRender = (data: any = {}, textItem: Array<{ label: string; key: string; type?: string }>) => {
  if (isEmpty(textItem)) return null;
  return (
    <>
      {map(textItem, ({ label, key, type }) => {
        const pswValue = data[key];
        return <TextItem label={label} value={pswValue} key={key} type={type} />;
      })}
    </>
  );
};

const TextItem = ({ value, label, type }: { value: string; label: string; type?: string }) => {
  const [show, setShow] = React.useState(false);
  const isPassword = type === 'password';
  return (
    <div className="mb-2">
      {label}: &nbsp;&nbsp;
      {!isPassword ? (
        value
      ) : (
        <Tooltip title={show ? undefined : i18n.t('click to view password')}>
          <span className={show ? '' : 'cursor-pointer'} onClick={() => setShow(true)}>
            {show ? value : '******'}
          </span>
        </Tooltip>
      )}
    </div>
  );
};

const InfoItem = ({ title, value, textItem }: IInfoProps) => {
  if (isEmpty(value)) return null;
  return (
    <div className="mb-6">
      <div className="text-desc mb-2">{title}</div>
      <div className="text-normal">{isString(value) ? value : getFileRender(value, textItem)}</div>
    </div>
  );
};

const DetailModal = ({ id, onClose, detail: originDetail }: IProps) => {
  const [{ detailVis, detail, detailMap }, updater, update] = useUpdate({
    detail: {} as Certificate.Detail,
    detailVis: false,
    detailMap: {} as Obj<Certificate.Detail>,
  });

  const { getDetail } = certificateStore.effects;

  React.useEffect(() => {
    update({
      detail: originDetail,
    });
  }, [originDetail, update]);

  React.useEffect(() => {
    updater.detailVis(!isEmpty(detail));
  }, [detail, updater]);

  const closeDetail = () => {
    onClose();
  };

  React.useEffect(() => {
    if (id) {
      const curDetail = detailMap[`${id}`];
      if (curDetail) {
        update({
          detail: curDetail,
        });
      } else {
        getDetail({ id }).then((res) => {
          update({
            detail: res,
            detailMap: { ...detailMap, [`${id}`]: res },
          });
        });
      }
    } else {
      update({
        detail: {},
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const getDetailComp = () => {
    if (isEmpty(detail)) return null;
    const { type } = detail;
    const renderData = [{ title: i18n.t('type'), value: type }] as any;
    if (type === typeMap.IOS.value) {
      renderData.push(
        ...[
          {
            title: `Keychain-p12 ${i18n.t('file')}`,
            value: get(detail, keyPrefix.iosKeyChainP12),
          },
          {
            title: `Debug-mobileprovision ${i18n.t('file')}`,
            value: get(detail, keyPrefix.iosDebug),
          },
          {
            title: `Release-mobileprovision ${i18n.t('file')}`,
            value: get(detail, keyPrefix.iosRelease),
          },
        ],
      );
    } else if (type === typeMap.Android.value) {
      renderData.push(
        ...[
          {
            title: `Debug-key/store ${i18n.t('file')}`,
            value: get(detail, keyPrefix.adrManualDebug),
            textItem: [
              { label: i18n.t('cmp:alias'), key: 'alias' },
              { label: 'Key password', key: 'keyPassword', type: 'password' },
              { label: 'Store password', key: 'storePassword', type: 'password' },
            ],
          },
          {
            title: `Release-key/store ${i18n.t('file')}`,
            value: get(detail, keyPrefix.adrManualRelease),
            textItem: [
              { label: i18n.t('cmp:alias'), key: 'alias' },
              { label: 'Key password', key: 'keyPassword', type: 'password' },
              { label: 'Store password', key: 'storePassword', type: 'password' },
            ],
          },
        ],
      );
    }
    return map(renderData, (item, idx) => <InfoItem key={`${idx}`} {...item} />);
  };

  return (
    <Modal
      visible={detailVis}
      title={detail.name || i18n.t('download')}
      footer={[
        <Button key="cancel" onClick={closeDetail}>
          {i18n.t('cancel')}
        </Button>,
      ]}
      onCancel={closeDetail}
    >
      {getDetailComp()}
    </Modal>
  );
};

export default DetailModal;
