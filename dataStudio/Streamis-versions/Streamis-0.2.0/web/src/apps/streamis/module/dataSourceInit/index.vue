<template>
  <div class="right-panel">
    <div class="panel-content">
      <img src="../../assets/images/u2680.png" />
      <span>请先选择一个Kafka Topic</span>
    </div>
  </div>
</template>

<script>
export default {
}
</script>

<style lang="scss">
  .right-panel{
      // height: 532px;
      height: 100%;
      border-left: 1px solid #AEAEAE;
      position: relative;
    .panel-content{
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      span{
          height:100%;
          display:inline-block;
          vertical-align:middle;
          font-family: 'Arial Negreta', 'Arial Normal', 'Arial';
          font-weight: 700;
          font-style: normal;
          font-size: 28px;
          color: #A1A1A1;
          margin-left: 60px;
        }
        img{
          vertical-align:middle;
        }
      }
  }
</style>