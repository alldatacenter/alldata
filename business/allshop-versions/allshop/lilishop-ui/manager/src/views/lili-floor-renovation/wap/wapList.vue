<template>
  <div class="wrapper">
    <Card class="category">
      <div :class="{active:i == selectedIndex}" class="category-item" v-for="(typeItem,i) in pageTypes" :key="typeItem.type">
        <div @click="clickType(typeItem.type,i)">{{typeItem.title}}</div>
      </div>
    </Card>
    <Card class="content">
      <Button type="primary" @click="handleAdd()">添加页面</Button>
      <div class="list">
        <Spin size="large" fix v-if="loading"></Spin>
        <div class="item item-title" >
          <div>页面名称</div>
          <div class="item-config">
            <div>状态</div>
            <div>操作</div>
          </div>
        </div>

        <div class="item" v-for="(item, index) in list" :key="index">
          <div>{{ item.name || "暂无模板昵称" }}</div>
          <div class="item-config">
            <i-switch v-model="item.pageShow" @on-change="changeSwitch(item)">
              <span slot="open">开</span>
              <span slot="close">关</span>
            </i-switch>
            <Button type="info" placement="right" @click="handleEdit(item)" size="small">修改</Button>
            <Poptip confirm title="删除此模板？" @on-ok="handleDel(item)" >
              <Button type="error" size="small">删除</Button>
            </Poptip>
          </div>
        </div>
        <div class="no-more" v-if="list.length ==0">暂无更多模板</div>
      </div>
      <Page :total="total" size="small" @on-change="(val) => {params.pageNumber = val; } " :current="params.pageNumber" :page-size="params.pageSize" show-sizer  :page-size-opts="[10, 20, 50]" @on-page-size-change="changePageSize"/>

    </Card>

  </div>

</template>
<script>
import * as API_Other from "@/api/other.js";
export default {
  // components: {region},
  data() {
    return {
      selectedIndex: 0, // 装修那个页面的下标
      columns: [ // 表头
        {
          title: "页面名称",
          key: "name",
        },
        {
          title: "状态",
        },
        {
          title: "操作",
          key: "action",
        },
      ],

      loading: false, // 加载状态
      pageTypes: [ // 装修类型
        {
          type: "INDEX",
          title: "首页",
        },
        {
          type: "SPECIAL",
          title: "专题",
        },
      ],
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 10,
        sort: "createTime",
        order: "desc",
        pageType: "INDEX",
        pageClientType: "H5",
      },
      total: 0, // 页面数量
      list: [], // 总数据
    };
  },
  watch: {
    params: {
      handler(val) {
        // this.pageNumber++;
        this.init();
      },
      deep: true,
    },
  },
  mounted() {
    this.init();
  },
  methods: {
    // 切换tab
    clickType(val,index) {
      this.params.pageNumber = 1
      this.selectedIndex = index
      this.params.pageType = val;
    },
    // 是否发布
    changeSwitch(item) {
      this.loading = true;
      API_Other.releasePageHome(item.id).then((res) => {
        if (res.result) {
          this.loading = false;
          this.$Message.success("发布成功");
          this.init();
        }

        this.init();

        this.loading = false;
      });
    },
    // 初始化数据
    init() {
      this.loading = true;
      API_Other.getHomeList(this.params).then((res) => {
        if (!res.result) return false;
        this.loading = false;
        res.result.records.forEach((item) => {
          if (item.pageShow == "OPEN") {
            item.pageShow = true;
          } else {
            item.pageShow = false;
          }
        });
        this.list = res.result.records;
        console.log(this.list);
        this.total = res.result.total;
      });
    },
    // 装修楼层
    handleEdit(val) {
      this.$router.push({
        path: "/floorList/main",
        query: { id: val.id, name: val.name, type: val.pageShow },
      });
    },
    // 添加模板
    handleAdd() {
      this.$router.push({
        path: "/floorList/main",
      });
    },
     // 分页 改变页数
     changePageSize(v) {
      this.params.pageNumber = 1;
      this.params.pageSize = v;
      this.init();
    },
    // 删除模板
    handleDel(val) {
      this.loading = true;
      API_Other.removePageHome(val.id).then((res) => {
        if (res.result) {
          this.loading = false;
          this.init();
          this.$Message.success("删除成功");
        }

        this.loading = false;
      });
    },
  },
};
</script>
<style scoped lang="scss">
.category-item {
  cursor: pointer;
  padding: 4px;
}
.active {
  background: #ededed;
}
.item-title {
  background: #d7e7f5!important;
  height: 54px;
}
.no-more {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
.wrapper {
  background: #fff;
  padding: 20px;
  display: flex;
}
.category {
  flex: 2;
}
.content {
  flex: 8;
}
* {
  margin: 5px;
}
.list {
  min-height: 600px;
  position: relative;
}
.item:nth-of-type(2) {
  margin: 0 5px;
}
.item {
  width: 100% !important;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  > .item-config {
    width: 180px;
    display: flex;
    justify-content: space-around;
    align-items: center;
  }
}
.item:nth-of-type(2n+1) {
  background: #f5f7fa;
}
</style>
