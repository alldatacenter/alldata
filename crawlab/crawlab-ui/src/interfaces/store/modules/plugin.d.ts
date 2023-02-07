type PluginStoreModule = BaseModule<PluginStoreState, PluginStoreGetters, PluginStoreMutations, PluginStoreActions>;

interface PluginStoreState extends BaseStoreState<CPlugin> {
  settings: Setting;
  publicPlugins: PublicPlugin[];
  activePublicPlugin?: PublicPlugin;
  activePublicPluginInfo?: PublicPluginInfo;
  installType: string;
}

type PluginStoreGetters = BaseStoreGetters<CPlugin>;

interface PluginStoreMutations extends BaseStoreMutations<CPlugin> {
  setSettings: StoreMutation<PluginStoreState, Setting>;
  setSettingsByKey: StoreMutation<PluginStoreState, string, string>;
  setPublicPlugins: StoreMutation<PluginStoreState, PublicPlugin[]>;
  resetPublicPlugins: StoreMutation<PluginStoreState>;
  setActivePublicPlugin: StoreMutation<PluginStoreState, PublicPlugin>;
  resetActivePublicPlugin: StoreMutation<PluginStoreState>;
  setActivePublicPluginInfo: StoreMutation<PluginStoreState, PublicPluginInfo>;
  resetActivePublicPluginInfo: StoreMutation<PluginStoreState>;
  setInstallType: StoreMutation<PluginStoreState, string>;
  resetInstallType: StoreMutation<PluginStoreState>;
}

interface PluginStoreActions extends BaseStoreActions<CPlugin> {
  getSettings: StoreAction<PluginStoreState>;
  saveSettings: StoreAction<PluginStoreState>;
  getPublicPluginList: StoreAction<PluginStoreState>;
  getPublicPluginInfo: StoreAction<PluginStoreState, string>;
}
