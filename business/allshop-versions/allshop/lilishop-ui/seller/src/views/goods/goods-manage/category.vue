<template>
  <div>
    <Card>
      <div class="operation">
        <Button @click="addParent">添加一级分类</Button>
        <Button @click="refresh">刷新列表</Button>
      </div>
      <tree-table
        ref="treeTable"
        size="default"
        :loading="loading"
        :data="tableData"

        :columns="columns"
        :border="true"
        :show-index="false"
        :is-fold="true"
        :expand-type="false"
        primary-key="id">
        <template slot="action" slot-scope="scope">
          <Button
            type="dashed"
            @click="edit(scope.row)"
            size="small"
            style="margin-right:5px"
          >编辑
          </Button>
          <Button
            v-show="scope.row.level != 1 "
            type="info"
            @click="addChildren(scope.row)"
            size="small"
            style="margin-right:5px"
          >添加子分类
          </Button>
          <Button
            type="error"
            @click="remove(scope.row)"
            size="small"
            style="margin-right:5px"
          >删除
          </Button>

        </template>
      </tree-table>

      <Modal :title="modalTitle" v-model="modalVisible" :mask-closable='false' :width="500">
        <Form ref="formAdd" :model="formAdd" :label-width="100" :rules="formValidate">
          <div v-if="showParent">
            <FormItem label="上级分类" prop="parentId">
              {{ parentTitle }}
              <Input v-model="formAdd.parentId" clearable style="width:100%;display:none"/>
            </FormItem>
          </div>
          <FormItem label="层级" prop="level" style="display:none">
            <Input v-model="formAdd.level" clearable style="width:100%"/>
          </FormItem>
          <FormItem label="分类名称" prop="labelName">
            <Input v-model="formAdd.labelName" maxlength="12" clearable style="width:100%"/>
          </FormItem>
          <FormItem label="排序值" prop="sortOrder" style="width:345px">
            <InputNumber v-model="formAdd.sortOrder" :min="1"></InputNumber>
          </FormItem>
        </Form>
        <div slot="footer">
          <Button type="text" @click="modalVisible=false">取消</Button>
          <Button type="primary" :loading="submitLoading" @click="submit">提交</Button>
        </div>
      </Modal>
    </Card>
  </div>
</template>
<script>
import * as API_Goods from "@/api/goods";

import TreeTable from "@/views/my-components/tree-table/Table/Table";

import { regular } from "@/utils";
import {VARCHAR20} from "../../../utils/regular";

export default {
  name: "store-category",
  components: {
    TreeTable
  },
  data() {
    return {
      submitLoading: false, // 提交loading
      loading: false, //表格加载的loading
      modalType: 0, // 添加或编辑标识
      modalVisible: false, // 添加或编辑显示
      modalTitle: "", // 添加或编辑标题
      showParent: false, // 是否展示上级菜单
      parentTitle: "", // 父级菜单名称
      formAdd: { // 添加或编辑表单对象初始化数据
        parentId: "",
        labelName: "",
        sortOrder: 1,
        level: 0,
      },
      // 表单验证规则
      formValidate: {
        labelName: [
          regular.REQUIRED,
          regular.VARCHAR20
        ],
        sortOrder: [
          regular.REQUIRED,
          regular.INTEGER
        ],
      },
      columns: [
        {
          title: "分类名称",
          key: "labelName",
          align: "left",
          minWidth: "120px",
        },
        {
          title: "操作",
          key: "action",
          align: "left",
          headerAlign: "center",
          width: "280px",
          type: "template",
          template: "action",
        }
      ],
      // 表格数据
      tableData: []
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getAllList();
    },
    // 刷新列表
    refresh() {
      this.loading = true;
      let that = this;
      setTimeout(function () {
        that.loading = false;
      }, 1000);
    },
    //添加子分类
    addChildren(v) {
      this.modalType = 0;
      this.modalTitle = "添加子分类";
      this.parentTitle = v.labelName;
      this.formAdd.level = eval(v.level + "+1");
      this.formAdd.labelName = "";
      this.showParent = true;
      delete this.formAdd.id;
      this.formAdd.parentId = v.id;
      this.modalVisible = true;
    },
    // 编辑分类
    edit(v) {
      this.modalType = 1;
      this.modalTitle = "编辑";
      this.formAdd.id = v.id;
      this.formAdd.labelName = v.labelName;
      this.formAdd.level = v.level;
      this.formAdd.parentId = v.parentId;
      this.formAdd.sortOrder = v.sortOrder;
      this.showParent = false;
      this.modalVisible = true;
    },
    //添加一级分类
    addParent() {
      this.modalType = 0;
      this.formAdd.labelName = "";
      this.modalTitle = "添加一级分类";
      this.parentTitle = "顶级分类";
      this.showParent = true;
      delete this.formAdd.id;
      this.formAdd.parentId = 0;
      this.formAdd.sortOrder = 1
      this.modalVisible = true;

    },
    //提交编辑和添加
    submit() {
      this.$refs.formAdd.validate(valid => {
        if (valid) {
          this.submitLoading = true;
          console.log(this.formAdd);
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete this.formAdd.id;
            API_Goods.addShopGoodsLabel(this.formAdd).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("添加成功");
                this.getAllList(0);
                this.modalVisible = false;
              }
            });
          } else {
            // 编辑
            API_Goods.editShopGoodsLabel(this.formAdd).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("修改成功");
                this.getAllList(0);
                this.modalVisible = false;
              }
            });
          }
        }
      });
    },
    // 确认删除分类
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        // 记得确认修改此处
        content: "您确认要删除 " + v.labelName + " ?",
        loading: true,
        onOk: () => {
          // 删除
          API_Goods.delCategdelShopGoodsLabel(v.id).then(res => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getAllList();
            }
          });
        }
      });
    },
    // 获取分类
    getAllList() {
      this.loading = true;
      API_Goods.getShopGoodsLabelList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          res.result.forEach(firstCate => {
            if (firstCate.children && firstCate.children.length) {
              firstCate.children.forEach(secondCate => {
                secondCate.parentId = firstCate.id
              })
            }
          });
          this.tableData = res.result;
        }
      });
    },
  },
  mounted() {
    this.init();
  }
};
</script>
<style lang="scss" scoped>
/deep/ .ivu-table-wrapper {
  overflow: auto;
}
.table {
  min-height: 100vh;
  height: auto;
}
</style>
