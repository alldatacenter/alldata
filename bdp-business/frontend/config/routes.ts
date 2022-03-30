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
        path: '/business/logan',
        name: 'logan',
        component: './Business/Logan',
        exact: true,
      },
      {
        path: '/business/collectreport',
        name: 'collectreport',
        component: './Business/CollectReport',
        exact: true,
      },
      {
        path: '/business/kafka',
        name: 'kafka',
        component: './Business/Kafka',
        exact: true,
      },
      {
        path: '/business/logconsume',
        name: 'logconsume',
        component: './Business/LogConsume',
        exact: true,
      },
      {
        path: '/business/spider',
        name: 'spider',
        component: './Business/Spider',
        exact: true,
      },
      {
        path: '/business/etl',
        name: 'etl',
        component: './Business/Etl',
        exact: true,
      },
      {
        path: '/business/ecommerce',
        name: 'ecommerce',
        component: './Business/ECommerce',
        exact: true,
      },
      {
        path: '/business/shop',
        name: 'shop',
        component: './Business/Shop',
        exact: true,
      },
      {
        path: '/business/business',
        name: 'business',
        component: './Business/Business',
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
            path: '/compute/offline',
            name: 'offline',
            component: './Compute/Offline',
            exact: true,
          },
          {
            path: '/compute/offlinedev',
            name: 'offlinedev',
            component: './Compute/OfflineDev',
            exact: true,
          },
          {
            path: '/compute/realtime',
            name: 'realtime',
            component: './Compute/RealTime',
            exact: true,
          },
          {
            path: '/compute/realtimedev',
            name: 'realtimedev',
            component: './Compute/RealTimeDev',
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
            path: '/devops/ambari',
            name: 'ambari',
            component: './Devops/Ambari',
            exact: true,
          },
          {
            path: '/devops/docker',
            name: 'docker',
            component: './Devops/Docker',
            exact: true,
          },
          {
            path: '/devops/io',
            name: 'io',
            component: './Devops/IO',
            exact: true,
          },
          {
            path: '/devops/k8s',
            name: 'k8s',
            component: './Devops/K8S',
            exact: true,
          },
          {
            path: '/devops/linux',
            name: 'linux',
            component: './Devops/Linux',
            exact: true,
          },
          {
            path: '/devops/promethues',
            name: 'promethues',
            component: './Devops/Promethues',
            exact: true,
          },
          {
            path: '/devops/skywalking',
            name: 'skywalking',
            component: './Devops/Skywalking',
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
            path: '/govern/dataquality',
            name: 'dataquality',
            component: './Govern/DataQuality',
            exact: true,
          },
          {
            path: '/govern/dataofflinedevstandard',
            name: 'datastandard',
            component: './Govern/DataStandard',
            exact: true,
          },
          {
            path: '/govern/dataservice',
            name: 'dataservice',
            component: './Govern/DataService',
            exact: true,
          },
          {
            path: '/govern/metadata',
            name: 'metadata',
            component: './Govern/Metadata',
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
            path: '/integrate/canal',
            name: 'canal',
            component: './Integrate/Canal',
            exact: true,
          },
          {
            path: '/integrate/cdc',
            name: 'cdc',
            component: './Integrate/CDC',
            exact: true,
          },
          {
            path: '/integrate/datax',
            name: 'datax',
            component: './Integrate/Datax',
            exact: true,
          },
          {
            path: '/integrate/flinkx',
            name: 'flinkx',
            component: './Integrate/FlinkX',
            exact: true,
          },
          {
            path: '/integrate/flume',
            name: 'flume',
            component: './Integrate/Flume',
            exact: true,
          },
          {
            path: '/integrate/fscrawler',
            name: 'fscrawler',
            component: './Integrate/Fscrawler',
            exact: true,
          },
          {
            path: '/integrate/offlinesync',
            name: 'offlinesync',
            component: './Integrate/OfflineSync',
            exact: true,
          },
          {
            path: '/integrate/realtimesync',
            name: 'realtimesync',
            component: './Integrate/RealtimeSync',
            exact: true,
          },
          {
            path: '/integrate/sqoop',
            name: 'sqoop',
            component: './Integrate/Sqoop',
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
            path: '/intelligence/dataanalyse',
            name: 'dataanalyse',
            component: './Intelligence/DataAnalyse',
            exact: true,
          },
          {
            path: '/intelligence/datamine',
            name: 'datamine',
            component: './Intelligence/DataMine',
            exact: true,
          },
          {
            path: '/intelligence/machinelearn',
            name: 'machinelearn',
            component: './Intelligence/MachineLearn',
            exact: true,
          },
          {
            path: '/intelligence/pytorch',
            name: 'pytorch',
            component: './Intelligence/Pytorch',
            exact: true,
          },
          {
            path: '/intelligence/tensorflow',
            name: 'tensorflow',
            component: './Intelligence/Tensorflow',
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
              path: '/ods/buriedpoint',
              name: 'buriedpoint',
              component: './Ods/BuriedPoint',
              exact: true,
            },
            {
              path: '/ods/flume',
              name: 'flume',
              component: './Ods/Flume',
              exact: true,
            },
            {
              path: '/ods/ftp',
              name: 'ftp',
              component: './Ods/Ftp',
              exact: true,
            },
            {
              path: '/ods/http',
              name: 'http',
              component: './Ods/Http',
              exact: true,
            },
            {
              path: '/ods/kafka',
              name: 'kafka',
              component: './Ods/Kafka',
              exact: true,
            },
            {
              path: '/ods/spider',
              name: 'spider',
              component: './Ods/Spider',
              exact: true,
            },
            {
              path: '/ods/ssh',
              name: 'ssh',
              component: './Ods/SSH',
              exact: true,
            },
            {
              path: '/ods/syslog',
              name: 'syslog',
              component: './Ods/Syslog',
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
          path: '/olap/clickhouse',
          name: 'clickhouse',
          component: './Olap/ClickHouse',
          exact: true,
        },
        {
          path: '/olap/doris',
          name: 'doris',
          component: './Olap/Doris',
          exact: true,
        },
        {
          path: '/olap/druid',
          name: 'druid',
          component: './Olap/Druid',
          exact: true,
        },
        {
          path: '/olap/hive',
          name: 'hive',
          component: './Olap/Hive',
          exact: true,
        },
        {
          path: '/olap/impala',
          name: 'impala',
          component: './Olap/Impala',
          exact: true,
        },
        {
          path: '/olap/kudu',
          name: 'kudu',
          component: './Olap/Kudu',
          exact: true,
        },
        {
          path: '/olap/kylin',
          name: 'kylin',
          component: './Olap/Kylin',
          exact: true,
        },
        {
          path: '/olap/presto',
          name: 'presto',
          component: './Olap/Presto',
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
          path: '/optimize/compress',
          name: 'compress',
          component: './Optimize/Compress',
          exact: true,
        },
        {
          path: '/optimize/cpu',
          name: 'cpu',
          component: './Optimize/Cpu',
          exact: true,
        },
        {
          path: '/optimize/http',
          name: 'http',
          component: './Optimize/Http',
          exact: true,
        },
        {
          path: '/optimize/jvm',
          name: 'jvm',
          component: './Optimize/Jvm',
          exact: true,
        },
        {
          path: '/optimize/memory',
          name: 'memory',
          component: './Optimize/Memory',
          exact: true,
        },
        {
          path: '/optimize/mpp',
          name: 'mpp',
          component: './Optimize/Mpp',
          exact: true,
        },
        {
          path: '/optimize/rpc',
          name: 'rpc',
          component: './Optimize/Rpc',
          exact: true,
        },
        {
          path: '/optimize/serialization',
          name: 'serialization',
          component: './Optimize/Serialization',
          exact: true,
        },
        {
          path: '/optimize/storagecalculation',
          name: 'storagecalculation',
          component: './Optimize/StorageCalculation',
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
          path: '/storage/clickhouse',
          name: 'clickhouse',
          component: './Storage/ClickHouse',
          exact: true,
        },
        {
          path: '/storage/drill',
          name: 'drill',
          component: './Storage/Drill',
          exact: true,
        },
        {
          path: '/storage/greenplum',
          name: 'greenplum',
          component: './Storage/Greenplum',
          exact: true,
        },
        {
          path: '/storage/hudi',
          name: 'hudi',
          component: './Storage/Hudi',
          exact: true,
        },
        {
          path: '/storage/iceberg',
          name: 'iceberg',
          component: './Storage/Iceberg',
          exact: true,
        },
        {
          path: '/storage/janusgraph',
          name: 'janusgraph',
          component: './Storage/JanusGraph',
          exact: true,
        },
        {
          path: '/storage/kudu',
          name: 'kudu',
          component: './Storage/Kudu',
          exact: true,
        },
        {
          path: '/storage/kylin',
          name: 'kylin',
          component: './Storage/Kylin',
          exact: true,
        },
        {
          path: '/storage/memcache',
          name: 'memcache',
          component: './Storage/Memcache',
          exact: true,
        },
        {
          path: '/storage/presto',
          name: 'presto',
          component: './Storage/Presto',
          exact: true,
        },
        {
          path: '/storage/rdbms',
          name: 'rdbms',
          component: './Storage/RDBMS',
          exact: true,
        },
        {
          path: '/storage/redis',
          name: 'redis',
          component: './Storage/Redis',
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
