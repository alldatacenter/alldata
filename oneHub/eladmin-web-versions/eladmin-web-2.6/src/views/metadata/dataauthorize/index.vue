<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="box-card tree-wrapper" shadow="always">
          <el-tree
            ref="leftTree"
            :data="leftTreeOptions"
            node-key="id"
            empty-text="加载中，请稍后"
            :props="leftTreeDefaultProps"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <span slot-scope="{ node, data }" class="custom-tree-node">
              <span><i v-if="node.level === 1" class="iconfont icon-zuzhi tree-folder" />{{ node.label }}</span>
            </span>
          </el-tree>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card class="box-card tree-wrapper" shadow="always">
          <el-row type="flex" justify="space-between">
            <el-col :span="12">
              <el-button-group>
                <el-button
                  v-hasPerm="['metadata:dataauthorize:refresh']"
                  type="warning"
                  icon="el-icon-refresh"
                  size="mini"
                  @click="handleCacheRefresh"
                >刷新缓存</el-button>
              </el-button-group>
            </el-col>
            <el-col :span="12">
              <el-button-group style="float: right;">
                <template v-if="showBtns">
                  <el-button v-hasPerm="['metadata:dataauthorize:edit']" v-if="disabled" size="mini" round @click="disabled = false">修改</el-button>
                  <el-button v-hasPerm="['metadata:dataauthorize:edit']" v-else size="mini" round @click="handSubmit">保存</el-button>
                </template>
              </el-button-group>
            </el-col>
          </el-row>
          <el-divider />
          <el-form ref="form" :model="form" :disabled="disabled">
            <el-tree
              ref="rightTree"
              :data="rightTreeOptions"
              show-checkbox
              accordion
              node-key="id"
              empty-text="加载中，请稍后"
              :props="rightTreeDefaultProps"
              highlight-current
              :expand-on-click-node="false"
              :check-strictly="checkStrictly"
            >
              <span slot-scope="{ node, data }" class="custom-tree-node">
                <span>
                  <i v-if="node.level === 1" class="iconfont icon-shujuku tree-folder" />
                  <i v-else-if="node.level === 2" class="iconfont icon-shujubiao tree-folder" />
                  <i v-else-if="node.level === 3" class="iconfont icon-shujuziduan tree-folder" />
                  {{ data.code ? (data.name ? data.code + '(' + data.name + ')' : data.code) : data.name }}
                </span>
              </span>
            </el-tree>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listRole } from '@/api/system/role'
import { getDataMetadataTree } from '@/api/metadata/datacolumn'
import { getAuthorizedMetadata, metadataAuthorize, refreshAuthorize } from '@/api/metadata/dataauthorize'

export default {
  name: 'DataAuthorize',
  data() {
    return {
      tableHeight: document.body.offsetHeight - 310 + 'px',
      // 左侧树
      leftTreeOptions: [],
      leftTreeDefaultProps: {
        children: 'children',
        label: 'roleName'
      },
      // 右侧树
      rightTreeOptions: [],
      rightTreeDefaultProps: {
        children: 'children',
        label: 'label'
      },
      checkStrictly: false,
      form: {},
      disabled: true,
      showBtns: false,
      btnSubmitEnable: false
    }
  },
  created() {
    this.getLeftTree()
    this.getRightTree()
  },
  methods: {
    getLeftTree() {
      listRole().then(response => {
        if (response.success) {
          const { data } = response
          const tree = {}
          tree.roleName = '角色组'
          tree.children = data
          this.leftTreeOptions = []
          this.leftTreeOptions.push(tree)
        }
      })
    },
    handleNodeClick(data) {
      if (data.id) {
        this.form.roleId = data.id
        getAuthorizedMetadata(data.id).then(response => {
          if (response.success) {
            this.$refs.rightTree.setCheckedKeys([])
            const checkedKeys = response.data || []
            if (checkedKeys && checkedKeys.length > 0) {
              this.checkStrictly = true
              this.$nextTick(() => {
                this.$refs.rightTree.setCheckedKeys(checkedKeys)
                this.checkStrictly = false
              })
            }
            this.showBtns = true
          }
        })
      }
    },
    getRightTree() {
      getDataMetadataTree('column').then(response => {
        if (response.success) {
          const { data } = response
          this.rightTreeOptions = data
        }
      })
    },
    handSubmit() {
      const data = []
      const checkedNodes = this.$refs.rightTree.getCheckedNodes(false, true)
      checkedNodes.forEach((item, index, arr) => {
        const obj = {}
        obj.objectId = item.id
        obj.roleId = this.form.roleId
        obj.objectType = item.type
        data.push(obj)
      })
      metadataAuthorize({ roleId: this.form.roleId, authorizeDataList: data }).then(response => {
        if (response.success) {
          this.$message.success('保存成功')
          this.disabled = true
        }
      })
    },
    handleCacheRefresh() {
      refreshAuthorize().then(response => {
        if (response.success) {
          this.$message.success('刷新缓存成功')
        } else {
          this.$message.error('刷新缓存失败')
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
.tree-wrapper {
  overflow-y: auto;
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
</style>
