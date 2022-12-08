<template>
  <div class="search">
    <Card>
      <Form
        ref="searchForm"
        :model="searchForm"
        inline
        :label-width="70"
        class="search-form"
      >
        <Form-item label="搜索日志" prop="searchKey">
          <Input
            type="text"
            v-model="searchForm.searchKey"
            placeholder="请输入搜索日志内容"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="操作人" prop="operatorName">
          <Input
            type="text"
            v-model="searchForm.operatorName"
            placeholder="请输入操作人"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="创建时间">
          <DatePicker
            type="daterange"
            v-model="selectDate"
            format="yyyy-MM-dd"
            clearable
            @on-change="selectDateRange"
            placeholder="选择起始时间"
            style="width: 200px"
          ></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn"
          >搜索</Button
        >
      </Form>
      <Row class="operation padding-row">
        <Button @click="getLogList" icon="md-refresh">刷新</Button>
        <Button type="dashed" @click="openTip = !openTip">{{
          openTip ? "关闭提示" : "开启提示"
        }}</Button>
      </Row>
      <Row v-show="openTip">
        <Alert show-icon>
          <span>展示详细内容</span>
          <Icon type="ios-bulb-outline" slot="icon"></Icon>
          <i-switch size="large" v-model="showDev">
            <span slot="open">开发</span>
            <span slot="close">普通</span>
          </i-switch>
        </Alert>
      </Row>

      <Table
        v-if="showDev"
        :loading="loading"
        border
        :columns="columns_dev"
        :data="data"
        ref="table"
        sortable="custom"
      >
      </Table>
      <Table
        v-else
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
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
          show-totalzx
          show-elevator
          show-sizer
        ></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import { getLogListData } from "@/api/index";

export default {
  name: "log-manage",
  data() {
    return {
      openTip: false, // 开启提示
      loading: true, // 加载状态
      selectDate: null, // 选择时间段
      showDev: false, //展示进阶日志
      searchForm: {
        // 请求参数
        type: 1,
        key: "",
        operatorName: "",
        pageNumber: 1,
        pageSize: 10,
        startDate: "",
        endDate: "",
        sort: "createTime",
        order: "desc",
      },
      columns: [
        // 表头
        {
          title: "操作名称",
          key: "name",
          width: 150,
          fixed: "left",
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "日志内容",
          key: "customerLog",
          minWidth: 200,
          fixed: "left",
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "操作用户",
          minWidth: 115,
          key: "username",
          width: 120,
          tooltip: true,
        },
        {
          title: "操作时间",
          key: "createTime",
          align: "center",
          width: 170,
        },
      ],
      columns_dev: [
        {
          title: "操作名称",
          key: "name",
          minWidth: 100,
          fixed: "left",
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "日志内容",
          key: "customerLog",
          minWidth: 120,
          fixed: "left",
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "操作用户",
          key: "username",
          width: 115,
        },
        {
          title: "IP",
          key: "ip",
          width: 150,
        },
        {
          title: "IP信息",
          key: "ipInfo",
          width: 150,
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "请求路径",
          width: 150,
          ellipsis: false,
          tooltip: true,
          key: "requestUrl",
        },
        {
          title: "请求类型",
          key: "requestType",
          width: 130,
          align: "center",
          filters: [
            {
              label: "GET",
              value: "GET",
            },
            {
              label: "POST",
              value: "POST",
            },
            {
              label: "PUT",
              value: "PUT",
            },
            {
              label: "DELETE",
              value: "DELETE",
            },
          ],
          filterMultiple: false,
          filterMethod(value, row) {
            if (value == "GET") {
              return row.requestType == "GET";
            } else if (value == "POST") {
              return row.requestType == "POST";
            } else if (value == "PUT") {
              return row.requestType == "PUT";
            } else if (value == "DELETE") {
              return row.requestType == "DELETE";
            }
          },
        },
        {
          title: "请求参数",
          minWidth: 100,
          key: "requestParam",
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "耗时-毫秒",
          key: "costTime",
          width: 140,
          align: "center",
          filters: [
            {
              label: "≤300毫秒",
              value: 0,
            },
            {
              label: "300毫秒<x<1000毫秒",
              value: 0.3,
            },
            {
              label: ">1000毫秒",
              value: 1,
            },
          ],
          filterMultiple: false,
          filterMethod(value, row) {
            if (value == 0) {
              return row.costTime <= 300;
            } else if (value == 0.3) {
              return row.costTime > 300 && row.costTime < 1000;
            } else {
              return row.costTime > 1000;
            }
          },
        },
        {
          title: "操作时间",
          key: "createTime",
          align: "center",
          width: 170,
          sortType: "desc",
        },
      ],
      data: [], // 日志数据
      total: 0, // 数据总数
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getLogList();
    },
    // 分页 修改页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getLogList();
    },
    // 分页 修改页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getLogList();
    },
    // 起止时间从新赋值
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.getLogList();
    },
    // 获取日志数据
    getLogList() {
      this.loading = true;
      getLogListData(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
