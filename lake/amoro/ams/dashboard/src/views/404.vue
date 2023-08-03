<template>
  <div class="g-flex-center">
    <div class="g-flex-col container">
      <p class="title">404 - Not Found</p>
      <router-link to="/" style="text-align: center">
        <a-button class="button g-mt-32">{{$t('backHome')}}</a-button>
      </router-link>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'Page404'
})
</script>

<style lang="less" scoped>
.container {
  padding-top: 100px;
  .title {
    font-size: 20px;
    font-weight: bold;
  }
  .button {
    background: #1890ff;
    border-radius: 3px;
    color: #ffffff;
  }
}
</style>
