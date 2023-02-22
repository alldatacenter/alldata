<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-02 16:19:05
 * @FilePath: \ddh-ui\src\layouts\CommonLayout.vue
-->
<template>
  <div class="common-layout">
    <div class="content"><slot></slot></div>
  </div>
</template>

<script>
import {mapState} from 'vuex'

export default {
  name: 'CommonLayout',
  computed: {
    ...mapState('setting', ['footerLinks', 'copyright'])
  }
}
</script>

<style scoped lang="less">
.common-layout{
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: auto;
  background-color: @layout-body-background;
  background-image: url('../assets/img/bg.png');
  background-repeat: no-repeat;
  background-size: 100% 92%;
  .content{
    padding: 32px 0;
    flex: 1;
    @media (min-width: 768px){
      padding: 28px 0 28px;
    }
  }
}
</style>
