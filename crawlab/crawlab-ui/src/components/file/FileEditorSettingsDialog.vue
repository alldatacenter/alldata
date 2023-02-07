<template>
  <div class="file-editor-settings-dialog">
    <el-dialog
      :model-value="visible"
      :title="t('components.file.editor.settings.title')"
      @close="onClose"
    >
      <el-menu :default-active="activeTabName" class="nav-menu" mode="horizontal" @select="onTabChange">
        <el-menu-item v-for="tab in tabs" :key="tab.name" :index="tab.name">
          {{ tab.title }}
        </el-menu-item>
      </el-menu>
      <el-form
        label-width="var(--cl-file-editor-settings-dialog-label-width)"
        class="form"
      >
        <el-form-item
          v-for="name in optionNames[activeTabName]"
          :key="name"
        >
          <template #label>
            <el-tooltip :content="getDefinitionDescription(name)" popper-class="help-tooltip" trigger="click">
              <font-awesome-icon :icon="['far', 'question-circle']" class="icon" size="sm"/>
            </el-tooltip>
            {{ getDefinitionTitle(name) }}
          </template>
          <cl-file-editor-settings-form-item v-model="options[name]" :name="name"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button plain type="info" @click="onClose">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" @click="onConfirm">{{ t('common.actions.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeMount, readonly, ref} from 'vue';
import {useStore} from 'vuex';
import {plainClone} from '@/utils/object';
import {getOptionDefinition, getThemes} from '@/utils/codemirror';
import {onBeforeRouteLeave} from 'vue-router';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'FileEditorSettingsDialog',
  setup() {
    const {t} = useI18n();

    const storeNamespace = 'file';
    const store = useStore();
    const {file} = store.state as RootStoreState;

    const options = ref<FileEditorConfiguration>({});

    const tabs = readonly([
      {name: 'general', title: t('components.file.editor.settings.tabs.general')},
      {name: 'edit', title: t('components.file.editor.settings.tabs.edit')},
      {name: 'indentation', title: t('components.file.editor.settings.tabs.indentation')},
      {name: 'cursor', title: t('components.file.editor.settings.tabs.cursor')},
    ]);

    const optionNames = readonly({
      general: [
        'theme',
        'keyMap',
        'lineWrapping',
        'lineNumbers',
        'maxHighlightLength',
        'spellcheck',
        'autocorrect',
        'autocapitalize',
      ],
      edit: [
        'lineWiseCopyCut',
        'pasteLinesPerSelection',
        'undoDepth',
      ],
      indentation: [
        'indentUnit',
        'smartIndent',
        'tabSize',
        'indentWithTabs',
        'electricChars',
      ],
      cursor: [
        'showCursorWhenSelecting',
        'cursorBlinkRate',
        'cursorScrollMargin',
        'cursorHeight',
      ],
    });

    const activeTabName = ref<string>(tabs[0].name);

    const visible = computed<boolean>(() => {
      const {editorSettingsDialogVisible} = file;
      return editorSettingsDialogVisible;
    });

    const themes = computed<string[]>(() => {
      return getThemes();
    });

    const resetOptions = () => {
      const {editorOptions} = file;
      options.value = plainClone(editorOptions);
    };

    const onClose = () => {
      store.commit(`${storeNamespace}/setEditorSettingsDialogVisible`, false);
      resetOptions();

      sendEvent('click_file_editor_settings_dialog_close');
    };

    const onConfirm = () => {
      store.commit(`${storeNamespace}/setEditorOptions`, options.value);
      store.commit(`${storeNamespace}/setEditorSettingsDialogVisible`, false);
      resetOptions();

      sendEvent('click_file_editor_settings_dialog_confirm');
    };

    const onTabChange = (tabName: string) => {
      activeTabName.value = tabName;

      sendEvent('click_file_editor_settings_dialog_tab_change', {tabName});
    };

    const getDefinitionDescription = (name: string) => {
      return getOptionDefinition(name)?.description;
    };

    const getDefinitionTitle = (name: string) => {
      return getOptionDefinition(name)?.title;
    };

    onBeforeMount(() => {
      resetOptions();
    });

    onBeforeRouteLeave(() => {
      store.commit(`${storeNamespace}/setEditorSettingsDialogVisible`, false);
    });

    return {
      options,
      activeTabName,
      tabs,
      optionNames,
      visible,
      themes,
      onClose,
      onConfirm,
      onTabChange,
      getDefinitionDescription,
      getDefinitionTitle,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.file-editor-settings-dialog {
  .nav-menu {
    .el-menu-item {
      height: 40px;
      line-height: 40px;
    }
  }

  .form {
    margin: 20px;
  }
}
</style>

<style scoped>
.file-editor-settings-dialog >>> .el-dialog .el-dialog__body {
  padding: 10px 20px;
}

.file-editor-settings-dialog >>> .el-form-item > .el-form-item__label .icon {
  cursor: pointer;
}

.file-editor-settings-dialog >>> .el-form-item > .el-form-item__content {
  width: 240px;
}

.file-editor-settings-dialog >>> .el-form-item > .el-form-item__content .el-input,
.file-editor-settings-dialog >>> .el-form-item > .el-form-item__content .el-select {
  width: 100%;
}
</style>
<style>
.help-tooltip {
  max-width: 240px;
}
</style>
