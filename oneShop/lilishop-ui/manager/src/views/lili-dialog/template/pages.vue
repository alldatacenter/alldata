<template>
  <div class="wrapper">
    <!-- TODO 目前数据少暂且不用 -->
    <!-- <div class="list">
      <div class="list-item active">
        文章页
      </div>
    </div> -->
    <div class="content">
      <Article @callbacked="callbackArticle" :selected="true" />
    </div>

  </div>
</template>
<script>
import Article from "@/views/page/article-manage/articleList.vue";
export default {
  components: {
    Article,
  },
  data() {
    return {};
  },
  methods: {
    callbackArticle(val) {
      val.___type = "pages";

      val.___path = "/pages/passport/article";

      this.$emit("selected", [val]);
    },
  },
};
</script>
<style lang="scss" scoped>
/deep/ .ivu-card-body {
  height: 414px;
  overflow: auto;
}
.ivu-table-wrapper ivu-table-wrapper-with-border {
  height: 300px !important;
}
.list {
  margin: 0 1.5%;
  height: 400px;
  overflow: auto;
  > .list-item {
    padding: 10px;
    transition: 0.35s;
    cursor: pointer;
  }
  .list-item:hover {
    background: #ededed;
  }
}
.list {
  flex: 2;
  width: auto;
}
.content {
  overflow: hidden;
  flex: 8;
  height: 431px;
}
.active {
  background: #ededed;
}
.wrapper {
  height: 416px;
  overflow: hidden;
}
/deep/ .ivu-table {
  height: 300px !important;
  overflow: auto;
}
/deep/ .ivu-card-body {
  padding: 0;
  height: auto;
}
</style>
