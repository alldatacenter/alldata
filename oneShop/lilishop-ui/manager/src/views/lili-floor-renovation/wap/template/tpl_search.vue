<template>
    <div class="layout">
        <div class="search">
            <Icon type="ios-search" />{{res.list[0].title}}
        </div>
    </div>
</template>
<script>
export default {
    props: ['res']
}
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.search{
    height: 32px;
    border-radius: 5px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ededed;
}
.layout{
    background: #fff;
}
</style>