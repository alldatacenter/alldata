<template>
  <div class="app-container">
    <el-alert :closable="false" title="三级菜单1" type="success" />
    <el-form label-width="170px" style="margin-top: 20px">
      <el-form-item label="三级菜单缓存功能测试区">
        <el-input v-model="input" placeholder="请输入内容" style="width: 360px;" />
      </el-form-item>
    </el-form>
    <div>
      <blockquote class="my-blockquote"> 三级菜单缓存配置教程</blockquote>
      <pre class="my-code">
    1、支持元数据管理
    2、支持数据质量
    3、支持数据服务
    4、支持数据标准
    5、支持数据资产
    6、支持BI报表
    7、支持工作流编排
 </pre>
    </div>
  </div>
</template>
<script>
export default {
  name: 'Test',
  data() {
    return {
      input: ''
    }
  }
}
</script>
<style scoped>
  .my-code a{
    color:#009688;
  }
</style>
