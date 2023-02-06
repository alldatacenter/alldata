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

import { i18n } from 'src/common';

export const getValidText = (v?: string, validType?: 'phone' | 'email' | 'password') => {
  const validMap = {
    phone: {
      pattern: /^(1[3|4|5|7|8|9])\d{9}$/,
      message: i18n.t('Please enter the correct {name}', { name: i18n.t('mobile') }),
    },
    email: {
      pattern: /^(\w-*\.*)+@(\w-?)+(\.\w{2,})+$/,
      message: i18n.t('Please enter the correct {name}', { name: i18n.t('email') }),
    },
    password: {
      pattern: /^(?=.*?[A-Z])(?=(.*[a-z]){1,})(?=(.*[\d]){0,})(?=(.*[\W]){1,})(?!.*\s).{8,}$/,
      message: i18n.t('password-tip'),
    },
  };

  if (!!v) {
    const curValid = validType && validMap[validType];
    return curValid && !curValid.pattern.test(v) ? curValid.message : '';
  } else {
    return i18n.t('can not be empty');
  }
};

export const getErrorValid = <T>(errorRes: UC.IKratosData): Partial<T> => {
  const errRes: UC.IKratosData = errorRes;
  const uiMsg = errRes?.ui?.messages?.[0]?.text;
  if (uiMsg) {
    return { page: uiMsg } as unknown as Partial<T>;
  } else {
    const uiNodes = errRes?.ui?.nodes;
    const errTips: Partial<T> = {};
    uiNodes?.forEach((errNode) => {
      const nodeErrMsg = errNode?.messages?.[0]?.text;
      const curKey = errNode?.attributes?.type as keyof T;
      if (nodeErrMsg && curKey) {
        errTips[curKey] = nodeErrMsg as unknown as T[keyof T];
      }
    });
    return { page: '', ...errTips };
  }
};
