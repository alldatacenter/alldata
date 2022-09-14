<template>
  <div class="container">
    <div class="navWrap">
      <div @click="jumpToCenter()" class="center">
        <Icon type="ios-home" size="20" />
        <span>{{ $t('message.streamis.routerName.realTimeJobCenter') }}</span>
      </div>
      <div class="slash">/</div>
      <div class="name">
        {{ $t('message.streamis.routerName.projectResourceFiles') }}
      </div>
    </div>
    <titleCard :title="$t('message.streamis.projectFile.fileList')">
      <div style="padding-bottom: 30px;">
        <div>
          <Form ref="queryForm" inline>
            <FormItem>
              <Input
                search
                v-model="query.filename"
                :placeholder="$t('message.streamis.projectFile.fileName')"
                @on-click="handleNameQuery"
                @on-enter="handleNameQuery"
              >
              </Input>
            </FormItem>
            <FormItem
              :label="$t('message.streamis.projectFile.createBy')"
              :label-width="120"
            >
              <Select
                v-model="query.username"
                class="select"
                style="width:120px"
              >
                <Option
                  v-for="(item, index) in createBys"
                  :value="item"
                  :key="index"
                >
                  {{
                    item === 'all'
                      ? $t('message.streamis.jobStatus.' + item)
                      : item
                  }}
                </Option>
              </Select>
            </FormItem>

            <FormItem>
              <Button
                type="primary"
                @click="handleQuery()"
                style="width:80px;height:30px;background:rgba(22, 155, 213, 1);margin-left: 80px;"
              >
                {{ $t('message.streamis.formItems.queryBtn') }}
              </Button>
            </FormItem>
          </Form>
          <Table :columns="columns" :data="tableDatas" :loading="loading">
            <template slot-scope="{ row, index }" slot="fileName">
              <div class="jobName" v-show="index === 0" @click="handleUpload()">
                <Icon type="md-add" class="upload" />
                <span>{{
                  $t('message.streamis.jobListTableColumns.upload')
                }}</span>
              </div>
              <div style="margin-left: 5px" v-show="index !== 0">
                {{ row.fileName }}
              </div>
            </template>
            <template slot-scope="{ row, index }" slot="version">
              <div v-show="index !== 0" class="versionWrap">
                <div class="version" @click="versionDetail(row)">
                  {{ row.version }}
                </div>
              </div>
            </template>
            <template slot-scope="{ row, index }" slot="operation">
              <div v-show="index !== 0">
                <a
                  :href="
                    `/api/rest_j/v1/streamis/streamProjectManager/project/files/download?id=${row.id}&projectName=${projectName}`
                  "
                  download
                >
                  <Button
                    type="primary"
                    :loading="buttonLoading && choosedRowId === row.id"
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
            :total="pageData.total"
            class="page"
            :page-size="pageData.pageSize"
            :current="pageData.pageNow"
            show-total
            show-elevator
            show-sizer
            @on-change="handlePageChange"
            @on-page-size-change="handlePageSizeChange"
          />
        </div>
      </div>
    </titleCard>
    <uploadFile
      :visible="uploadVisible"
      :projectName="projectName"
      @fileModalCancel="fileModalCancel"
      @fileUploadSuccess="fileUploadSuccess"
    />
    <fileVersionDetail
      :visible="versionVisible"
      :datas="versionDatas"
      :total="versionTotal"
      :loading="versionLoading"
      :projectName="projectName"
      @modalCancel="modalCancel"
      @refreshVersionDatas="refreshVersionDatas"
      @delelteSuccess="delelteSuccess"
    />
  </div>
</template>
<script>
import api from '@/common/service/api'
import titleCard from '@/apps/streamis/components/titleCard'
import fileVersionDetail from '@/apps/streamis/module/fileVersionDetail'
import uploadFile from '@/apps/streamis/module/uploadFile'
import moment from 'moment'

/**
 * 渲染特殊表头
 */
