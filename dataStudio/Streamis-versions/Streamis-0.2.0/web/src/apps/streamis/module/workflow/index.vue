<template>
  <div class="container">
    <div
      v-for="(item, index) in workflowData.nodes"
      :key="index"
      class="itemWrap"
    >
      <div class="nodeWrap">
        <div class="imgWrap">
          <img :src="require('../../assets/images/' + item.icon)" />
        </div>
        <p>{{ item.title }}</p>
      </div>
      <div v-show="workflowData.nodes.length - 1 !== index" class="arrow">
        <img src="../../assets/images/u2063.png" />
      </div>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      workflowData: {
        nodes: [
          {
            key: "fc5f3a40-27f9-4565-8543-37936d7a5fea",
            title: "kalfka_source",
            nodeType: "kalfka",
            icon: "u2450.png"
          },
          {
            key: "32838f20-6519-4e63-bec6-2dce79673344",
            title: "transform_sql",
            nodeType: "flink",
            icon: "u2453.png"
          },
          {
            key: "bdfed3a5-d110-4d61-bde5-0d1536201364",
            title: "save_to_db",
            nodeType: "mysql",
            icon: "u2451.png"
          },
          {
            key: "bdfed3a5-d110-4d61-bde5-0d1536201389",
            title: "show_charts",
            nodeType: "charts",
            icon: "u2466.svg"
          },
          {
            key: "bdfed3a5-d110-4d61-bde5-0d1536201364",
            title: "save_to_db",
            nodeType: "mysql",
            icon: "u2451.png"
          },
          {
            key: "bdfed3a5-d110-4d61-bde5-0d1536201389",
            title: "show_charts",
            nodeType: "charts",
            icon: "u2466.svg"
          }
        ]
      }
    };
  }
};
</script>
<style lang="scss" scoped>
.container {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
}
.itemWrap {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
}
.nodeWrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  border: 2px solid rgba(121, 121, 121, 1);
  border-radius: 5px;
  box-sizing: border-box;
  padding: 6px 8px;
  min-width: 200px;
  .imgWrap {
    height: 40px;
    width: 60px;
    & img {
      height: 100%;
    }
  }
}
.arrow {
  height: 53px;
  position: relative;
  & img{
    position: absolute;
    top: -2px;
  }
}
</style>
