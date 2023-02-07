import {RouteRecordRaw} from 'vue-router';
import {getStore} from '@/store';
import components from '@/demo/router/views';

export const initDemoRoutes = () => {
  // store
  const store = getStore();
  const state = store.state as RootStoreState;
  const {
    layout: layoutState
  } = state;

  const menuItems = [
    {
      title: 'Demo',
      icon: ['fa', 'laptop'],
      children: [
        {
          title: 'Nav',
          icon: ['fa', 'route'],
          children: [
            {path: '/demo/nav/sidebar/list', title: 'Sidebar List', icon: ['fa', 'list']},
            {path: '/demo/nav/sidebar/tree', title: 'Sidebar Tree', icon: ['fa', 'folder-tree']},
          ]
        },
        {
          title: 'Chart',
          icon: ['fa', 'chart-bar'],
          children: [
            {path: '/demo/chart/line', title: 'Line Chart', icon: ['fa', 'chart-line']},
            {path: '/demo/chart/metric', title: 'Metric', icon: ['fa', 'dashboard']},
          ]
        },
        {
          title: 'Input',
          icon: ['fa', 'square-check'],
          children: [
            {path: '/demo/input/list', title: 'Input List', icon: ['fa', 'list']},
          ]
        }
      ],
    }
  ] as MenuItem[];

  // merge sidebar menu items
  store.commit('layout/setMenuItems', [
    ...layoutState.menuItems,
    ...menuItems,
  ]);
};

export const getDemoRoutes = (): Array<RouteRecordRaw> => {
  return [
    ...components,
  ];
};
