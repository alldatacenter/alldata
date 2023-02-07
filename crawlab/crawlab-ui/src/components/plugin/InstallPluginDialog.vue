<template>
  <cl-dialog
    :title="t('components.plugin.install.title')"
    :visible="activeDialogKey === 'install'"
    width="1200px"
    @close="onClose"
    @confirm="onConfirm"
  >
    <div class="top">
      <el-radio-group
        v-model="internalInstallType"
        class="install-type-select"
        type="button"
        @change="onInstallTypeChange"
      >
        <el-radio-button :label="PLUGIN_INSTALL_TYPE_PUBLIC">
          {{ t('components.plugin.installType.label.public') }}
        </el-radio-button>
        <el-radio-button :label="PLUGIN_INSTALL_TYPE_GIT">
          {{ t('components.plugin.installType.label.git') }}
        </el-radio-button>
        <el-radio-button :label="PLUGIN_INSTALL_TYPE_LOCAL">
          {{ t('components.plugin.installType.label.local') }}
        </el-radio-button>
      </el-radio-group>
      <el-alert show-icon type="info" class="notice" :closable="false">
        {{ noticeContent }}
      </el-alert>
    </div>
    <cl-install-public-plugin
      v-if="installType === PLUGIN_INSTALL_TYPE_PUBLIC"
    />
    <cl-plugin-form
      v-else
    />
  </cl-dialog>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useStore} from 'vuex';
import usePlugin from '@/components/plugin/plugin';
import {
  PLUGIN_INSTALL_TYPE_PUBLIC,
  PLUGIN_INSTALL_TYPE_GIT,
  PLUGIN_INSTALL_TYPE_LOCAL,
} from '@/constants/plugin';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'InstallPluginDialog',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'plugin';
    const store = useStore();
    const {
      plugin: pluginState,
    } = store.state as RootStoreState;

    const {
      installType,
    } = usePlugin(store);

    const onClose = () => {
      store.commit(`${ns}/hideDialog`, 'install');

      sendEvent('click_install_plugin_dialog_close');
    };

    const onConfirm = async () => {
      // skip public
      if (installType.value === PLUGIN_INSTALL_TYPE_PUBLIC) {
        store.commit(`${ns}/hideDialog`, 'install');
        return;
      }

      await store.dispatch(`${ns}/create`, {
        install_url: pluginState.form.install_url,
        install_type: PLUGIN_INSTALL_TYPE_LOCAL,
      });
      store.commit(`${ns}/hideDialog`, 'install');

      sendEvent('click_install_plugin_dialog_confirm', {
        installType: installType.value,
      });

      await store.dispatch(`${ns}/getList`);
    };

    const internalInstallType = ref<string>(PLUGIN_INSTALL_TYPE_PUBLIC);

    const onInstallTypeChange = (value: string) => {
      store.commit(`${ns}/setInstallType`, value);

      sendEvent('click_install_plugin_dialog_install_type_change', {
        installType: value,
      });
    };

    onBeforeUnmount(() => {
      store.commit(`${ns}/resetInstallType`);
    });

    const noticeContent = computed<string>(() => {
      switch (installType.value) {
        case PLUGIN_INSTALL_TYPE_PUBLIC:
          return t('components.plugin.installType.notice.public');
        case PLUGIN_INSTALL_TYPE_GIT:
          return t('components.plugin.installType.notice.git');
        case PLUGIN_INSTALL_TYPE_LOCAL:
          return t('components.plugin.installType.notice.local');
        default:
          return '';
      }
    });

    return {
      ...usePlugin(store),
      PLUGIN_INSTALL_TYPE_PUBLIC,
      PLUGIN_INSTALL_TYPE_GIT,
      PLUGIN_INSTALL_TYPE_LOCAL,
      onClose,
      onConfirm,
      internalInstallType,
      onInstallTypeChange,
      noticeContent,
      t,
    };
  },
});
</script>

<style scoped lang="scss">
.top {
  margin-bottom: 20px;
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;

  .install-type-select {
    width: 200px;
    flex: 0 0 200px;
  }

  .notice {
    width: calc(100% - 200px - 20px);
    flex: 1 0 auto;
    margin-left: 20px;
  }
}
</style>
