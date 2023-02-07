import {RouteRecordRaw} from 'vue-router';
import DemoLineChart from '@/demo/views/chart/DemoLineChart.vue';
import DemoMetric from '@/demo/views/chart/DemoMetric.vue';

const endpoint = '/demo/chart';

export default [
  {
    path: `${endpoint}/line`,
    component: DemoLineChart,
  },
  {
    path: `${endpoint}/metric`,
    component: DemoMetric,
  }
] as Array<RouteRecordRaw>;
