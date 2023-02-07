import {RouteRecordRaw} from 'vue-router';
import {TAB_NAME_OVERVIEW, TAB_NAME_TRIGGERS, TAB_NAME_TEMPLATE} from '@/constants';

const endpoint = 'notifications';

export default [
  {
    name: 'NotificationList',
    path: `${endpoint}`,
    component: () => import('@/views/notification/list/NotificationList.vue'),
  },
  {
    name: 'NotificationDetail',
    path: `${endpoint}/:id`,
    redirect: to => {
      return {path: to.path + '/' + TAB_NAME_OVERVIEW};
    },
    component: () => import('@/views/notification/detail/NotificationDetail.vue'),
    children: [
      {
        path: TAB_NAME_OVERVIEW,
        component: () => import('@/views/notification/detail/tabs/NotificationDetailTabOverview.vue'),
      },
      {
        path: TAB_NAME_TRIGGERS,
        component: () => import('@/views/notification/detail/tabs/NotificationDetailTabTriggers.vue'),
      },
      {
        path: TAB_NAME_TEMPLATE,
        component: () => import('@/views/notification/detail/tabs/NotificationDetailTabTemplate.vue'),
      },
    ],
  },
] as Array<RouteRecordRaw>;
