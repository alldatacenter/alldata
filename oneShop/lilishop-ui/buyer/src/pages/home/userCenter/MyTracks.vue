<template>
  <div class="wrapper">
    <!-- 卡片组件 -->
    <card _Title="我的足迹" :_Size="16"></card>
    <Button class="del-btn" @click="clearAll" type="primary">删除全部</Button>
    <!-- 订单列表 -->
    <empty v-if="list.length === 0" />
    <ul class="track-list" v-else>
      <li
        v-for="(item, index) in list"
        :key="index"
        @click="goodsDetail(item.id, item.goodsId)"
      >
        <img :src="item.thumbnail" :alt="item.thumbnail" width="200" height="200" />
        <p class="ellipsis">{{ item.goodsName }}</p>
        <p>{{ item.price | unitPrice("￥") }}</p>
        <span class="del-icon" @click.stop="clearById(item.goodsId)">
          <Icon type="md-trash" />
        </span>
      </li>
    </ul>
    <!-- 分页 -->
    <div class="page-size">
      <Page
        :total="total"
        @on-change="changePageNum"
        @on-page-size-change="changePageSize"
        :page-size="params.pageSize"
        show-sizer
      >
      </Page>
    </div>
  </div>
</template>

<script>
import { tracksList, clearTracks, clearTracksById } from "@/api/member";
export default {
  name: "MyTrack",
  data() {
    return {
      list: [], // 我的足迹，商品列表
      spinShow: false, // 控制loading是否加载
      params: {
        pageNumber: 1,
        pageSize: 30,
        order: "desc",
        sort: "updateTime",
      },
      total: 0,
    };
  },
  mounted() {
    this.getList();
  },
  methods: {
    goodsDetail(skuId, goodsId) {
      // 跳转商品详情
      let routeUrl = this.$router.resolve({
        path: "/goodsDetail",
        query: { skuId, goodsId },
      });
      window.open(routeUrl.href, "_blank");
    },
    // 跳转店铺首页
    shopPage(id) {
      let routeUrl = this.$router.resolve({
        path: "/Merchant",
        query: { id: id },
      });
      window.open(routeUrl.href, "_blank");
    },
    clearAll() {
      // 清除全部足迹
      this.$Modal.confirm({
        title: "删除",
        content: "<p>确定要删除全部足迹吗？</p>",
        onOk: () => {
          clearTracks().then((res) => {
            if (res.success) {
              this.$Message.success("删除成功");
              this.getList();
            }
          });
        },
        onCancel: () => {},
      });
    },
    clearById(id) {
      // 清除全部足迹
      clearTracksById(id).then((res) => {
        if (res.success) {
          this.$Message.success("删除成功");
          this.getList();
        }
      });
    },
    changePageNum(val) {
      // 修改页码
      this.params.pageNumber = val;
      this.getList();
    },
    changePageSize(val) {
      // 修改页数
      this.params.pageNumber = 1;
      this.params.pageSize = val;
      this.getList();
    },
    getList() {
      // 获取足迹列表
      this.spinShow = true;
      tracksList(this.params).then((res) => {
        this.spinShow = false;
        if (res.success && res.result.records.length) {
          this.list = res.result.records;
        } else {
          this.list = [];
        }
      });
    },
  },
};
</script>
<style scoped lang="scss">
.wrapper {
  margin-bottom: 40px;
}
.del-btn {
  margin: 0 0 10px 15px;
}

.track-list {
  display: flex;
  flex-wrap: wrap;
  li {
    width: 200px;
    overflow: hidden;
    margin-left: 15px;
    margin-bottom: 10px;
    border: 1px solid #eee;
    position: relative;
    &:hover {
      cursor: pointer;
      box-shadow: 1px 1px 3px #999;
      .del-icon {
        display: block;
      }
    }
    p {
      padding: 0 5px;
      margin: 3px 0;
    }
    p:nth-child(2) {
      color: #999;
    }
    p:nth-child(3) {
      color: $theme_color;
    }
    .del-icon {
      display: none;
      font-size: 30px;
      background-color: rgba(0, 0, 0, 0.3);
      position: absolute;
      width: 40px;
      height: 40px;
      line-height: 40px;
      text-align: center;
      right: 0;
      top: 0;
      cursor: pointer;
    }
  }
}
.page-size {
  margin: 15px 0px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
</style>
