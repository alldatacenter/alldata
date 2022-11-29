<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="box-card tree-wrapper" shadow="always">
          <div class="body-wrapper">
            <el-tree
              ref="tree"
              :data="treeOptions"
              node-key="id"
              empty-text="加载中，请稍后"
              :props="defaultProps"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              @node-click="handleNodeClick"
            >
              <template slot-scope="{ node, data }">
                <span class="custom-tree-node">
                  <span><i v-if="node.level === 1" class="iconfont icon-zuzhi tree-folder" />{{ data.name ? node.label + '(' + data.name + ')' : node.label }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card class="box-card" shadow="always">
          <el-row>
            <el-col :span="24">
              <el-button v-hasPerm="['standard:mapping:auto']" type="primary" size="mini" @click="handleAuto">自动对照</el-button>
              <el-button v-hasPerm="['standard:mapping:manual']" type="primary" size="mini" @click="handleManual">手动对照</el-button>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-alert
                type="success"
                :closable="false"
                style="margin: 15px 0;"
              >
                <span slot="title">
                  {{ title }}
                </span>
              </el-alert>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-alert
                type="warning"
                :closable="false"
              >
                <span slot="title">
                  {{ description }}
                </span>
              </el-alert>
            </el-col>
          </el-row>
          <el-row id="jsplumbContainer" :gutter="20">
            <el-col :span="9">
              <table id="leftTable">
                <thead>
                  <tr>
                    <th>字典编码</th>
                    <th>字典名称</th>
                    <th>映射编码</th>
                    <th>映射名称</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(data, index) in leftTableDataList" :id="data.id" :key="index">
                    <td>{{ data.colCode }}</td>
                    <td>{{ data.colName }}</td>
                    <td>{{ data.contrastGbCode }}</td>
                    <td><span v-hasPerm="['standard:mapping:cancel']" v-if="data.contrastGbId" style="padding-right: 10px;" @click="handleCancel(data.id)"><i class="el-icon-circle-close" style="color: red" /></span>{{ data.contrastGbName }}</td>
                  </tr>
                </tbody>
              </table>
            </el-col>
            <el-col :span="9" :offset="6">
              <table id="rightTable">
                <thead>
                  <tr>
                    <th>标准编码</th>
                    <th>标准名称</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(data, index) in rightTableDataList" :id="data.id" :key="index">
                    <td>{{ data.gbCode }}</td>
                    <td>{{ data.gbName }}</td>
                  </tr>
                </tbody>
              </table>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getContrastTree } from '@/api/standard/contrast'
import { getDictMapping, dictAutoMapping, dictManualMapping, dictCancelMapping } from '@/api/standard/dictmapping'
import { jsPlumb } from 'jsplumb'

