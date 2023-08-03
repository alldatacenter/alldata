<template>
  <div class="login-wrap">
    <div class="img-logo">
      <img src="@/assets/images/logo.svg" class="arctic-logo" alt="">
    </div>
    <a-form
      :model="formState"
      name="normal_login"
      class="login-form label-120"
      @finish="onFinish"
    >
      <a-form-item
        label=""
        name="username"
        :rules="[{ required: true, message: placeholder.usernamePh }]"
      >
        <a-input v-model:value="formState.username" :placeholder="placeholder.usernamePh">
          <template #prefix>
            <UserOutlined class="site-form-item-icon" />
          </template>
        </a-input>
      </a-form-item>
      <a-form-item
        label=""
        name="password"
        :rules="[{ required: true, message: placeholder.passwordPh }]"
      >
        <a-input-password v-model:value="formState.password" :placeholder="placeholder.passwordPh">
          <template #prefix>
            <LockOutlined class="site-form-item-icon" />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item>
        <a-button :disabled="disabled" type="primary" html-type="submit" class="login-form-button">
          {{$t('signin')}}
        </a-button>
      </a-form-item>
    </a-form>
    <p class="desc">{{$t('welecomeTip')}}</p>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, onMounted, reactive } from 'vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import loginService from '@/services/login.service'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import useStore from '@/store'

interface FormState {
  username: string;
  password: string;
}

export default defineComponent({
  name: 'Login',
  components: {
    UserOutlined,
    LockOutlined
  },
  setup() {
    const router = useRouter()
    const formState = reactive<FormState>({
      username: '',
      password: ''
    })
    const placeholder = reactive(usePlaceholder())
    const onFinish = async(values: FormState) => {
      try {
        const store = useStore()
        const res = await loginService.login({
          user: values.username,
          password: values.password
        })
        if (res.code !== 200) {
          message.error(res.message)
          return
        }
        const { path, query } = store.historyPathInfo
        router.replace({
          path: path || '/',
          query
        })
      } catch (error) {
        message.error((error as Error).message)
      }
    }

    const disabled = computed(() => {
      return !(formState.username && formState.password)
    })
    onMounted(() => {
    })
    return {
      placeholder,
      formState,
      onFinish,
      disabled
    }
  }
})

</script>

<style lang="less" scoped>
.login-wrap {
  padding-top: 120px;
  height: 100%;
  width: 400px;
  margin: auto;
  .img-logo {
    margin: auto;
    margin-bottom: 32px;
    text-align: center;
    .arctic-logo {
      width: 200px;
    }
  }
  .desc {
    margin-top: 20px;
  }
  .login-form-button {
    width: 100%;
  }
}
</style>
