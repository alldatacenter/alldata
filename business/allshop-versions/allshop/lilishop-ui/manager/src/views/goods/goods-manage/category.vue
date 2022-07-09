<template>
  <div>
    <Card>
      <div class="mb_10">
        <Button @click="addParent" icon="md-add">添加一级分类</Button>
      </div>
      <Table
        class="table"
        :load-data="handleLoadData"
        row-key="id"
        :loading="loading"
        :data="tableData"
        :columns="columns"
      >
        <template slot="action" slot-scope="scope">
          <Dropdown v-show="scope.row.level == 2" trigger="click">
            <Button size="small">
              绑定
              <Icon type="ios-arrow-down"></Icon>
            </Button>
            <DropdownMenu slot="list">
              <DropdownItem @click.native="brandOperation(scope.row)"
                >编辑绑定品牌</DropdownItem
              >
              <DropdownItem @click.native="specOperation(scope.row)"
                >编辑绑定规格</DropdownItem
              >
              <DropdownItem @click.native="parameterOperation(scope.row)"
                >编辑绑定参数</DropdownItem
              >
            </DropdownMenu>
          </Dropdown>

          &nbsp;
          <Dropdown trigger="click">
            <Button size="small">
              操作
              <Icon type="ios-arrow-down"></Icon>
            </Button>
            <DropdownMenu slot="list">
              <DropdownItem @click.native="edit(scope.row)">编辑</DropdownItem>
              <DropdownItem
                v-if="scope.row.deleteFlag == 1"
                @click.native="enable(scope.row)"
                >启用</DropdownItem
              >
              <DropdownItem
                v-if="scope.row.deleteFlag == 0"
                @click.native="disable(scope.row)"
                >禁用</DropdownItem
              >
              <DropdownItem @click.native="remove(scope.row)">删除</DropdownItem>
            </DropdownMenu>
          </Dropdown>
          &nbsp;
          <Button
            v-show="scope.row.level != 2"
            type="primary"
            @click="addChildren(scope.row)"
            size="small"
            icon="md-add"
            style="margin-right: 5px"
            >添加子分类
          </Button>
        </template>

        <template slot="commissionRate" slot-scope="scope">
          {{ scope.row.commissionRate }}%
        </template>

        <template slot="deleteFlag" slot-scope="{ row }">
          <Tag
            :class="{ ml_10: row.deleteFlag }"
            :color="row.deleteFlag == false ? 'success' : 'error'"
          >
            {{ row.deleteFlag == false ? "正常启用" : "禁用" }}</Tag
          >
        </template>
      </Table>

      <Modal
        :title="modalTitle"
        v-model="modalVisible"
        :mask-closable="false"
        :width="500"
      >
        <Form ref="form" :model="formAdd" :label-width="100" :rules="formValidate">
          <div v-if="showParent">
            <FormItem label="上级分类" prop="parentId">
              {{ parentTitle }}
              <Input
                v-model="formAdd.parentId"
                clearable
                style="width: 100%; display: none"
              />
            </FormItem>
          </div>
          <FormItem label="层级" prop="level" style="display: none">
            <Input v-model="formAdd.level" clearable style="width: 100%" />
          </FormItem>
          <FormItem label="分类名称" prop="name">
            <Input v-model="formAdd.name" clearable style="width: 100%" />
          </FormItem>
          <FormItem label="分类图标" prop="image" v-if="formAdd.level !== 1">
            <upload-pic-input
              v-model="formAdd.image"
              style="width: 100%"
            ></upload-pic-input>
          </FormItem>
          <FormItem label="排序值" prop="sortOrder" style="width: 345px">
            <InputNumber v-model="formAdd.sortOrder"></InputNumber>
          </FormItem>
          <FormItem label="佣金比例(%)" prop="commissionRate" style="width: 345px">
            <InputNumber v-model="formAdd.commissionRate"></InputNumber>
          </FormItem>
          <FormItem label="是否启用" prop="deleteFlag">
            <i-switch
              size="large"
              v-model="formAdd.deleteFlag"
              :true-value="0"
              :false-value="1"
            >
              <span slot="open">启用</span>
              <span slot="close">禁用</span>
            </i-switch>
          </FormItem>
        </Form>
        <div slot="footer">
          <Button type="text" @click="modalVisible = false">取消</Button>
          <Button type="primary" :loading="submitLoading" @click="Submit">提交</Button>
        </div>
      </Modal>

      <Modal
        :title="modalBrandTitle"
        v-model="modalBrandVisible"
        :mask-closable="false"
        :width="500"
      >
        <Form ref="brandForm" :model="brandForm" :label-width="100">
          <Select v-model="brandForm.categoryBrands" filterable multiple>
            <Option v-for="item in brandWay" :value="item.id" :key="item.id">{{
              item.name
            }}</Option>
          </Select>
        </Form>
        <div slot="footer">
          <Button type="text" @click="modalBrandVisible = false">取消</Button>
          <Button type="primary" :loading="submitLoading" @click="saveCategoryBrand"
            >提交</Button
          >
        </div>
      </Modal>

      <Modal
        :title="modalSpecTitle"
        v-model="modalSpecVisible"
        :mask-closable="false"
        :width="500"
      >
        <Form ref="specForm" :model="specForm" :label-width="100">
          <Select v-model="specForm.categorySpecs" multiple>
            <Option
              v-for="item in specifications"
              :value="item.id"
              :key="item.id"
              :label="item.specName"
            >
            </Option>
          </Select>
        </Form>
        <div slot="footer">
          <Button type="text" @click="modalSpecVisible = false">取消</Button>
          <Button type="primary" :loading="submitLoading" @click="saveCategorySpec"
            >提交</Button
          >
        </div>
      </Modal>
    </Card>
  </div>
