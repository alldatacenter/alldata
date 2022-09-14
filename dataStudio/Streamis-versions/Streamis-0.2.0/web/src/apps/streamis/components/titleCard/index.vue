<template>
  <div class="container">
    <div class="title">
      <div />
      <p>{{ title }}</p>
    </div>
    <slot />
  </div>
</template>
<script>
export default {
  props: {
    title: String
  }
};
</script>
<style lang="scss" scoped>
.container {
  border: 1px solid rgba(228, 228, 228, 1);
  box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.35);
  border-radius: 5px;
  box-sizing: border-box;
  padding: 20px 30px;
}
.title {
  display: flex;
  align-items: flex-start;
  padding-bottom: 15px;
  & div {
    width: 5px;
    height: 22px;
    background-color: rgba(45, 140, 240, 1);
  }
  & p {
    font-weight: 700;
    font-size: 16px;
    color: #797979;
    margin-left: 10px;
  }
}
</style>
