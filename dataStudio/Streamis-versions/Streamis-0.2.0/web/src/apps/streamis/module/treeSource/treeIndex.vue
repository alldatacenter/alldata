<template>
  <!--数据源内容(动态面板): Source表左侧数据源Sink表左侧 -->
  <div class="left-container">
    <!--搜索 -->
    <div class="search">
      <!--搜索---文本框 -->
      <div class="search-text">
        <Input v-model="search" :border="false" placeholder="搜索" />
      </div>
      <!--搜索---图标 -->
      <div class="search-icon">
        <Icon type="md-sync" />
      </div>
    </div>
    <!--数据源下拉选择框 -->
    <div class="select">
      <div class="select-type">
        <SvgIcon class="dataSourceType-icon" :icon-class="dataTypeIcon" />
        <Select v-model="dataSourceType" style="width: 70px" :disabled="!!node">
          <Option v-for="item in typeList" :value="item.name" :key="item.id">{{ item.name }}</Option>
        </Select>
      </div>
      <!--集群下拉选择框 -->
      <div class="select-colony">
        <SvgIcon class="colony-icon" icon-class="colony" />
        <Select
          v-model="colonyType"
          style="width: 70px; margin-left: 5px"
          @on-change="changeSecList"
        >
          <Option
            v-for="item in clusterList"
            :value="item.dataSourceName"
            :key="item.id"
          >{{ item.dataSourceName }}</Option>
        </Select>
      </div>
    </div>
    <!--数据源内容(库表模式) -->
    <div class="libtable">
      <Spin v-if="!tableList.length" size="large" fix />
      <tree-list
        class="we-side-bar-content v-hivedb-list"
        :data="tableList"
        :filter-text="search"
        :open-node="openNode"
        :loading="isPending"
        @we-click="onClick"
      />
    </div>
  </div>
</template>

<script>
import util from '@/common/util'
import TreeList from './tree'
import api from '@/common/service/api'
export default {
  props: {
    node: {
      type: Object
    },
    expands: {
      type: Object
    },
    checkChange: Function
  },
  components: {
    TreeList
  },
  data() {
    return {
      dataTypeIcon: '',
      search: '',
      colonyId: '',
      //默认选中kafka 随着工作流传来的页面而改变
      dataSourceType: '',
      colonyType: '',
      clusterList: [],
      typeList: [],
      tableList: [],
      isPending: false,
      openNode: {}
    }
  },
  mounted() {
    //获取数据源类型
    this.getDataSourceType()
  },
  created() {},
  watch: {
    colonyId: {
      handler(newValue, oldValue) {
        if (oldValue != '') {
          this.getDbs()
        }
      }
    }
  },
  methods: {
    getDataSourceType() {
      this.dataSourceType = this.node.type.split('.')[2]
      api.fetch('streamis/dataSourceType', 'post').then(res => {
        this.typeList = res.dataSourceTypes
        this.typeList.forEach(item => {
          if (item.name === this.dataSourceType) {
            this.dataSourceTypeId = item.id
            //获取集群信息
            this.getColonyType()
          }
        })
      })
      // 根据数据源类型的名字找到对应的id
      // 需要循环数据源类型数组 是哪一个类型就显示哪一张图片
      if (this.dataSourceType === 'kafka') {
        this.dataTypeIcon = 'apachekafka'
      } else if (this.dataSourceType === 'mysql') {
        this.dataTypeIcon = 'mysql'
      }
    },
    getColonyType() {
      const params = {
        //数据源类型的id
        dataSourceTypeId: this.dataSourceTypeId,
        system: 'streamis',
        //传空值，不传会报错
        name: ''
      }
      api.fetch('streamis/dataSourceCluster?', params, 'post').then(res => {
        this.clusterList = res.dataSourceCluster
        //通过数据源类型的id去查找对应的集群
        this.clusterList.forEach(item => {
          if (item.dataSourceTypeId == this.dataSourceTypeId) {
            this.colonyType = item.dataSourceName
            this.colonyId = item.id
            this.getDbs()
          }
        })
      })
    },
    getDbs() {
      const params = {
        //集群的id 默认进来选中哪个集群就传哪个集群
        dataSourceId: this.colonyId,
        system: 'streamis'
      }
      api.fetch('streamis/dataBases?', params, 'post').then(res => {
        if (res && Array.isArray(res.dataBases)) {
          res.dataBases.forEach(db => {
            this.tableList.push({
              _id: util.guid(),
              name: db,
              dataType: 'db',
              iconCls: 'fi-hivedb',
              children: []
            })
          })
          if (this.expands) {
            let dbnode = this.tableList.find(item => item.name === this.expands.db)
            if (dbnode) {
              this.loadDBTable(dbnode, true)
            }
          } else {
            // 切换DB，重置
            this.$emit('currentTable', {})
          }
        }
      })
    },
    loadDBTable(item, open) {
      //发送请求 获取二级菜单
      const params = {
        //集群的id 默认进来选中哪个集群就传哪个集群
        dataSourceId: this.colonyId,
        system: 'streamis',
        dataBase: item.name
      }
      this.openNode[item._id] = !this.openNode[item._id]
      if (item.children && item.children.length) return
      if (this.isPending) {
        return this.$Message.warning(
          this.$t('message.scripts.constants.warning.api')
        )
      }
      this.isPending = true
      api.fetch('streamis/tables?', params, 'post').then(res => {
        this.isPending = false
        item.loaded = true
        const temArray = res.tables.map(table => {
          return {
            _id: util.guid(),
            dbName: item.name,
            name: table.tableName,
            value: table.tableName,
            dataType: 'tb',
            iconCls: 'fi-table',
            children: [],
            ...table
          }
        })
        if (open) {
          let tb = temArray.find(item => item.name === this.expands.table)
          if (tb) {
            this.toggleTB(tb, true)
            tb.active = true
          }
          this.openNode[item._id] = true
        }
        this.$set(item, 'children', temArray)
        this.tableList = [...this.tableList]
      })
    },
    async getTableColumns(node) {
      const params = {
        dataSourceId: this.colonyId,
        system: 'streamis',
        dataBase: node.dbName,
        table: node.name,
        dataSourceType: this.dataSourceType
      }
      if (this.isPending) {
        return this.$Message.warning(
          this.$t('message.scripts.constants.warning.api')
        )
      }
      this.isPending = true
      const res = await api.fetch('streamis/columns?', params, 'post')
      this.isPending = false
      node.loaded = true
      //把类型、集群、表名传递给父组件 传一个对象过去 然后取值
      let dataBase = {
        dataSourceType: this.dataSourceType,
        colonyType: this.colonyType,
        tableName: node.name,
        dataBase: node.dbName,
        // 额外展示的一些信息
        extraUis: res.extraUis || [],
        columns: res.columns,
        colonyId: this.colonyId
      }
      // 没有第三级字段不需要设置children，如果有则需要设置
      // const temArray = res.columns.map(col=>{
      //   return {
      //     name: col.columnName,
      //     dataType: 'field',
      //     iconCls: 'fi-field',
      //   }
      // })
      // this.$set(item, 'children', temArray)
      this.tableList = [...this.tableList]
      this.$emit('sourceConfig', dataBase)
    },
    toggleTB(node, load = false) {
      if (this.checkChange && !load) {
        this.checkChange(node, this.loadTableColumns)
      } else {
        this.loadTableColumns(node)
      }
    },
    async loadTableColumns(node) {
      this.$emit('currentTable', node)
      if (node.children && node.children.length) return
      if (this.isPending)
        return this.$Message.warning(
          this.$t('message.scripts.constants.warning.api')
        )
      await this.getTableColumns(node)
    },
    changeSecList(val) {
      this.clusterList.forEach(item => {
        if (item.dataSourceName === val) {
          this.colonyType = item.dataSourceName
          this.colonyId = item.id
          this.tableList = []
          this.getDbs()
        }
      })
    },
    onClick({ item }) {
      switch (item.dataType) {
        case 'db':
          this.loadDBTable(item)
          break
        case 'tb':
          this.toggleTB(item)
          break
        case 'field':
          break
      }
    }
  }
}
</script>

