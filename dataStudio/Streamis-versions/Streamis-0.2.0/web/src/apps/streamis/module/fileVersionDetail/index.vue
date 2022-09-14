<template>
  <div>
    <Modal
      :title="$t('message.streamis.versionDetail.modalTitle')"
      v-model="visible"
      footer-hide
      width="1200"
      @on-cancel="cancel"
    >
      <Table :columns="columns" :data="datas" :loading="versionLoading" border>
        <template slot-scope="{ row }" slot="operation">
          <div>
            <a
              :href="
                `/api/rest_j/v1/streamis/streamProjectManager/project/files/download?id=${row.id}&projectName=${projectName}`
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
            <Poptip
              confirm
              transfer
              :title="$t('message.streamis.projectFile.delelteConfirm')"
              @on-ok="() => handleDelete(row)"
            >
              <Button
                style="width:55px;height:22px;background:#ff0000;margin-right: 5px; font-size:14px;color: #fff;"
              >
                {{ $t('message.streamis.projectFile.delete') }}
              </Button></Poptip
            >
          </div>
        </template>
      </Table>
      <Page
        :total="total"
        class="page"
        :page-size="pageData.pageSize"
        :current="pageData.pageNow"
        show-total
        show-elevator
        show-sizer
        @on-change="handlePageChange"
        @on-page-size-change="handlePageSizeChange"
      />
    </Modal>
  </div>
</template>
<script>
import table from '../../../../components/table/table.vue'
import api from '@/common/service/api'
export default {
  components: { table },
  props: {
    visible: Boolean,
    datas: Array,
    versionLoading: Boolean,
    total: Number,
    projectName: String
  },
  data() {
    return {
      columns: [
        {
          title: this.$t('message.streamis.jobListTableColumns.version'),
          key: 'version'
        },
        {
          title: this.$t('message.streamis.projectFile.createBy'),
          key: 'createBy'
        },
        {
          title: this.$t('message.streamis.projectFile.versionDescription'),
          key: 'comment'
        },
        {
          title: this.$t('message.streamis.projectFile.createTime'),
          key: 'createTime'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.operation'),
          key: 'operation',
          slot: 'operation'
        }
      ],
      loading: false,
      choosedRowId: '',
      pageData: {
        total: 0,
        pageNow: 1,
        pageSize: 10
      }
    }
  },
  methods: {
    handlePageChange(page) {
      console.log(page)
      this.pageData.pageNow = page
      this.$emit('refreshVersionDatas', this.pageData)
    },
    handlePageSizeChange(pageSize) {
      console.log(pageSize)
      this.pageData.pageSize = pageSize
      this.pageData.pageNow = 1
      this.$emit('refreshVersionDatas', this.pageData)
    },
    showVersionInfo(row) {
      console.log(row)
    },
    showDetail(row) {
      console.log(row)
    },
    showLogs(row) {
      console.log(row)
    },
    ok() {
      this.$Message.info('Clicked ok')
    },
    cancel() {
      this.$emit('modalCancel')
    },
    handleDelete(rowData) {
      api
        .fetch(
          'streamis/streamProjectManager/project/files/version/delete?ids=' +
            rowData.id,
          'get'
        )
        .then(res => {
          console.log(res)
          this.handlePageSizeChange(this.pageData.pageSize)
          this.$emit('delelteSuccess')
        })
        .catch(e => {
          console.log(e)
        })
    }
  }
}
</script>
<style lang="scss" scoped>
.page {
  margin-top: 20px;
}
</style>
