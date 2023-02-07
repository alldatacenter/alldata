<template>
  <div ref="fileEditor" class="file-editor">
    <div :class="navMenuCollapsed ? 'collapsed' : ''" class="nav-menu">
      <div
        :style="{
            backgroundColor: style.backgroundColorGutters,
            color: style.color,
          }"
        class="nav-menu-top-bar"
      >
        <div class="left">
          <el-input
            v-model="fileSearchString"
            :style="{
                color: style.color,
              }"
            class="search"
            clearable
            :placeholder="t('components.file.editor.sidebar.search.placeholder')"
            @change="onFileSearch"
          >
            <template #prefix>
              <el-icon class="el-input__icon">
                <font-awesome-icon :icon="['fa', 'search']"/>
              </el-icon>
            </template>
          </el-input>
        </div>
        <div class="right">
          <el-tooltip v-if="false" :content="t('components.file.editor.sidebar.settings')">
            <span class="action-icon" @click="showSettings = true">
              <div class="background"/>
              <font-awesome-icon :icon="['fa', 'cog']"/>
            </span>
          </el-tooltip>
          <el-tooltip :content="t('components.file.editor.sidebar.toggle.hideFiles')">
            <span class="action-icon" @click="onToggleNavMenu">
              <div class="background"/>
              <font-awesome-icon :icon="['fa', 'minus']"/>
            </span>
          </el-tooltip>
        </div>
      </div>
      <cl-file-editor-nav-menu
        :active-item="activeFileItem"
        :default-expand-all="!!fileSearchString"
        :default-expanded-keys="defaultExpandedKeys"
        :items="files"
        :style="style"
        @node-click="onNavItemClick"
        @node-db-click="onNavItemDbClick"
        @node-drop="onNavItemDrop"
        @ctx-menu-new-file="onContextMenuNewFile"
        @ctx-menu-new-directory="onContextMenuNewDirectory"
        @ctx-menu-rename="onContextMenuRename"
        @ctx-menu-clone="onContextMenuClone"
        @ctx-menu-delete="onContextMenuDelete"
        @drop-files="onDropFiles"
      />
    </div>
    <div class="file-editor-content">
      <cl-file-editor-nav-tabs
        ref="navTabs"
        :active-tab="activeFileItem"
        :tabs="tabs"
        :style="style"
        @tab-click="onTabClick"
        @tab-close="onTabClose"
        @tab-close-others="onTabCloseOthers"
        @tab-close-all="onTabCloseAll"
        @tab-dragend="onTabDragEnd"
      >
        <template v-if="navMenuCollapsed" #prefix>
          <el-tooltip :content="t('components.file.editor.sidebar.toggle.showFiles')">
            <span class="action-icon expand-files" @click="onToggleNavMenu">
              <div class="background"/>
              <font-awesome-icon :icon="['fa', 'bars']"/>
            </span>
          </el-tooltip>
        </template>
      </cl-file-editor-nav-tabs>
      <div
        ref="codeMirrorEditor"
        :class="showCodeMirrorEditor ? '' : 'hidden'"
        :style="{
            scrollbar: style.backgroundColorGutters,
          }"
        class="code-mirror-editor"
      />
      <div
        v-show="!showCodeMirrorEditor"
        :style="{
            backgroundColor: style.backgroundColor,
            color: style.color,
          }"
        class="empty-content"
      >
        {{ t('components.file.editor.empty.placeholder') }}
      </div>
      <template v-if="navTabs && navTabs.showMoreVisible">
        <cl-file-editor-nav-tabs-show-more-context-menu
          :tabs="tabs"
          :visible="showMoreContextMenuVisible"
          @hide="onShowMoreHide"
          @tab-click="onClickShowMoreContextMenuItem"
        >
          <div
            :style="{
                background: style.backgroundColor,
                color: style.color,
              }"
            class="nav-tabs-suffix"
          >
            <el-tooltip :content="t('components.file.editor.sidebar.showMore')">
              <span class="action-icon" @click.prevent="onShowMoreShow">
                <div class="background"/>
                <font-awesome-icon :icon="['fa', 'angle-down']"/>
              </span>
            </el-tooltip>
          </div>
        </cl-file-editor-nav-tabs-show-more-context-menu>
      </template>
    </div>
  </div>
  <div ref="codeMirrorTemplate" class="code-mirror-template"/>
  <div ref="styleRef" v-html="extraStyle"/>
  <cl-file-editor-settings-dialog/>
