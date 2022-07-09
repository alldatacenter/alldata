<template>
  <div style="width: 100%">
    <Card>
      <Button @click="handleAddParamsGroup" type="primary">添加</Button>
    </Card>

    <div class="row">
      <Card v-if="paramsGroup.length == 0"> 暂无参数绑定信息 </Card>
      <div class="paramsGroup" v-else>
        <Card
          style="width: 350px; margin: 7px"
          v-for="(group, index) in paramsGroup"
          :key="index"
          :bordered="false"
        >
          <p slot="title">
            <Icon type="ios-film-outline"></Icon>&nbsp;{{ group.groupName }}
          </p>
          <p slot="extra">
            <Dropdown slot="extra">
              <a href="javascript:void(0)">
                操作
                <Icon type="ios-arrow-down"></Icon>
              </a>
              <Dropdown-menu slot="list">
                <Dropdown-item @click.native="handleEditParamsGroup(group)"
                  >编辑</Dropdown-item
                >
                <Dropdown-item @click.native="handleDeleteParamGroup(group)"
                  >删除</Dropdown-item
                >
              </Dropdown-menu>
            </Dropdown>
            <Icon type="arrow-down-b"></Icon>
          </p>
          <template v-if="group.params && group.params.length > 0">
            <div
              v-for="(param, paramId) in group.params"
              :key="paramId"
              class="params"
            >
              <span>{{ param.paramName }}</span>

              <span>
                <i-button type="text" @click="handleEditParams(group, param)"
                  >编辑</i-button
                >
                <i-button
                  type="text"
                  size="small"
                  style="color: #f56c6c"
                  @click="handleDeleteParam(group, param)"
                  >删除</i-button
                >
              </span>
            </div>
          </template>
          <div v-else style="align-content: center">暂无数据...</div>
          <div style="text-align: center">
            <i-button type="text" @click="handleAddParams(group)"
              >添加</i-button
            >
          </div>
        </Card>
      </div>
    </div>
    <div>
      <Modal
        :title="modalTitle"
        v-model="dialogParamsVisible"
        :mask-closable="false"
        :width="500"
      >
        <Form
          ref="paramForm"
          :model="paramForm"
          :label-width="100"
          :rules="formValidate"
        >
          <FormItem label="参数名称" prop="paramName">
            <Input v-model="paramForm.paramName" style="width: 100%" />
          </FormItem>
          <FormItem label="可选值" prop="options">
            <Select
              v-model="paramForm.options"
              placeholder="输入后回车添加"
              multiple
              filterable
              allow-create
              :popper-append-to-body="false"
              popper-class="spec-values-popper"
              style="width: 100%; text-align: left; margin-right: 10px"
            >
              <Option
                v-for="(item, itemIndex) in ops.options"
                :value="item"
                :key="itemIndex"
                :label="item"
              >
                {{ item }}
              </Option>
            </Select>
          </FormItem>
          <FormItem label="选项" prop="specName3">
            <Checkbox label="1" v-model="paramForm.required">必填</Checkbox>
            <Checkbox label="1" v-model="paramForm.isIndex">可索引</Checkbox>
          </FormItem>
          <FormItem label="排序" prop="sort">
            <InputNumber
              :min="0"
              type="number"
              v-model="paramForm.sort"
              style="width: 100%"
            />
          </FormItem>
        </Form>

        <div slot="footer">
          <Button type="text" @click="dialogParamsVisible = false">取消</Button>
          <Button
            type="primary"
            :loading="submitLoading"
            @click="submitParamForm"
            >提交</Button
          >
        </div>
      </Modal>
    </div>

    <div>
      <Modal
        :title="modalTitle"
        v-model="dialogParamsGroupVisible"
        :mask-closable="false"
        :width="500"
      >
        <Form
          @submit.native.prevent
          @keydown.enter.native="submitParamGroupForm"
          ref="paramGroupForm"
          :model="paramGroupForm"
          :label-width="100"
          :rules="paramGroupValidate"
        >
          <FormItem label="参数名称" prop="groupName">
            <Input v-model="paramGroupForm.groupName" style="width: 100%" />
          </FormItem>
        </Form>

        <div slot="footer">
          <Button type="text" @click="dialogParamsGroupVisible = false"
            >取消</Button
          >
          <Button
            type="primary"
            :loading="submitLoading"
            @click="submitParamGroupForm"
            >提交</Button
          >
        </div>
      </Modal>
    </div>
  </div>
</template>
<script>
import {
  getCategoryParamsListData,
  insertGoodsParams,
  updateGoodsParams,
  deleteParams,
  insertParamsGroup,
  updateParamsGroup,
  deleteParamsGroup,
} from "@/api/goods";

import { regular } from "@/utils";