function renderSpecialHeader(h, params) {
  return h('div', [
    h('strong', params.column.title),
    h(
      'span',
      {
        style: {
          color: 'red'
        }
      },
      '*'
    )
  ])
}
export default {
  components: { titleCard, uploadFile, fileVersionDetail },
  data() {
    console.log(this.$route.params)
    return {
      query: {
        filename: '',
        username: 'all'
      },
      createBys: ['all'],

      tableDatas: [{}],
      columns: [
        {
          title: this.$t('message.streamis.projectFile.fileName'),
          key: 'fileName',
          slot: 'fileName',
          renderHeader: renderSpecialHeader
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.version'),
          key: 'version',
          slot: 'version',
          renderHeader: renderSpecialHeader
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.lastRelease'),
          key: 'createBy',
          renderHeader: renderSpecialHeader
        },
        {
          title: this.$t(
            'message.streamis.jobListTableColumns.lastReleaseTime'
          ),
          key: 'createTime'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.description'),
          key: 'comment'
        },
        {
          title: this.$t('message.streamis.jobListTableColumns.operation'),
          key: 'operation',
          slot: 'operation'
        }
      ],
      jobMoudleRouter: [
        'paramsConfiguration',
        'alertConfiguration',
        'runningHistory',
        'runningLogs'
      ],
      loading: false,
      buttonLoading: false,
      choosedRowId: '',
      versionVisible: false,
      versionDatas: [],
      uploadVisible: false,
      versionTotal: 0,
      versionLoading: false,
      versionData: {},
      pageData: {
        total: 0,
        pageNow: 1,
        pageSize: 10
      },
      projectName: this.$route.params.projectName
    }
  },
  mounted() {
    console.log(this.$route.params.projectName)
    this.getJobList()
    this.getUsers()
  },
  methods: {
    getJobList(isQuery) {
      if (this.loading) {
        return
      }
      this.loading = true
      const params = {
        projectName: this.projectName,
        pageNow: this.pageData.pageNow,
        pageSize: this.pageData.pageSize
      }
      let queries = Object.entries(params)
        .map(entry => entry.join('='))
        .join('&')
      if (isQuery) {
        const temp = []
        Object.keys(this.query).forEach(key => {
          const value = this.query[key]
          const query = key + '=' + value
          if (value) {
            if (key === 'username') {
              if (value !== 'all') {
                temp.push(query)
              }
            } else {
              temp.push(query)
            }
          }
        })
        if (temp.length > 0) {
          queries = queries + '&' + temp.join('&')
        }
      } else {
        this.query = {
          filename: '',
          username: 'all'
        }
      }
      api
        .fetch(
          'streamis/streamProjectManager/project/files/list?' + queries,
          'get'
        )
        .then(res => {
          console.log(res)
          this.loading = false
          if (res) {
            const datas = (res.files || []).filter(item => !!item)
            datas.forEach(item => {
              if (item && item.createTime) {
                const newDate = moment(new Date(item.createTime)).format(
                  'YYYY-MM-DD HH:mm:ss'
                )
                item.createTime = newDate
              }
            })
            datas.unshift({})
            this.tableDatas = datas
            this.loading = false
            this.pageData.total = res.totalPage
          }
        })
        .catch(e => {
          console.log(e)
          this.loading = false
        })
    },
    handleQuery() {
      this.pageData.pageNow = 1
      this.getJobList(true)
    },
    getUsers() {
      api
        .fetch('streamis/streamJobManager/config/getWorkspaceUsers', 'get')
        .then(res => {
          console.log(res)
          if (res && res.users) {
            this.createBys = ['all', ...res.users]
          }
        })
        .catch(e => console.log(e))
    },
    jumpToCenter() {
      this.$router.push({
        name: 'RealTimeJobCenter',
        query: {
          projectName: this.projectName
        }
      })
    },

    handleUpload() {
      console.log(1234444)
      this.uploadVisible = true
    },
    fileModalCancel() {
      this.uploadVisible = false
    },
    modalCancel() {
      this.versionVisible = false
    },
    fileUploadSuccess(res) {
      if (res && res.status !== 0 && res.message) {
        this.$Message.error(res.message)
      }
      this.getJobList()
    },
    jarUploadError(err, res) {
      if (res && res.status !== 0 && res.message) {
        this.$Message.error(res.message)
      }
    },
    versionDetail(data, pageData) {
      console.log(data)
      this.loading = this.versionVisible ? false : true
      this.versionLoading = true
      if (!pageData) {
        this.versionData = data
      }
      pageData = pageData || {}
      const params = {
        fileName: data.fileName,
        projectName: data.projectName,
        pageNow: pageData.pageNow || 1,
        pageSize: pageData.pageSize || 10
      }
      const queries = Object.entries(params)
        .map(entry => entry.join('='))
        .join('&')
      api
        .fetch(
          'streamis/streamProjectManager/project/files/version/list?' + queries,
          'get'
        )
        .then(res => {
          console.log(res)
          this.loading = false
          this.versionLoading = false
          if (res) {
            this.versionVisible = true
            const datas = (res.files || []).filter(item => !!item)
            datas.forEach(item => {
              if (item && item.createTime) {
                const newDate = moment(new Date(item.createTime)).format(
                  'YYYY-MM-DD HH:mm:ss'
                )
                item.createTime = newDate
              }
            })
            this.versionDatas = datas
            this.versionTotal = res.totalPage
          }
        })
        .catch(e => {
          console.log(e)
          this.loading = false
          this.versionLoading = false
        })
    },
    refreshVersionDatas(pageData) {
      this.versionDetail(this.versionData, pageData)
    },
    handlePageChange(page) {
      console.log(page)
      this.pageData.pageNow = page
      this.getJobList(true)
    },
    handlePageSizeChange(pageSize) {
      console.log(pageSize)
      this.pageData.pageSize = pageSize
      this.pageData.pageNow = 1
      this.getJobList(true)
    },
    handleDelete(rowData) {
      this.loading = true
      api
        .fetch(
          `streamis/streamProjectManager/project/files/delete?fileName=${rowData.fileName}&projectName=${rowData.projectName}`,
          'get'
        )
        .then(res => {
          console.log(res)
          this.loading = false
          this.handlePageSizeChange(this.pageData.pageSize)
        })
        .catch(e => {
          this.loading = false
          console.log(e)
        })
    },
    delelteSuccess() {
      this.getJobList(true)
    }
  }
}
</script>
<style lang="scss" scoped>
.container {
  padding: 10px 30px 0;
}
.divider {
  height: 30px;
}
.versionWrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}
.version {
  background-color: #008000;
  text-align: center;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  padding: 0px 3px;
}
.navWrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  font-size: 14px;
  height: 60px;
  .center {
    cursor: pointer;
  }
  .slash {
    padding: 0 10px;
  }
  .name {
    font-weight: 700;
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
.jobName {
  display: flex;
  cursor: pointer;
}
.more {
  font-size: 20px;
}
.upload {
  font-size: 24px;
}
.page {
  margin-top: 20px;
}
</style>
