<template>
  <div class="lang-icon">
    <Dropdown @on-click="langChange">
      <Icon type="md-globe" size="26"/>
      <DropdownMenu slot="list">
        <DropdownItem name="zh-CN">简体中文</DropdownItem>
        <DropdownItem name="en-US">English</DropdownItem>
      </DropdownMenu>
    </Dropdown>
  </div>
</template>

<script>
export default {
  name: "langSwitch",
  methods: {
    langChange(v) {
      this.$i18n.locale = v;
      this.$store.commit("switchLang", v);
    }
  }
};
</script>

<style lang="scss" scoped>
.lang-icon {
  position: fixed;
  top: 2vh;
  right: 1.5vw;
  cursor: pointer;
}
</style>
