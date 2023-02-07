<template>
  <cl-dialog
    :visible="fileUploadVisible"
    :title="title"
    :confirm-loading="confirmLoading"
    :confirm-disabled="confirmDisabled"
    @close="onUploadClose"
    @confirm="onUploadConfirm"
  >
    <cl-file-upload
      ref="fileUploadRef"
      :mode="mode"
      :get-input-props="getInputProps"
      :open="open"
      @mode-change="onModeChange"
      @files-change="onFilesChange"
    />
  </cl-Dialog>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeUnmount, ref} from 'vue';
import {useStore} from 'vuex';
import {ElMessage} from 'element-plus';
import {FileWithPath} from 'file-selector';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';
import useSpiderService from '@/services/spider/spiderService';
import {getOSPathSeparator} from '@/utils';
import {useRoute} from 'vue-router';
import {FILE_UPLOAD_MODE_DIR} from '@/constants';
import {useDropzone} from 'crawlab-vue3-dropzone';
import useSpiderDetail from '@/views/spider/detail/useSpiderDetail';

export default defineComponent({
  name: 'UploadSpiderFilesDialog',
  setup() {
    // i18n
    const {t} = useI18n();

    // route
    const route = useRoute();

    // store
    const ns = 'spider';
    const store = useStore();
    const {
      spider: state,
    } = store.state as RootStoreState;

    const fileUploadRef = ref();

    const mode = computed(() => state.fileMode);
    const files = computed(() => state.files);
    const fileUploadVisible = computed(() => state.activeDialogKey === 'uploadFiles');
    const name = computed(() => state.form?.name);

    const confirmLoading = ref<boolean>(false);
    const confirmDisabled = computed<boolean>(() => !files.value?.length);

    const {
      activeId,
    } = useSpiderDetail();

    const isDetail = computed<boolean>(() => !!activeId.value);

    const {
      listRootDir,
      saveFileBinary,
    } = useSpiderService(store);

    const id = computed<string>(() => {
      if (isDetail.value) {
        return route.params.id as string;
      } else {
        return state.form?._id as string;
      }
    });

    const hasMultiDir = computed<boolean>(() => {
      if (!files.value) return false;
      const set = new Set<string>();
      for (const f of files.value) {
        const lv1 = f.path?.split(getOSPathSeparator())[0] as string;
        if (!set.has(lv1)) {
          set.add(lv1);
        }
        if (set.size > 1) {
          return true;
        }
      }
      return false;
    });

    const setInfo = () => {
      // set file upload info
      const info = {
        fileCount: files.value.length,
        filePaths: files.value.map(f => f.path || f.name),
      } as FileUploadInfo;
      if (mode.value === FILE_UPLOAD_MODE_DIR) {
        const f = files.value[0];
        info.dirName = f.path?.split(getOSPathSeparator())[0];
      }
      fileUploadRef.value?.setInfo(info);
    };

    const getFilePath = (f: FileWithPath): string => {
      const path = f.path;
      if (!path) return f.name;
      if (hasMultiDir.value) {
        return path;
      } else {
        return path.split(getOSPathSeparator()).filter((_: any, i: number) => i > 0).join(getOSPathSeparator());
      }
    };

    const uploadFiles = async () => {
      if (!files.value) return;
      await Promise.all(files.value.map((f: FileWithPath) => {
        return saveFileBinary(id.value, getFilePath(f), f as File);
      }));
      store.commit(`${ns}/resetFiles`);
      await listRootDir(id.value);
    };

    const onClickUpload = () => {
      store.commit(`${ns}/showDialog`, 'uploadFiles');

      sendEvent('click_spider_detail_actions_upload');
    };

    const onUploadClose = () => {
      store.commit(`${ns}/hideDialog`);
    };

    const onUploadConfirm = async () => {
      confirmLoading.value = true;
      try {
        sendEvent('click_spider_detail_actions_upload_confirm', {
          mode: mode.value
        });

        await uploadFiles();
        await ElMessage.success(t('common.message.success.upload'));
      } catch (e: any) {
        await ElMessage.error(e);
      } finally {
        confirmLoading.value = false;
        store.commit(`${ns}/hideDialog`);
        fileUploadRef.value?.clearFiles();
      }
    };

    const {
      getInputProps,
      open,
    } = useDropzone({
      onDrop: (fileList: InputFile[]) => {
        store.commit(`${ns}/setFiles`, fileList.map(f => f as FileWithPath));

        // set file upload info
        setInfo();
      },
    });

    const onModeChange = (value: string) => {
      store.commit(`${ns}/setFileMode`, value);

      // reset file upload info
      fileUploadRef.value?.resetInfo();
    };

    const onFilesChange = (fileList: FileWithPath[]) => {
      if (!fileList.length) return;

      // set files
      store.commit(`${ns}/setFiles`, fileList);

      // set file upload info
      setInfo();

      sendEvent('click_spider_detail_actions_files_change');
    };

    const title = computed(() => {
      return t('components.file.upload.title') + (name.value ? ` - ${name.value}` : '');
    });

    onBeforeUnmount(() => {
      store.commit(`${ns}/resetFileMode`);
      store.commit(`${ns}/resetFiles`);
      store.commit(`${ns}/hideDialog`);
    });

    return {
      fileUploadRef,
      mode,
      files,
      fileUploadVisible,
      confirmLoading,
      confirmDisabled,
      onClickUpload,
      onUploadClose,
      onUploadConfirm,
      onModeChange,
      onFilesChange,
      getInputProps,
      open,
      title,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
</style>
