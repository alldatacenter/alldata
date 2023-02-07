const nodes: LViewsNodes = {
  table: {
    columns: {
      name: 'Name',
      nodeType: 'Node Type',
      status: 'Status',
      ip: 'IP',
      mac: 'MAC Address',
      hostname: 'Hostname',
      runners: 'Runners',
      enabled: 'Enabled',
      tags: 'Tags',
      description: 'Description',
    }
  },
  navActions: {
    new: {
      label: 'New Node',
      tooltip: 'Create a new node'
    },
    filter: {
      search: {
        placeholder: 'Search nodes'
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        type: {
          label: 'Node Type'
        },
        status: {
          label: 'Status'
        },
        enabled: {
          label: 'Enabled'
        },
      }
    }
  },
  notice: {
    create: {
      title: 'Create Node',
      content: 'Nodes can only be created via manual installation at this moment. You can refer to the link below for more details.',
      link: {
        label: 'Node Installation',
        url: 'https://docs-next.crawlab.cn/en/guide/installation/',
      },
    },
  },
};

export default nodes;
