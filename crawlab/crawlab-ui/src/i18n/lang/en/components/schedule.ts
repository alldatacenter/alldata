const schedule: LComponentsSchedule = {
  form: {
    name: 'Name',
    spider: 'Spider',
    cron: 'Cron Expression',
    cronInfo: 'Cron Info',
    command: 'Command',
    param: 'Param',
    defaultMode: 'Default Mode',
    enabled: 'Enabled',
    selectedTags: 'Selected Tags',
    selectedNodes: 'Selected Nodes',
    description: 'Description',
  },
  rules: {
    message: {
      invalidCronExpression: 'Invalid cron expression. [min] [hour] [day of month] [month] [day of week]',
    }
  },
  message: {
    success: {
      enable: 'Enabled successfully',
      disable: 'Disabled successfully',
    }
  },
  cron: {
    title: {
      cronDescription: 'Cron Description',
      nextRun: 'Next Run',
      cron: 'Cron Expression',
      description: 'Description',
      next: 'Next',
    },
  }
};

export default schedule;
