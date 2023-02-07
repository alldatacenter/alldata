<template>
  <div class="my-settings">
    <cl-simple-layout padding="0">
      <cl-nav-actions ref="navActions" class="nav-actions">
        <cl-nav-action-group>
          <cl-nav-action-button
            :icon="['fa', 'save']"
            button-type="label"
            :label="t('components.nav.actions.save')"
            type="success"
            @click="onSave"
          />
        </cl-nav-action-group>
      </cl-nav-actions>
      <cl-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        class="user-form"
      >
        <!-- Row -->
        <cl-form-item
          :span="2"
          :label="t('components.user.form.username')"
          prop="username"
          required
        >
          <el-input
            v-model="form.username"
            :placeholder="t('components.user.form.username')"
          />
        </cl-form-item>
        <cl-form-item
          :span="2"
          :label="t('components.user.form.password')"
          prop="password"
          required
        >
          <cl-label-button
            :icon="['fa','lock']"
            :label="t('components.user.form.changePassword')"
            type="danger"
            @click="onChangePassword"
          />
        </cl-form-item>
        <!-- ./Row -->

        <!-- Row -->
        <cl-form-item
          :span="2"
          :label="t('components.user.form.email')"
          prop="email"
        >
          <el-input
            v-model="form.email"
            :placeholder="t('components.user.form.email')"
            type="email"
          />
        </cl-form-item>
        <cl-form-item
          :span="2"
          :label="t('components.user.form.role')"
          prop="role"
          required
        >
          <el-select v-model="form.role">
            <el-option :value="ROLE_ADMIN" :label="t('components.user.role.admin')"/>
            <el-option :value="ROLE_NORMAL" :label="t('components.user.role.normal')"/>
          </el-select>
        </cl-form-item>
        <!-- ./Row -->
      </cl-form>
    </cl-simple-layout>
  </div>
</template>

<script lang="ts">
import {defineComponent, onBeforeMount, ref} from 'vue';
import {useStore} from 'vuex';
import {plainClone} from '@/utils/object';
import useUser from '@/components/user/user';
import {ROLE_ADMIN, ROLE_NORMAL} from '@/constants/user';
import {useI18n} from 'vue-i18n';
import {ElMessage} from 'element-plus';

export default defineComponent({
  name: 'MySettings',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'user';
    const store = useStore();

    const {
      formRules,
      onChangePasswordFunc,
    } = useUser(store);

    const form = ref<User>({});

    onBeforeMount(() => {
      form.value = plainClone(store.getters['user/me']) as User;
    });

    const onChangePassword = () => onChangePasswordFunc(form.value._id);

    const onSave = async () => {
      await store.dispatch(`${ns}/postMe`, form.value);
      await ElMessage.success(t('common.message.success.save'));
      await store.dispatch(`${ns}/getMe`);
    };

    return {
      form,
      formRules,
      ROLE_ADMIN,
      ROLE_NORMAL,
      onChangePassword,
      onSave,
      t,
    };
  }
});

</script>

<style scoped lang="scss">
.my-settings {
  height: 100%;

  .user-form {
    margin-top: 20px;
  }
}
</style>
