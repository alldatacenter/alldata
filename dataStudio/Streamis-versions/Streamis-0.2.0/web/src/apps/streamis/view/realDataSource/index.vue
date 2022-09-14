<template>
  <div class="dataSource">
    <div class="container">
      <!--左边树 -->
      <div class="leftContainer">
        <treeSource
          :node="node"
          :checkChange="checkChange"
          @sourceConfig="getDataSourceConfig"
          @currentTable="currentTableChange"
        />
      </div>
      <div class="rightContainer">
        <template v-if="!currentTable.name">
          <dataSourceInit />
        </template>
        <template v-if="currentTable.name && !isShowSql">
          <div class="right-container">
            <div class="panel-table-data">
              <div class="panel-color">
                <img class="icon" src="../../assets/images/u2618.png" />
                <div class="text">
                  <span>数据源</span>
                </div>
                <img src="../../assets/images/u2616.png" />
              </div>
              <div class="panel-pg"></div>
            </div>
            <div class="datasource">
              <div class="data-title">
                <span style="color: #000000">数据源</span>
                <span style="color: #ff0000">*</span>
              </div>
              <div class="data-text">
                <span>{{ dataBase.dataSourceType }} - {{ dataBase.colonyType }}：</span>
                <span>{{ dataBase.dataBase+'.'+dataBase.tableName }}</span>
              </div>
              <div class="data-text" v-if="showInput">
                <span style="margin-right: 4px">{{ extraUisLable }}:</span>
                <Input v-model="extraUisName" @on-change="changeExtraUI" placeholder="请输入消费组名" style="width: 200px" />
              </div>
            </div>
            <div class="panel-table-data">
              <div class="panel-color">
                <img class="icon" src="../../assets/images/u2618.png" />
                <div class="text">
                  <span>表字段</span>
                </div>
                <img src="../../assets/images/u2616.png" />
              </div>
              <div class="panel-pg"></div>
            </div>
            <!-- 把二级菜单的id值传入给表格去发送请求 -->
            <tableFieldsList :data="tableDatas" :tableMetaId="currentTable.streamisTableMetaId" @change="onModifyFields" />
            <div class="panel-table-data">
              <div class="panel-color">
                <img class="icon" src="../../assets/images/u2618.png" />
                <div class="text">
                  <span>表信息</span>
                </div>
                <img src="../../assets/images/u2616.png" />
              </div>
              <div class="panel-pg"></div>
            </div>
            <tableInfo
              :formData.sync="formData" 
              @change="onChangeInfo" 
              @saveDataSource="addStreamis"
              @cancelDataSource="cancelDataSource"/>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
