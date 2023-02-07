export declare global {
  interface EnvDepsSetting extends BaseModel {
    key?: string;
    name?: string;
    description?: string;
    enabled?: string;
    cmd?: string;
    proxy?: string;
    last_update_ts?: string;
  }

  interface EnvDepsTask extends BaseModel {
    status?: string;
    error?: string;
    setting_id?: string;
    type?: string;
    node_id?: string;
    action?: string;
    dep_names?: string[];
    upgrade?: boolean;
    update_ts?: string;
  }

  interface EnvDepsDependency extends BaseModel {
    node_id?: string;
    type?: string;
    name?: string;
    version?: string;
    latest_version?: string;
    description?: string;
    result?: EnvDepsDependencyResult;
  }

  interface EnvDepsDependencyResult {
    name?: string;
    node_ids?: string[];
    versions?: string[];
    latest_version?: string;
    count?: number;
    upgradable?: boolean;
    downgradable?: boolean;
    installable?: boolean;
  }

  interface EnvDepsTask extends BaseModel {
    status?: string;
    error?: string;
    setting_id?: string;
    type?: string;
    node_id?: string;
    action?: string;
    dep_names?: string[];
    upgrade?: boolean;
    update_ts?: string;
  }

  interface EnvDepsLog extends BaseModel {
    task_id?: string;
    content?: string;
    update_ts?: string;
  }

  interface EnvDepsInstallPayload {
    // Names     []string             `json:"names"`
    // Mode      string               `json:"mode"`
    // Upgrade   bool                 `json:"upgrade"`
    // NodeIds   []primitive.ObjectID `json:"node_ids"`
    // UseConfig bool                 `json:"use_config"`
    // SpiderId  primitive.ObjectID   `json:"spider_id"`
    names: string[];
    mode?: string;
    upgrade?: boolean;
    node_ids?: string[];
    use_config?: boolean;
    spider_id?: string;
  }

  interface EnvDepsUninstallPayload {
    mode?: string;
    names?: string[];
    nodes?: CNode[];
    node_ids?: string[];
  }
}
