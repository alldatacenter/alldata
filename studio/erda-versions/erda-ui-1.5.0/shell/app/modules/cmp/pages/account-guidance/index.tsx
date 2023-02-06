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
import cloudAccountStore from 'app/modules/cmp/stores/cloud-account';
import { guidanceImgMap } from './config';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import { get, map, reduce } from 'lodash';
import { getAccountsFieldsList } from 'cmp/common/cloud-common';
import i18n from 'i18n';
import './index.scss';

interface ITypeProps {
  onChosen: any;
  updater: any;
  type: string;
  name: string;
  icon: {
    default: string;
    active: string;
  };
  description: string;
  disabled: boolean;
}

const TypeCard = (props: ITypeProps) => {
  const { onChosen, updater, ...type } = props;
  const [isHover, setIsHover] = React.useState(false);
  const onHover = () => setIsHover(true);
  const outHover = () => setIsHover(false);
  const onClick = () => {
    updater.initialSelect(get(type, 'val', ''));
    onChosen(type);
  };
  const isAbleHover = isHover && !type.disabled;
  return (
    <div
      className={`guidance-type-card
        ${type.disabled ? 'not-allowed' : ''}`}
      onMouseEnter={onHover}
      onMouseLeave={outHover}
      onClick={!type.disabled ? onClick : () => {}}
    >
      <div className="type-icon">
        <img src={isAbleHover ? get(type, 'icon.active') : get(type, 'icon.default')} alt="cloud-type" />
      </div>
      <div className="type-name">{type.name}</div>
      <div className="type-description">{type.description}</div>
    </div>
  );
};

const cloudAccountArr = [
  {
    type: 'alicould',
    name: i18n.t('alibaba cloud'),
    val: 'aliyun',
    icon: guidanceImgMap.alicloud,
    description: i18n.t('cmp:after-adding-key'),
    disabled: false,
  },
  {
    type: 'txcloud',
    name: i18n.t('cmp:other cloud vendors'),
    icon: guidanceImgMap.txcloud,
    description: i18n.t('stay tuned'),
    disabled: true,
  },
];

const selectArr = reduce(
  cloudAccountArr,
  (arr: any[], { name, val: value }) => {
    if (name && value) arr.push({ value, name });
    return arr;
  },
  [],
);

interface IProps {
  afterSubmit?: (resBody: any) => any;
}

const AccountGuidance = (props: IProps) => {
  const { afterSubmit } = props;
  const { addItem } = cloudAccountStore.effects;

  const [{ formVisible, initialSelect }, updater] = useUpdate({
    formVisible: false,
    initialSelect: '',
  });

  const handleFormSubmit = (data: CLOUD_ACCOUNTS.Account) => {
    updater.formVisible(false);
    return addItem(data).then(afterSubmit);
  };

  return (
    <>
      <div className="account-guidance">
        <div className="guidance-title mb-2">{i18n.t('cmp:select-cloud-account')}</div>
        <p className="guidance-desc text-sub mb-6">{i18n.t('cmp:after-config-can-do')}</p>
        <div className="guidance-type-row mb-4">
          {map(cloudAccountArr, (item) => (
            <TypeCard key={item.type} updater={updater} onChosen={() => updater.formVisible(true)} {...item} />
          ))}
        </div>
      </div>
      <FormModal
        title={i18n.t('add {name}', { name: i18n.t('account') })}
        visible={formVisible}
        fieldsList={getAccountsFieldsList(selectArr, initialSelect)}
        onCancel={() => updater.formVisible(false)}
        onOk={handleFormSubmit}
      />
    </>
  );
};

export default AccountGuidance;
