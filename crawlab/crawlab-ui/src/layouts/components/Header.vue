<template>
  <div :class="sidebarCollapsed ? 'collapsed' : ''" class="header-container">
    <el-header height="var(--cl-header-height)" class="header">
      <div class="left">
      </div>
      <div class="right">
        <el-dropdown class="lang">
          <span class="el-dropdown-link item action ">
            <font-awesome-icon class="icon" :icon="['fa', 'globe']"/>
            {{ langName }}
            <el-icon class="el-icon--right">
              <arrow-down/>
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-track="{
                  code: 'click_header_lang',
                  params: {locale: 'en'}
                }"
                :class="locale === 'en' ? 'active' : ''"
                @click="() => setLang('en')"
              >
                {{ t('global.lang', [], {locale: 'en'}) }}
              </el-dropdown-item>
              <el-dropdown-item
                v-track="{
                  code: 'click_header_lang',
                  params: {locale: 'zh'}
                }"
                :class="locale === 'zh' ? 'active' : ''"
                @click="() => setLang('zh')"
              >
                {{ t('global.lang', [], {locale: 'zh'}) }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown
          v-locate="'me'"
          class="me"
        >
          <span class="el-dropdown-link item action">
            <font-awesome-icon class="icon" :icon="['far', 'user']"/>
            {{ username }}
            <el-icon class="el-icon--right">
              <arrow-down/>
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-track="{code: 'click_header_disclaimer'}"
                @click="onClickDisclaimer"
              >
                {{ t('layouts.components.header.disclaimer') }}
              </el-dropdown-item>
              <el-dropdown-item
                v-track="{code: 'click_header_my_settings'}"
                @click="onClickMySettings"
              >
                {{ t('layouts.components.header.mySettings') }}
              </el-dropdown-item>
              <el-dropdown-item
                v-track="{code: 'click_header_logout'}"
                @click="onLogout"
              >
                <span v-locate="'logout'">{{ t('layouts.components.header.logout') }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {useStore} from 'vuex';
import {useRouter} from 'vue-router';
import {useI18n} from 'vue-i18n';
import {setGlobalLang} from '@/utils/i18n';
import {ArrowDown} from '@element-plus/icons';

export default defineComponent({
  name: 'Header',
  components: {
    ArrowDown,
  },
  setup() {
    // i18n
    const {t, locale} = useI18n();

    // router
    const router = useRouter();

    // store
    const store = useStore();

    // store states
    const {
      layout: layoutState,
    } = store.state as RootStoreState;

    // whether side is collapsed
    const sidebarCollapsed = computed(() => {
      return layoutState.sidebarCollapsed;
    });

    // language name
    const langName = computed<string>(() => {
      return t('global.lang', [], {locale: locale.value});
    });

    // set language
    const setLang = (lang: Lang) => {
      setGlobalLang(lang);
      store.commit('common/setLang', lang);
    };

    // current user's username
    const username = computed<string | undefined>(() => {
      const me = store.getters['user/me'] as User | undefined;
      if (!me) return;
      return me.username;
    });

    // on logout hook
    const onLogout = () => {
      setTimeout(() => {
        // clear token
        localStorage.removeItem('token');

        // clear me
        store.commit('user/resetMe');

        // navigate to login page
        router.push('/login');
      }, 10);
    };

    // on click disclaimer
    const onClickDisclaimer = () => {
      router.push('/misc/disclaimer');
    };

    // on click my settings
    const onClickMySettings = () => {
      router.push('/misc/my-settings');
    };

    return {
      sidebarCollapsed,
      locale,
      langName,
      username,
      setLang,
      onLogout,
      onClickDisclaimer,
      onClickMySettings,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.header-container {
  height: var(--cl-header-height);
  width: calc(100vw - var(--cl-sidebar-width));
  background-color: var(--cl-header-bg);
  transition: width var(--cl-sidebar-collapse-transition-duration);

  &.collapsed {
    width: calc(100vw - var(--cl-sidebar-width-collapsed));
  }

  .header {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-left: none;
    border-bottom: 1px solid var(--cl-header-border-color);

    .left {
      display: flex;
      align-items: center;
    }

    .right {
      display: flex;
      align-items: center;

      .item {
        margin-left: 20px;
        display: flex;
        align-items: center;

        &.action {
          cursor: pointer;
        }

        .icon {
          margin-right: 6px;
        }
      }
    }
  }
}
</style>

<style scoped>
.el-dropdown-menu__item.active {
  background: #ecf5ff;
  color: #409eff;
}
</style>
