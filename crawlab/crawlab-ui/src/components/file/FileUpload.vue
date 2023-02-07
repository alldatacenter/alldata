<template>
  <div class="file-upload">
    <div class="mode-select">
      <el-radio-group v-model="internalMode" @change="onModeChange">
        <el-radio
          v-for="{value, label} in modeOptions"
          :key="value"
          :label="value"
          :class="value"
        >
          {{ label }}
        </el-radio>
      </el-radio-group>
    </div>

    <template v-if="mode === FILE_UPLOAD_MODE_FILES">
      <el-upload
        ref="uploadRef"
        class="file-upload-action"
        :on-change="onFileChange"
        :http-request="() => {}"
        drag
        multiple
        :show-file-list="false"
      >
        <el-icon class="el-icon--upload">
          <upload-filled/>
        </el-icon>
        <div class="el-upload__text">{{ t('components.file.upload.buttons.files.dragFilesHereOr') }}
          <em>{{ t('components.file.upload.buttons.files.clickToUpload') }}</em>
        </div>
      </el-upload>
      <input v-bind="getInputProps()" multiple>
    </template>
    <template v-else-if="mode === FILE_UPLOAD_MODE_DIR">
      <div class="folder-upload-action-wrapper">
        <cl-button
          size="large"
          class-name="file-upload-action"
          @click="open"
        >
          <i class="fa fa-folder"></i>
          {{ t('components.file.upload.buttons.folder.clickToSelectFolderToUpload') }}
        </cl-button>
        <template v-if="!!dirInfo?.dirName && dirInfo?.fileCount">
          <cl-tag
            type="primary"
            class="info-tag"
            :label="dirInfo?.dirName"
            :icon="['fa', 'folder']"
            :tooltip="t('components.file.upload.tooltip.fileName')"
          />
          <cl-tag
            type="success"
            class="info-tag"
            :label="dirInfo?.fileCount"
            :icon="['fa', 'hashtag']"
            :tooltip="t('components.file.upload.tooltip.filesCount')"
          />
        </template>
      </div>
      <input v-bind="getInputProps()" webkitdirectory multiple>
    </template>
    <div v-if="dirInfo?.filePaths?.length > 0" class="file-list-wrapper">
      <h4 class="title">
        {{ t('components.file.upload.fileList.title') }}
      </h4>
      <ul class="file-list">
        <li v-for="(path, $index) in dirInfo?.filePaths" :key="$index" class="file-item">
          {{ path }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeMount, PropType, ref, watch} from 'vue';
import {FILE_UPLOAD_MODE_DIR, FILE_UPLOAD_MODE_FILES} from '@/constants/file';
import {ElUpload, UploadFile} from 'element-plus/lib/components/upload/src/upload.type';
import {plainClone} from '@/utils/object';
import {useI18n} from 'vue-i18n';
import {UploadFilled} from '@element-plus/icons';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'FileUpload',
  components: {
    UploadFilled,
  },
  props: {
    mode: {
      type: String as PropType<FileUploadMode>,
    },
    getInputProps: {
      type: Function as PropType<() => void>,
    },
    open: {
      type: Function as PropType<() => void>,
    },
  },
  emits: [
    'mode-change',
    'files-change',
  ],
  setup(props: FileUploadProps, {emit}) {
    const {t} = useI18n();

    const modeOptions = computed<FileUploadModeOption[]>(() => [
      {
        label: t('components.file.upload.mode.folder'),
        value: FILE_UPLOAD_MODE_DIR,
      },
      {
        label: t('components.file.upload.mode.files'),
        value: FILE_UPLOAD_MODE_FILES,
      },
    ]);
    const internalMode = ref<string>();

    const uploadRef = ref<ElUpload>();

    const dirPath = ref<string>();

    const dirInfo = ref<FileUploadInfo>();

    const setInfo = (info: FileUploadInfo) => {
      dirInfo.value = plainClone(info);
    };

    const resetInfo = () => {
      dirInfo.value = undefined;
    };

    watch(() => props.mode, () => {
      internalMode.value = props.mode;
      uploadRef.value?.clearFiles();
    });

    const onFileChange = (file: UploadFile, fileList: UploadFile[]) => {
      emit('files-change', fileList.map(f => f.raw));
    };

    const clearFiles = () => {
      uploadRef.value?.clearFiles();
      resetInfo();
    };

    const onModeChange = (mode: string) => {
      emit('mode-change', mode);
      resetInfo();

      sendEvent('click_file_upload_mode_change', {mode});
    };

    onBeforeMount(() => {
      const {mode} = props;
      internalMode.value = mode;
    });

    return {
      uploadRef,
      FILE_UPLOAD_MODE_FILES,
      FILE_UPLOAD_MODE_DIR,
      modeOptions,
      internalMode,
      dirPath,
      onFileChange,
      clearFiles,
      onModeChange,
      dirInfo,
      setInfo,
      resetInfo,
      t,
    };
  },
});
</script>

<style scoped lang="scss">
.file-upload {
  .mode-select {
    margin-bottom: 20px;
  }

  .el-upload {
    width: 100%;
  }

  .folder-upload {
    display: flex;
    align-items: center;

  }

  .file-list-wrapper {
    .title {
      margin-bottom: 0;
      padding-bottom: 0;
    }

    .file-list {
      list-style: none;
      max-height: 400px;
      overflow: auto;
      border: 1px solid var(--cl-info-plain-color);
      padding: 10px;
      margin-top: 10px;

      .file-item {
      }
    }
  }
}
</style>

<style scoped>
.file-upload >>> .el-upload,
.file-upload >>> .el-upload .el-upload-dragger {
  width: 100%;
}

.file-upload >>> .folder-upload .info-tag {
  margin-left: 10px;
}
</style>