</template>
<script>
import {
  insertCategory,
  updateCategory,
  getBrandListData,
  delCategory,
  getCategoryBrandListData,
  saveCategoryBrand,
  getSpecificationList,
  getCategorySpecListData,
  disableCategory,
  saveCategorySpec,
  getCategoryTree,
} from "@/api/goods";

import uploadPicInput from "@/views/my-components/lili/upload-pic-input";
import { regular } from "@/utils";
export default {
  name: "goods-category",
  components: {
    uploadPicInput,
  },
  data() {
    return {
      submitLoading: false, //加载状态
      categoryList: [], // 分类列表
      loading: false, // 加载状态
      brands: [], //品牌集合
      specifications: [], //规格集合
      categoryId: "", // 分类id
      categorySpecs: [], //已经选择的规格
      modalType: 0, // 添加或编辑标识
      modalVisible: false, // 添加或编辑显示
      modalBrandVisible: false, //品牌关联编辑显示
      modalSpecVisible: false, //品牌关联编辑显示
      modalTitle: "", // 添加或编辑标题
      showParent: false, // 是否展示上级菜单
      parentTitle: "", // 父级菜单名称
      modalBrandTitle: "", // 品牌弹窗标题
      modalSpecTitle: "", // 规格弹窗标题
      formAdd: {
        // 添加或编辑表单对象初始化数据
        parentId: "",
        name: "",
        image: "",
        sortOrder: 0,
        deleteFlag: 0,
        commissionRate: 0,
        level: 0,
      },
      brandForm: {
        categoryBrands: [],
      },
      brandWay: "", //请求绑定品牌的信息
      specForm: {}, // 规格数据
      // 表单验证规则
      formValidate: {
        commissionRate: [regular.REQUIRED, regular.INTEGER],
        name: [regular.REQUIRED, regular.VARCHAR20],
        sortOrder: [regular.REQUIRED, regular.INTEGER],
      },
      columns: [
        {
          title: "分类名称",
          key: "name",
          tree: true,
        },
        {
          title: "状态",
          slot: "deleteFlag",
        },
        {
          title: "佣金",
          key: "commissionRate",

          slot: "commissionRate",
        },
        {
          title: "操作",
          key: "action",

          slot: "action",
        },
      ],
      tableData: [], // 表格数据
      categoryIndex: 0, // 分类id
      checkedCategoryChildren: "", //选中的分类子级
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getAllList(0);
      this.getBrandList();
      this.getSpecList();
    },
    //获取所有品牌
    getBrandList() {
      getBrandListData().then((res) => {
        this.brandWay = res;
      });
    },
    //获取所有规格
    getSpecList() {
      getSpecificationList().then((res) => {
        if (res.length != 0) {
          this.specifications = res;
        }
      });
    },
    //弹出品牌关联框
    brandOperation(v) {
      getCategoryBrandListData(v.id).then((res) => {
        this.categoryId = v.id;
        this.modalBrandTitle = "品牌关联";
        this.brandForm.categoryBrands = res.result.map((item) => item.id);
        this.modalBrandVisible = true;
      });
    },
    //弹出规格关联框
    specOperation(v) {
      getCategorySpecListData(v.id).then((res) => {
        this.categoryId = v.id;
        this.modalSpecTitle = "规格关联";
        this.specForm.categorySpecs = res.map((item) => item.id);
        this.modalSpecVisible = true;
      });
    },
    //保存分类规格绑定
    saveCategorySpec() {
      saveCategorySpec(this.categoryId, this.specForm).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("操作成功");
          this.modalSpecVisible = false;
        }
      });
    },
    //保存分类品牌绑定
    saveCategoryBrand() {
      saveCategoryBrand(this.categoryId, this.brandForm).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("操作成功");
          this.modalBrandVisible = false;
        }
      });
    },
    // 编辑绑定参数
    parameterOperation(v) {
      this.$router.push({ name: "parameter", query: { id: v.id } });
    },
    // 添加子分类
    addChildren(v) {
      this.modalType = 0;
      this.modalTitle = "添加子分类";
      this.parentTitle = v.name;
      this.formAdd.level = eval(v.level + "+1");
      this.formAdd.commissionRate = v.commissionRate;
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
      this.formAdd.name = v.name;
      this.formAdd.level = v.level;
      this.formAdd.parentId = v.parentId;
      this.formAdd.sortOrder = v.sortOrder;
      this.formAdd.commissionRate = v.commissionRate;
      this.formAdd.deleteFlag = v.deleteFlag;
      this.formAdd.image = v.image;
      this.showParent = false;
      this.modalVisible = true;
    },
    // 添加一级分类
    addParent() {
      this.modalType = 0;
      this.modalTitle = "添加一级分类";
      this.parentTitle = "顶级分类";
      this.showParent = true;
      this.$refs.form.resetFields();
      delete this.formAdd.id;
      this.formAdd.parentId = 0;
      this.modalVisible = true;
    },
    // 提交
    Submit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete this.formAdd.id;
            insertCategory(this.formAdd).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("添加成功");
                this.getAllList(this.categoryIndex);
                this.modalVisible = false;
                this.$refs.form.resetFields();
              }
            });
          } else {
            // 编辑
            updateCategory(this.formAdd).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("修改成功");
                this.getAllList(this.categoryIndex);
                this.modalVisible = false;
                this.$refs.form.resetFields();
              }
            });
          }
        }
      });
    },
    // 删除分类
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除 " + v.name + " ?",
        loading: true,
        onOk: () => {
          // 删除
          delCategory(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getAllList(0);
            }
          });
        },
      });
    },

    // 异步手动加载分类名称
    handleLoadData(item, callback) {
      if (item.level == 0) {
        let categoryList = JSON.parse(JSON.stringify(this.categoryList));
        categoryList.forEach((val) => {
          if (val.id == item.id) {
            val.children.map((child) => {
              child._loading = false;
              child.children = [];
            });
            // 模拟加载
            setTimeout(() => {
              callback(val.children);
            }, 1000);
          }
        });
      } else {
        this.deepCategoryChildren(item.id, this.categoryList);
        setTimeout(() => {
          callback(this.checkedCategoryChildren);
        }, 1000);
      }
    },

    // 通过递归children来实现手动加载数据
    deepCategoryChildren(id, list) {
      if (id != "0" && list.length != 0) {
        for (let i = 0; i < list.length; i++) {
          let item = list[i];
          if (item.id == id) {
            this.checkedCategoryChildren = item.children;
            return;
          } else {
            this.deepCategoryChildren(id, item.children);
          }
        }
      }
    },
    // 获取分类数据
    getAllList(parent_id) {
      this.loading = true;
      getCategoryTree(parent_id).then((res) => {
        this.loading = false;
        if (res.success) {
          localStorage.setItem("category", JSON.stringify(res.result));
          this.categoryList = JSON.parse(JSON.stringify(res.result));
          this.tableData = res.result.map((item) => {
            if (item.children.length != 0) {
              item.children = [];
              item._loading = false;
            }
            return item;
          });
        }
      });
    },
    // 启用分类
    enable(v) {
      this.$Modal.confirm({
        title: "确认启用",
        content: "您是否要启用当前分类 " + v.name + " 及其子分类?",
        loading: true,
        okText: "是",
        cancelText: "否",
        onOk: () => {
          disableCategory(v.id, { enableOperations: 0 }).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getAllList(0);
            }
          });
        },
        onCancel: () => {
          this.getAllList(0);
        },
      });
    },
    // 禁用分类
    disable(v) {
      this.$Modal.confirm({
        title: "确认禁用",
        content: "您是否要禁用当前分类 " + v.name + " 及其子分类?",
        loading: true,
        okText: "是",
        cancelText: "否",
        onOk: () => {
          disableCategory(v.id, { enableOperations: true }).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getAllList(0);
            }
          });
        },
        onCancel: () => {
          this.getAllList(0);
        },
      });
    },
  },
  mounted() {
    this.init();
  },
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
