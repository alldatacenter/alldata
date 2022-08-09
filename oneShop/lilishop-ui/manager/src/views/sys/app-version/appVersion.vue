<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form" @keydown.enter.native="handleSearch">
        <Form-item label="系统类型" prop="orderSn">
          <Select v-model="searchForm.type" placeholder="请选择系统类型" clearable style="width: 200px">
            <Option value="IOS">苹果</Option>
            <Option value="ANDROID">安卓</Option>
          </Select>
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search">搜索</Button>
      </Form>

      <Button class="mt_10 mb_10" @click="addAppVersion" type="primary">添加</Button>

      <Table :loading="loading" border :columns="columns" :data="data" ref="table" sortable="custom"  ></Table>

      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]" size="small"
          show-total show-elevator show-sizer></Page>
      </Row>
    </Card>

    <Modal :title="modalTitle" v-model="modalVisible" :mask-closable="false" :width="1000">
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="版本名称" prop="versionName">
          <Input v-model="form.versionName" maxlength="15" clearable style="width: 40%" />

        </FormItem>
        <FormItem label="版本号" prop="version">
          <Input v-model="form.version" maxlength="15" clearable style="width: 40%" />
          <span class="tips">在移动端项目->manifest.json->基础配置->应用版本名称中查看</span>
        </FormItem>
        <FormItem label="更新时间" prop="versionUpdateDate">
          <DatePicker v-model="form.versionUpdateDate" type="datetime" format="yyyy-MM-dd HH:mm:ss" clearable placeholder="请选择" style="width: 200px"></DatePicker>
        </FormItem>
        <FormItem label="强制更新">
          <RadioGroup type="button" button-style="solid" v-model="form.forceUpdate">
            <Radio :label="1">强制更新</Radio>
            <Radio :label="0">非强制更新</Radio>
          </RadioGroup>
          <span class="tips" v-if="form.forceUpdate == 1">
           强制更新即为应用中必须更新此版本。不更新则无法继续使用App
          </span>
          <span class="tips" v-if="form.forceUpdate == 0">
           非强制更新为应用中推荐更新此版本。不更新还可以继续使用
          </span>
        </FormItem>
        <FormItem label="类型">
          <RadioGroup type="button" button-style="solid" v-model="form.type">
            <Radio label="IOS">苹果</Radio>
            <Radio label="ANDROID">安卓</Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="下载地址" prop="downloadUrl">
          <Input v-model="form.downloadUrl" maxlength="100" clearable style="width: 40%" />
          <span class="tips" v-if="form.type == 'IOS'">
            AppStore中App项目下载目录。可从下载App页面点击分享，拷贝链接
          </span>
          <span class="tips" v-else>
            安卓该链接为应用的下载地址
          </span>
        </FormItem>
        <FormItem class="form-item-view-el" label="更新内容" prop="content">
          <Input v-model="form.content" :rows="6" maxlength="100" show-word-limit type="textarea" placeholder="Enter something..." />
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="saveAppVersion">保存</Button>
      </div>
    </Modal>
    <div>
      <!-- 查看版本信息 -->
      <Modal :title="queryModalTitle" v-model="queryModalVisible" :width="700">
        <Form>
          <div class="div-version">
            <FormItem label="版本名称:">
              {{form.versionName}}
            </FormItem>
          </div>
          <div class="div-version">
            <FormItem label="版本号:">
              {{form.version}}
            </FormItem>
          </div>

          <FormItem label="更新时间:">
            <div>
              {{versionUpdateDate}}
            </div>
          </FormItem>
          <FormItem label="强制更新:">
            <span v-if="form.forceUpdate == 1">强制更新</span>
            <span v-else>非强制更新</span>
          </FormItem>
          <FormItem label="类型">
            <span v-if="form.type == 'IOS'">IOS</span>
            <span v-else>安卓</span>
          </FormItem>
          <FormItem label="下载地址:">
            {{form.downloadUrl}}
          </FormItem>
          <FormItem label="更新内容:">
            <div v-html="form.content">

            </div>
          </FormItem>
        </Form>
        <div slot="footer">
          <Button type="text" @click="queryModalVisible = false">取消</Button>
        </div>
      </Modal>
    </div>
  </div>
</template>

<script>
import * as API_Setting from "@/api/setting";

