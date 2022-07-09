<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" @keydown.enter.native="handleSearch" inline :label-width="70" class="search-form">
        <Form-item label="会员名称" prop="memberName">
          <Input type="text" v-model="searchForm.memberName" clearable placeholder="请输入会员名称" style="width: 200px" />
        </Form-item>
        <Form-item label="商品名称" prop="goodsName">
          <Input type="text" v-model="searchForm.goodsName" clearable placeholder="请输入商品名" style="width: 200px" />
        </Form-item>
        <Form-item label="评价" prop="orderStatus">
          <Select v-model="searchForm.grade" placeholder="请选择" clearable style="width: 200px">
            <Option value="GOOD">好评</Option>
            <Option value="MODERATE">中评</Option>
            <Option value="WORSE">差评</Option>
          </Select>
        </Form-item>
        <Form-item label="评论日期">
          <DatePicker v-model="selectDate" type="datetimerange" format="yyyy-MM-dd HH:mm:ss" clearable @on-change="selectDateRange" placeholder="选择起始时间" style="width: 200px"></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
        <Button @click="handleReset" class="search-btn">重置</Button>
      </Form>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table" class="mt_10"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]" size="small"
          show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
    <Modal :title="modalTitle" v-model="modalVisible" :mask-closable="false" :width="500">
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="评价内容">
          <span v-if="!content">暂无评价</span>
          <span v-else>
            <div>
              <Input v-model="content" type="textarea" maxlength="200" disabled :rows="4" clearable style="width:90%" />
            </div>
          </span>
        </FormItem>
        <FormItem label="评价图片" style="padding-top: 10px" v-if="detailInfo.haveImage == 1">
          <upload-pic-thumb v-model="image" :disable="true" :remove="false" :isView="true"></upload-pic-thumb>
        </FormItem>
        <FormItem label="回复内容" prop="reply">
          <Input v-if="replyStatus == false" v-model="form.reply" type="textarea" maxlength="200" :rows="4" clearable style="width:90%" />
          <span v-else>
            <Input v-model="form.reply" type="textarea" maxlength="200" disabled :rows="4" clearable style="width:90%" />
          </span>
        </FormItem>
        <FormItem label="回复图片" prop="replyImage" style="padding-top: 18px" v-if="detailInfo.haveReplyImage == 1 || replyStatus == false">
          <upload-pic-thumb v-if="replyStatus == false" v-model="form.replyImage" :limit="5"></upload-pic-thumb>
          <upload-pic-thumb v-else v-model="form.replyImage" :disable="true" :remove="false"></upload-pic-thumb>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button v-if="replyStatus == false" type="primary" :loading="submitLoading" @click="handleSubmit">回复
        </Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import * as API_Member from "@/api/member";
import uploadPicThumb from "@/views/my-components/lili/upload-pic-thumb";

export default {
  name: "memberComment",
  components: {
    uploadPicThumb,
  },
  data() {
    return {
      detailInfo: {}, // 详情信息
      image: [], //评价图片
      replyStatus: false, //回复状态
      modalVisible: false, // 添加或编辑显示
      modalTitle: "", // 添加或编辑标题
      loading: true, // 表单加载状态
      content: "", //评价内容
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        startDate: "", // 起始时间
        endDate: "", // 终止时间
      },
      selectDate: null,
      form: {
        replyImage: [],
        reply: "",
      },
      // 表单验证规则
      formValidate: {
        reply: [{ required: true, message: "请输入回复内容", trigger: "blur" }],
      },
      submitLoading: false, // 添加或编辑提交状态
      columns: [
        // 表头
        {
          title: "会员名称",
          key: "memberName",
          minWidth: 150,
          tooltip: true,
        },
        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 150,
          tooltip: true,
        },
        {
          title: "评价内容",
          key: "content",
          minWidth: 300,
          tooltip: true,
        },
        {
          title: "评价",
          key: "grade",
          width: 100,
          render: (h, params) => {
            if (params.row.grade == "GOOD") {
              return h("Tag", { props: { color: "green" } }, "好评");
            } else if (params.row.grade == "MODERATE") {
              return h("Tag", { props: { color: "orange" } }, "中评");
            } else {
              return h("Tag", { props: { color: "red" } }, "差评");
            }
          },
        },
        {
          title: "状态",
          key: "status",
          width: 100,
          render: (h, params) => {
            if (params.row.status === "OPEN") {
              return h("Tag", { props: { color: "green" } }, "展示");
            } else {
              return h("Tag", { props: { color: "red" } }, "隐藏");
            }
          },
        },
        {
          title: "回复状态",
          key: "replyStatus",
          width: 110,
          render: (h, params) => {
            if (params.row.replyStatus) {
              return h("Tag", { props: { color: "green" } }, "已回复");
            } else {
              return h("Tag", { props: { color: "blue" } }, "未回复");
            }
          },
        },

        {
          title: "创建日期",
          key: "createTime",
          width: 170,
        },

        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: 'right',
          width: 120,
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
                "详细"
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
    init() {
      // 初始化数据
      this.getDataList();
    },
    // 改变页数
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },
    // 改变页码
    changePageSize(v) {
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 重置
    handleReset() {
      this.searchForm = {};
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 清除选中状态
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    // 选择日期回调
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    // 获取列表数据
    getDataList() {
      this.loading = true;
      API_Member.getMemberReview(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    //回复
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          API_Member.replyMemberReview(this.form.id, this.form).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("回复成功");
              this.getDataList();
              this.modalVisible = false;
            }
          });
        }
      });
    },
    // 获取详情
    detail(v) {
      this.form.replyImage = [];
      this.loading = true;
      API_Member.getMemberInfoReview(v.id).then((res) => {
        this.loading = false;
        if (res.success) {
          //赋值
          this.form.id = res.result.id;
          this.content = res.result.content;
          this.form.reply = res.result.reply;
          this.replyStatus = res.result.replyStatus;
          if (res.result.images) {
            this.image = (res.result.images || "").split(",");
          }
          if (res.result.replyImage) {
            this.form.replyImage = (res.result.replyImage || "").split(",");
          }
          this.detailInfo = res.result;
          //弹出框
          this.modalVisible = true;
          this.modalTitle = "详细";
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss">
// 建议引入通用样式 可删除下面样式代码
@import "@/styles/table-common.scss";
</style>
