<template>
  <div class="search">
    <Row>
      <Card>
        <Row @keydown.enter.native="handleSearch">
          <Form
            ref="searchForm"
            :model="searchForm"
            inline
            :label-width="70"
            class="search-form"
          >
            <Form-item label="会员名称" prop="username">
              <Input
                type="text"
                v-model="searchForm.username"
                placeholder="请输入会员名称"
                clearable
                style="width: 200px"
              />
            </Form-item>

            <Form-item label="联系方式" prop="mobile">
              <Input
                type="text"
                v-model="searchForm.mobile"
                placeholder="请输入会员联系方式"
                clearable
                style="width: 200px"
              />
            </Form-item>
            <Button
              @click="handleSearch"
              class="search-btn"
              type="primary"
              icon="ios-search"
              >搜索</Button
            >
          </Form>
        </Row>
        <Table
          :loading="loading"
          border
          :columns="columns"
          :data="data"
          ref="table"
          class="mt_10"
          sortable="custom"
        >
        </Table>
        <Row type="flex" justify="end" class="mt_10">
          <Page
            :current="searchForm.pageNumber"
            :total="total"
            :page-size="searchForm.pageSize"
            @on-change="changePage"
            @on-page-size-change="changePageSize"
            :page-size-opts="[10, 20, 50]"
            size="small"
            show-total
            show-elevator
            show-sizer
          ></Page>
        </Row>
      </Card>
    </Row>

    <!-- 修改模态框 -->
    <Modal
      v-model="descFlag"
      :title="descTitle"
      @on-ok="handleSubmitModal"
      width="500"
    >
      <Form
        ref="formValidate"
        :model="formValidate"
        :rules="ruleValidate"
        :label-width="80"
      >
        <FormItem label="头像">
          <img :src="formValidate.face" class="face" />
          <Button
            type="text"
            class="upload"
            @click="
              () => {
                this.picModelFlag = true;
                this.$refs.ossManage.selectImage = true;
              }
            "
            >修改
          </Button>
          <input type="file" style="display: none" id="file" />
        </FormItem>
        <FormItem label="会员名称" prop="name">
          <Input
            v-model="formValidate.username"
            style="width: 200px"
            disabled
          />
        </FormItem>
        <FormItem label="用户昵称" prop="name">
          <Input v-model="formValidate.nickName" style="width: 200px" />
        </FormItem>
        <FormItem label="性别" prop="sex">
          <RadioGroup
            type="button"
            button-style="solid"
            v-model="formValidate.sex"
          >
            <Radio :label="1">
              <span>男</span>
            </Radio>
            <Radio :label="0">
              <span>女</span>
            </Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="修改密码" prop="password">
          <Input
            type="password"
            style="width: 220px"
            password
            v-model="formValidate.newPassword"
          />
        </FormItem>
        <FormItem label="生日" prop="birthday">
          <DatePicker
            type="date"
            format="yyyy-MM-dd"
            v-model="formValidate.birthday"
            style="width: 220px"
          ></DatePicker>
        </FormItem>
        <FormItem label="所在地" prop="mail">
          <div class="form-item" v-if="!updateRegion">
            <Input disabled style="width: 250px" :value="formValidate.region" />
            <Button
              type="text"
              @click="
                () => {
                  this.updateRegion = !this.updateRegion;
                }
              "
              >修改
            </Button>
          </div>
          <div class="form-item" v-else>
            <region style="width: 250px" @selected="selectedRegion" />
          </div>
        </FormItem>
      </Form>
    </Modal>
    <Modal width="1200px" v-model="picModelFlag">
      <ossManage @callback="callbackSelected" ref="ossManage" />
    </Modal>
  </div>
</template>

<script>
import region from "@/views/lili-components/region";
import * as API_Member from "@/api/member.js";
import ossManage from "@/views/sys/oss-manage/ossManage";