export default {
  name: 'DictMapping',
  data() {
    return {
      tableHeight: document.body.offsetHeight - 310 + 'px',
      // 遮罩层
      loading: false,
      // 左侧对照数据
      leftTableDataList: [],
      // 右侧标准字典数据
      rightTableDataList: [],
      // 左侧树
      treeOptions: [],
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      contrastId: undefined,
      title: '',
      description: '',
      jsPlumb: null,
      // 连接点数据
      connEndpoints: []
    }
  },
  created() {
    this.getTree()
  },
  mounted() {
    this.jsPlumb = jsPlumb.getInstance({
      // 锚点位置；对任何没有声明描点的Endpoint设置锚点，用于source及target节点
      Anchor: ['Right', 'Left'],
      // 连线的source和target Anchor
      Anchors: ['Right', 'Left'],
      // 鼠标不能拖动删除线
      ConnectionsDetachable: true,
      // 删除线的时候节点不删除
      DeleteEndpointsOnDetach: false,
      // 连线的样式，有四种默认类型：Bezier（贝塞尔曲线），Straight（直线），Flowchart（流程图），State machine（状态机）
      Connector: 'Bezier',
      // 父级元素id；假如页面元素所在上层不同，最外层父级一定要设置
      Container: 'jsplumbContainer'
    })
  },
  methods: {
    getTree() {
      getContrastTree().then(response => {
        if (response.success) {
          const { data } = response
          const tree = {}
          tree.label = '对照表'
          tree.children = data
          this.treeOptions = []
          this.treeOptions.push(tree)
        }
      })
    },
    /** 节点单击事件 */
    handleNodeClick(data, node) {
      if (node.level === 4) {
        this.contrastId = data.id
        this.getDictMapping()
      }
    },
    getDictMapping() {
      this.loading = true
      getDictMapping(this.contrastId).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.leftTableDataList = data.left
          this.rightTableDataList = data.right
          this.title = data.title
          this.description = data.description
          this.$nextTick(() => {
            this.initJsPlumb()
          })
        }
      })
    },
    initJsPlumb() {
      const _this = this
      // 初始化节点设置
      const config = {
        // 源端点
        isSource: false,
        // 目标端点
        isTarget: false,
        connector: ['Bezier', { gap: 10, cornerRadius: 5, alwaysRespectStubs: true }],
        // 端点的形状
        endpoint: ['Dot', {
          radius: 8,
          fill: '#84cfff'
        }],
        paintStyle: {
          strokeStyle: '#84cfff',
          stroke: '#84cfff',
          fill: '#84cfff',
          fillStyle: '#84cfff',
          radius: 6,
          lineWidth: 2
        },
        // 鼠标悬浮在端点的颜色
        hoverPaintStyle: { stroke: '#409EFF' },
        // 连接线的颜色，大小样式
        connectorStyle: {
          outlineStroke: '#84cfff',
          strokeWidth: 1
        },
        // 鼠标悬浮在连接线上的样式
        connectorHoverStyle: {
          strokeWidth: 4
        }
      }
      this.connEndpoints = []
      // 清除端点、连接
      _this.jsPlumb.reset()
      // 设置监听事件
      _this.jsPlumb.ready(function() {
        // 双击连接线（删除）
        _this.jsPlumb.bind('dblclick', function(conn, originalEvent) {
          console.log('dblclick', conn)
          _this.$confirm('删除选中连线, 是否继续？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            _this.jsPlumb.deleteConnection(conn)
          }).catch(() => {
          })
        })
        // 连线
        _this.jsPlumb.bind('connection', function(evt) {
          _this.connEndpoints.push({
            sourceId: evt.sourceId,
            targetId: evt.targetId
          })
        })
        // 删除连线
        _this.jsPlumb.bind('connectionDetached', function(evt) {
          _this.connEndpoints.splice(_this.connEndpoints.findIndex(item => item.sourceId === evt.sourceId && item.targetId === evt.targetId), 1)
        })
      })
      const leftConfig = Object.assign({}, config)
      leftConfig.isSource = true
      // 过滤掉已映射数据
      this.leftTableDataList.filter(item => item.status === '0').forEach(function(item, index, arr) {
        _this.jsPlumb.addEndpoint(item.id, {
          anchors: ['Right'],
          uuid: item.id
        }, leftConfig)
      })
      const rightConfig = Object.assign({}, config)
      rightConfig.isTarget = true
      // 过滤掉已映射数据
      const rightTableData = [...this.rightTableDataList].filter(x => [...this.leftTableDataList].every(y => y.contrastGbId !== x.id))
      rightTableData.forEach(function(item, index, arr) {
        _this.jsPlumb.addEndpoint(item.id, {
          anchors: ['Left'],
          uuid: item.id
        }, rightConfig)
      })
    },
    handleAuto() {
      if (!this.contrastId) {
        this.$message.warning('请先设置对照关系')
        return
      }
      dictAutoMapping(this.contrastId).then(response => {
        if (response.success) {
          this.$message.success('对照成功')
          this.getDictMapping()
        }
      })
    },
    handleManual() {
      if (this.connEndpoints.length > 0) {
        dictManualMapping({ endpoints: this.connEndpoints }).then(response => {
          if (response.success) {
            this.$message.success('对照成功')
            this.getDictMapping()
          }
        })
      } else {
        this.$message.warning('请先设置对照关系')
      }
    },
    handleCancel(id) {
      this.$confirm('是否取消对照关系？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        dictCancelMapping(id).then(response => {
          if (response.success) {
            this.$message.success('取消对照成功')
            this.getDictMapping()
          }
        })
      }).catch(() => {
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.right-toolbar {
  float: right;
}
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
.tree-wrapper {
  overflow-y: auto;
  .body-wrapper {
    margin: -10px;
    ::v-deep .custom-tree-node {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 14px;
      .tree-folder {
        margin-right: 5px;
        color: #f6cf07;
      }
    }
  }
}
#leftTable, #rightTable {
  width: 100%;
  margin: 15px 0;
  border-collapse: collapse;
  tr th {
    color: #909399;
    font-weight: bold;
    background-color: #f5f5f5;
  }
  tr th, tr td {
    font-size: 14px;
    border: 1px solid #EBEEF5;
    padding: 5px 10px;
  }
}
</style>
