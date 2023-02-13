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

import { useRequest } from '@/hooks';
import request from '@/utils/request';
import type { NodeInfo } from './NodeInfo';
import { nodes } from '..';

export const useListNodeDao = ({ options }) => {
  return useRequest<{
    total: number;
    list: NodeInfo[];
  }>(
    {
      url: '/node/list',
      method: 'POST',
      data: {
        ...options,
      },
    },
    {
      refreshDeps: [options],
    },
  );
};

export const useFindNodeDao = ({ onSuccess }) => {
  return useRequest<NodeInfo, [string]>(
    async id => {
      const result = await request(`/node/get/${id}`);
      const LoadEntity = nodes.find(item => item.value === result.type)?.LoadEntity;
      const Entity = (await LoadEntity())?.default;
      const parseData = new Entity().parse(result);

      return {
        ...parseData,
        inCharges: result.inCharges?.split(','),
        clusterTags: result.clusterTags?.split(','),
      };
    },
    {
      manual: true,
      onSuccess,
    },
  );
};

export const useSaveNodeDao = () => {
  return {
    runAsync: async data => {
      const LoadEntity = nodes.find(item => item.value === data.type)?.LoadEntity;
      const Entity = (await LoadEntity())?.default;
      const stringifyData = new Entity().stringify(data);
      const result = await request({
        url: `/node/${Boolean(data.id) ? 'update' : 'save'}`,
        method: 'POST',
        data: {
          ...stringifyData,
          inCharges: (data.inCharges as any)?.join(','),
          clusterTags: (data.clusterTags as any)?.join(','),
        },
      });

      return result;
    },
  };
};

export const useDeleteNodeDao = () => {
  return {
    runAsync: id =>
      request({
        url: `/node/delete/${id}`,
        method: 'DELETE',
      }),
  };
};
