const plugin: LComponentsPlugin = {
  form: {
    installType: 'Install Type',
    autoStart: 'Auto Start',
    name: 'Name',
    installUrl: 'Install URL',
    installPath: 'Install Path',
    command: 'Command',
    description: 'Description',
  },
  installType: {
    label: {
      public: 'Public',
      git: 'Git',
      local: 'Local'
    },
    notice: {
      public: 'Install official public plugins from Crawlab Team',
      git: 'Install 3rd-party plugins on Git url',
      local: 'Install from local source',
    },
  },
  install: {
    title: 'Install Plugin',
    repoUrl: 'Repo URL',
    author: 'Author',
    pushedAt: 'Pushed At',
    updatedAt: 'Updated At',
  },
  settings: {
    title: 'Settings',
    label: {
      installSource: 'Install Source',
      goProxy: 'Go Proxy',
    },
    tips: {
      installSource: 'You can select Install Source as "Gitee" to speed up plugin installation if you are in Mainland China.',
      goProxy: 'You can set Go Proxy to speed up plugin compilation if you are in Mainland China.',
    },
    goProxy: {
      default: 'Default',
    }
  },
  status: {
    label: {
      installing: 'Installing',
      stopped: 'Stopped',
      running: 'Running',
      error: 'Error',
      unknown: 'Unknown',
      installed: 'Installed',
    },
    tooltip: {
      installing: 'Installing plugin',
      stopped: 'Plugin is stopped',
      running: 'Plugin is currently running',
      error: 'Plugin stopped with an error',
      unknown: 'Unknown plugin status',
      installed: 'Plugin is already installed',
    },
    nodes: {
      tooltip: {
        installing: 'Installing Nodes',
        stopped: 'Stopped Nodes',
        running: 'Running Nodes',
        error: 'Error Nodes',
        unknown: 'Unknown Nodes',
      }
    }
  },
};

export default plugin;
