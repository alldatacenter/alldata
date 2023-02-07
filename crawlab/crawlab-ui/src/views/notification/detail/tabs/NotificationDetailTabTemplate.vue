<template>
  <div class="notification-detail-tab-template">
    <el-input
      v-model="internalTitle"
      class="title"
      :placeholder="t('views.notification.settings.form.title')"
      @input="onTitleChange"
    />
    <div class="simple-mde">
      <textarea :value="form.template" ref="simpleMDERef"/>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent, onMounted, onBeforeUnmount, ref, watch} from 'vue';
import {translate} from '@/utils';
import 'simplemde/dist/simplemde.min.js';
import {useStore} from 'vuex';
import useNotification from '@/components/notification/notification';
import useNotificationDetail from '@/views/notification/detail/useNotificationDetail';

const t = translate;

export default defineComponent({
  name: 'NotificationDetailTabTemplate',
  setup() {
    // store
    const ns = 'notification';
    const store = useStore();
    const {
      form,
    } = useNotification(store);

    const simpleMDERef = ref();

    const simpleMDE = ref();

    const internalTitle = ref();

    onMounted(() => {
      simpleMDE.value = new window.SimpleMDE({
        element: simpleMDERef.value,
        spellChecker: false,
        placeholder: t('views.notification.settings.form.templateContent'),
      });
      simpleMDE.value.codemirror.on('change', () => {
        store.commit(`${ns}/setTemplateContent`, simpleMDE.value.value());
      });

      const {title} = form.value;
      internalTitle.value = title;

      const codeMirrorEl = document.querySelector('.CodeMirror');
      if (!codeMirrorEl) return;
      codeMirrorEl.setAttribute('style', 'height: 100%; min-height: 100%;');
    });

    onBeforeUnmount(() => {
      simpleMDE.value.toTextArea();
      simpleMDE.value = null;
    });

    watch(() => form.value.template, (template) => {
      if (simpleMDE.value) {
        simpleMDE.value.value(template);
      }
    });

    watch(() => form.value.title, (title) => {
      internalTitle.value = title;
    });

    const onTitleChange = (title: string) => {
      store.commit(`${ns}/setTemplateTitle`, title);
    };

    return {
      ...useNotificationDetail(),
      form,
      simpleMDERef,
      internalTitle,
      onTitleChange,
      t,
    };
  },
});
</script>

<style scoped>
.notification-detail-tab-template {
  /*min-height: 100%;*/
  display: flex;
  flex-direction: column;
}

.notification-detail-tab-template .title {
  /*margin-bottom: 20px;*/
}

.notification-detail-tab-template .title >>> .el-input__inner {
  border: none;
}

.notification-detail-tab-template >>> .editor-toolbar {
  border-radius: 0;
  border-left: none;
  border-right: none;
}

.notification-detail-tab-template >>> .CodeMirror {
  border-radius: 0;
}
</style>
