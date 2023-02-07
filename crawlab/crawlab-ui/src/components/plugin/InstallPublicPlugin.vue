<template>
  <div class="container">
    <div v-loading="loadingPublicPlugins" class="sidebar">
      <cl-public-plugin-item
        v-for="(p, $index) in publicPlugins"
        :key="$index"
        :plugin="p"
        :active="p?.full_name === activePublicPlugin?.full_name"
        clickable
        :status="getPluginStatus(p)"
        :installed="isInstalled(p)"
        @click="onClickPlugin(p)"
        @install="onInstallPlugin(p)"
      />
    </div>
    <div v-loading="loadingActivePublicPluginInfo" class="content">
      <cl-public-plugin-content
        v-if="!!activePublicPlugin && !!activePublicPluginInfo"
        :plugin="activePublicPlugin"
        :info="activePublicPluginInfo"
        :status="getPluginStatus(activePublicPlugin)"
        :installed="isInstalled(activePublicPlugin)"
        @install="onInstallPlugin(activePublicPlugin)"
      />
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent, onBeforeMount, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useStore} from 'vuex';
import usePlugin from '@/components/plugin/plugin';
import {ElMessageBox} from 'element-plus';
import {
  PLUGIN_INSTALL_TYPE_PUBLIC, PLUGIN_STATUS_ERROR,
  PLUGIN_STATUS_INSTALLING,
  PLUGIN_STATUS_RUNNING,
  PLUGIN_STATUS_STOPPED
} from '@/constants/plugin';

export default defineComponent({
  name: 'InstallPublicPlugin',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'plugin';
    const store = useStore();

    const {
      allPluginDictByFullName,
    } = usePlugin(store);

    const loadingPublicPlugins = ref(false);
    const loadingActivePublicPluginInfo = ref(false);

    const isClickingInstall = ref(false);
    const onClickPlugin = async (p: PublicPlugin) => {
      if (isClickingInstall.value) return;

      store.commit(`${ns}/setActivePublicPlugin`, p);

      loadingActivePublicPluginInfo.value = true;
      try {
        await store.dispatch(`${ns}/getPublicPluginInfo`, p?.full_name);
      } finally {
        loadingActivePublicPluginInfo.value = false;
      }
    };

    const onInstallPlugin = async (p: PublicPlugin) => {
      isClickingInstall.value = true;
      setTimeout(() => isClickingInstall.value = false, 100);

      await ElMessageBox.confirm(
        t('common.messageBox.confirm.install'),
        t('common.actions.install'),
        {
          type: 'warning',
          cancelButtonClass: 'install-plugin-cancel-btn',
          confirmButtonClass: 'install-plugin-confirm-btn',
        }
      );
      await store.dispatch(`${ns}/create`, {
        name: p.name,
        short_name: p.name,
        full_name: p.full_name,
        install_type: PLUGIN_INSTALL_TYPE_PUBLIC,
      });
      await Promise.all([
        store.dispatch(`${ns}/getList`),
        store.dispatch(`${ns}/getAllList`),
      ]);
    };

    const getPluginStatus = (p: PublicPlugin): string => {
      return allPluginDictByFullName.value[p.full_name]?.status?.[0]?.status;
    };

    const isInstalling = (p: PublicPlugin): boolean => {
      return getPluginStatus(p) === PLUGIN_STATUS_INSTALLING;
    };

    const isInstalled = (p: PublicPlugin): boolean => {
      if (!getPluginStatus(p)) return false;
      return [
        PLUGIN_STATUS_STOPPED,
        PLUGIN_STATUS_RUNNING,
        PLUGIN_STATUS_ERROR,
      ].includes(getPluginStatus(p));
    };

    let handle: any;
    onBeforeMount(async () => {
      loadingPublicPlugins.value = true;
      try {
        await store.dispatch(`${ns}/getPublicPluginList`);
      } finally {
        loadingPublicPlugins.value = false;
      }

      handle = setInterval(() => {
        store.dispatch(`${ns}/getAllList`);
      }, 5000);
    });

    onBeforeUnmount(() => {
      store.commit(`${ns}/resetActivePublicPlugin`);

      clearInterval(handle);
    });

    return {
      ...usePlugin(store),
      onClickPlugin,
      onInstallPlugin,
      loadingPublicPlugins,
      loadingActivePublicPluginInfo,
      isInstalling,
      isInstalled,
      getPluginStatus,
      t,
    };
  },
});
</script>

<style scoped lang="scss">
.container {
  display: flex;
  border: 1px solid var(--cl-info-medium-light-color);
  height: calc(100vh - 400px);
  min-height: 360px;

  .sidebar {
    flex: 0 0 450px;
    border-right: 1px solid var(--cl-info-medium-light-color);
    overflow-y: auto;
  }

  .content {
    flex: 1 0 auto;
    height: 100%;
    width: calc(100% - 450px);
    overflow-y: auto;
  }
}
</style>
