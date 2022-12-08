<template>
  <div id="main" class="app-main">
    <router-view></router-view>
  </div>
</template>

<script>
export default {

};
</script>

<style>
html,
body {
  width: 100%;
  height: 100%;
  background: #f0f0f0;
}

.app-main {
  width: 100%;
  height: 100%;
}

.br button {
  margin-right: 5px;
}

.operation button {
  margin-right: 5px;
}

.ivu-btn-text:focus {
  box-shadow: none !important;
}

.ivu-tag {
  cursor: pointer;
}
.tox-notifications-container{
  display: none !important;
}
</style>
