import {RouteRecordRaw} from 'vue-router';

const endpoint = 'home';

export default [
  {
    name: 'Home',
    path: endpoint,
    component: () => import('@/views/home/Home.vue'),
  },
] as Array<RouteRecordRaw>;
