<template>
  <div class="model-carousel">
    <div class="nav-body clearfix">
      <!-- 侧边导航占位 -->
      <div class="nav-side"></div>
      <div class="nav-content">
        <!-- 轮播图 -->
        <Carousel autoplay>
          <CarouselItem v-for="(item, index) in data.options.list" :key="index">
            <div style="overflow: hidden">
              <img
                :src="item.img"
                width="790"
                @click="linkTo(item.url)"
                height="340"
                class="hover-pointer"
              />
            </div>
          </CarouselItem>
        </Carousel>
      </div>
      <div class="nav-right">
        <div class="person-msg">
          <img :src="userInfo.face" v-if="userInfo.face" alt />
          <Avatar icon="ios-person" class="mb_10" v-else size="80" />
          <div>
            Hi, {{
              userInfo.nickName || `欢迎来到${config.title}` | secrecyMobile
            }}
          </div>
          <div v-if="userInfo.id">
            <Button class="btns" shape="circle" @click="$router.push('home')"
              >会员中心</Button
            >
          </div>
          <div v-else>
            <Button class="btns" @click="$router.push('login')" shape="circle"
              >请登录</Button
            >
          </div>
        </div>
        <div class="shop-msg">
          <div>
            <span>常见问题</span>
            <ul class="article-list">
              <li
                class="ellipsis"
                :alt="article.title"
                v-for="(article, index) in articleList"
                :key="index"
                @click="goArticle(article.id)"
              >
                {{ article.title }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { articleList } from "@/api/common.js";
import storage from "@/plugins/storage";
export default {
  name: "modelCarousel",
  props: ["data"],
  data() {
    return {
      config: require("@/config"),
      userInfo: {}, // 用户信息
      articleList: [], // 常见问题
      params: {
        // 请求常见问题参数
        pageNumber: 1,
        pageSize: 5,
        type: "ANNOUNCEMENT",
        sort: "sort",
      },
    };
  },
  methods: {
    getArticleList() {
      // 获取常见问题列表
      articleList(this.params).then((res) => {
        if (res.success) {
          this.articleList = res.result.records;
        }
      });
    },
    goArticle(id) {
      // 跳转文章详情
      let routeUrl = this.$router.resolve({
        path: "/article",
        query: { id },
      });
      window.open(routeUrl.href, "_blank");
    },
  },
  mounted() {
    if (storage.getItem("userInfo"))
      this.userInfo = JSON.parse(storage.getItem("userInfo"));
    this.getArticleList();
  },
};
</script>

<style scoped lang="scss">
.model-carousel {
  width: 1200px;
  height: 340px;
  overflow: hidden;
}

.btns {
  background-color:#363634 ;
  line-height:30px;
  color: white !important;
}
.btns:hover {
  background-color: #363634;
  line-height:32px !important;
  color: #e5d790 !important;
}
/* 导航主体 */
.nav-body {
  width: 1200px;
  height: 340px;
  margin: 0px auto;
}

.nav-side {
  height: 100%;
  width: 200px;
  float: left;
  padding: 0px;
  color: #fff;
  background-color: #6e6568;
}

/*导航内容*/
.nav-content {
  width: 790px;
  overflow: hidden;
  float: left;
  position: relative;
}
.nav-right {
  float: left;
  width: 210px;
  .person-msg {
    display: flex;
    align-items: center;
    flex-direction: column;
    margin: 20px auto;
    button {
      height: 30px !important;
      margin-top: 10px;
      line-height: 30px;
      border: none;
    }
    .ivu-btn-default {
      // color: $theme_color;
      // border-color: $theme_color;
    }
    img {
      margin-bottom: 10px;
      width: 80px;
      height: 80px;
      border-radius: 50%;
    }
  }
  .shop-msg {
    div {
      width: 100%;
      margin: 10px 27px;
      span {
        cursor: pointer;
        text-align: center;
        font-weight: bold;
        margin-left: 5px;
      }
      span:nth-child(1) {
        @include content_color($theme_color);
        margin-left: 0;
      }
      span:nth-child(2) {
        font-weight: normal;
      }
      span:nth-child(3):hover {
        color: $theme_color;
      }
    }
    ul {
      li {
        cursor: pointer;
        margin: 5px 0;
        color: #999395;
        width: 150px;
        font-size: 12px;
        &:hover {
          color: $theme_color;
        }
      }
    }
  }
}
</style>
