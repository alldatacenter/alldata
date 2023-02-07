interface LViewsEnv {
  deps: {
    settings: {
      form: {
        key: string;
        name: string;
        description: string;
        enabled: string;
        cmd: string;
        proxy: string;
      };
      lang: {
        python: {
          description: string;
        };
        node: {
          description: string;
        };
      };
    };
    dependency: {
      form: {
        name: string;
        latestVersion: string;
        installedVersion: string;
        installedNodes: string;
        allNodes: string;
        selectedNodes: string;
        upgrade: string;
        mode: string;
      };
    };
    task: {
      tasks: string;
      form: {
        action: string;
        node: string;
        status: string;
        dependencies: string;
        time: string;
        logs: string;
      };
    };
    spider: {
      form: {
        name: string;
        dependencyType: string;
        requiredVersion: string;
        installedVersion: string;
        installedNodes: string;
      };
    };
    common: {
      status: {
        installed: string;
        installable: string;
        upgradable: string;
        downgradable: string;
        noDependencyType: string;
      };
      actions: {
        installAndUpgrade: string;
        installAndDowngrade: string;
        searchDependencies: string;
      };
    };
  };
}
