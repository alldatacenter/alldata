<template>
  <cl-form
    v-if="form"
    ref="formRef"
    :model="form"
    :rules="formRules"
    :selective="isSelectiveForm"
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
        v-locate="'username'"
        v-model="form.username"
        :disabled="isFormItemDisabled('username')"
        :placeholder="t('components.user.form.username')"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.user.form.password')"
      prop="password"
      required
    >
      <el-input
        v-if="(isSelectiveForm || !isDetail) && !isMySettings"
        v-locate="'password'"
        v-model="form.password"
        :disabled="isFormItemDisabled('password')"
        :placeholder="t('components.user.form.password')"
        type="password"
      />
      <cl-label-button
        v-else
        id="password"
        class-name="password"
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
        v-locate="'email'"
        v-model="form.email"
        :disabled="isFormItemDisabled('email')"
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
      <el-select
        v-locate="'role'"
        v-model="form.role"
        :disabled="isFormItemDisabled('role')"
      >
        <el-option v-locate="ROLE_ADMIN" :value="ROLE_ADMIN" :label="t('components.user.role.admin')"/>
        <el-option v-locate="ROLE_NORMAL" :value="ROLE_NORMAL" :label="t('components.user.role.normal')"/>
      </el-select>
    </cl-form-item>
    <!-- ./Row -->
  </cl-form>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {useStore} from 'vuex';
import useUser from '@/components/user/user';
import {ROLE_ADMIN, ROLE_NORMAL} from '@/constants/user';
import useUserDetail from '@/views/user/detail/useUserDetail';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'UserForm',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'user';
    const store = useStore();

    const {
      activeId,
    } = useUserDetail();

    const {
      onChangePasswordFunc,
    } = useUser(store);

    const onChangePassword = () => onChangePasswordFunc(activeId.value);

    const isDetail = computed<boolean>(() => !!activeId.value);

    return {
      ...useUser(store),
      ROLE_ADMIN,
      ROLE_NORMAL,
      onChangePassword,
      isDetail,
      t,
    };
  },
});
</script>

<style scoped>
</style>
