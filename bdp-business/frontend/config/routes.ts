const routes = [
  {
    path: '/user',
    layout: false,
    routes: [
      {
        path: '/user/login',
        name: 'login',
        component: './User/Login',
      },
    ],
  },
  {
    path: '/dashboard',
    name: 'dashboard', // menu 的多语言字段
    icon: 'desktop', // menu 的 icon
    component: './Dashboard',
    access: 'dashboardCanView',
  },
  {
    path: '/business',
    name: 'business',
    icon: 'radar-chart',
    access: 'businessCanManage',
    routes: [
      {
        path: '/business/account',
        name: 'account',
        component: './Business/Account',
        exact: true,
      },
    ],
  },
   {
        path: '/compute',
        name: 'compute',
        icon: 'radar-chart',
        access: 'computeCanManage',
        routes: [
          {
            path: '/compute/account',
            name: 'account',
            component: './Compute/Account',
            exact: true,
          },
        ],
    },

  {
        path: '/devops',
        name: 'devops',
        icon: 'radar-chart',
        access: 'devopsCanManage',
        routes: [
          {
            path: '/devops/account',
            name: 'account',
            component: './Devops/Account',
            exact: true,
          },
        ],
    },
  {
        path: '/govern',
        name: 'govern',
        icon: 'radar-chart',
        access: 'governCanManage',
        routes: [
          {
            path: '/govern/account',
            name: 'account',
            component: './Govern/Account',
            exact: true,
          },
        ],
    },

   {
        path: '/integrate',
        name: 'integrate',
        icon: 'radar-chart',
        access: 'integrateCanManage',
        routes: [
          {
            path: '/integrate/account',
            name: 'account',
            component: './Integrate/Account',
            exact: true,
          },
        ],
    },

  {
        path: '/intelligence',
        name: 'intelligence',
        icon: 'radar-chart',
        access: 'intelligenceCanManage',
        routes: [
          {
            path: '/intelligence/account',
            name: 'account',
            component: './Intelligence/Account',
            exact: true,
          },
        ],
    },
    {
          path: '/ods',
          name: 'ods',
          icon: 'radar-chart',
          access: 'odsCanManage',
          routes: [
            {
              path: '/ods/account',
              name: 'account',
              component: './Ods/Account',
              exact: true,
            },
          ],
      },
  {
      path: '/olap',
      name: 'olap',
      icon: 'radar-chart',
      access: 'olapCanManage',
      routes: [
        {
          path: '/olap/account',
          name: 'account',
          component: './Olap/Account',
          exact: true,
        },
      ],
  },
  {
      path: '/optimize',
      name: 'optimize',
      icon: 'radar-chart',
      access: 'optimizeCanManage',
      routes: [
        {
          path: '/optimize/account',
          name: 'account',
          component: './Optimize/Account',
          exact: true,
        },
      ],
  },
  {
      path: '/storage',
      name: 'storage',
      icon: 'radar-chart',
      access: 'storageCanManage',
      routes: [
        {
          path: '/storage/account',
          name: 'account',
          component: './Storage/Account',
          exact: true,
        },
      ],
  },

  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    component: './404',
  },
]

export default routes
