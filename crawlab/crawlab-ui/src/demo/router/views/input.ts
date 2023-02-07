import {RouteRecordRaw} from 'vue-router';
import DemoInputList from '@/demo/views/input/DemoInputList.vue';

const endpoint = '/demo/input';

export default [
  {
    path: `${endpoint}/list`,
    component: DemoInputList,
  },
] as Array<RouteRecordRaw>;
