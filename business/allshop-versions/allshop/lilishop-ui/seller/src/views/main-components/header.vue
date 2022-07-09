<template>
  <div>
    <Row class="header">
      <img class="logo" src="@/assets/logo.png" >
    </Row>
  </div>
</template>

<script>
export default {
  name: "header",
};
</script>

<style lang="scss" scoped>
.header {
  margin-bottom: 6vh;
  align-items: center;
  display: flex;
  justify-content: center !important;
}
.logo {
  width: 440px;
  height: 158px;
}
</style>
