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

// import React from 'react';
import { groupForm } from '@/metas/group';
import { excludeObjectArray } from '@/utils';

export const getFormContent = ({ editing, isCreate, isUpdate }) => {
  const excludeKeys = ['ensemble'].concat(isCreate ? 'mqResource' : '');
  const fields = excludeObjectArray(excludeKeys, groupForm);

  return isCreate
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
    : fields.map(item => ({
        ...item,
        type: transType(editing, item),
        suffix:
          typeof item.suffix === 'object' && !editing
            ? {
                ...item.suffix,
                type: 'text',
              }
            : item.suffix,
        extra: null,
      }));
};

function transType(editing: boolean, conf) {
  const arr = [
    {
      name: [
        'name',
        'description',
        'inCharges',
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
