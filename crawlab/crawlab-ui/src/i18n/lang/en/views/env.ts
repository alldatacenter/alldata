const env: LViewsEnv = {
  deps: {
    settings: {
      form: {
        key: 'Key',
        name: 'Name',
        description: 'Description',
        enabled: 'Enabled',
        cmd: 'Command',
        proxy: 'Proxy',
      },
      lang: {
        python: {
          description: 'Python dependencies',
        },
        node: {
          description: 'Node.js dependencies',
        }
      },
    },
    dependency: {
      form: {
        name: 'Name',
        latestVersion: 'Latest version',
        installedVersion: 'Installed version',
        installedNodes: 'Installed nodes',
        allNodes: 'All nodes',
        selectedNodes: 'Selected nodes',
        upgrade: 'Upgrade',
        mode: 'Mode',
      },
    },
    task: {
      tasks: 'Tasks',
      form: {
        action: 'Action',
        node: 'Node',
        status: 'Status',
        dependencies: 'Dependencies',
        time: 'Time',
        logs: 'Logs',
      },
    },
    spider: {
      form: {
        name: 'Name',
        dependencyType: 'Dependency type',
        requiredVersion: 'Required version',
        installedVersion: 'Installed version',
        installedNodes: 'Installed nodes',
      },
    },
    common: {
      status: {
        installed: 'Installed',
        installable: 'Installable',
        upgradable: 'Upgradable',
        downgradable: 'Downgradable',
        noDependencyType: 'No dependency type',
      },
      actions: {
        installAndUpgrade: 'Install and upgrade',
        installAndDowngrade: 'Install and downgrade',
        searchDependencies: 'Search dependencies',
      },
    }
  },
};

export default env;
