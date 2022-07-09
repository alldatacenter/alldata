

<template>
  <div class="error500">
    <div class="error500-body-con">
      <Card>
        <div class="error500-body-con-title">
          5<span class="error500-0-span"
            ><Icon type="social-freebsd-devil"></Icon></span
          ><span class="error500-0-span"
            ><Icon type="social-freebsd-devil"></Icon
          ></span>
        </div>
        <p class="error500-body-con-message">Oops! the server is wrong</p>
        <div class="error500-btn-con">
          <Button @click="goHome" size="large" style="width: 200px" type="text"
            >返回首页</Button
          >
          <Button
            @click="backPage"
            size="large"
            style="width: 200px; margin-left: 40px"
            type="primary"
            >返回上一页</Button
          >
        </div>
      </Card>
    </div>
  </div>
</template>

<script>
export default {
  name: "Error500",
  methods: {
    // 返回上一页
    backPage() {
      this.$router.go(-1);
    },
    // 返回首页
    goHome() {
      this.$router.push({
        name: "home_index",
      });
    },
  },
};
</script>
<style lang="scss" scoped>
@keyframes error500animation {
  0% {
    transform: rotateZ(0deg);
  }
  20% {
    transform: rotateZ(-10deg);
  }
  40% {
    transform: rotateZ(5deg);
  }
  60% {
    transform: rotateZ(-5deg);
  }
  80% {
    transform: rotateZ(10deg);
  }
  100% {
    transform: rotateZ(0deg);
  }
}
.error500 {
  &-body-con {
    width: 700px;
    height: 500px;
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    &-title {
      text-align: center;
      font-size: 240px;
      font-weight: 700;
      color: #2d8cf0;
      height: 260px;
      line-height: 260px;
      margin-top: 40px;
      .error500-0-span {
        display: inline-block;
        position: relative;
        width: 170px;
        height: 170px;
        border-radius: 50%;
        border: 20px solid #ed3f14;
        color: #ed3f14;
        margin-right: 10px;
        i {
          display: inline-block;
          font-size: 120px;
          position: absolute;
          bottom: -10px;
          left: 10px;
          transform-origin: center bottom;
          animation: error500animation 3s ease 0s infinite alternate;
        }
      }
    }
    &-message {
      display: block;
      text-align: center;
      font-size: 30px;
      font-weight: 500;
      letter-spacing: 4px;
      color: #dddde2;
    }
  }
  &-btn-con {
    text-align: center;
    padding: 20px 0;
    margin-bottom: 40px;
  }
}
</style>