<script>
import api from '@/common/service/api'
import treeSource from '@/apps/streamis/module/treeSource/treeIndex'
import dataSourceInit from '@/apps/streamis/module/dataSourceInit'
import tableFieldsList from '@/apps/streamis/module/tableFieldsList'
import tableInfo from '@/apps/streamis/module/tableInfoRealTime'
export default {
  // 接收工作流传来的节点数据
  props: ['node','data'],
  components: {
    tableFieldsList: tableFieldsList.component,
    tableInfo: tableInfo,
    treeSource: treeSource,
    dataSourceInit: dataSourceInit
  },
  data() {
    return {
      changeExtraUisName: false,
      changeTableInfo: false,
      changeFieldList: false,
      showInput: '',
      currentTable: {},
      // 用户自己填的东西
      extraUisName: '',
      extraUisLable: '',
      // 字段信息的值
      fieldsList: [],
      tableDatas: [],
      isShowSql: false,
      formData: {
        nodeName: '',
        tableName: '',
        alias: '',
        tags: '',
        description: '',
        id: '',
        grantUser: '',
        scope: ''
      },
      dataBase: '',
      sqltext: ''
    }
  },
  mounted() {},
  watch: {
  },
  methods: {
    //保存表信息和字段信息
    addStreamis() {
      if (!this.extraUisName) {
        this.$Message.warning('请输入消费组名')
        return
      }
      // 未更改
      if (
        !this.changeFieldList &&
        !this.changeExtraUisName &&
        !this.changeTableInfo
      ) {
        return
      }
      if (
        !this.formData.tableName ||
        !this.formData.scope ||
        !this.formData.layer
      ) {
        this.$Message.warning('表名、作用域、所属分层不能为空')
        return
      }
      if (this.fieldsList) {
        this.fieldsList = this.fieldsList.filter(item => item.fieldName)
      }
      this.formData.nodeName = this.dataBase.dataBase
      this.formData.name = this.currentTable.name
      this.formData.tags = this.formData.tags || ''
      this.formData.linkisDatasourceName = this.dataBase.dataBase+'.'+this.dataBase.tableName
      if (this.data.processData){
        this.formData.workspaceName = this.data.processData.workspaceName
        this.formData.workspaceId = this.data.processData.workspaceId
      }
      let params = {
        streamisTableMeta: this.formData,
        streamisTableFields: this.fieldsList,
        streamisExtraInfo: [
          {
            key: this.extraUis.key,
            value: this.extraUisName,
            streamisTableMetaId: this.currentTable.streamisTableMetaId
          }
        ],
        streamisDatasourceAuthority: {
          authorityId: 11,//(工程id/任务id)
          grantUser: this.formData.grantUser
        }
      }
      api.fetch('streamis/save', params, 'post').then(res => {
        if (res) {
          if(this.node) {
            this.node.jobContent = {...this.node.jobContent, datasourceId: res.streamisTableMetaId}
          }
          this.$emit(
            'save',
            {},
            {
              ...this.node
            }
          )
          this.changeFieldList = false
          this.changeTableInfo = false
          this.changeExtraUisName = false
          this.$Message.success('保存成功')
          this.currentTable.streamisDataSource = true
          this.currentTable.streamisTableMetaId = res.streamisTableMetaId
        } else {
          this.$Message.error('保存失败')
        }
      })
    },
    changSql() {
      this.isShowSql = true
      //像后端发送请求 翻译成sql
      const params = {
        streamisTableMetaId: this.currentTable.streamisTableMetaId,
        dataSourceId: this.dataBase.colonyId,
        nodeName: this.currentTable.name,
        streamisExtraInfo: this.streamisExtraInfo
      }
      api.fetch('streamis/transfer', params, 'post').then(res => {
        if (res.streamisDataSourceCode) {
          this.sqltext = res.streamisDataSourceCode.executionCode
        }
      })
    },
    backGraph() {
      this.isShowSql = false
    },
    /**
     * 检查是否修改了当前表数据源信息未保存,如不保存则继续切换表，所填信息丢弃
     */
    checkChange(tableNode, cb) {
      if (
        this.changeFieldList ||
        this.changeTableInfo ||
        this.changeExtraUisName
      ) {
        this.$Modal.confirm({
          title: '提示',
          content: '修改暂未保存，是否保存?',
          okText: '保存',
          cancelText: '不保存',
          onOk: () => {
            this.addStreamis()
          },
          onCancel: () => {
            if (cb) {
              cb(tableNode)
            }
            this.changeFieldList = false
            this.changeTableInfo = false
            this.changeExtraUisName = false
          }
        })
      } else {
        if (cb) {
          cb(tableNode)
        }
      }
    },
    currentTableChange(tableNode) {
      // 切换表，重置初始值
      this.currentTable = tableNode
      this.extraUisName = ''
      this.formData = {
        nodeName: '',
        tableName: '',
        alias: '',
        tags: '',
        description: '',
        id: ''
      }
      this.fieldsList = []
      this.tableDatas = [{}]
      this.changeFieldList = false
      this.changeTableInfo = false
      this.changeExtraUisName = false
      this.getFieldsList()
      this.isShowSql = false
    },

    getDataSourceConfig(dataBase = {}) {
      if (Array.isArray(dataBase.extraUis) && dataBase.extraUis.length) {
        this.showInput = dataBase.extraUis[0].id
        this.dataBase = dataBase
        this.extraUisLable = dataBase.extraUis[0].lableName
        this.extraUis = dataBase.extraUis[0]
      }
    },
    onModifyFields(change, list) {
      this.changeFieldList = change
      this.fieldsList = list
    },
    changeExtraUI() {
      this.changeExtraUisName = this.extraUisName !== this.getExtraUiName();
    },
    getChangeFieldList(val) {
      if (val === true) {
        this.changeFieldList = val
      }
    },
    onChangeInfo(hasChange) {
      this.changeTableInfo = hasChange
    },
    getFieldsList() {
      if (this.currentTable.streamisDataSource && this.currentTable.streamisTableMetaId) {
        api
          .fetch('streamis/streamisTableMetaInfo/' + this.currentTable.streamisTableMetaId, 'get')
          .then(res => {
            if (res) {
              const datas = res.streamisDatasourceFields || []
              this.fieldsList = datas
              this.tableDatas = [{}, ...datas]
              this.formData = res.streamisTableMeta
              this.changeTableInfo = false
              let streamisExtraInfo = res.streamisExtraInfo || []
              this.streamisExtraInfo = streamisExtraInfo
              this.extraUisName = this.getExtraUiName()
            }
          })
          .catch(e => console.log(e))
      } else {
        this.tableDatas = [{}]
      }
    },
    getExtraUiName() {
      if (this.streamisExtraInfo && this.streamisExtraInfo.length !== 0) {
        return this.streamisExtraInfo[0].value
      } else {
        return ''
      }
    },
    cancelDataSource(){
      this.currentTable = {}
    }
  }
}
</script>
<style lang="scss" scoped>
.dataSource {
  .container {
    display: flex;
    width: 100%;
    height: 100vh;

    .leftContainer {
      flex-shrink: 1;
      height: 100%;
    }

    .rightContainer {
      flex-shrink: 1;
      width: 100%;
      height: 100%;

      .designer-toolbar {
        height: 40px;
        padding-left: 10px;
        background: #f7f7f7;
        color: #000;
        border: 1px solid #d7dde4;

        .button {
          float: left;
          margin: 6px 5px 6px 0;
          padding: 0;
          height: 18px;
          text-align: left;
          border: 1px solid transparent;
          border-radius: 2px;
          cursor: pointer;

          svg {
            margin-right: 5px;
            color: #666;
          }

          span {
            vertical-align: middle;
            color: #666;
          }

          .icon {
            display: inline-block;
            vertical-align: text-top;
            width: 18px;
            height: 18px;
            color: #333;
            text-align: center;
          }
        }

        .devider {
          float: left;
          border-left: 1px solid #e3e8ee;
          height: 18px;
          margin: 8px 5px;
        }
      }

      .sqlInput {
        width: 100%;
        height: 329px;
        font-family: 'Arial Normal', 'Arial';
        font-weight: 400;
        font-style: normal;
        font-size: 13px;
        text-decoration: none;
        color: #000000;
        text-align: left;
        margin-left: -11px;
        margin-top: 5px;
        padding: 20px;
        box-sizing: border-box;
      }

      .right-container {
        border-width: 0px;
        border-left: 1px solid #aeaeae;
        height: 100%;

        .panel-table-data {
          display: flex;

          .panel-pg {
            width: calc(100vw - 230px);
            height: 33px;
            background-color: rgba(107, 107, 107, 1);
            margin-left: -16px;
          }

          .panel-color {
            position: relative;
            width: 123px;
            height: 33px;
            z-index: 1;

            .icon {
              position: absolute;
              top: 4px;
              left: 18px;
              width: 25px;
              height: 25px;
            }

            .text {
              position: absolute;
              left: 49px;
              top: 5px;
              margin-left: 7px;

              span {
                font-weight: 700;
                font-style: normal;
                font-size: 16px;
                color: #ffffff;
              }
            }
          }
        }

        .datasource {
          display: flex;
          padding: 18px 0px 25px 48px;

          .data-title {
            width: 48px;
            white-space: nowrap;
            line-height: 38px;
          }

          .data-text {
            width: auto;
            height: 41px;
            background: inherit;
            background-color: rgba(204, 204, 255, 1);
            border: none;
            border-radius: 5px;
            box-shadow: none;
            padding-left: 4px;
            padding-right: 4px;
            margin-left: 84px;
            font-size: 13px;
            color: #0033cc;
            line-height: 41px;
            text-align: center;

            span:nth-child(2) {
              margin-left: 9px;
            }
          }
        }
      }
    }
  }
}
</style>
