<template>
  <a-config-provider :locale="locale === 'en' ? enUS : zhCN">
    <router-view/>
  </a-config-provider>
</template>

<script lang="ts" setup>
import enUS from 'ant-design-vue/es/locale/en_US'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { ref } from 'vue'
import dayjs from 'dayjs'

const locale = ref(enUS.locale)

</script>

<style>
#app {
  font-family: -apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Hiragino Sans GB','Microsoft YaHei','Helvetica Neue',Helvetica,Arial,sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
  width: 100%;
}
</style>
