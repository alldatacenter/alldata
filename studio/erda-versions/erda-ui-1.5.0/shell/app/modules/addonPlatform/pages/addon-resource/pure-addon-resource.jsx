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
import { isEmpty } from 'lodash';
import moment from 'moment';
import { IF } from 'common';
import { ENV_NAME, PLAN_NAME } from 'app/modules/addonPlatform/pages/common/configs';
import i18n from 'i18n';

import './addon-resource.scss';

const AddonResource = (props) => {
  const { resourceInfo } = props;
  if (isEmpty(resourceInfo)) return null;

  const { createdAt, reference, workspace, addonName, plan, cluster, version } = resourceInfo;
  const instanceData = [
    { key: i18n.t('common:middleware'), value: addonName },
    { key: i18n.t('version'), value: version },
    { key: i18n.t('common:running cluster'), value: cluster },
    { key: i18n.t('running environment'), value: ENV_NAME[workspace] },
    { key: i18n.t('common:specifications'), value: PLAN_NAME[plan] },
    { key: i18n.t('common:reference times'), value: reference },
    { key: i18n.t('create time'), value: moment(createdAt).format('YYYY-MM-DD HH:mm:ss') },
  ];

  return (
    <div className="addon-resource-panel">
      <div className="resource-config-panel">
        <div className="basic-info">
          {instanceData.map(({ key, value, hasValue = true }) => {
            return (
              <IF key={key} check={hasValue}>
                <div>
                  <div className="info-key">{key}</div>
                  <div className="info-value">{value}</div>
                </div>
              </IF>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default AddonResource;
