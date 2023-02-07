import {RouteRecordRaw} from 'vue-router';

const endpoint = 'env';

export default [
  {
    name: 'EnvDeps',
    path: `${endpoint}/deps`,
    redirect: to => {
      return {path: to.path + '/settings'};
    }
  },
  {
    name: 'EnvDepsSettings',
    path: `${endpoint}/deps/settings`,
    component: () => import('@/views/env/deps/setting/DependencySettings.vue'),
  },
  {
    name: 'EnvDepsPython',
    path: `${endpoint}/deps/python`,
    component: () => import('@/views/env/deps/python/DependencyPython.vue'),
  },
  {
    name: 'EnvDepsNode',
    path: `${endpoint}/deps/node`,
    component: () => import('@/views/env/deps/node/DependencyNode.vue'),
  }
] as Array<RouteRecordRaw>;
