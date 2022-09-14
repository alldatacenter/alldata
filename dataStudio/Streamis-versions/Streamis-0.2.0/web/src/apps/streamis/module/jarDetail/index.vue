<template>
  <div>
    <div class="itemWrap" v-if="!isSql">
      <p>{{ $t('message.streamis.jobDetail.flinkJarPac') }}</p>
      <div>
        <Table :columns="columns" :data="jarData.mainClassJar || []" border>
          <template slot-scope="{ row }" slot="operation">
            <div>
              <a
                :href="
                  `/api/rest_j/v1/streamis/streamProjectManager/project/files/download?id=${row.id}&projectName=${jarData.projectName || ''}`
                "
                download
              >
                <Button
                  type="primary"
                  style="width:55px;height:22px;background:rgba(22, 155, 213, 1);margin-right: 5px"
                >
                  {{ $t('message.streamis.projectFile.download') }}
                </Button>
              </a>
            </div>
          </template>
        </Table>
      </div>
    </div>
    <div class="itemWrap" v-if="isSql">
      <p>{{ $t('message.streamis.jobDetail.sqlContent') }}</p>
      <div class="sql">{{ jarData.sql }}</div>
    </div>
    <div class="itemWrap" v-if="!isSql">
      <p>Program Arguement</p>
      <div class="programArguement">{{ jarData.args }}</div>
    </div>
    <div class="itemWrap" v-if="!isSql">
      <p>{{ $t('message.streamis.jobDetail.dependJarPac') }}</p>
      <div>
        <Table
          :columns="columns.filter(item => item.key !== 'mainClass')"
          :data="jarData.dependencyJars || []"
          border
        >
          <template slot-scope="{ row }" slot="operation">
            <div>
              <a
                :href="
                  `/api/rest_j/v1/streamis/streamProjectManager/project/files/download?id=${row.id}&projectName=${jarData.projectName || ''}`
                "
                download
              >
                <Button
                  type="primary"
                  style="width:55px;height:22px;background:rgba(22, 155, 213, 1);margin-right: 5px"
                >
                  {{ $t('message.streamis.projectFile.download') }}
                </Button>
              </a>
            </div>
          </template>
        </Table>
      </div>
    </div>
    <div class="itemWrap" v-if="!isSql">
      <p>{{ $t('message.streamis.jobDetail.userResource') }}</p>
      <div>
        <Table
          :columns="columns.filter(item => item.key !== 'mainClass')"
          :data="jarData.resources || []"
          border
        >
          <template slot-scope="{ row }" slot="operation">
            <div>
              <a
                :href="
                  `/api/rest_j/v1/streamis/streamProjectManager/project/files/download?id=${row.id}&projectName=${jarData.projectName || ''}`
                "
                download
              >
                <Button
                  type="primary"
                  style="width:55px;height:22px;background:rgba(22, 155, 213, 1);margin-right: 5px"
                >
                  {{ $t('message.streamis.projectFile.download') }}
                </Button>
              </a>
            </div>
          </template>
        </Table>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    jarData: Object,
    isSql: Boolean
  },
  data() {
    return {
      columns: [
        {
          title: 'id',
          key: 'id'
        },
        {
          title: this.$t('message.streamis.jobDetail.columns.name'),
          key: 'fileName'
        },
        {
          title: this.$t('message.streamis.jobDetail.columns.version'),
          key: 'version'
        },
        {
          title: this.$t(
            'message.streamis.jobDetail.columns.versionDescription'
          ),
          key: 'comment'
        },
        {
          title: "Main Class",
          key: "mainClass"
        },
        {
          title: this.$t(
            'message.streamis.jobDetail.columns.versionUploadTime'
          ),
          key: 'createTime'
        },
        {
          title: this.$t('message.streamis.jobDetail.columns.operation'),
          key: 'operation',
          slot: 'operation'
        }
      ],
      projectName: this.$route.params.projectName
    }
  }
}
</script>
<style lang="scss" scoped>
.itemWrap {
  padding: 10px;
  & > p {
    font-weight: 700;
    font-size: 16px;
  }
  & > div {
    margin-left: 20px;
    margin-top: 10px;
  }
}
.programArguement {
  background: rgba(94, 94, 94, 1);
  color: #fff;
  padding: 10px 20px;
  min-height: 64px;
}
.sql {
  background: #f8f8f9;
  padding: 10px 20px;
  min-height: 64px;
}
</style>
