import {RouteRecordRaw} from 'vue-router';
import nav from '@/demo/router/views/nav';
import chart from '@/demo/router/views/chart';
import input from '@/demo/router/views/input';

export default [
  ...nav,
  ...chart,
  ...input,
] as Array<RouteRecordRaw>;
