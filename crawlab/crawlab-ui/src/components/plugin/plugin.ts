import {computed, readonly} from 'vue';
import {Store} from 'vuex';
import useForm from '@/components/form/form';
import usePluginService from '@/services/plugin/pluginService';
import {getDefaultFormComponentData} from '@/utils/form';
import {getPluginBaseUrlOptions, getPluginGoproxyOptions} from '@/utils/plugin';

type Plugin = CPlugin;

// form component data
const formComponentData = getDefaultFormComponentData<Plugin>();

const usePlugin = (store: Store<RootStoreState>) => {
  // store
  const ns = 'plugin';
  const {
    plugin: state
  } = store.state as RootStoreState;

  // form rules
  const formRules = readonly<FormRules>({});

  // base url options
  const baseUrlOptions = getPluginBaseUrlOptions();

  // goproxy options
  const goproxyOptions = getPluginGoproxyOptions();

  // settings
  const settings = computed<{ [key: string]: string }>(() => state.settings);

  // public plugins
  const publicPlugins = computed<PublicPlugin[]>(() => state.publicPlugins);

  // active public plugin
  const activePublicPlugin = computed<PublicPlugin | undefined>(() => state.activePublicPlugin);

  // active public plugin info
  const activePublicPluginInfo = computed<PublicPluginInfo | undefined>(() => state.activePublicPluginInfo);

  // all plugin dict by full name
  const allPluginDictByFullName = computed<{ [key: string]: CPlugin }>(() => {
    const dict = {} as { [key: string]: CPlugin };
    state.allList.forEach(p => {
      if (!p.full_name) return;
      dict[p.full_name] = p;
    });
    return dict;
  });

  // install type
  const installType = computed<string>(() => state.installType);

  return {
    ...useForm(ns, store, usePluginService(store), formComponentData),
    formRules,
    baseUrlOptions,
    settings,
    publicPlugins,
    activePublicPlugin,
    activePublicPluginInfo,
    allPluginDictByFullName,
    installType,
    goproxyOptions,
  };
};

export default usePlugin;
