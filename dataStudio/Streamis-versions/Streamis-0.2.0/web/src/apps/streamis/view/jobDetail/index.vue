<template>
  <div class="container">
    <div class="navWrap">
      <div @click="jumpToCenter()" class="center">
        <Icon type="ios-home" size="20" />
        <span>{{ $t('message.streamis.routerName.realTimeJobCenter') }}</span>
      </div>
      <div class="slash">/</div>
      <div class="name">{{ name }}</div>
      <div class="version">{{ version }}</div>
      <div class="statusWrap">
        <div
          class="circle"
          :style="{ borderColor: status.color }"
          v-if="!!status.name"
        ></div>
        <p :style="{ color: status.color }" v-if="!!status.name">
          {{ $t(`message.streamis.jobStatus.${status.name}`) }}
        </p>
      </div>
      <div class="linkis" v-if="!isHistory">
        <Button
          type="primary"
          @click="jumpToYarn()"
          style="height:24px;background:rgba(22, 155, 213, 1);margin-left: 15px;"
        >
          {{ $t('message.streamis.enterYarn') }}
        </Button>
      </div>
    </div>
    <div class="tabWrap">
      <Tabs type="card" v-model="choosedModule">
        <TabPane
          name="jobSummary"
          :label="$t('message.streamis.moduleName.jobSummary')"
          v-if="!isHistory"
        >
          <div class="contWrap">
            <jobSummary @links-info="linksInfo" />
          </div>
        </TabPane>
        <TabPane
          name="jobHistory"
          :label="$t('message.streamis.moduleName.jobHistory')"
        >
          <div class="contWrap">
            <jobHistory />
          </div>
        </TabPane>
        <TabPane
          name="jobConfig"
          :label="$t('message.streamis.moduleName.jobConfig')"
          v-if="!isHistory"
        >
          <div class="contWrap">
            <jobConfig />
          </div>
        </TabPane>
        <TabPane
          name="jobDetail"
          :label="$t('message.streamis.moduleName.jobDetail')"
        >
          <div class="contWrap">
            <jobDetail />
          </div>
        </TabPane>
        <TabPane
          name="jobAlert"
          :label="$t('message.streamis.moduleName.alert')"
          v-if="!isHistory"
        >
          <div class="contWrap">
            <jobAlert />
          </div>
        </TabPane>
      </Tabs>
    </div>
  </div>
</template>
<script>
import jobSummary from '@/apps/streamis/module/jobSummary'
import jobHistory from '@/apps/streamis/module/jobHistory'
import jobConfig from '@/apps/streamis/module/jobConfig'
import jobDetail from '@/apps/streamis/module/jobDetail'
import jobAlert from '@/apps/streamis/module/jobAlert'
import { allJobStatuses } from '@/apps/streamis/common/common'
import { Message } from 'view-design'
export default {
  components: {
    jobSummary,
    jobHistory: jobHistory.component,
    jobDetail: jobDetail.component,
    jobConfig: jobConfig.component,
    jobAlert: jobAlert.component
  },
  data() {
    const status = allJobStatuses.find(
      item => item.code === this.$route.params.status
    )
    console.log(this.$route.params)
    return {
      choosedModule: this.$route.params.module || 'jobSummary',
      tabs: ['jobSummary', 'jobHistory', 'jobConfig', 'jobDetail'],
      name: this.$route.params.name,
      version: this.$route.params.version,
      projectName: this.$route.params.projectName,
      status: status || {},
      isHistory: !!this.$route.params.isHistory,
      linkInfo: {},
      inIframe: true
    }
  },
  methods: {
    jumpToYarn() {
      console.log(this.linkInfo)
      if (this.linkInfo.applicationUrl) {
        window.open(this.linkInfo.applicationUrl)
      } else {
        Message.error(this.$t('message.streamis.jobDetail.urlEmpty'))
      }
    },
    jumpToCenter() {
      this.$router.push({
        name: 'RealTimeJobCenter',
        query: { projectName: this.$route.params.projectName }
      })
    },
    linksInfo(data) {
      console.log(data)
      this.linkInfo = { ...data }
    }
  }
}
</script>
<style lang="scss" scoped>
.container {
  padding: 0;
}
.divider {
  height: 30px;
}
.navWrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  font-size: 14px;
  height: 60px;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1000;
  padding: 10px 30px 0;
  background: #fff;
  .center {
    cursor: pointer;
  }
  .slash {
    padding: 0 10px;
  }
  .name {
    font-weight: 700;
  }
  .version {
    border-radius: 2px;
    padding: 0px 5px;
    background: rgba(22, 155, 213, 1);
    margin-left: 8px;
    color: #fff;
    font-size: 13px;
  }
  .statusWrap {
    display: flex;
    font-size: 12px;
    margin-left: 120px;
    .circle {
      width: 10px;
      height: 10px;
      border: 2px solid #000;
      border-radius: 5px;
      box-sizing: border-box;
      margin-top: 3px;
      margin-right: 3px;
    }
  }
}
.tabWrap {
  margin-top: 60px;
  padding: 0px 30px;
}
.contWrap {
  padding-bottom: 20px;
  height: calc(100vh - 115px);
  overflow-y: auto;
}
</style>
