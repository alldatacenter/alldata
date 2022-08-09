<template>
    <div class="layout">
        <div class="flex-one">
            <img :src="res.list[0].img" alt="">
        </div>
    </div>
</template>
<script>
export default {
    props: ['res']
}
</script>
<style lang="scss" scoped>
@import './tpl.scss';
.flex-one{
    width: 100%;
    display: block;
    height: 110px;
    overflow: hidden;
    >img{
        width: 100%;
        height: 100%;
    }
}
</style>