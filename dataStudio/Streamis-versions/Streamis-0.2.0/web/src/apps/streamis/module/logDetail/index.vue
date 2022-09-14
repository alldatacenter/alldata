<template>
  <div>
    <Modal
      :title="$t('message.streamis.logDetail.logDetail')"
      v-model="visible"
      footer-hide
      width="1200"
      @on-cancel="cancel"
    >
      <div>
        <Form ref="queryForm" inline>
          <FormItem>
            <Select
              v-model="query.logType"
              @on-change="handleQuery"
              :placeholder="$t('message.streamis.logDetail.logTypeKeywords')"
            >
              <Option v-for="item in logTypeList" :value="item.value" :key="item.value">{{ item.label }}</Option>
            </Select>
          </FormItem>
          <FormItem>
            <Input
              search
              v-model="query.ignoreKeywords"
              :placeholder="$t('message.streamis.logDetail.ignoreKeywords')"
              @on-enter="handleQuery"
            >
            </Input>
          </FormItem>
          <FormItem>
            <Input
              search
              v-model="query.onlyKeywords"
              :placeholder="$t('message.streamis.logDetail.onlyKeywords')"
              @on-enter="handleQuery"
            >
            </Input>
          </FormItem>

          <FormItem>
            <Button
              type="primary"
              @click="handleQuery()"
              style="margin-left: 30px;"
            >
              {{ $t('message.streamis.formItems.queryBtn') }}
            </Button>
          </FormItem>
        </Form>
        <Input
          v-model="logs"
          type="textarea"
          :autosize="{ minRows: 10, maxRows: 15 }"
          readonly
          :placeholder="$t('message.streamis.logDetail.noLog')"
        />
        <div class="btnWrap">
          <Button
            type="primary"
            @click="handleMore('pre')"
            :disabled="fromLine === 1"
          >
            {{ $t('message.streamis.logDetail.pre') }}
          </Button>
          <Button
            type="primary"
            :disabled="endLine === fromLine"
            @click="handleMore('next')"
            style="margin-left: 30px;"
          >
            {{ $t('message.streamis.logDetail.next') }}
          </Button>
          <Button
            type="primary"
            @click="handleMore('more')"
            style="margin-left: 30px;"
          >
            {{ $t('message.streamis.logDetail.latestLog') }}
          </Button>
        </div>
        <Spin fix v-if="spinShow"></Spin>
      </div>
    </Modal>
  </div>
</template>
<script>
import api from '@/common/service/api'
export default {
  props: {
    visible: Boolean,
    datas: Array,
    fromHistory: Boolean,
    projectName: String,
    taskId: Number
  },
  data() {
    return {
      query: {
        ignoreKeywords: '',
        onlyKeywords: '',
        logType: 'client'
      },
      logTypeList: [{
        label: this.$t('message.streamis.logDetail.clientLabel'),
        value: 'client'
      }, {
        label: this.$t('message.streamis.logDetail.yarnLabel'),
        value: 'yarn'
      }],
      fromLine: 1,
      endLine: 0,
      logs: '',
      spinShow: false
    }
  },
  methods: {
    getDatas(taskId) {
      // const logs = new Array(1000).fill(
      //   'pps/pps/streamis/module/versionDetailtreamis/module/versionDetailpps/streamis/module/versionDetailpps/streamis/module/versionDetailpps/streamis/module/versionDetailpps/streamis/module/versionDetail'
      // )
      // this.logs = logs.join('\n')
      const { id } = this.$route.params || {}
      let queries = `?jobId=${id}&fromLine=${this.fromLine}&pageSize=100`
      if (taskId || this.taskId) queries += `&taskId=${taskId || this.taskId}`;
      Object.keys(this.query).forEach(key => {
        const value = this.query[key]
        if (value) {
          queries = `${queries}&${key}=${value}`
        }
      })
      this.spinShow = true
      api
        .fetch('streamis/streamJobManager/job/logs' + queries, 'get')
        .then(res => {
          this.spinShow = false
          if (res && res.logs) {
            if (res.logs.endLine <= this.fromLine) {
              this.fromLine = res.logs.endLine;
              this.endLine = res.logs.endLine;
            }
            this.logs = res.logs.logs.join('\n')
          } else {
            this.logs = ''
          }
        })
        .catch(e => {
          console.log(e)
          this.logs = ''
          this.spinShow = false
        })
    },
    cancel() {
      this.fromLine = 1
      this.spinShow = false
      this.query = {
        ignoreKeywords: '',
        onlyKeywords: '',
        logType: 'client'
      }
      this.$emit('modalCancel')
    },
    handleMore(type) {
      if (type === 'more') {
        this.fromLine = 1
        this.query = {
          ...this.query,
          ignoreKeywords: '',
          onlyKeywords: '',
        }
      } else if (type === 'next') {
        this.fromLine = this.fromLine + 100;
      } else {
        this.fromLine = this.fromLine > 100 ? this.fromLine - 100 : 1
      }
      this.getDatas()
    },
    handleQuery() {
      this.fromLine = 1
      this.getDatas()
    }
  }
}
</script>
<style lang="scss" scoped>
.btnWrap {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}
</style>
