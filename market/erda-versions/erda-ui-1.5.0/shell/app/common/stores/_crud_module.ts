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

import { createStore } from 'core/cube';
import { getDefaultPaging } from 'common/utils';
import { CubeState } from 'cube-state/dist/typings';
import { map } from 'lodash';
import agent from 'agent';
import i18n from 'i18n';

export interface Effects {
  [key: string]: EffectFn;
}
export type EffectFn = (...payload: any) => Promise<any>;
interface IProps<P> {
  name: string;
  services: {
    get: (arg: any) => IPagingResp<P>;
    add?: (arg: any) => Promise<any>;
    update?: (arg: any) => Promise<any>;
    delete?: (arg: any) => Promise<any>;
  };
  effects?: CubeState.EnhanceEffects<IState<P>>;
}
export interface IState<P> {
  paging: IPaging;
  list: P[];
}

export const createCRUDStore = <P>(props: IProps<P>) => {
  const { name, services, effects } = props;
  const initState: IState<P> = {
    paging: getDefaultPaging(),
    list: [],
  };

  const thisStore = createStore({
    name,
    state: initState,
    effects: {
      async getList({ call, update }, payload) {
        const result = await call(services.get, payload, { paging: { key: 'paging' } });
        update({ list: result.list });
        return { ...result };
      },
      async addItem({ call }, payload) {
        if (services.add) {
          const res = await call(services.add, payload, { successMsg: i18n.t('added successfully') });
          return res;
        }
        return null;
      },
      async updateItem({ call }, payload) {
        if (services.update) {
          const res = await call(services.update, payload, { successMsg: i18n.t('updated successfully') });
          return res;
        }
      },
      async deleteItem({ call }, payload) {
        if (services.delete) {
          const res = await call(services.delete, payload, { successMsg: i18n.t('deleted successfully') });
          return res;
        }
      },
    },
    reducers: {
      clearList(state) {
        return { ...state, list: [], paging: getDefaultPaging() };
      },
    },
  });
  return effects ? thisStore.extend({ effects }) : thisStore;
};

export interface ICRUDStore extends ReturnType<typeof createCRUDStore> {
  [pro: string]: any;
}

interface IServiceProps {
  API: {
    get: string;
    add?: string;
    update?: string;
    delete?: string;
  };
}

export const createCRUDService = <P>({ API }: IServiceProps) => {
  const replacePattern = /\{([\w.])+\}/g;
  const services = {
    getList: (payload: any): IPagingResp<P> => {
      return agent
        .get(API.get)
        .query(payload)
        .then((response: any) => response.body);
    },
    addItem: API.add
      ? (data: P) => {
          return agent
            .post(API.add)
            .send(data)
            .then((response: any) => response.body);
        }
      : undefined,
    updateItem: API.update
      ? (payload: P) => {
          let api = API.delete || '';
          const matches = (API.delete as string).match(replacePattern);
          map(matches, (match) => {
            api = api.replace(match, payload[match]);
          });
          return agent
            .put(api)
            .send(payload)
            .then((response: any) => response.body);
        }
      : undefined,
    deleteItem: API.delete
      ? (payload: any) => {
          let api = API.delete || '';
          const matches = (API.delete as string).match(replacePattern);
          map(matches, (match) => {
            api = api.replace(match, payload[match]);
          });
          return agent.delete(api).then((response: any) => response.body);
        }
      : undefined,
  };
  return services;
};
