const nodes: LViewsNodes = {
  table: {
    columns: {
      name: '名称',
      nodeType: '节点类别',
      status: '状态',
      ip: 'IP',
      mac: 'MAC 地址',
      hostname: '主机名',
      runners: '执行器',
      enabled: '是否启用',
      tags: '标签',
      description: '描述',
    }
  },
  navActions: {
    new: {
      label: '新建节点',
      tooltip: '添加一个新节点'
    },
    filter: {
      search: {
        placeholder: '搜索节点'
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        type: {
          label: '节点类别'
        },
        status: {
          label: '状态'
        },
        enabled: {
          label: '是否启用'
        },
      }
    }
  },
  notice: {
    create: {
      title: '创建节点',
      content: '目前只能通过手动部署来创建节点，您可以参考下面的链接查看更多详情。',
      link: {
        label: '安装节点',
        url: 'https://docs-next.crawlab.cn/zh/guide/installation/',
      },
    },
  },
};

export default nodes;
