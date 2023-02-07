const node: LComponentsNode = {
  form: {
    key: '唯一标识',
    name: '名称',
    tags: '标签',
    type: '类别',
    ip: 'IP',
    mac: 'MAC 地址',
    hostname: '主机名',
    enabled: '是否启用',
    max_runners: '最大执行器数',
    description: '描述'
  },
  nodeType: {
    label: {
      master: '主节点',
      worker: '工作节点'
    }
  },
  nodeStatus: {
    label: {
      unregistered: '未注册',
      registered: '已注册',
      online: '在线',
      offline: '离线',
      unknown: '未知',
    },
    tooltip: {
      unregistered: '节点正在等待注册',
      registered: '节点已注册，正在等待在线',
      online: '节点处于在线状态',
      offline: '节点处于离线状态',
      unknown: '未知节点状态',
    }
  },
  nodeRunners: {
    tooltip: {
      unavailable: '目前没有可用执行器',
      running: '总共 {max} 个执行器中的 {running} 个正在运行',
      available: '所有执行器均可用',
    },
  },
};

export default node;