export default {
  name: "categoryParams",
  data() {
    return {
      submitLoading: false,
      /** 分类ID */
      categoryId: this.$route.query.id,
      /** 参数组 */
      paramsGroup: [],
      /** 添加或编辑标识 */
      modalType: 0,
      /** 添加或编辑标题 */
      modalTitle: "",
      /** 参数添加或编辑弹出框 */
      dialogParamsVisible: false,
      /** 参数组添加或编辑弹出框 */
      dialogParamsGroupVisible: false,
      //参数表单
      paramForm: {
        sort: 1,
      },
      /** 参数值 **/
      ops: {
        options: [],
      },
      // 参数表单
      paramGroupForm: {},
      /** 添加、编辑参数 规格 */
      formValidate: {
        paramName: [regular.REQUIRED, regular.VARCHAR5],
        options: [regular.REQUIRED, regular.VARCHAR255],
        sort: [regular.REQUIRED, regular.INTEGER],
      },
      /** 参数组*/
      paramGroupValidate: {
        groupName: [regular.REQUIRED, regular.VARCHAR5],
      },
    };
  },
  filters: {
    paramTypeFilter(val) {
      return val === 1 ? "输入项" : "选择项";
    },
  },
 
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
    },
    //弹出添加参数框
    handleAddParams(group) {
      this.paramForm = {
        paramName: "",
        paramType: 1,
        options: "",
        required: false,
        isIndex: false,
        sort: 0,
        groupId: group.groupId,
        categoryId: this.categoryId,
      };
      this.modalTitle = "添加参数";
      this.modalType = 0;
      this.dialogParamsVisible = true;
    },
    //弹出修改参数框
    handleEditParams(group, param) {
      console.log(group, param);
      this.paramForm = {
        paramName: param.paramName,
        options: param.options.split(","),
        required: param.required == 1 ? true : false,
        isIndex: param.isIndex == 1 ? true : false,
        groupId: param.groupId || "",
        categoryId: param.categoryId || "",
        sort: param.sort || 1,
        id: param.id,
      };
      this.ops.options = this.paramForm.options;
      this.modalType = 1;
      this.modalTitle = "修改参数";
      this.dialogParamsVisible = true;
    },
    //弹出修改参数组框
    handleEditParamsGroup(group) {
      this.paramGroupForm = {
        groupName: group.groupName,
        categoryId: this.categoryId,
        id: group.groupId,
      };
      this.modalType = 1;
      this.modalTitle = "修改参数组";
      this.dialogParamsGroupVisible = true;
    },
    // 添加参数
    handleAddParamsGroup() {
      this.paramGroupForm = {};
      this.ops = {};
      (this.paramGroupForm.categoryId = this.categoryId), (this.modalType = 0);

      this.modalTitle = "添加参数组";
      this.dialogParamsGroupVisible = true;
    },
    //保存参数组
    submitParamGroupForm() {
      this.$refs.paramGroupForm.validate((valid) => {
        if (valid) {
          if (this.modalType === 0) {
            insertParamsGroup(this.paramGroupForm).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("参数组修改成功");
                this.getDataList();
                this.dialogParamsVisible = false;
              }
            });
          } else {
            console.warn(this.paramGroupForm);
            updateParamsGroup(this.paramGroupForm).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("参数组修改成功");
                this.getDataList();
                this.dialogParamsVisible = false;
              }
            });
          }
          this.dialogParamsGroupVisible = false;
        }
      });
    },
    //保存参数
    submitParamForm() {
      this.$refs.paramForm.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          let data = JSON.parse(JSON.stringify(this.paramForm));
          data.isIndex = Number(data.isIndex);
          data.required = Number(data.required);
          if (this.modalType === 0) {
            insertGoodsParams(data).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("参数添加成功");
                this.getDataList();
                this.dialogParamsVisible = false;
              }
            });
          } else {
            console.warn(data.isIndex);
            data.isIndex = Number(data.isIndex);
            data.required = Number(data.required);
            updateGoodsParams(data).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("参数修改成功");
                this.getDataList();
                this.dialogParamsVisible = false;
              }
            });
          }
        }
      });
    },
    // 获取分类列表
    getDataList() {
      getCategoryParamsListData(this.categoryId).then((res) => {
        if (res.success) {
          this.paramsGroup = res.result;
        }
      });
    },
    //删除参数方法
    handleDeleteParam(group, param) {
      this.$Modal.confirm({
        title: "确认删除",
        // 记得确认修改此处
        content: "您确认要删除 " + param.paramName + " ?",
        loading: true,
        onOk: () => {
          // 删除
          deleteParams(param.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除参数成功");
              this.getDataList();
            }
          });
        },
      });
    },
    //删除参数组方法
    handleDeleteParamGroup(group) {
      this.$Modal.confirm({
        title: "确认删除",
        // 记得确认修改此处
        content: "您确认要删除 " + group.groupName + " ?",
        loading: true,
        onOk: () => {
          // 删除
          deleteParamsGroup(group.groupId).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除参数成功");
              this.getDataList();
            }
          });
        },
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss">
.row {
  overflow: hidden;
  margin: 20px 0;
}

.params {
  align-items: center;
  display: flex;
  padding: 3px;
  background-color: #f5f7fa;
  font-size: 14px;
  justify-content: space-between;
}

.ivu-card-head {
  background-color: #f5f7fa;
}

.ivu-btn {
  font-size: 13px;
}

.paramsGroup {
  flex-wrap: wrap;
  display: flex;
  flex-direction: row;
  width: 100%;
}
</style>
