<template>
  <div class="login-container">
    <canvas id="canvas" class="login-canvas"/>
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      auto-complete="on"
      class="login-form"
      label-position="left"
    >
      <h3 class="title">
        <img :src="logo" alt="logo" class="logo-img"/>
        <span class="logo-title">Crawlab</span>
        <span class="logo-sub-title">
          <div class="logo-sub-title-block">
            {{ t(systemInfo.edition || '') }}
          </div>
          <div class="logo-sub-title-block">
            {{ systemInfo.version }}
          </div>
        </span>
      </h3>
      <el-form-item prop="username" style="margin-bottom: 28px;">
        <el-input
          v-model="loginForm.username"
          :placeholder="t('views.login.loginForm.username')"
          auto-complete="on"
          name="username"
          type="text"
          size="large"
          @keyup.enter="onLogin"
        />
      </el-form-item>
      <el-form-item prop="password" style="margin-bottom: 28px;">
        <el-input
          v-model="loginForm.password"
          :placeholder="t('views.login.loginForm.password')"
          auto-complete="on"
          name="password"
          type="password"
          size="large"
          @keyup.enter="onLogin"
        />
      </el-form-item>
      <el-form-item v-if="isSignup" prop="confirmPassword" style="margin-bottom: 28px;">
        <el-input
          v-model="loginForm.confirmPassword"
          :placeholder="t('views.login.loginForm.confirmPassword')"
          auto-complete="on"
          name="confirm-password"
          size="large"
        />
      </el-form-item>
      <el-form-item v-if="isSignup" prop="email" style="margin-bottom: 28px;">
        <el-input
          v-model="loginForm.email"
          :placeholder="t('views.login.loginForm.email')"
          name="email"
          size="large"
        />
      </el-form-item>
      <el-form-item style="border: none">
        <el-button
          v-if="isSignup"
          :loading="loading"
          style="width:100%;"
          type="primary"
          size="large"
        >
          {{ t('views.login.loginForm.signUp') }}
        </el-button>
        <el-button
          v-if="!isSignup"
          :loading="loading"
          style="width:100%;"
          type="primary"
          size="large"
          @click="onLogin"
        >
          {{ t('views.login.loginForm.signIn') }}
        </el-button>
      </el-form-item>
      <div class="alternatives">
        <div class="left">
          <el-tooltip :content="t('views.login.forgotPassword.content')" trigger="click">
            <span class="forgot-password">{{ t('views.login.forgotPassword.label') }}</span>
          </el-tooltip>
        </div>
      </div>
      <div class="tips">
        <span>{{ t('views.login.initial.title') }}: admin/admin</span>
        <!--TODO: implement github stars-->
        <a v-if="false" href="https://github.com/crawlab-team/crawlab" style="float:right" target="_blank">
          <img alt="github-stars" src="https://img.shields.io/github/stars/crawlab-team/crawlab?logo=github">
        </a>
      </div>
      <div class="lang">
        <span :class="lang==='zh'?'active':''" @click="setLang('zh')">中文</span>
        |
        <span :class="lang==='en'?'active':''" @click="setLang('en')">English</span>
      </div>
      <div v-if="false" class="documentation">
        <a href="https://docs-next.crawlab.cn" target="_blank">{{ t('views.login.documentation') }}</a>
      </div>
      <div class="mobile-warning" v-if="isShowMobileWarning">
        <el-alert :closable="false" type="error">
          {{
            t('views.login.mobile.warning')
          }}
        </el-alert>
      </div>
    </el-form>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onMounted, onUnmounted, ref} from 'vue';
import {isValidUsername} from '@/utils/validate';
import {useRoute, useRouter} from 'vue-router';
import logo from '@/assets/svg/logo';
import {ElMessage} from 'element-plus';
import useRequest from '@/services/request';
import {useStore} from 'vuex';
import {setGlobalLang} from '@/utils/i18n';
import {useI18n} from 'vue-i18n';
import {LOCAL_STORAGE_KEY_TOKEN} from '@/constants/localStorage';

const {
  post,
} = useRequest();

