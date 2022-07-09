<template>
  <div class="foot">
    <Row type="flex" justify="space-around" class="help">
      <a class="item" :href="config.website" target="_blank">帮助</a>
      <a class="item" :href="config.website" target="_blank">隐私</a>
      <a class="item" :href="config.website" target="_blank">条款</a>
    </Row>
    <Row type="flex" justify="center" class="copyright">
      Copyright © {{year}} - Present
      <a :href="config.website" class="href" target="_blank" style="margin:0 5px;">{{config.title}}</a>
    </Row>
  </div>
</template>

<script>

const config = require('@/config/index')
export default {
  name: "footer",
  data() {
    return {
      config,
      year: new Date().getFullYear(), // 年
    };
  },
};
</script>

<style lang="scss" scoped>
.foot {
  position: fixed;
  bottom: 4vh;
  width: 368px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  .help {
    margin: 0 auto;
    margin-bottom: 1vh;
    width: 60%;
    .item {
      color: rgba(0, 0, 0, 0.45);
    }
    :hover {
      color: rgba(0, 0, 0, 0.65);
    }
  }
}

</style>
