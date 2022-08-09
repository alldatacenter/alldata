<template>
  <div class="add-eval">
    <div class="title">
      <card _Title="订单投诉" :_Size="16"></card>
      <p>
        <span class="fontsize_16 global_color">{{ statusLabel[detail.complainStatus] }}</span>
        <span class="color999 ml_20">创建时间：</span><span>{{ detail.createTime }}</span>
        <span class="color999 ml_20">{{ detail.createTime }}</span>
      </p>
    </div>

    <Alert class="l_title" show-icon type="warning">我的申诉信息</Alert>
    <table cellspacing="0" cellpadding='0' border="1">
      <tr>
        <td>投诉商品</td>
        <td class="hover-color" @click="linkTo(`/goodsDetail?goodsId=${detail.goodsId}&skuId=${detail.skuId}`)"><img :src="detail.goodsImage" width="60" alt=""> &nbsp;{{ detail.goodsName }}</td>
      </tr>
      <tr>
        <td>投诉主题</td>
        <td>{{ detail.complainTopic }}</td>
      </tr>
      <tr>
        <td>投诉内容</td>
        <td>{{ detail.content }}</td>
      </tr>
      <tr>
        <td>补充内容</td>
        <td>
          <div style="display:flex;align-items:center;">
            <template v-if="detail.images">
              <div class="demo-upload-list" v-for="(img, index) in detail.images.split(',')" :key="index">
                <img :src="img">
                <div class="demo-upload-list-cover">
                  <Icon type="ios-eye-outline" @click.native="handleView(img)"></Icon>
                </div>
              </div>
            </template>
            <div v-else>暂无</div>
          </div>
        </td>
      </tr>
    </table>

    <Alert class="l_title" show-icon type="warning">商家申诉信息</Alert>
    <table cellspacing="0" cellpadding='0' border="1">
      <tr>
        <td>申诉时间</td>
        <td>{{ detail.appealTime || '暂无' }}</td>
      </tr>
      <tr>
        <td>申诉内容</td>
        <td>{{ detail.appealContent || '暂无' }}</td>
      </tr>
      <tr>
        <td>申诉凭证</td>
        <td>

          <div style="display:flex;align-items:center;">
            <template v-if="detail.appealImages">
              <div class="demo-upload-list" v-for="(img, index) in detail.appealImages.split(',')" :key="index">
                <img :src="img">
                <div class="demo-upload-list-cover">
                  <Icon type="ios-eye-outline" @click.native="handleView(img)"></Icon>
                </div>
              </div>
            </template>
            <div v-else>暂无</div>
          </div>
        </td>
      </tr>
    </table>

    <Alert class="l_title" show-icon type="warning">平台仲裁</Alert>
    <table cellspacing="0" cellpadding='0' border="1">
      <tr>
        <td>仲裁意见</td>
        <td>{{ detail.arbitrationResult || '暂无' }}</td>
      </tr>
    </table>

    <Alert class="l_title" show-icon type="warning">对话详情</Alert>
    <div class="speak-way" v-if="detail.orderComplaintCommunications && detail.orderComplaintCommunications.length">
      <div
        class="speak-msg seller"
        :class="{'speak-buyer': item.owner == 'BUYER', 'speak-platform': item.owner == 'PLATFORM', 'speak-seller': item.owner == 'STORE',}"
        :key="i"
        v-for="(item, i) in detail.orderComplaintCommunications"
      >
        {{
          item.owner == "PLATFORM"
            ? "平台"
            : item.owner == "BUYER"
            ? "买家"
            : "卖家"
        }}[{{ item.createTime }}]：
        <span>{{ item.content }}</span>
      </div>
    </div>
    <div v-else>暂无对话</div>
    <table cellspacing="0" cellpadding='0' border="1" v-if="detail.complainStatus!='COMPLETE'">
      <tr>
        <td>回复：</td>
        <td><label>
          <input type="textarea" maxlength="200" :rows="4" clearable
                 style="width:260px" v-model="params.content" />
        </label></td>
      </tr>
      <tr>
        <td></td>
        <td>
          <Button type="primary"  :loading="submitLoading" @click="handleSubmit" style="margin-left: 5px">
            回复
          </Button>
        </td>
      </tr>
    </table>

    <Modal title="View Image" v-model="visible">
      <img :src="previewImage" v-if="visible" style="width: 100%">
    </Modal>
  </div>
</template>
<script>
import {getComplainDetail} from '@/api/member.js';
import {communication} from '@/api/order';
export default {
  data () {
    return {
      detail: {}, // 评价详情
      visible: false, // 图片预览
      previewImage: '', // 预览图片地址
      loading: false, // 加载状态
      submitLoading: false, // 回复消息loading
      // 状态
      statusLabel: {
        NO_APPLY: '未申请',
        APPLYING: '申请中',
        COMPLETE: '已完成',
        EXPIRED: '已失效',
        CANCEL: '已取消',
        NEW: '待审核'
      },
      // 商家回复内容
      params: {
        content: '',
        complainId: ''
      }
    }
  },
  methods: {
    getDetail () { // 获取投诉详情
      getComplainDetail(this.$route.query.id).then(res => {
        if (res.success) this.detail = res.result
      })
    },
    goGoodsDetail (skuId, goodsId) { // 跳转商品详情
      let routerUrl = this.$router.resolve({
        path: '/goodsDetail',
        query: {skuId, goodsId}
      })
      window.open(routerUrl.href, '_blank')
    },
    handleView (name) { // 预览图片
      this.previewImage = name;
      this.visible = true;
    },
    // 回复消息
    handleSubmit () {
      if (this.params.content === '') {
        this.$Message.error('请填写对话内容');
        return;
      }
      this.submitLoading = true;
      this.params.complainId = this.$route.query.id;
      communication(this.params).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success('对话成功');
          this.params.content = '';
          this.getDetail();
        }
      });
    }
  },
  mounted () {
    this.getDetail()
  }
}
</script>
<style lang="scss" scoped>
.speak-way {
  background-color: #fff;
  width: 100%;
  border: 1px solid #999;

  .speak-seller {
    background-color: #fff2c6;
  }

  .speak-platform {
    background-color: #ffe2d1;
  }

  .speak-buyer {
    background-color: #c1d6d5;
  }
}

.speak-msg {
  height: 40px;
  line-height: 40px;
  padding: 0 5px;
  border-radius: 5px;
  margin: 5px;
  background-color: #eee;
}

.demo-upload-list {
  display: inline-block;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
  border: 1px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  position: relative;
  box-shadow: 0 1px 1px rgba(0, 0, 0, .2);
  margin-right: 4px;
  margin-top: 10px;
}

.demo-upload-list img {
  width: 100%;
  height: 100%;
}

.demo-upload-list-cover {
  display: none;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, .6);
}

.demo-upload-list:hover .demo-upload-list-cover {
  display: block;
}

.demo-upload-list-cover i {
  color: #fff;
  font-size: 30px;
  cursor: pointer;
  margin: 0 2px;
}

.icon-upload {
  width: 58px;
  height: 58px;
  line-height: 58px;
  text-align: center;
  display: inline-block;
  border: 1px dashed #999;
  border-radius: 4px;
  margin-top: 10px;

  &:hover {
    cursor: pointer;
    border-color: $theme_color;
  }
}

.l_title {
  margin: 10px 0;
}

table {
  width: 100%;
  border-color: #eee;

  td {
    padding: 10px;

    &:nth-child(1) {
      width: 120px;
      color: #999;
    }
  }
}
</style>