export default defineComponent({
  name: 'Login',
  setup() {
    // store
    const store = useStore();
    const {
      common: commonState,
    } = store.state as RootStoreState;

    // current route
    const route = useRoute();

    // router
    const router = useRouter();

    // i18n
    const {t} = useI18n();

    // loading
    const loading = ref<boolean>(false);

    // is signup
    const isSignup = computed(() => route.path === '/signup');

    // login form
    const loginForm = ref<LoginForm>({
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
    });

    // login form ref
    const loginFormRef = ref();

    const validateUsername = (rule: any, value: any, callback: any) => {
      if (!isValidUsername(value)) {
        callback(new Error(t('views.login.errors.incorrectUsername')));
      } else {
        callback();
      }
    };

    const validatePass = (rule: any, value: any, callback: any) => {
      if (value.length < 5) {
        callback(new Error(t('views.login.errors.passwordLength')));
      } else {
        callback();
      }
    };

    const validateConfirmPass = (rule: any, value: any, callback: any) => {
      if (!isSignup.value) return callback();
      if (value !== loginForm.value.password) {
        callback(new Error(t('views.login.errors.passwordSame')));
      } else {
        callback();
      }
    };

    const loginRules: LoginRules = {
      username: [{required: true, trigger: 'blur', validator: validateUsername}],
      password: [{required: true, trigger: 'blur', validator: validatePass}],
      confirmPassword: [{required: true, trigger: 'blur', validator: validateConfirmPass}]
    };

    const isShowMobileWarning = ref<boolean>(false);

    const allowRegister = ref<boolean>(false);

    const internalLang = ref<string>(localStorage.getItem('lang') || 'en');

    const lang = computed<string | null>(() => internalLang.value || localStorage.getItem('lang'));

    const setLang = (lang: Lang) => {
      internalLang.value = lang;
      setGlobalLang(lang);
    };

    // validate and perform login request
    const login = async () => {
      // skip if login form ref is empty
      if (!loginFormRef.value) return;

      // validate login form
      await loginFormRef.value.validate();

      // username and password
      const {username, password} = loginForm.value;

      // set loading
      loading.value = true;

      try {
        // perform login request
        const res = await post<LoginForm, ResponseWithData>('/login', {
          username,
          password,
        });

        // validate data
        if (!res?.data) {
          await ElMessage.error(t('views.login.errors.unauthorized'));
          return;
        }

        // set token to local storage
        localStorage.setItem(LOCAL_STORAGE_KEY_TOKEN, res.data);

        // initialize plugins
        // initPlugins(router, store)
        //   .then(() => console.info('[Crawlab] plugins initialized'))
        //   .catch(e => console.warn('[Crawlab] initializing plugins with error', e));

        // redirect to home page
        await router.push('/');

      } catch (e: any) {
        // error
        if (e.toString().includes('401')) {
          // unauthorized
          await ElMessage.error(t('views.login.errors.unauthorized'));
        } else {
          // other error
          await ElMessage.error(e.toString());
        }
        throw e;
      } finally {
        // unset loading
        loading.value = false;
      }
    };

    // on login hook
    const onLogin = async () => {
      // login
      await login();

      // get current user (me)
      await store.dispatch('user/getMe');
    };

    const systemInfo = computed<SystemInfo>(() => commonState.systemInfo || {});

    onMounted(() => {
      // initialize canvas
      if (window.innerWidth >= 1024) {
        window?.initCanvas?.();
      } else {
        isShowMobileWarning.value = true;
      }
    });
    onUnmounted(() => {
      // reset canvas
      if (window.resetCanvas) {
        window.resetCanvas();
      }
    });

    return {
      loginForm,
      loginFormRef,
      loginRules,
      isShowMobileWarning,
      allowRegister,
      isSignup,
      loading,
      lang,
      logo,
      setLang,
      onLogin,
      systemInfo,
      t,
    };
  },
});
</script>

<style lang="scss" rel="stylesheet/scss" scoped>
.login-container {
  position: fixed;
  height: 100%;
  width: 100%;
  background-color: var(--cl-white);

  .login-form {
    background: transparent;
    position: absolute;
    left: 0;
    right: 0;
    width: 480px;
    max-width: 100%;
    padding: 35px 35px 15px 35px;
    margin: 120px auto;
  }

  .tips {
    font-size: 14px;
    color: #666;
    margin-bottom: 10px;
    background: transparent;

    span {
      &:first-of-type {
        margin-right: 22px;
      }
    }
  }

  .svg-container {
    padding: 6px 5px 6px 15px;
    color: #889aa4;
    vertical-align: middle;
    width: 30px;
    display: inline-block;
  }

  .title {
    font-family: "Verdana", serif;
    /*font-style: italic;*/
    font-weight: 600;
    font-size: 24px;
    color: #409EFF;
    margin: 0px auto 20px auto;
    text-align: center;
    cursor: default;

    display: flex;
    align-items: center;
    height: 128px;

    .logo-img {
      height: 80px;
    }

    .logo-title {
      font-family: BlinkMacSystemFont, -apple-system, segoe ui, roboto, oxygen, ubuntu, cantarell, fira sans, droid sans, helvetica neue, helvetica, arial, sans-serif;
      font-size: 56px;
      font-weight: 600;
      margin-left: 24px;
      color: #409eff;
    }

    .logo-sub-title {
      font-family: BlinkMacSystemFont, -apple-system, segoe ui, roboto, oxygen, ubuntu, cantarell, fira sans, droid sans, helvetica neue, helvetica, arial, sans-serif;
      font-size: 20px;
      height: 48px;
      line-height: 48px;
      margin-left: 20px;
      font-weight: 500;
      color: var(--cl-info-medium-color);
      opacity: 0.8;

      .logo-sub-title-block {
        display: flex;
        align-items: center;
        height: 24px;
        line-height: 24px;
      }
    }
  }

  .show-pwd {
    position: absolute;
    right: 10px;
    top: 7px;
    font-size: 16px;
    color: #889aa4;
    cursor: pointer;
    user-select: none;
  }

  .alternatives {
    border-bottom: 1px solid #ccc;
    display: flex;
    justify-content: space-between;
    font-size: 14px;
    color: #666;
    font-weight: 400;
    margin-bottom: 10px;
    padding-bottom: 10px;

    .forgot-password {
      cursor: pointer;
    }

    .sign-in,
    .sign-up {
      cursor: pointer;
      color: #409EFF;
      font-weight: 600;
    }
  }

  .lang {
    margin-top: 20px;
    text-align: center;
    color: #666;

    span {
      cursor: pointer;
      margin: 10px;
      font-size: 14px;
    }

    span.active {
      font-weight: 600;
      text-decoration: underline;
    }

    span:hover {
      text-decoration: underline;
    }
  }

  .documentation {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: #409eff;
    font-weight: bolder;

    &:hover {
      text-decoration: underline;
    }
  }

  .mobile-warning {
    margin-top: 20px;
  }

}
</style>
<style scoped>
.mobile-warning >>> .el-alert .el-alert__description {
  font-size: 1.2rem;
}

#canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
}
</style>
