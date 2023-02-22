<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-02 15:14:56
 * @FilePath: \ddh-ui\src\layouts\footer\PageFooter.vue
-->
<template>
  <div class="footer">
    <div class="copyright">
     © 2022 DataSophon
    </div>
  </div>
</template>

<script>
export default {
  name: 'PageFooter',
  props: ['copyright', 'linkList']
}
</script>

<style lang="less" scoped>
  .footer{
    height: 78px;
    line-height: 78px;
    background: #EFF3FA;
    text-align: center;
    .copyright{
      color: @text-color-second;
      font-size: 14px;
      i {
          margin: 0 4px;
      }
    }
    .links{
      margin-bottom: 8px;
      a:not(:last-child) {
        margin-right: 40px;
      }
      a{
        color: @text-color-second;
        -webkit-transition: all .3s;
        transition: all .3s;
      }
    }
  }
</style>
