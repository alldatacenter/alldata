<template>
  <div>
    <div class="itemWrap">
      <p>{{ $t('message.streamis.jobListTableColumns.jobType') }}:</p>
      <div>
        {{ jobType }}
      </div>
    </div>
    <div class="itemWrap">
      <p>{{ $t('message.streamis.jobSummary.realTimeTraffic') }}:</p>
      <div class="realTimeTraffic">
        <div
          class="trafficItem"
          v-for="(item, index) in realTimeTraffic"
          :key="index"
          :style="{ 'margin-top': index > 0 ? '5px' : 0 }"
        >
          <div class="trafficName" style="margin-left:0;">
            <img src="../../assets/images/u4002.png" />
            <p>{{ item.sourceKey }}</p>
          </div>
          <div class="trafficName">
            <img src="../../assets/images/u4002.png" />
            <p>{{ item.transformKey }}</p>
          </div>
          <div class="trafficName">
            <img src="../../assets/images/u4002.png" />
            <p>{{ item.sinkKey }}</p>
          </div>
          <div class="trafficSpeed speedOne">
            <img src="../../assets/images/u1909.png" />
            <p>{{ item.sourceSpeed }}</p>
          </div>
          <div class="trafficSpeed speedTwo">
            <img src="../../assets/images/u1909.png" />
            <p>{{ item.sinkSpeed }}</p>
          </div>
        </div>
      </div>
    </div>
    <div class="itemWrap">
      <p>{{ $t('message.streamis.jobSummary.dataNumber') }}:</p>
      <div class="dataNumber">
        <div class="dataWrap">
          <div
            v-for="(item, index) in dataNumber"
            :key="index"
            :style="{ 'margin-top': index > 0 ? '5px' : 0 }"
          >
            {{ item.dataName }}
          </div>
        </div>
        <div class="dataWrap">
          <div
            v-for="(item, index) in dataNumber"
            :key="index"
            class="number"
            :style="{ 'margin-top': index > 0 ? '5px' : 0 }"
          >
            {{ item.dataNumber }}
          </div>
        </div>
        <div class="dataWrap">
          <div
            v-for="(item, index) in dataNumber"
            :key="index"
            :style="{ 'margin-top': index > 0 ? '5px' : 0 }"
          >
            {{ $t('message.streamis.jobSummary.unit') }}
          </div>
        </div>
      </div>
    </div>
    <div class="itemWrap">
      <p>{{ $t('message.streamis.jobSummary.loadCondition') }}:</p>
      <Table :columns="loadColumns" :data="loadCondition" border>
        <template slot-scope="{ row }" slot="memoryUse">
          <div class="memory">
            <div
              :style="{
                width: row.memoryPercent + '%',
                background:
                  row.memoryPercent > 50
                    ? 'rgba(255, 204, 51, 1)'
                    : 'rgba(0, 128, 0, 1)'
              }"
            >
              {{ row.memoryUse }}
            </div>
          </div>
        </template>
      </Table>
    </div>
  </div>
</template>
<script>
import api from '@/common/service/api'
export default {
  data() {
    return {
      loadColumns: [
        {
          title: this.$t('message.streamis.jobSummary.loadColumns.type'),
          key: 'type'
        },
        {
          title: this.$t('message.streamis.jobSummary.loadColumns.host'),
          key: 'host'
        },
        {
          title: this.$t('message.streamis.jobSummary.loadColumns.memoryUse'),
          key: 'memoryUse',
          slot: 'memoryUse',
          minWidth: 160
        },
        {
          title: this.$t('message.streamis.jobSummary.loadColumns.gcTotalTime'),
          key: 'gcTotalTime'
        },
        {
          title: this.$t(
            'message.streamis.jobSummary.loadColumns.gcLastConsume'
          ),
          key: 'gcLastConsume'
        },
        {
          title: this.$t('message.streamis.jobSummary.loadColumns.gcLastTime'),
          key: 'gcLastTime'
        }
      ],
      loadCondition: [],
      dataNumber: [],
      realTimeTraffic: [],
      jobType: this.$route.params.jobType
    }
  },
  mounted() {
    console.log(this.$route.params)
    this.getDatas()
  },
  methods: {
    getDatas() {
      const { id, version } = this.$route.params || {}

      api
        .fetch(
          `streamis/streamJobManager/job/details?jobId=${id}&version=${version}`,
          'get'
        )
        .then(res => {
          console.log(res)
          if (res && res.details) {
            const conditions = res.details.loadCondition || []
            this.loadCondition = conditions.map(item => {
              item.memoryUse = item.memory + ' / ' + item.totalMemory + ' G'
              item.memoryPercent = Math.ceil(
                (item.memory * 100) / item.totalMemory
              )
              return item
            })
            this.dataNumber = res.details.dataNumber
            this.realTimeTraffic = [...res.details.realTimeTraffic]
            this.$emit('links-info', res.details.linkisJobInfo)
          }
        })
        .catch(e => console.log(e))
    },
    showVersionInfo(row) {
      console.log(row)
    },
    showDetail(row) {
      console.log(row)
    },
    showLogs(row) {
      console.log(row)
    }
  }
}
</script>
<style lang="scss" scoped>
.itemWrap {
  display: flex;
  margin-top: 30px;
  & > p {
    width: 120px;
  }
  & > div {
    flex: 1;
  }
}
.dataWrap > div {
  display: flex;
  height: 30px;
  justify-content: flex-start;
  align-items: center;
}
.dataWrap:first-child {
  margin-top: 0px;
}
.dataNumber {
  display: flex;
}
.number {
  margin-left: 20px;
  background: rgb(0, 128, 0);
  padding-left: 10px;
  padding-right: 10px;
  margin-right: 5px;
  color: #fff;
}
.memory {
  display: flex;
  width: 100%;
  height: 100%;
  background: rgba(215, 215, 215, 1);
  border-radius: 3px;
  position: relative;
  overflow: hidden;
  & > div {
    display: flex;
    justify-content: flex-end;
    color: #fff;
    padding-right: 5px;
  }
}
.trafficItem {
  position: relative;
  height: 80px;
  display: flex;
}
.trafficName {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  width: 100px;
  padding-top: 5px;
  margin-left: 75px;
  & p {
    margin-top: 10px;
  }
}
.trafficSpeed {
  position: absolute;
  top: 0;
  & img {
    margin-top: 5px;
  }
  & p {
    position: absolute;
    top: 0px;
    width: calc(100% - 15px);
    display: flex;
    justify-content: center;
    align-items: center;
  }
}
.speedOne {
  left: 61px;
}
.speedTwo {
  left: 236px;
}
</style>
