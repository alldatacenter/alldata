const plugin: LComponentsPlugin = {
  form: {
    installType: '安装类别',
    autoStart: '自动开始',
    name: '名称',
    installUrl: '安装 URL',
    installPath: '安装路径',
    command: '命令',
    description: '描述',
  },
  installType: {
    label: {
      public: '公共',
      git: 'Git',
      local: '本地'
    },
    notice: {
      public: '安装 Crawlab 团队维护的官方公共插件',
      git: '安装 Git URL 地址对应的第三方插件',
      local: '从本地安装插件',
    },
  },
  install: {
    title: '安装插件',
    repoUrl: '仓库地址',
    author: '作者',
    pushedAt: '最近提交时间',
    updatedAt: '最近更新时间',
  },
  settings: {
    title: '设置',
    label: {
      installSource: '安装源',
      goProxy: 'Go 代理',
    },
    tips: {
      installSource: '如果您在中国内地，您可以选择安装源为 "Gitee" 来加速插件安装过程',
      goProxy: '如果您在中国内地，您可以设置 Go 代理 来加速插件编译过程',
    },
    goProxy: {
      default: '默认',
    },
  },
  status: {
    label: {
      installing: '安装中',
      stopped: '已停止',
      running: '运行中',
      error: '错误',
      unknown: '未知',
      installed: '已安装',
    },
    tooltip: {
      installing: '正在安装插件',
      stopped: '插件已终止',
      running: '插件正在运行',
      error: '插件异常终止',
      unknown: '未知插件状态',
      installed: '插件已安装',
    },
    nodes: {
      tooltip: {
        installing: '正在安装节点',
        stopped: '已停止节点',
        running: '运行中节点',
        error: '错误节点',
        unknown: '未知节点',
      }
    }
  },
};

export default plugin;
