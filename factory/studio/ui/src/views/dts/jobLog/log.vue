<template>
  <div>
    <div style="background-color: #304156; padding:10px 0; text-align:right;"><el-button type="primary" style="margin-right:20px;" @click="loadLog">刷新日志</el-button></div>
    <div class="log-container">
      <pre :loading="logLoading" v-text="logContent" />
    </div>
  </div>
</template>
<script>
import * as log from '@/api/dts/datax-job-log'
export default {
  data() {
    return {
      logContent: '',
      logLoading: false
    }
  },
  created() {
    this.loadLog()
  },
  methods: {
    loadLog() {
      this.logLoading = true

      log.viewJobLog(this.$route.query.executorAddress, this.$route.query.triggerTime, this.$route.query.id, this.$route.query.fromLineNum).then(response => {
        // 判断是否是 '\n'，如果是表示显示完成，不重新加载
        if (response.content.logContent === '\n') {
          // this.jobLogQuery.fromLineNum = response.toLineNum - 20;
          // 重新加载
          // setTimeout(() => {
          //   this.loadLog()
          // }, 2000);
        } else {
          this.logContent = response.content.logContent
        }
        this.logLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
  .log-container {
    background: #f5f5f5;
    height: 500px;
    overflow: scroll;
    margin:20px;
    border:1px solid #ddd;

    pre {
      display: block;
      padding: 10px;
      margin: 0 0 10.5px;
      word-break: break-all;
      word-wrap: break-word;
      color: #334851;
      background-color: #f5f5f5;
      // border: 1px solid #ccd1d3;
      border-radius: 1px;
    }
  }
</style>
