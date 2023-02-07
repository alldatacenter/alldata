interface LComponentsPlugin {
  form: {
    installType: string;
    autoStart: string;
    name: string;
    installUrl: string;
    installPath: string;
    command: string;
    description: string;
  };
  installType: {
    label: {
      public: string;
      git: string;
      local: string;
    };
    notice: {
      public: string;
      git: string;
      local: string;
    };
  };
  install: {
    title: string;
    repoUrl: string;
    author: string;
    pushedAt: string;
    updatedAt: string;
  };
  settings: {
    title: string;
    label: {
      installSource: string;
      goProxy: string;
    };
    tips: {
      installSource: string;
      goProxy: string;
    };
    goProxy: {
      default: string;
    };
  };
  status: {
    label: {
      installing: string;
      stopped: string;
      running: string;
      error: string;
      unknown: string;
      installed: string;
    };
    tooltip: {
      installing: string;
      stopped: string;
      running: string;
      error: string;
      unknown: string;
      installed: string;
    };
    nodes: {
      tooltip: {
        installing: string;
        stopped: string;
        running: string;
        error: string;
        unknown: string;
      }
    }
  };
}
