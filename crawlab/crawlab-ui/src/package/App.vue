<template>
  <el-config-provider :locale="locale">
    <router-view/>
  </el-config-provider>
</template>
<script lang="ts">
import {computed, defineComponent, onBeforeMount} from 'vue';
import {useStore} from 'vuex';
import en from 'element-plus/lib/locale/lang/en';
import zh from 'element-plus/lib/locale/lang/zh-cn';
import {getI18n} from '@/i18n';

export default defineComponent({
  name: 'App',
  setup() {
    // store
    const store = useStore();

    // locale
    const locale = computed(() => {
      const lang = getI18n().global.locale.value;
      return lang === 'zh' ? zh : en;
    });

    onBeforeMount(() => {
      // system info
      store.dispatch('common/getSystemInfo');
    });

    return {
      locale,
    };
  },
});
</script>
