const notification: LViewsNotification = {
  navActions: {
    new: {
      label: 'New Notification',
      tooltip: 'Create a new notification',
    },
    filter: {
      search: {
        placeholder: 'Search notifications',
      }
    }
  },
  settings: {
    form: {
      name: 'Name',
      description: 'Description',
      type: 'Type',
      enabled: 'Enabled',
      title: 'Title',
      template: 'Template',
      templateContent: 'Template Content',
      mail: {
        smtp: {
          server: 'SMTP Server',
          port: 'SMTP Port',
          user: 'SMTP User',
          password: 'SMTP Password',
          sender: {
            email: 'Sender Email',
            identity: 'Sender Identity',
          },
        },
        to: 'To',
        cc: 'CC',
      },
      mobile: {
        webhook: 'Webhook',
      },
    },
    type: {
      mail: 'Mail',
      mobile: 'Mobile',
    },
  },
  triggers: {
    models: {
      tags: 'Tags',
      nodes: 'Nodes',
      projects: 'Projects',
      spiders: 'Spiders',
      tasks: 'Tasks',
      jobs: 'Jobs',
      schedules: 'Schedules',
      users: 'Users',
      settings: 'Settings',
      tokens: 'Tokens',
      variables: 'Variables',
      task_stats: 'Task Stats',
      plugins: 'Plugins',
      spider_stats: 'Spider Stats',
      data_sources: 'Data Sources',
      data_collections: 'Data Collections',
      passwords: 'Passwords',
    },
  },
  tabs: {
    overview: 'Overview',
    triggers: 'Triggers',
    template: 'Template',
  },
};

export default notification;
