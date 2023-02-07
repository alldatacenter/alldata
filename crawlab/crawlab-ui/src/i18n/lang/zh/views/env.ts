const env: LViewsEnv = {
  deps: {
    settings: {
      form: {
        key: '键',
        name: '名称',
        description: '描述',
        enabled: '启用',
        cmd: '命令',
        proxy: '代理',
      },
      lang: {
        python: {
          description: 'Python 环境依赖',
        },
        node: {
          description: 'Node.js 环境依赖',
        }
      },
    },
    dependency: {
      form: {
        name: '名称',
        latestVersion: '最新版本',
        installedVersion: '已安装版本',
        installedNodes: '已安装节点',
        allNodes: '所有节点',
        selectedNodes: '已选节点',
        upgrade: '是否升级',
        mode: '模式',
      },
    },
    task: {
      tasks: '任务',
      form: {
        action: '操作',
        node: '节点',
        status: '状态',
        dependencies: '依赖',
        time: '时间',
        logs: '日志',
      },
    },
    spider: {
      form: {
        name: '名称',
        dependencyType: '依赖类型',
        requiredVersion: '所需版本',
        installedVersion: '已安装版本',
        installedNodes: '已安装节点',
      },
    },
    common: {
      status: {
        installed: '已安装',
        installable: '可安装',
        upgradable: '可升级',
        downgradable: '可降级',
        noDependencyType: '无依赖类型',
      },
      actions: {
        installAndUpgrade: '安装并升级',
        installAndDowngrade: '安装并降级',
        searchDependencies: '搜索依赖',
      },
    }
  },
};

export default env;
