const notification: LViewsNotification = {
  navActions: {
    new: {
      label: '新建通知',
      tooltip: '创建一个新的通知',
    },
    filter: {
      search: {
        placeholder: '搜索通知',
      }
    }
  },
  settings: {
    form: {
      name: '名称',
      description: '描述',
      type: '类型',
      enabled: '是否启用',
      title: '标题',
      template: '模板',
      templateContent: '模板内容',
      mail: {
        smtp: {
          server: 'SMTP 服务器',
          port: 'SMTP 端口',
          user: 'SMTP 用户',
          password: 'SMTP 密码',
          sender: {
            email: '发件人邮箱',
            identity: '发件人身份',
          },
        },
        to: '收件人',
        cc: '抄送',
      },
      mobile: {
        webhook: 'Webhook',
      },
    },
    type: {
      mail: '邮件',
      mobile: '移动端',
    },
  },
  triggers: {
    models: {
      tags: '标签',
      nodes: '节点',
      projects: '项目',
      spiders: '爬虫',
      tasks: '任务',
      jobs: '作业',
      schedules: '调度',
      users: '用户',
      settings: '设置',
      tokens: '令牌',
      variables: '变量',
      task_stats: '任务统计',
      plugins: '插件',
      spider_stats: '爬虫统计',
      data_sources: '数据源',
      data_collections: '数据集',
      passwords: '密码',
    }
  },
  tabs: {
    overview: '概览',
    triggers: '触发器',
    template: '模板',
  },
};

export default notification;
