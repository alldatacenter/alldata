<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card class="box-card tree-wrapper" shadow="always">
        <div class="body-wrapper">
          <el-tree
            ref="ruleType"
            :data="ruleTypeOptions"
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
                <span v-if="node.level === 1"><i class="iconfont icon-zuzhi tree-folder" />{{ node.label }}</span>
                <span v-else>{{ node.label }} (错误数：<span style="color: #ef5b5b">{{ data.checkErrorCount }}</span>)</span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-card>
    </el-col>
    <el-col :span="18">
      <el-card class="box-card" shadow="always">
        <el-form ref="queryForm" :model="queryParams" :inline="true">
          <el-form-item label="规则名称" prop="ruleName">
            <el-input
              v-model="queryParams.ruleName"
              placeholder="请输入规则名称"
              clearable
              size="small"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loading"
          :data="tableDataList"
          border
          tooltip-effect="dark"
          :height="tableHeight"
          style="width: 100%;margin: 15px 0;"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="序号" width="55" align="center">
            <template slot-scope="scope">
              <span>{{ scope.$index +1 }}</span>
            </template>
          </el-table-column>
          <template v-for="(item, index) in tableColumns">
            <el-table-column
              v-if="item.show"
              :key="index"
              :prop="item.prop"
              :label="item.label"
              :formatter="item.formatter"
              align="center"
              show-overflow-tooltip
            />
          </template>
        </el-table>

        <el-pagination
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :current-page.sync="queryParams.pageNum"
          :page-size.sync="queryParams.pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import { listRuleType, pageCheckReport } from '@/api/quality/checkreport'

export default {
  name: 'CheckStatList',
  data() {
    return {
      tableHeight: document.body.offsetHeight - 310 + 'px',
      // 展示切换
      showOptions: {
        data: {},
        showList: true
      },
      // 遮罩层
      loading: true,
      // 表格头
      tableColumns: [
        { prop: 'ruleName', label: '规则名称', show: true },
        { prop: 'ruleType', label: '规则类型', show: true },
        { prop: 'ruleSource', label: '数据源', show: true },
        { prop: 'ruleTable', label: '数据表', show: true },
        { prop: 'ruleColumn', label: '核查字段', show: true },
        { prop: 'checkTotalCount', label: '核查数量', show: true },
        { prop: 'checkErrorCount', label: '不合规数量', show: true },
        { prop: 'checkDate', label: '核查时间', show: true }
      ],
      // 表格数据
      tableDataList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        ruleTypeId: '',
        ruleName: ''
      },
      // 左侧树
      ruleTypeOptions: [],
      defaultProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  created() {
    this.getTree()
    this.getList()
  },
  methods: {
    getTree() {
      listRuleType().then(response => {
        if (response.success) {
          const { data } = response
          const tree = {}
          tree.name = '核查规则类型'
          tree.children = data
          this.ruleTypeOptions = []
          this.ruleTypeOptions.push(tree)
        }
      })
    },
    /** 节点单击事件 */
    handleNodeClick(data) {
      if (data.id) {
        this.queryParams.ruleTypeId = data.id
        this.getList()
      }
    },
    /** 查询数据源列表 */
    getList() {
      this.loading = true
      pageCheckReport(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          const { data } = response
          this.tableDataList = data.data
          this.total = data.total
        }
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 20,
        ruleTypeId: '',
        ruleName: ''
      }
      this.handleQuery()
    },
    handleSizeChange(val) {
      console.log(`每页 ${val} 条`)
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = val
      this.getList()
    },
    handleCurrentChange(val) {
      console.log(`当前页: ${val}`)
      this.queryParams.pageNum = val
      this.getList()
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
</style>
