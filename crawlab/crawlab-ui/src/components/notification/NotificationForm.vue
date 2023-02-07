<template>
  <cl-form
    v-if="form"
    ref="formRef"
    :model="form"
    :selective="isSelectiveForm"
  >
    <cl-form-item
      :span="2"
      :label="t('views.notification.settings.form.name')"
      prop="name"
      required
    >
      <el-input
        v-model="form.name"
        :placeholder="t('views.notification.settings.form.name')"
        @change="onChange"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('views.notification.settings.form.description')"
      prop="description"
    >
      <el-input
        v-model="form.description"
        type="textarea"
        :placeholder="t('views.notification.settings.form.description')"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('views.notification.settings.form.type')"
      prop="type"
    >
      <el-select v-model="form.type">
        <el-option value="mail" :label="t('views.notification.settings.type.mail')"/>
        <el-option value="mobile" :label="t('views.notification.settings.type.mobile')"/>
      </el-select>
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('views.notification.settings.form.enabled')"
      prop="enabled"
    >
      <cl-switch v-model="form.enabled"/>
    </cl-form-item>

    <template v-if="form.type === 'mail'">
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.smtp.server')"
        prop="mail.server"
        required
      >
        <el-input
          v-model="form.mail.server"
          :placeholder="t('views.notification.settings.form.mail.smtp.server')"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.smtp.port')"
        prop="mail.port"
        required
      >
        <el-input
          v-model="form.mail.port"
          :placeholder="t('views.notification.settings.form.mail.smtp.port')"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.smtp.user')"
        prop="mail.user"
      >
        <el-input
          v-model="form.mail.user"
          :placeholder="t('views.notification.settings.form.mail.smtp.user')"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.smtp.password')"
        prop="mail.password"
      >
        <el-input
          v-model="form.mail.password"
          :placeholder="t('views.notification.settings.form.mail.smtp.password')"
        />
      </cl-form-item>
      <cl-form-item :span="2" :label="t('views.notification.settings.form.mail.smtp.sender.email')"
                prop="mail.sender_email">
        <el-input
          v-model="form.mail.sender_email"
          :placeholder="t('views.notification.settings.form.mail.smtp.sender.email')"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.smtp.sender.identity')"
        prop="mail.sender_identity"
      >
        <el-input
          v-model="form.mail.sender_identity"
          :placeholder="t('views.notification.settings.form.mail.smtp.sender.identity')"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.to')"
        prop="mail.to"
        required
      >
        <el-input
          v-model="form.mail.to"
          :placeholder="t('views.notification.settings.form.mail.to')"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :label="t('views.notification.settings.form.mail.cc')"
        prop="mail.cc"
      >
        <el-input
          v-model="form.mail.cc"
          :placeholder="t('views.notification.settings.form.mail.cc')"
        />
      </cl-form-item>
    </template>

    <template v-else-if="form.type === 'mobile'">
      <cl-form-item
        :span="4"
        :label="t('views.notification.settings.form.mobile.webhook')"
        prop="mobile.webhook"
      >
        <el-input
          v-model="form.mobile.webhook"
          :placeholder="t('views.notification.settings.form.mobile.webhook')"
        />
      </cl-form-item>
    </template>
  </cl-form>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useI18n} from 'vue-i18n';
import {useStore} from 'vuex';
import useNotification from '@/components/notification/notification';

export default defineComponent({
  name: 'NotificationForm',
  props: {
    readonly: {
      type: Boolean,
    }
  },
  emits: [
    'update:modelValue',
  ],
  setup(props, {emit}) {
    // i18n
    const {t} = useI18n();

    // store
    const store = useStore();

    return {
      ...useNotification(store),
      t,
    };
  },
});
</script>

<style scoped>

</style>
