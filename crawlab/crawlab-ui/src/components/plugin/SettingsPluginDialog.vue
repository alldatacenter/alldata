<template>
  <cl-dialog
    :title="t('components.plugin.settings.title')"
    :visible="activeDialogKey === 'settings'"
    @close="onClose"
    @confirm="onConfirm"
  >
    <cl-form class="settings-form" :model="settings.value">
      <cl-form-item
        :span="2"
        :offset="2"
        :label="t('components.plugin.settings.label.installSource')"
        prop="plugin_base_url"
      >
        <el-select
          v-locate="'install-source'"
          v-model="settings.value.plugin_base_url"
          :placeholder="t('components.plugin.settings.label.installSource')"
        >
          <el-option
            v-for="(op, $index) in baseUrlOptions"
            :key="$index"
            :label="op.label"
            :value="op.value"
          />
        </el-select>
      </cl-form-item>
      <cl-form-item
        :span="4"
        label=" "
      >
        <el-alert class="alert-tip" type="info" :closable="false" show-icon>
          {{ t('components.plugin.settings.tips.installSource') }}
        </el-alert>
      </cl-form-item>
      <cl-form-item
        :span="2"
        :offset="2"
        :label="t('components.plugin.settings.label.goProxy')"
        prop="go_proxy"
      >
        <el-select
          v-locate="'go-proxy'"
          v-model="settings.value.go_proxy"
          :placeholder="t('components.plugin.settings.label.goProxy')"
          clearable
        >
          <el-option
            v-for="(op, $index) in goproxyOptions"
            :key="$index"
            :label="op.label"
            :value="op.value"
          />
        </el-select>
      </cl-form-item>
      <cl-form-item
        :span="4"
        label=" "
      >
        <el-alert class="alert-tip" type="info" :closable="false" show-icon>
          {{ t('components.plugin.settings.tips.goProxy') }}
        </el-alert>
      </cl-form-item>
    </cl-form>
  </cl-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import usePlugin from '@/components/plugin/plugin';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'SettingsPluginDialog',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'plugin';
    const store = useStore();

    const {
      settings,
    } = usePlugin(store);

    const onClose = () => {
      store.commit(`${ns}/hideDialog`, 'settings');

      sendEvent('click_settings_plugin_dialog_close');
    };

    const onConfirm = async () => {
      await store.dispatch(`${ns}/saveSettings`);
      store.commit(`${ns}/hideDialog`, 'settings');

      sendEvent('click_settings_plugin_dialog_confirm', {
        ...settings.value,
      });
    };

    return {
      ...usePlugin(store),
      onClose,
      onConfirm,
      t,
    };
  },
});
</script>

<style scoped>
/*.settings-form >>> .alert-tip {*/
/*  position: absolute;*/
/*  left: 100%;*/
/*  top: 0;*/
/*  margin: 0 0 0 10px;*/
/*  width: calc(150px + 100%);*/
/*  flex: 1 0 auto;*/
/*}*/
</style>
