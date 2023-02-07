interface LComponentsNode {
  form: {
    key: string;
    name: string;
    tags: string;
    type: string;
    ip: string;
    mac: string;
    hostname: string;
    enabled: string;
    max_runners: string;
    description: string;
  };
  nodeType: {
    label: {
      master: string;
      worker: string;
    };
  };
  nodeStatus: {
    label: {
      unregistered: string;
      registered: string;
      online: string;
      offline: string;
      unknown: string;
    };
    tooltip: {
      unregistered: string;
      registered: string;
      online: string;
      offline: string;
      unknown: string;
    };
  };
  nodeRunners: {
    tooltip: {
      unavailable: string;
      running: string;
      available: string;
    };
  };
}
