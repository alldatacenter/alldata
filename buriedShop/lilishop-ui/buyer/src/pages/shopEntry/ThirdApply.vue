<template>
  <div class="person-msg">
    <Form ref="thirdForm" :model="form" :rules="rules" :label-width="140">
      <h4>基础信息</h4>
      <FormItem prop="storeName" label="店铺名称">
        <Input
          type="text"
          v-model="form.storeName"
          placeholder="请填写店铺名称"
        />
      </FormItem>

      <FormItem prop="storeLogo" label="店铺logo">
        <Upload
          ref="uploadLogo"
          :show-upload-list="false"
          :on-success="handleSuccess"
          :format="['jpg', 'jpeg', 'png', 'gif']"
          :max-size="2048"
          :before-upload="beforeUpload"
          :on-format-error="handleFormatError"
          :on-exceeded-size="handleMaxSize"
          :on-error="uploadErr"
          multiple
          :action="action"
          :headers="accessToken"
        >
          <Button type="info" :loading="uploadLoading">上传logo</Button>
        </Upload>
        <div class="describe">请压缩图片在2M以内，格式为gif，jpg，png</div>
        <div
          class="img-list"
          v-for="(item, index) in form.storeLogo"
          :key="index"
        >
          <img :src="item" width="100" height="" alt="" />
          <div class="cover">
            <Icon
              type="ios-eye-outline"
              @click.native="handleView(item)"
            ></Icon>
            <Icon
              type="ios-trash-outline"
              @click.native="handleRemove(index, 'storeLogo')"
            ></Icon>
          </div>
        </div>
      </FormItem>
      <FormItem prop="goodsManagementCategory" label="店铺经营类目">
        <Select
          v-model="form.goodsManagementCategory"
          multiple
          style="width: 300px"
        >
          <Option
            v-for="item in categoryList"
            :value="item.id"
            :key="item.id"
            >{{ item.name }}</Option
          >
        </Select>
      </FormItem>
      <FormItem prop="storeCenter" label="店铺定位">
        <Button
          type="info"
          v-if="!form.storeCenter"
          @click="$refs.liliMap.showMap = true"
        >点击获取店铺定位</Button>
        <Button
          type="success"
          v-else
          @click="$refs.liliMap.showMap = true"
        >已定位</Button>
      </FormItem>
      <FormItem prop="storeAddressIdPath" label="店铺所在地">
        <region
          style="width: 250px"
          @selected="selectedRegion"
          :addressId="address"
        />
      </FormItem>
      <FormItem prop="storeAddressDetail" label="店铺详细地址">
        <Input
          type="text"
          v-model="form.storeAddressDetail"
          placeholder="请填写店铺详细地址"
        />
      </FormItem>
      <FormItem prop="storeDesc" label="店铺简介">
        <Input
          type="textarea"
          v-model="form.storeDesc"
          maxlength="200"
          show-word-limit
          :rows="4"
          placeholder="请输入店铺简介"
        />
      </FormItem>

      <FormItem>
        <Button @click="$emit('change', 1)">返回</Button>
        <Button type="primary" :loading="loading" @click="next"
          >提交平台审核</Button
        >
      </FormItem>
    </Form>
    <Modal title="View Image" v-model="visible">
      <img :src="previewPicture" v-if="visible" style="width: 100%" />
    </Modal>
    <lili-map ref="liliMap" @getAddress="getAddress" :useApi="false"></lili-map>
  </div>
