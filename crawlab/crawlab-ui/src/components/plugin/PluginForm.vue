<template>
  <cl-form
    v-if="form"
    ref="formRef"
    :model="form"
    :selective="isSelectiveForm"
  >
    <template v-if="isDialog">
      <!--Row-->
      <cl-form-item
        :span="2"
        :label="t('components.plugin.form.autoStart')"
      >
        <cl-switch v-model="form.auto_start"/>
      </cl-form-item>
      <!--./Row-->

      <!--Row-->
      <template v-if="installType === PLUGIN_INSTALL_TYPE_GIT">
        <cl-form-item
          :span="4"
          :label="t('components.plugin.form.installUrl')"
          prop="install_url"
          required
        >
          <el-input
            v-model="form.install_url"
            :placeholder="t('components.plugin.form.installUrl')"
          />
        </cl-form-item>
      </template>
      <template v-else-if="installType === PLUGIN_INSTALL_TYPE_LOCAL">
        <cl-form-item
          :span="4"
          :label="t('components.plugin.form.installPath')"
          prop="install_url"
          required
        >
          <el-input
            v-model="form.install_url"
            :placeholder="t('components.plugin.form.installPath')"
          />
        </cl-form-item>
      </template>
      <!--./Row-->
    </template>

    <template v-else>
      <!--Row-->
      <cl-form-item
        :span="2"
        :offset="2"
        :label="t('components.plugin.form.name')"
        not-editable
        prop="name"
        required
      >
        <el-input
          v-model="form.name"
          disabled
          :placeholder="t('components.plugin.form.name')"
        />
      </cl-form-item>
      <!--./Row-->

      <!--Row-->
      <cl-form-item
        :span="2"
        :label="t('components.plugin.form.command')"
        prop="cmd"
      >
        <el-input
          v-model="form.cmd"
          disabled
          :placeholder="t('components.plugin.form.command')"
        />
      </cl-form-item>
      <!--./Row-->

      <!--Row-->
      <cl-form-item
        :span="4"
        :label="t('components.plugin.form.description')"
        prop="description"
      >
        <el-input
          v-model="form.description"
          disabled
          :placeholder="t('components.plugin.form.description')"
          type="textarea"
        />
      </cl-form-item>
    </template>

  </cl-form>
  <!--./Row-->
</template>

<script lang="ts">
import {computed, defineComponent, ref} from 'vue';
import {useStore} from 'vuex';
import usePlugin from '@/components/plugin/plugin';
import {
  PLUGIN_INSTALL_TYPE_PUBLIC,
  PLUGIN_INSTALL_TYPE_GIT,
  PLUGIN_INSTALL_TYPE_LOCAL,
} from '@/constants/plugin';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'PluginForm',
  props: {
    readonly: {
      type: Boolean,
    }
  },
  setup(props, {emit}) {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'plugin';
    const store = useStore();
    const {plugin: state} = store.state as RootStoreState;

    const isDialog = computed<boolean>(() => !!state.activeDialogKey);

    const installFromUrl = ref<boolean>(false);

    return {
      ...usePlugin(store),
      isDialog,
      installFromUrl,
      PLUGIN_INSTALL_TYPE_PUBLIC,
      PLUGIN_INSTALL_TYPE_GIT,
      PLUGIN_INSTALL_TYPE_LOCAL,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
