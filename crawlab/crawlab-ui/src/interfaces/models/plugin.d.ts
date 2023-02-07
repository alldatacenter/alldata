import {
  PLUGIN_DEPLOY_MODE_ALL,
  PLUGIN_DEPLOY_MODE_MASTER,
  PLUGIN_STATUS_ERROR,
  PLUGIN_STATUS_RUNNING,
  PLUGIN_STATUS_STOPPED,
  PLUGIN_INSTALL_TYPE_PUBLIC,
  PLUGIN_INSTALL_TYPE_GIT,
  PLUGIN_INSTALL_TYPE_LOCAL,
} from '@/constants/plugin';

export declare global {
  interface CPlugin extends BaseModel {
    name?: string;
    full_name?: string;
    description?: string;
    type?: string;
    proto?: string;
    active?: boolean;
    endpoint?: string;
    cmd?: string;
    event_key?: {
      include?: string;
      exclude?: string;
    };
    install_type?: PluginInstallType;
    install_url?: string;
    deploy_mode?: PluginDeployMode;
    auto_start?: boolean;
    ui_components?: PluginUIComponent[];
    ui_sidebar_navs?: MenuItem[];
    ui_assets?: PluginUIAsset[];
    status?: PluginStatus[];
  }

  interface PublicPlugin {
    id: number;
    name: string;
    full_name: string;
    description: string;
    html_url: string;
    pushed_at: string;
    created_at: string;
    updated_at: string;
    owner: {
      id: number;
      login: string;
      html_url: string;
    };
  }

  interface PublicPluginInfo {
    repo: PublicPlugin;
    pluginJson: CPlugin;
    readme: string;
  }

  interface PluginUIComponent {
    name?: string;
    title?: string;
    src?: string;
    type?: string;
    path?: string;
    parent_paths?: string[];
    children?: PluginUIComponent[];
  }

  interface PluginUIAsset {
    path?: string;
    type?: string;
  }

  type PluginDeployMode = PLUGIN_DEPLOY_MODE_MASTER | PLUGIN_DEPLOY_MODE_ALL;

  interface PluginStatus extends BaseModel {
    plugin_id?: string;
    node_id?: string;
    node?: CNode;
    status?: PLUGIN_STATUS_STOPPED | PLUGIN_STATUS_RUNNING | PLUGIN_STATUS_ERROR;
    pid?: number;
    error?: string;
  }

  type PluginInstallType = PLUGIN_INSTALL_TYPE_PUBLIC | PLUGIN_INSTALL_TYPE_GIT | PLUGIN_INSTALL_TYPE_LOCAL;
}
