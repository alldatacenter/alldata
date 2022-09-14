<template>
  <div>
    <Modal
      :title="$t('message.streamis.versionDetail.modalTitle')"
      v-model="visible"
      footer-hide
      width="1200"
      @on-cancel="cancel"
    >
      <Table :columns="columns" :data="datas" border>
        <template slot-scope="{ row }" slot="operation" v-if="!fromHistory">
          <div>
            <Button
              @click="showDetail(row)"
              style="background: rgba(22, 155, 213, 1);margin-right: 5px; font-size:14px;color: #fff;"
            >
              {{ $t('message.streamis.versionDetail.showDetail') }}
            </Button>
          </div>
        </template>
      </Table>
    </Modal>
  </div>
</template>
<script>
import table from '../../../../components/table/table.vue'
export default {
  components: { table },
  props: {
    visible: Boolean,
    datas: Array,
    fromHistory: Boolean,
    projectName: String
  },
  data() {
    return {
      columns: [
        {
          title: this.$t('message.streamis.versionDetail.jobId'),
          key: 'id'
        },
        {
          title: this.$t('message.streamis.versionDetail.version'),
          key: 'version'
        },
        {
          title: this.$t('message.streamis.versionDetail.description'),
          key: 'description'
        },
        {
          title: this.$t('message.streamis.versionDetail.createTime'),
          key: 'releaseTime'
        },
        {
          title: this.$t('message.streamis.versionDetail.creator'),
          key: 'createBy'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.operation'),
          key: 'operation',
          slot: 'operation'
        }
      ]
    }
  },
  methods: {
    showDetail(rowData) {
      console.log(rowData)
      this.$router.push({
        name: 'JobDetail',
        params: {
          id: rowData.id,
          module: 'jobHistory',
          name: rowData.name,
          version: rowData.version,
          status: rowData.status,
          jobType: rowData.jobType,
          isHistory: true,
          projectName: this.projectName
        }
      })
    },
    ok() {
      this.$Message.info('Clicked ok')
    },
    cancel() {
      this.$emit('modalCancel')
    }
  }
}
</script>
<style lang="scss" scoped></style>
