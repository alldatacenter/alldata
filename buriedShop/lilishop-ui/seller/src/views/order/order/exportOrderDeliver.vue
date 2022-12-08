<template>
  <Card>
    <div class="step-list">
      <div class="step-item" @click="handleCheckStep(item)" :class="{'active':item.checked}" v-for="(item,index) in stepList" :key="index">
        <img class="img" :src="item.img" alt="">
        <div>
          <h2>{{item.title}}</h2>
        </div>
      </div>
    </div>

    <div v-for="(item,index) in stepList" :key="index">
      <!-- 下载 -->
      <div v-if="item.checked && index ==0" class="tpl">

        <Button @click="downLoad">下载导入模板</Button>
      </div>
      <!-- 上传 -->
      <div v-if="item.checked && index ==1" class="tpl">
        <Upload :before-upload="handleUpload" name="files" style="width:50%; height:400px;" accept="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel"
          multiple type="drag" :action="action" :headers="accessToken">
          <div style="padding: 50px 0">
            <Icon type="ios-cloud-upload" size="102" style="color: #3399ff"></Icon>
            <h2>选择或拖拽文件上传</h2>
          </div>
        </Upload>
      </div>
      <!-- 上传 -->
      <div v-if="item.checked && index ==2" class="tpl success">

        <h1>发货完成</h1>

        <div>
          <Button class="btn" @click="close">关闭页面</Button>
          <Button class="btn" type="primary" @click="navigationToGoodsOrder">商品订单</Button>
        </div>
      </div>

    </div>

  </Card>
</template>

<script>
import JsonExcel from "vue-json-excel";
import { downLoadDeliverExcel, uploadDeliverExcel } from "@/api/order.js";
import { baseUrl } from "@/libs/axios.js";
export default {
  components: {
    "download-excel": JsonExcel,
  },
  data() {
    return {
      file: "",
      action: baseUrl + "/order/order/batchDeliver", // 上传接口
      accessToken: {}, // 验证token
      // 步骤集合
      stepList: [
        {
          img: require("@/assets/download.png"),
          title: "1.下载批量发货导入模板",
          checked: true,
        },
        {
          img: require("@/assets/upload.png"),
          title: "2.上传数据",
          checked: false,
        },
        {
          img: require("@/assets/success.png"),
          title: "3.完成",
          checked: false,
        },
      ],
    };
  },
  mounted() {
    this.accessToken.accessToken = this.getStore("accessToken");
  },
  methods: {
    // 点击选择步骤
    handleCheckStep(val) {
      if (val.title.search("3") == -1) {
        console.warn(val);
        this.stepList.map((item) => {
          item.checked = false;
        });
        val.checked = true;
      }
    },
    // 上传数据
    handleUpload(file) {
      this.file = file;
      this.upload();
      return false;
    },
    // 跳转订单列表
    navigationToGoodsOrder() {
      this.$router.push({
        path: "/order/orderList",
      });
    },
    // 关闭页面
    close() {
      this.$store.commit("removeTag", "export-order-deliver");
      localStorage.storeOpenedList = JSON.stringify(
        this.$store.state.app.storeOpenedList
      );
      this.$router.go(-1);
    },

    /**
     * 上传文件
     */
    async upload() {
      let fd = new FormData();
      fd.append("files", this.file);
      let res = await uploadDeliverExcel(fd);
      if (res.success) {
        this.stepList.map((item) => {
          item.checked = false;
        });

        this.stepList[2].checked = true;
      }
    },

    /**
     * 下载excel
     */
    downLoad() {
      downLoadDeliverExcel()
        .then((res) => {
          const blob = new Blob([res], {
            type: "application/vnd.ms-excel;charset=utf-8",
          });
          //对于<a>标签，只有 Firefox 和 Chrome（内核） 支持 download 属性
          //IE10以上支持blob但是依然不支持download
          if ("download" in document.createElement("a")) {
            //支持a标签download的浏览器
            const link = document.createElement("a"); //创建a标签
            link.download = "批量发货导入模板.xls"; //a标签添加属性
            link.style.display = "none";
            link.href = URL.createObjectURL(blob);
            document.body.appendChild(link);
            link.click(); //执行下载
            URL.revokeObjectURL(link.href); //释放url
            document.body.removeChild(link); //释放标签
          } else {
            navigator.msSaveBlob(blob, fileName);
          }
        })
        .catch((err) => {
          console.log(err);
        });
    },
  },
};
</script>

<style lang="scss" scoped>
.step-list {
  width: 80%;
  min-width: 500px;
  max-width: 1160px;
  margin: 0 auto;
  display: flex;
  padding: 40px;
  justify-content: space-between;
}
h2 {
  text-align: center;
  margin: 10px 0;
}
.tpl {
  margin: 50px 0;
  display: flex;
  justify-content: center;
}
.active {
  background: #efefef;
  border-radius: 0.8em;
}
.step-item {
  width: 100%;
  padding: 0 20px;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  transition: 0.35s;
  cursor: pointer;
}
img {
  width: 100px;
  height: 100px;
}
.success {
  align-items: center;
  flex-direction: column;
  > h1 {
    font-size: 28px;
    margin: 10px;
  }
  /deep/ .btn {
    margin: 10px;
  }
}
</style>