</template>
<script>
import { applyThird } from '@/api/shopentry';
import { getCategory } from '@/api/goods';
import Map from '@/components/map/index';
import storage from '@/plugins/storage';
import { commonUrl } from '@/plugins/request.js';
import region from '@/components/map/region.vue';
export default {
  props: {
    content: {
      default: {},
      type: Object
    }
  },
  components: { liliMap: Map, region },
  data () {
    return {
      loading: false, // 加载状态
      uploadLoading: false, // 上传加载状态
      action: commonUrl + '/common/common/upload/file', // 上传地址
      accessToken: {}, // 验证token
      previewPicture: '', // 预览图片
      address: '', // 回显地址
      visible: false, // 图片预览
      form: { // 表单数据
        storeLogo: []
      },
      rules: { // 验证规则
        goodsManagementCategory: [
          { required: true, message: '请选择店铺经营类目' }
        ],
        storeName: [{ required: true, message: '请填写店铺名称' }],
        storeLogo: [{ required: true, message: '请上传店铺logo' }],
        storeDesc: [{ required: true, message: '请填写店铺简介' }],
        storeCenter: [{ required: true, message: '请选择店铺位置' }],
        storeAddressIdPath: [{ required: true, message: '请选择店铺位置' }],
        storeAddressDetail: [{ required: true, message: '请输入店铺详细地址' }]
      },
      categoryList: [] // 分类数据
    };
  },
  methods: {
    // 下一步
    next () {
      this.$refs.thirdForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          let params = JSON.parse(JSON.stringify(this.form));
          params.storeLogo = this.form.storeLogo.toString();
          params.goodsManagementCategory = this.form.goodsManagementCategory.toString();
          applyThird(params)
            .then((res) => {
              this.loading = false;
              if (res.success) this.$emit('change', 3);
              this.$parent.getData()
            })
            .catch(() => {
              this.loading = false;
            });
        } else {
          console.log('error');
        }
      });
    },
    // 上传之前
    beforeUpload () {
      this.uploadLoading = true;
      if (this.form.storeLogo.length >= 3) {
        this.$Message.warning('最多上传三张图片')
        return false;
      }
    },
    // 上传成功回调
    handleSuccess (res, file) {
      this.uploadLoading = false;
      this.form.storeLogo.push(res.result);
    },
    // 上传格式错误
    handleFormatError (file) {
      this.uploadLoading = false;
      this.$Notice.warning({
        title: 'The file format is incorrect',
        desc: '上传文件格式不正确'
      });
    },
    // 上传大小限制
    handleMaxSize (file) {
      this.uploadLoading = false;
      this.$Notice.warning({
        title: 'Exceeding file size limit',
        desc: '文件大小不能超过2M'
      })
    },
    // 上传失败
    uploadErr () {
      this.uploadLoading = false;
    },
    // 查看图片
    handleView (item) {
      this.previewPicture = item;
      this.visible = true;
    },
    // 删除图片
    handleRemove (index, listName) {
      this.form[listName].splice(index, 1);
    },
    // 选择坐标回调
    getAddress (item) {
      this.$set(
        this.form,
        'storeCenter',
        item.position.lng + ',' + item.position.lat
      );
    },
    // 获取商品分类
    getCategoryList () {
      getCategory(0).then((res) => {
        if (res.success) this.categoryList = res.result;
      });
    },
    // 地址选择回显
    selectedRegion (item) {
      this.$set(this.form, 'storeAddressIdPath', item[0].toString());
      this.$set(
        this.form,
        'storeAddressPath',
        item[1].toString().replace(/\s/g, '')
      );
    }
  },
  mounted () {
    this.accessToken.accessToken = storage.getItem('accessToken');
    this.getCategoryList();
    if (this.content != {}) {
      this.form = JSON.parse(JSON.stringify(this.content));
      if (this.form.storeLogo) {
        this.form.storeLogo = this.content.storeLogo.split(',');
        this.form.goodsManagementCategory = this.content.goodsManagementCategory.split(
          ','
        );
      } else {
        this.form.storeLogo = [];
      }
      this.address = this.form.storeAddressIdPath;
      this.$forceUpdate();
    }
    this.$refs.thirdForm.resetFields()
  }
};
</script>
<style lang="scss" scoped>
h4 {
  margin-bottom: 10px;
  padding: 0 10px;
  border: 1px solid #ddd;
  background-color: #f8f8f8;
  font-weight: bold;
  color: #333;
  font-size: 14px;
  line-height: 40px;
  text-align: left;
}
.ivu-input-wrapper {
  width: 300px;
}
.img-list {
  display: inline-block;
  margin: 10px;
  width: 100px;
  height: auto;
  position: relative;
  .cover {
    display: none;
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(0, 0, 0, 0.6);
    width: inherit;
    height: inherit;
    align-items: center;
    justify-content: space-around;
    i {
      color: #fff;
      font-size: 30px;
      cursor: pointer;
    }
  }
  &:hover .cover {
    display: flex;
  }
}
.describe {
  font-size: 12px;
  color: #999;
}
</style>
