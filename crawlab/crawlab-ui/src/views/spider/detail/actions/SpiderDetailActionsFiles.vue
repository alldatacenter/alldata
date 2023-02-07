<template>
  <cl-nav-action-group>
    <cl-nav-action-fa-icon
      :icon="['fa', 'laptop-code']"
      :tooltip="t('components.spider.actions.files.tooltip.fileEditorActions')"
    />
    <cl-nav-action-item>
      <cl-fa-icon-button
        :icon="['fa', 'upload']"
        :tooltip="t('components.spider.actions.files.tooltip.uploadFiles')"
        type="primary"
        id="upload-btn"
        class-name="upload-btn"
        @click="onClickUpload"
      />
      <cl-fa-icon-button
        :icon="['fa', 'cog']"
        :tooltip="t('components.spider.actions.files.tooltip.fileEditorSettings')"
        type="info"
        id="open-settings-btn"
        class-name="open-settings-btn"
        @click="onOpenFilesSettings"
      />
    </cl-nav-action-item>
  </cl-nav-action-group>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'SpiderDetailActionsFiles',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const store = useStore();

    const onClickUpload = () => {
      store.commit(`spider/showDialog`, 'uploadFiles');

      sendEvent('click_spider_detail_actions_upload');
    };

    const onOpenFilesSettings = () => {
      store.commit(`file/setEditorSettingsDialogVisible`, true);

      sendEvent('click_spider_detail_actions_files_settings');
    };

    return {
      onClickUpload,
      onOpenFilesSettings,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
