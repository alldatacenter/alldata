/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, { useCallback, useMemo } from 'react';
import { Divider } from 'antd';
import i18n from '@/i18n';
import { useLoadMeta, GroupMetaType } from '@/metas';
import { excludeObjectArray } from '@/utils';

export const useFormContent = ({ mqType, editing, isCreate, isUpdate }) => {
  const { Entity } = useLoadMeta<GroupMetaType>('group', mqType);

  const entityFields = useMemo(() => {
    return Entity ? new Entity().renderRow() : [];
  }, [Entity]);

  const excludeKeys = ['ensemble'].concat(isCreate ? 'mqResource' : '');
  const fields = excludeObjectArray(excludeKeys, entityFields || []);

  const formContent = isCreate
    ? fields.map(item => {
        if (item.name === 'inlongGroupId' && isUpdate) {
          return {
            ...item,
            props: {
              ...item.props,
              disabled: true,
            },
          };
        }
        return item;
      })
    : fields.map(item => {
        const t = transType(editing, item);
        return {
          ...item,
          type: t,
          suffix:
            typeof item.suffix === 'object' && !editing
              ? {
                  ...item.suffix,
                  type: 'text',
                }
              : item.suffix,
          extra: null,
          rules: t === 'text' ? undefined : item.rules,
        };
      });

  const { Entity: DefaultEntity } = useLoadMeta<GroupMetaType>('group', '');

  const isMqKey = useCallback(
    formName => {
      const defaultGroupKeysI18nMap = DefaultEntity?.I18nMap || {};
      return !defaultGroupKeysI18nMap[formName] || ['mqType', 'mqResource'].includes(formName);
    },
    [DefaultEntity?.I18nMap],
  );

  const basicFormContent = formContent.filter(item => !isMqKey(item.name));
  const mqFormContent = formContent.filter(item => isMqKey(item.name));

  return [
    {
      type: <Divider orientation="left">{i18n.t('pages.GroupDetail.Info.Basic')}</Divider>,
      col: 24,
    },
    ...basicFormContent,
    {
      type: <Divider orientation="left">{i18n.t('pages.GroupDetail.Info.Mq')}</Divider>,
      col: 24,
    },
    ...mqFormContent,
  ];
};

function transType(editing: boolean, conf) {
  const arr = [
    {
      name: [
        'name',
        'description',
        'inCharges',
        'dataReportType',
        'ensemble',
        'writeQuorum',
        'ackQuorum',
        'ttl',
        'retentionTime',
        'retentionSize',
      ],
      as: 'text',
      active: !editing,
    },
  ].reduce((acc, cur) => {
    return acc.concat(Array.isArray(cur.name) ? cur.name.map(name => ({ ...cur, name })) : cur);
  }, []);

  const map = new Map(arr.map(item => [item.name, item]));
  if (map.has(conf.name)) {
    const item = map.get(conf.name);
    return item.active ? item.as : conf.type;
  }

  return 'text';
}