export default {
  name: "memberRecycle",
  components: {
    region,
    ossManage,
  },
  data() {
    return {
      selectedMember: false, //是否是其他组件调用
      descTitle: "", // modal标题
      descFlag: false, //编辑查看框
      openSearch: true, // 显示搜索
      loading: true, // 表单加载状态
      updateRegion: false, // 显示所在地
      searchForm: {
        // 请求参数
        pageNumber: 1,
        pageSize: 10,
        order: "desc",
        username: "",
        mobile: "",
        disabled: "CLOSE",
      },
      picModelFlag: false, // 选择图片
      formValidate: {}, // 表单数据
      ruleValidate: {}, //修改验证
      columns: [
        {
          title: "会员名称",
          align: "left",
          key: "username",
          tooltip: true,
        },
        {
          title: "昵称",
          align: "left",
          key: "nickName",
          tooltip: true,
        },
        {
          title: "联系方式",
          width: 130,
          key: "mobile",
          render: (h, params) => {
            if (params.row.mobile == null) {
              return h("div", [h("span", {}, "")]);
            } else {
              return h("div", [h("span", {}, params.row.mobile)]);
            }
          },
        },
        {
          title: "注册时间",
          key: "createTime",
          width: 180,
        },

        {
          title: "积分数量",
          align: "left",
          width: 120,
          render: (h, params) => {
            return h(
              "div",
              {},
              params.row.point == void 0 ? "0" : params.row.point
            );
          },
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          width: 200,
          fixed: "right",
          render: (h, params) => {
            return h(
              "div",
              {
                style: {
                  display: "flex",
                  justifyContent: "center",
                },
              },
              [
                h(
                  "Button",
                  {
                    props: {
                      size: "small",
                    },
                    style: {
                      marginRight: "5px",
                      display: this.selectedMember ? "block" : "none",
                    },
                    on: {
                      click: () => {
                        this.callback(params.row);
                      },
                    },
                  },
                  "选择"
                ),
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
                      size: "small",
                      type: "success",
                    },
                    style: {
                      marginRight: "5px",
                      display: this.selectedMember ? "none" : "block",
                    },
                    on: {
                      click: () => {
                        this.enable(params.row);
                      },
                    },
                  },
                  "启用"
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
                        this.editPerm(params.row);
                      },
                    },
                  },
                  "编辑"
                ),
              ]
            );
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  methods: {
    // 回调给父级
    callback(val) {
      this.$emit("callback", val);
    },
    // 初始化数据
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
      this.searchForm.pageSize = 10;
      this.getData();
    },
    //查看详情修改
    editPerm(val) {
      this.descTitle = `查看用户 ${val.username}`;
      this.descFlag = true;
      this.updateRegion = false;
      this.getMemberInfo(val.id);
    },
    /**
     * 查询查看会员详情
     */
    getMemberInfo(id) {
      API_Member.getMemberInfoData(id).then((res) => {
        if (res.result) {
          this.$set(this, "formValidate", res.result);
        }
      });
    },
    //查询会员列表
    getData() {
      API_Member.getMemberListData(this.searchForm).then((res) => {
        if (res.success) {
          this.loading = false;
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    // 选中的图片
    callbackSelected(val) {
      this.picModelFlag = false;
      this.formValidate.face = val.url;
    },

    // 选中的地址
    selectedRegion(val) {
      this.region = val[1];
      this.regionId = val[0];
    },

    //详细
    detail(row) {
      this.$router.push({ name: "member-detail", query: { id: row.id } });
    },
    //启用会员
    enable(v) {
      let params = {
        memberIds: [v.id],
        disabled: true,
      };
      this.$Modal.confirm({
        title: "提示",
        content: "<p>启用用此会员？</p>",
        onOk: () => {
          API_Member.updateMemberStatus(params).then((res) => {
            if (res.success) {
              this.$Message.success("禁用成功");
              this.getData();
            } else {
              // this.$Message.error(res.message);
            }
          });
        },
      });
    },

    // 提交修改数据
    handleSubmitModal() {
      const { nickName, sex, username, face, newPassword, id } =
        this.formValidate;
      let time = new Date(this.formValidate.birthday);
      let birthday =
        time.getFullYear() + "-" + (time.getMonth() + 1) + "-" + time.getDate();
      let submit = {
        regionId: this.regionId || "",
        region: this.region || "",
        nickName,
        username,
        sex,
        birthday,
        face: face || "",
        id,
      };
      if (newPassword) {
        submit.password = this.md5(newPassword);
      }
      API_Member.updateMember(submit).then((res) => {
        if (res.result) {
          this.$Message.success("修改成功！");
          this.init();
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.face {
  width: 60px;
  height: 60px;
  border-radius: 50%;
}
</style>