export default {
  name: "appVersion",
  data() {
    return {
      queryModalVisible: false, // 版本信息modal
      queryModalTitle: "查看更新信息", // modal标题
      loading: true, // 表单加载状态
      modalVisible: false, // 添加app版本模态框
      modalTitle: "", // 添加app版本标题
      modalType: 0, // 新增、编辑标识
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        type: "",
      },
      form: {
        // 添加或编辑表单对象初始化数据
        versionName: "",
        version: "",
        forceUpdate: 1,
        type: "IOS",
        downloadUrl: "",
        content: "",
        versionUpdateDate: "",
      },
      versionUpdateDate: "", // 更新时间
      // 表单验证规则
      formValidate: {
        version: [
          { required: true, message: "版本号不能为空", trigger: "blur" },
        ],
        versionName: [
          { required: true, message: "版本名称不能为空", trigger: "blur" },
        ],
        downloadUrl: [
          { required: true, message: "下载地址不能为空", trigger: "blur" },
        ],
        versionUpdateDate: [{ required: true, message: "更新时间不能为空" }],
      },
      submitLoading: false, // 添加或编辑提交状态
      columns: [
        {
          title: "版本名称",
          key: "versionName",
          minWidth: 100,
        },
        {
          title: "版本号",
          key: "version",
          minWidth: 120,
        },
        {
          title: "强制更新",
          key: "forceUpdate",
          width: 100,
          render: (h, params) => {
            return h(
              "div",
              {
                props: {
                  color: params.row.forceUpdate ? "primary" : "default",
                },
              },
              params.row.forceUpdate == 0 ? "非强制" : "强制"
            );
          },
        },
        {
          title: "类型",
          key: "type",
          minWidth: 80,
          render: (h, params) => {
            if (params.row.type == "IOS") {
              return h("div", [h("span", {}, "苹果")]);
            } else {
              return h("div", [h("span", {}, "安卓")]);
            }
          },
        },

        {
          title: "更新时间",
          key: "versionUpdateDate",
          minWidth: 120,
          sortable: true,
        },

        {
          title: "操作",
          key: "action",
          align: "center",
          width: 230,
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.detail(params.row);
                    },
                  },
                },
                "查看"
              ),
              h(
                "Button",

                {
                  props: {
                    type: "info",
                    size: "small",
                    ghost: true,
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.editAppVersion(params.row);
                    },
                  },
                },
                "修改"
              ),
              h(
                "Button",
                {
                  props: {
                    type: "error",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.remove(params.row);
                    },
                  },
                },
                "删除"
              ),
            ]);
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  methods: {
    // 获取表格数据
    init() {
      this.getData();
    },
    // 分页 修改页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getData();
    },
    // 分页 修改页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getData();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.getData();
    },
    // 获取数据
    getData() {
      this.loading = true;
      API_Setting.appVersionPage(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    //添加app版本信息
    addAppVersion() {
      this.modalVisible = true;
      this.modalTitle = "添加APP版本信息";
      const versionUpdateDate = this.$options.filters.unixToDate(
        new Date() / 1000
      );
      this.form = {
        forceUpdate: 0,
        type: "IOS",
        versionUpdateDate: versionUpdateDate,
        content: " ",
      };
    },
    //编辑app版本信息
    editAppVersion(v) {
      this.modalVisible = true;
      this.modalTitle = "修改APP版本信息";
      this.modalType = 1;
      v.forceUpdate ? (v.forceUpdate = 1) : (v.forceUpdate = 0);
      this.form = v;

      console.log(this.form);
    },
    //保存app版本信息
    saveAppVersion() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          const versionUpdateDate = this.$options.filters.unixToDate(
            this.form.versionUpdateDate / 1000
          );
          this.form.versionUpdateDate = versionUpdateDate;
          this.form.updateTime = versionUpdateDate;
          if (this.modalType == 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete this.form.id;
            API_Setting.addVersion(this.form).then((res) => {
              this.submitLoading = false;
              if (res && res.success) {
                this.$Message.success("添加成功");
                this.modalVisible = false;
                this.getData();
              }
            });
          } else {
            API_Setting.editVersion(this.form, this.form.id).then((res) => {
              this.submitLoading = false;
              if (res && res.success) {
                this.$Message.success("修改成功");
                this.modalVisible = false;
                this.getData();
              }
            });
          }
        }
      });
    },
    // 删除app版本信息
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除么?",
        loading: true,
        onOk: () => {
          // 删除
          API_Setting.deleteVersion(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
              this.getData();
            }
          });
        },
      });
    },
    // 查看
    detail(v) {
      this.queryModalVisible = true;
      this.form = JSON.parse(JSON.stringify(v));
      //时间格式化有问题，所以另外单独给时间赋值
      this.versionUpdateDate = this.form.versionUpdateDate;
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.search-form {
  width: 100%;
}
.tips {
  margin-left: 10px;
  color: #999;
}
</style>