<style lang="scss">
.left-container {
  border-width: 0px;
  background-color: rgba(255, 255, 255, 1);
  background-image: none;
  font-family: 'Arial Normal', 'Arial';
  font-weight: 400;
  font-style: normal;
  margin-left: 5px;
  height: 100%;
  .search {
    display: flex;
    line-height: 40px;
    width: 230px;
    height: 40px;
    font-size: 12px;
    color: #aeaeae;
    border-bottom: 1px solid #aeaeae;
    .search-text {
      width: 206px;
      height: 40px;
    }
    .search-icon {
      font-size: 20px;
      color: #6666ff;
      cursor: pointer;
      font-weight: 700;
    }
  }
  .name-item {
    word-break: break-all;
    white-space: nowrap;
    text-overflow: ellipsis;
    width: 100%;
    overflow: hidden;
  }
  .select {
    .ivu-select .ivu-select-dropdown {
      z-index: 999;
    }
    display: flex;
    margin-top: 10px;
    .select-type {
      background-color: rgba(204, 204, 255, 1);
      border-radius: 7px;
      display: flex;
      margin-right: 8px;
      .select-type {
        width: 10px;
        height: 10px;
      }
      .dataType-img {
        width: 30px;
        height: 32px;
      }
      .dataSourceType-icon {
        font-size: 22px;
      }
    }
    .select-colony {
      border-radius: 7px;
      background-color: rgba(204, 204, 255, 1);
      display: flex;
      .colony-img {
        width: 28px;
        height: 26px;
        margin-right: 2px;
      }
      .colony-icon {
        font-size: 22px;
        margin-left: 6px;
      }
    }
    .ivu-select-single .ivu-select-selection .ivu-select-placeholder {
      color: #6b6b6b;
    }
    .ivu-select-prefix {
      font-size: 20px;
      font-weight: 400;
    }
    .ivu-select-selection {
      background-color: rgba(204, 204, 255, 1);
      border: 0px;
    }
  }
  .libtable {
    margin-top: 20px;
    width: 228px;
    // height: 464px;
    height: 100%;
    overflow: auto;
    .ivu-icon-ios-arrow-down:before {
      content: '';
    }
    .ivu-menu-vertical .ivu-menu-item,
    .ivu-menu-vertical .ivu-menu-submenu-title {
      padding: 7px 24px;
    }
    .ivu-menu-light.ivu-menu-vertical
      .ivu-menu-item-active:not(.ivu-menu-submenu):after {
      content: '';
      background: #fff;
    }
    .streamisIcon {
      color: #3300ff;
      font-size: 16px;
      margin-left: 15px;
    }
  }
  // 菜单的选中样式隐藏
  .ivu-menu-vertical.ivu-menu-light:after {
    display: none;
  }
  .ivu-select-single .ivu-select-selection .ivu-select-selected-value {
    padding-left: 0;
    color: #6b6b6b;
  }
}
</style>
