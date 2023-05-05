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
import { Icon as CustomIcon } from 'common';
import { FormInstance } from 'core/common/interface';
import { regRules } from 'common/utils';
import i18n from 'i18n';

export interface IFormProps {
  form: FormInstance;
  isReadonly?: boolean;
  curRef?: any;
  data?: any;
}

export const regRulesMap = {
  subnet: {
    pattern:
      /^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[0-9]{1,2})(\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[0-9]{1,2})){3}\/[0-9]+$/,
    message: i18n.t('cmp:please fill in the correct network segment'),
  },
  wildcardDomain: {
    pattern: /^(?=^.{3,255}$)[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+$/,
    message: i18n.t('cmp:please fill in the correct extensive domain'),
  },
  port: { pattern: /^([0-9])+$/, message: i18n.t('cmp:please enter the correct port') },
  clusterName: {
    pattern: /^[a-z0-9]{1,20}-[a-z0-9]{1,20}$/,
    message: i18n.t('cmp:letters and numbers, separated by hyphen'),
  },
  absolutePath: { pattern: /^\//, message: i18n.t('cmp:absolute path starts with /') },
  ipWithComma: {
    pattern:
      /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*$/,
    message: i18n.t('Please enter the IPv4 list, separated by comma.'),
  },
  ...regRules,
};

interface IReadonlyProps {
  fieldsList: IField[];
  data?: any;
}
interface IField {
  label?: string;
  name?: string;
  viewType?: string;
  itemProps?: {
    [proName: string]: any;
    type?: string;
  };
  getComp?: (o?: any) => any;
}

export const FormUnitContainer = ({ children, title, curRef }: { children: any; title: string; curRef?: any }) => {
  const [isPresent, setIsPresent] = React.useState(true);
  return (
    <div className={`form-container ${isPresent ? 'block' : 'hidden'}`} ref={curRef}>
      {title ? (
        <div className="form-title">
          {title}
          <CustomIcon type="chevron-down" onClick={() => setIsPresent(!isPresent)} />
        </div>
      ) : null}
      <div className="form-content">{children}</div>
    </div>
  );
};
