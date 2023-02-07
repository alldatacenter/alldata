import {RouteRecordRaw} from 'vue-router';

const endpoint = 'misc';

export default [
  {
    name: 'Disclaimer',
    path: `${endpoint}/disclaimer`,
    component: () => import('@/views/misc/Disclaimer.vue'),
  },
  {
    name: 'MySettings',
    path: `${endpoint}/my-settings`,
    component: () => import('@/views/misc/MySettings.vue'),
  },
] as Array<RouteRecordRaw>;