</template>

<script lang="ts">
import {computed, defineComponent, onMounted, onUnmounted, PropType, ref, watch} from 'vue';
import {Editor, EditorConfiguration, KeyMap} from 'codemirror';
import {MimeType} from 'codemirror/mode/meta';
import {useStore} from 'vuex';
import {getCodemirrorEditor, getCodeMirrorTemplate, initTheme} from '@/utils/codemirror';
import {FILE_ROOT} from '@/constants/file';

// codemirror mode
import 'codemirror/mode/meta';

// codemirror utils
import '@/utils/codemirror';

// components
import FileEditorNavMenu from '@/components/file/FileEditorNavMenu.vue';
import FileEditorNavTabs from '@/components/file/FileEditorNavTabs.vue';
import FileEditorSettingsDialog from '@/components/file/FileEditorSettingsDialog.vue';
import FileEditorNavTabsShowMoreContextMenu from '@/components/file/FileEditorNavTabsShowMoreContextMenu.vue';
import {emptyArrayFunc} from '@/utils/func';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

// codemirror mode import cache
const codeMirrorModeCache = new Set<string>();

// codemirror tab content cache
const codeMirrorTabContentCache = new Map<string, string>();

export default defineComponent({
  name: 'FileEditor',
  props: {
    content: {
      type: String,
      required: true,
      default: '',
    },
    activeNavItem: {
      type: Object as PropType<FileNavItem>,
      required: false,
    },
    navItems: {
      type: Array as PropType<FileNavItem[]>,
      required: true,
      default: emptyArrayFunc,
    },
    defaultExpandedKeys: {
      type: Array as PropType<string[]>,
      required: false,
      default: emptyArrayFunc,
    },
  },
  emits: [
    'content-change',
    'tab-click',
    'node-click',
    'node-db-click',
    'node-drop',
    'save-file',
    'ctx-menu-new-file',
    'ctx-menu-new-directory',
    'ctx-menu-rename',
    'ctx-menu-clone',
    'ctx-menu-delete',
    'drop-files',
  ],
  setup(props: FileEditorProps, {emit}) {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'spider';
    const store = useStore();
    const {file} = store.state as RootStoreState;

    const fileEditor = ref<HTMLDivElement>();

    const codeMirrorEditor = ref<HTMLDivElement>();

    const tabs = ref<FileNavItem[]>([]);

    const activeFileItem = computed<FileNavItem | undefined>(() => props.activeNavItem);

    const style = ref<FileEditorStyle>({});

    const fileSearchString = ref<string>('');

    const navMenuCollapsed = ref<boolean>(false);

    const showSettings = ref<boolean>(false);

    const styleRef = ref<HTMLDivElement>();

    let editor: Editor | null = null;

    let codeMirrorTemplateEditor: Editor | null = null;

    const codeMirrorTemplate = ref<HTMLDivElement>();

    let codeMirrorEditorSearchLabel: HTMLSpanElement | undefined;

    let codeMirrorEditorSearchInput: HTMLInputElement | undefined;

    const navTabs = ref<typeof FileEditorNavTabs>();

    const showMoreContextMenuVisible = ref<boolean>(false);

    const showCodeMirrorEditor = computed<boolean>(() => {
      return !!activeFileItem.value;
    });

    const language = computed<MimeType | undefined>(() => {
      const fileName = activeFileItem.value?.name;
      if (!fileName) return;
      return window.CodeMirror?.findModeByFileName?.(fileName);
    });

    const languageMime = computed<string | undefined>(() => language.value?.mime);

    const options = computed<FileEditorConfiguration>(() => {
      const {editorOptions} = file as FileStoreState;
      return {
        ...editorOptions,
        mode: languageMime.value || 'text',
      };
    });

    const content = computed<string>(() => {
      const {content} = props as FileEditorProps;
      return content || '';
    });

    const extraStyle = computed<string>(() => {
      return `<style>
.file-editor .file-editor-nav-menu::-webkit-scrollbar {
  background-color: ${style.value.backgroundColor};
  width: 8px;
  height: 8px;
}
.file-editor .file-editor-nav-menu::-webkit-scrollbar-thumb {
  background-color: var(--cl-primary-color);
  border-radius: 4px;
}
.file-editor .file-editor-content .code-mirror-editor .CodeMirror-vscrollbar::-webkit-scrollbar {
  background-color: ${style.value.backgroundColor};
  width: 8px;
}
.file-editor .file-editor-content .code-mirror-editor .CodeMirror-hscrollbar::-webkit-scrollbar {
  background-color: ${style.value.backgroundColor};
  height: 8px;
}
.file-editor .file-editor-content .code-mirror-editor .CodeMirror-vscrollbar::-webkit-scrollbar-thumb,
.file-editor .file-editor-content .code-mirror-editor .CodeMirror-hscrollbar::-webkit-scrollbar-thumb {
  background-color: var(--cl-primary-color);
  border-radius: 4px;
}
.file-editor .file-editor-nav-tabs::-webkit-scrollbar {
  display: none;
}
</style>`;
    });

    const codeMirrorTemplateContent = computed<string>(() => {
      return getCodeMirrorTemplate();
    });

    const updateEditorOptions = () => {
      for (const k in options.value) {
        const key = k as keyof EditorConfiguration;
        const value = options.value[key];
        editor?.setOption(key, value);
      }
    };

    const updateEditorContent = () => {
      editor?.setValue(content.value || '');
    };

    const updateStyle = () => {
      // codemirror style: background color / color / height
      const el = codeMirrorEditor.value as HTMLElement;
      const cm = el.querySelector('.CodeMirror');
      if (!cm) return;
      const computedStyle = window.getComputedStyle(cm);
      style.value = {
        backgroundColor: computedStyle.backgroundColor,
        color: computedStyle.color,
        height: computedStyle.height,
      };

      // gutter
      const cmGutters = el.querySelector('.CodeMirror-gutters');
      if (!cmGutters) return;
      const computedStyleGutters = window.getComputedStyle(cmGutters);
      style.value.backgroundColorGutters = computedStyleGutters.backgroundColor;
    };

    const updateTheme = async () => {
      await initTheme(options.value.theme);
    };

    const updateMode = async () => {
      const mode = language.value?.mode;
      if (!mode || codeMirrorModeCache.has(mode)) return;
      // @ts-ignore
      await import(`codemirror/mode/${mode}/${mode}.js`);
      codeMirrorModeCache.add(mode);
    };

    const updateSearchInput = () => {
      codeMirrorEditorSearchLabel = codeMirrorEditor.value?.querySelector<HTMLSpanElement>('.CodeMirror-search-label') || undefined;
      codeMirrorEditorSearchInput = codeMirrorEditor.value?.querySelector<HTMLInputElement>('.CodeMirror-search-field') || undefined;
      if (!codeMirrorEditorSearchInput) return;
      codeMirrorEditorSearchInput.onblur = () => {
        if (codeMirrorEditorSearchLabel?.textContent?.includes('Search')) {
          setTimeout(() => {
            codeMirrorEditorSearchInput?.parentElement?.remove();
            editor?.focus();
          }, 10);
        }
      };
    };

    const getContentCache = (tab: FileNavItem) => {
      if (!tab.path) return;
      const key = tab.path;
      const content = codeMirrorTabContentCache.get(key);
      emit('content-change', content as string);
      setTimeout(updateEditorContent, 0);
    };

    const updateContentCache = (tab: FileNavItem, content: string) => {
      if (!tab.path) return;
      const key = tab.path as string;
      codeMirrorTabContentCache.set(key, content as string);
    };

    const deleteContentCache = (tab: FileNavItem) => {
      if (!tab.path) return;
      const key = tab.path;
      codeMirrorTabContentCache.delete(key);
    };

    const deleteOtherContentCache = (tab: FileNavItem) => {
      if (!tab.path) return;
      const key = tab.path;
      const content = codeMirrorTabContentCache.get(key);
      codeMirrorTabContentCache.clear();
      codeMirrorTabContentCache.set(key, content as string);
    };

    const clearContentCache = () => {
      codeMirrorTabContentCache.clear();
    };

    const getFilteredFiles = (items: FileNavItem[]): FileNavItem[] => {
      return items
        .filter(d => {
          if (!d.is_dir) {
            return d.name?.toLowerCase().includes(fileSearchString.value.toLowerCase());
          }
          if (d.children) {
            const children = getFilteredFiles(d.children);
            if (children.length > 0) {
              return true;
            }
          }
          return false;
        })
        .map(d => {
          if (!d.is_dir) return d;
          d.children = getFilteredFiles(d.children || []);
          return d;
        });
    };

    const files = computed<FileNavItem[]>(() => {
      const {navItems} = props as FileEditorProps;
      const root: FileNavItem = {
        path: FILE_ROOT,
        name: FILE_ROOT,
        is_dir: true,
        children: fileSearchString.value ? getFilteredFiles(navItems) : navItems,
      };
      return [root];
    });

    const updateTabs = (item?: FileNavItem) => {
      // add tab
      if (item && !tabs.value.find(t => t.path === item.path)) {
        if (tabs.value.length === 0) {
          store.commit(`${ns}/setActiveFileNavItem`, item);
          editor?.focus();
        }
        tabs.value.push(item);
        getContentCache(item);
      }
    };

    const onNavItemClick = (item: FileNavItem) => {
      emit('node-click', item);

      sendEvent('click_file_editor_nav_menu_item_click');
    };

    const onNavItemDbClick = (item: FileNavItem) => {
      store.commit(`${ns}/setActiveFileNavItem`, item);
      emit('node-db-click', item);

      // update tabs
      updateTabs(item);

      sendEvent('click_file_editor_nav_menu_item_dbclick');
    };

    const onNavItemDrop = (draggingItem: FileNavItem, dropItem: FileNavItem) => {
      emit('node-drop', draggingItem, dropItem);

      sendEvent('click_file_editor_nav_menu_item_drop');
    };

    const onContextMenuNewFile = (item: FileNavItem, name: string) => {
      emit('ctx-menu-new-file', item, name);

      sendEvent('click_file_editor_nav_menu_item_context_menu_new_file');
    };

    const onContextMenuNewDirectory = (item: FileNavItem, name: string) => {
      emit('ctx-menu-new-directory', item, name);

      sendEvent('click_file_editor_nav_menu_item_context_menu_new_directory');
    };

    const onContextMenuRename = (item: FileNavItem, name: string) => {
      emit('ctx-menu-rename', item, name);

      sendEvent('click_file_editor_nav_menu_item_context_menu_rename');
    };

    const onContextMenuClone = (item: FileNavItem, name: string) => {
      emit('ctx-menu-clone', item, name);

      sendEvent('click_file_editor_nav_menu_item_context_menu_clone');
    };

    const onContextMenuDelete = (item: FileNavItem) => {
      emit('ctx-menu-delete', item);

      sendEvent('click_file_editor_nav_menu_item_context_menu_delete');
    };

    const onContentChange = (cm: Editor) => {
      const content = cm.getValue();
      if (!activeFileItem.value) return;
      emit('content-change', content);

      // update in cache
      updateContentCache(activeFileItem.value, content);

      sendEvent('click_file_editor_content_change');
    };

    const onTabClick = (tab: FileNavItem) => {
      store.commit(`${ns}/setActiveFileNavItem`, tab);
      emit('tab-click', tab);

      // get from cache and update content
      getContentCache(tab);

      sendEvent('click_file_editor_tab_click');
    };

    const closeTab = (tab: FileNavItem) => {
      const idx = tabs.value.findIndex(t => t.path === tab.path);
      if (idx !== -1) {
        tabs.value.splice(idx, 1);
      }
      if (activeFileItem.value) {
        if (activeFileItem.value.path === tab.path) {
          if (idx === 0) {
            store.commit(`${ns}/setActiveFileNavItem`, tabs.value[0]);
          } else {
            store.commit(`${ns}/setActiveFileNavItem`, tabs.value[idx - 1]);
          }
        }

        // get from cache
        setTimeout(() => {
          if (activeFileItem.value) {
            getContentCache(activeFileItem.value);
          }
        }, 0);
      }

      // delete in cache
      deleteContentCache(tab);
    };

    const onTabClose = (tab: FileNavItem) => {
      closeTab(tab);

      sendEvent('click_file_editor_tab_close');
    };

    const onTabCloseOthers = (tab: FileNavItem) => {
      tabs.value = [tab];
      store.commit(`${ns}/setActiveFileNavItem`, tab);

      // clear cache and update current tab content
      deleteOtherContentCache(tab);

      sendEvent('click_file_editor_tab_close_others');
    };

    const onTabCloseAll = () => {
      tabs.value = [];
      store.commit(`${ns}/resetActiveFileNavItem`);

      // clear cache
      clearContentCache();

      sendEvent('click_file_editor_tab_close_all');
    };

    const onTabDragEnd = (newTabs: FileNavItem[]) => {
      tabs.value = newTabs;

      sendEvent('click_file_editor_tab_dragend');
    };

    const onShowMoreShow = () => {
      showMoreContextMenuVisible.value = true;

      sendEvent('click_file_editor_showmore_show');
    };

    const onShowMoreHide = () => {
      showMoreContextMenuVisible.value = false;

      sendEvent('click_file_editor_showmore_hide');
    };

    const onClickShowMoreContextMenuItem = (tab: FileNavItem) => {
      store.commit(`${ns}/setActiveFileNavItem`, tab);
      emit('tab-click', tab);

      sendEvent('click_file_editor_showmore_click_context_menu_item');
    };

    const keyMapSave = () => {
      if (!activeFileItem.value) return;
      emit('save-file', activeFileItem.value);
    };

    const keyMapClose = () => {
      if (!activeFileItem.value) return;
      closeTab(activeFileItem.value);
    };

    const addSaveKeyMap = (cm: Editor) => {
      const map = {
        'Cmd-S': keyMapSave,
        'Ctrl-S': keyMapSave,
        // 'Cmd-W': keyMapClose,
        'Ctrl-W': keyMapClose,
      } as KeyMap;
      cm.addKeyMap(map);
    };

    const onToggleNavMenu = () => {
      navMenuCollapsed.value = !navMenuCollapsed.value;

      sendEvent('click_file_editor_nav_menu_toggle', {collapse: !navMenuCollapsed.value});
    };

    const listenToKeyboardEvents = () => {
      editor?.on('blur', () => {
        updateSearchInput();
      });
    };

    const unlistenToKeyboardEvents = () => {
      document.onkeydown = null;
    };

    const update = async () => {
      await updateMode();
      await updateTheme();
      updateEditorOptions();
      updateStyle();
    };

    watch(() => JSON.stringify(options.value), update);
    watch(() => JSON.stringify(activeFileItem.value), update);

    const onDropFiles = (files: InputFile[]) => {
      emit('drop-files', files);

      sendEvent('click_file_editor_drop_files');
    };

    const onFileSearch = () => {
      sendEvent('click_file_editor_file_search');
    };

    const initEditor = async () => {
      // init codemirror editor
      const el = codeMirrorEditor.value as HTMLElement;
      editor = getCodemirrorEditor(el, options.value);

      // add save key map
      addSaveKeyMap(editor);

      // on editor change
      editor.on('change', onContentChange);

      // update editor options
      updateEditorOptions();

      // update editor content
      updateEditorContent();

      // update editor theme
      await updateTheme();

      // update styles
      updateStyle();

      // listen to keyboard events key
      listenToKeyboardEvents();

      // by default show line numbers (this is a hack to make line numbers visible)
      store.commit(`file/setEditorOptions`, {
        ...options.value,
        lineNumbers: true,
      });

      // init codemirror template
      const elTemplate = codeMirrorTemplate.value as HTMLElement;
      codeMirrorTemplateEditor = getCodemirrorEditor(elTemplate, options.value);
      codeMirrorTemplateEditor.setValue(codeMirrorTemplateContent.value);
      codeMirrorTemplateEditor.setOption('mode', 'text/x-python');
    };

    onMounted(initEditor);

    onUnmounted(() => {
      // turnoff listening to keyboard events
      unlistenToKeyboardEvents();
    });

    return {
      fileEditor,
      codeMirrorEditor,
      tabs,
      activeFileItem,
      fileSearchString,
      navMenuCollapsed,
      styleRef,
      codeMirrorTemplate,
      showSettings,
      showCodeMirrorEditor,
      navTabs,
      showMoreContextMenuVisible,
      languageMime,
      options,
      style,
      files,
      extraStyle,
      onNavItemClick,
      onNavItemDbClick,
      onNavItemDrop,
      onContextMenuNewFile,
      onContextMenuNewDirectory,
      onContextMenuRename,
      onContextMenuClone,
      onContextMenuDelete,
      onContentChange,
      onTabClick,
      onTabClose,
      onTabCloseOthers,
      onTabCloseAll,
      onTabDragEnd,
      onToggleNavMenu,
      onShowMoreShow,
      onShowMoreHide,
      onClickShowMoreContextMenuItem,
      updateTabs,
      updateEditorContent,
      updateContentCache,
      onDropFiles,
      onFileSearch,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.file-editor {
  height: 100%;
  display: flex;

  .nav-menu {
    flex-basis: var(--cl-file-editor-nav-menu-width);
    min-width: var(--cl-file-editor-nav-menu-width);
    display: flex;
    flex-direction: column;
    transition: all var(--cl-file-editor-nav-menu-collapse-transition-duration);

    &.collapsed {
      min-width: 0;
      flex-basis: 0;
      overflow: hidden;
    }

    .nav-menu-top-bar {
      flex-basis: var(--cl-file-editor-nav-menu-top-bar-height);
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 12px;
      padding: 0 10px 0 0;

      .left,
      .right {
        display: flex;
      }
    }
  }

  .file-editor-content {
    position: relative;
    flex: 1;
    display: flex;
    min-width: calc(100% - var(--cl-file-editor-nav-menu-width));
    flex-direction: column;

    .code-mirror-editor {
      flex: 1;

      &.hidden {
        position: fixed;
        top: -100vh;
        left: 0;
        height: 100vh;
      }
    }

    .empty-content {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .nav-tabs-suffix {
      width: 30px;
      position: absolute;
      top: 0;
      right: 0;
      z-index: 5;
      display: flex;
      align-items: center;
      justify-content: center;
      height: var(--cl-file-editor-nav-tabs-height);
    }
  }

  .action-icon {
    position: relative;
    height: 16px;
    width: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 12px;

    &:hover {
      .background {
        background-color: var(--cl-file-editor-mask-bg);
        border-radius: 8px;
      }
    }

    &.expand-files {
      width: 29px;
      text-align: center;
    }

    .background {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
    }
  }
}

.code-mirror-template {
  position: fixed;
  top: -100vh;
  left: 0;
  height: 100vh;
}
</style>
<style scoped>
.file-editor .nav-menu .nav-menu-top-bar >>> .search.el-input .el-input__inner {
  border: none;
  background: transparent;
  color: inherit;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror {
  position: relative;
  min-height: 100%;
  border: none;
  border-radius: 0;
  padding: 0;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror.dialog-opened {
  position: relative;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror-dialog {
  font-size: 14px;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror-dialog.CodeMirror-dialog-top {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror-dialog.CodeMirror-dialog-bottom {
  position: absolute;
  bottom: 10px;
  right: 10px;
  z-index: 2;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror-search-field {
  background-color: transparent;
  border: 1px solid;
  color: inherit;
  outline: none;
}

.file-editor .file-editor-content .code-mirror-editor >>> .CodeMirror-linenumber {
}
</style>
