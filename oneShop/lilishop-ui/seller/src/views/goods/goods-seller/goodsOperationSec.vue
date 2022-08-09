<template>
  <div>
    <div class="content-goods-publish">
      <Form
        ref="baseInfoForm"
        :model="baseInfoForm"
        :label-width="120"
        :rules="baseInfoFormRule"
      >
        <div class="base-info-item">
          <h4>基本信息</h4>
          <div class="form-item-view">
            <FormItem label="商品分类">
              <span class="goods-category-name">{{
                this.baseInfoForm.categoryName[0]
              }}</span>
              <span> &gt; {{ this.baseInfoForm.categoryName[1] }}</span>
              <span> &gt; {{ this.baseInfoForm.categoryName[2] }}</span>
            </FormItem>
            <FormItem label="商品名称" prop="goodsName">
              <Input
                type="text"
                v-model="baseInfoForm.goodsName"
                placeholder="商品名称"
                clearable
                style="width: 260px"
              />
            </FormItem>

            <FormItem label="商品价格" prop="price">
              <Input
                type="text"
                v-model="baseInfoForm.price"
                placeholder="商品价格"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="商品卖点" prop="sellingPoint">
              <Input
                v-model="baseInfoForm.sellingPoint"
                type="textarea"
                :rows="4"
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="商品品牌" prop="brandId">
              <Select
                v-model="baseInfoForm.brandId"
                filterable
                style="width: 200px"
              >
                <Option
                  v-for="item in brandList"
                  :value="item.id"
                  :key="item.id"
                  :label="item.name"
                ></Option>
              </Select>
            </FormItem>
          </div>
          <h4>商品交易信息</h4>
          <div class="form-item-view">
            <FormItem
              class="form-item-view-el"
              label="计量单位"
              prop="goodsUnit"
            >
              <Select v-model="baseInfoForm.goodsUnit" style="width: 100px">
                <Scroll :on-reach-bottom="handleReachBottom">
                  <Option
                    v-for="(item, index) in goodsUnitList"
                    :key="index"
                    :value="item"
                    >{{ item }}
                  </Option>
                </Scroll>
              </Select>
            </FormItem>
            <FormItem
              class="form-item-view-el"
              label="销售模式"
              prop="salesModel"
            >
              <RadioGroup
                type="button"
                v-if="baseInfoForm.goodsType != 'VIRTUAL_GOODS'"
                button-style="solid"
                v-model="baseInfoForm.salesModel"
              >
                <Radio title="零售型" label="RETAIL">零售型</Radio>
                <Radio title="批发型" label="WHOLESALE">批发型</Radio>
              </RadioGroup>
              <RadioGroup
                type="button"
                v-else
                button-style="solid"
                v-model="baseInfoForm.salesModel"
              >
                <Radio title="零售型" label="RETAIL">
                  <span>虚拟型</span>
                </Radio>
              </RadioGroup>
            </FormItem>
          </div>
          <h4>商品规格及图片</h4>
          <div class="form-item-view">
            <FormItem
              class="form-item-view-el required"
              label="商品图片"
              prop="goodsGalleryFiles"
            >
              <div style="display: flex; flex-wrap: flex-start">
                <vuedraggable
                  :list="baseInfoForm.goodsGalleryFiles"
                  :animation="200"
                >
                  <div
                    class="demo-upload-list"
                    v-for="(item, __index) in baseInfoForm.goodsGalleryFiles"
                    :key="__index"
                  >
                    <template>
                      <img :src="item.url" />
                      <div class="demo-upload-list-cover">
                        <div>
                          <Icon
                            type="md-search"
                            size="30"
                            @click.native="handleViewGoodsPicture(item.url)"
                          ></Icon>
                          <Icon
                            type="md-trash"
                            size="30"
                            @click.native="handleRemoveGoodsPicture(item)"
                          ></Icon>
                        </div>
                      </div>
                    </template>
                  </div>
                </vuedraggable>

                <Upload
                  ref="upload"
                  :show-upload-list="false"
                  :on-success="handleSuccessGoodsPicture"
                  :format="['jpg', 'jpeg', 'png']"
                  :on-format-error="handleFormatError"
                  :on-exceeded-size="handleMaxSize"
                  :max-size="1024"
                  :before-upload="handleBeforeUploadGoodsPicture"
                  multiple
                  type="drag"
                  :action="uploadFileUrl"
                  :headers="{ ...accessToken }"
                  style="margin-left: 10px"
                >
                  <div style="width: 148px; height: 148px; line-height: 148px">
                    <Icon type="md-add" size="20"></Icon>
                  </div>
                </Upload>
              </div>
              <Modal title="View Image" v-model="goodsPictureVisible">
                <img
                  :src="previewGoodsPicture"
                  v-if="goodsPictureVisible"
                  style="width: 100%"
                />
              </Modal>
            </FormItem>
            <div class="layout" style="width: 100%">
              <Collapse v-model="open_panel">
                <Panel name="1">
                  自定义规格项
                  <div slot="content">
                    <Form>
                      <div
                        v-for="(item, $index) in skuInfo"
                        :key="$index"
                        class="sku-item-content"
                      >
                        <Card :bordered="true" class="ivu-card-body">
                          <Button
                            type="primary"
                            slot="extra"
                            @click="handleCloseSkuItem($index, item)"
                          >
                            删除规格
                          </Button>
                          <div>
                            <FormItem
                              label="规格名"
                              class="sku-item-content-val flex"
                            >
                              <AutoComplete
                                style="width: 150px"
                                v-model="item.name"
                                :maxlength="30"
                                placeholder="请输入规格名称"
                                :filter-method="filterMethod"
                                :data="skuData"
                                @on-change="editSkuItem"
                              >
                              </AutoComplete>
                            </FormItem>
                          </div>
                          <div class="flex sku-val">
                            <Form :model="item" class="flex">
                              <!--规格值文本列表-->
                              <FormItem
                                v-for="(val, index) in item.spec_values"
                                :key="index"
                                class="sku-item-content-val flex"
                                label="规格项"
                                :prop="'spec_values.' + index + '.value'"
                                :rules="[regular.REQUIRED, regular.VARCHAR60]"
                              >
                                <AutoComplete
                                  v-model="val.value"
                                  style="width: 150px"
                                  :maxlength="30"
                                  placeholder="请输入规格项"
                                  :filter-method="filterMethod"
                                  :data="skuVal"
                                  @on-focus="changeSkuVals(item.name)"
                                  @on-change="
                                    skuValueChange(val.value, index, item)
                                  "
                                >
                                </AutoComplete>
                                <Button
                                  type="primary"
                                  size="small"
                                  style="margin-left: 10px"
                                  @click="handleCloseSkuValue(val, index)"
                                >
                                  删除
                                </Button>
                              </FormItem>
                            </Form>
                          </div>
                          <div>
                            <Button @click="addSpec($index, item)"
                              >添加规格值</Button
                            >
                          </div>
                        </Card>
                      </div>
                    </Form>
                    <Button
                      class="add-sku-btn"
                      type="primary"
                      size="small"
                      @click="addSkuItem"
                      >添加规格项</Button
                    >
                    &nbsp;
                    <Button
                      class="add-sku-btn"
                      type="warning"
                      size="small"
                      @click="handleClearSku"
                      >清空规格项</Button
                    >
                  </div>
                </Panel>
                <Panel name="2">
                  规格详细
                  <div slot="content">
                    <div slot="content">
                      <!-- #TODO 此处有待优化  -->
                      <Table
                        class="mt_10"
                        :columns="skuTableColumn"
                        :data="skuTableData"
                        style="
                          width: 100%;
                          .ivu-table-overflowX {
                            overflow-x: hidden;
                          }
                        "
                      >
                        <template slot-scope="{ row }" slot="sn">
                          <Input
                            clearable
                            v-model="row.sn"
                            placeholder="请输入货号"
                            @on-change="updateSkuTable(row, 'sn')"
                          />
                        </template>
                        <div
                          slot-scope="{ row }"
                          slot="weight"
                          v-if="baseInfoForm.goodsType != 'VIRTUAL_GOODS'"
                        >
                          <Input
                            clearable
                            v-model="row.weight"
                            placeholder="请输入重量"
                            @on-change="updateSkuTable(row, 'weight')"
                          />
                        </div>
                        <template slot-scope="{ row }" slot="quantity">
                          <Input
                            clearable
                            v-model="row.quantity"
                            placeholder="请输入库存"
                            @on-change="updateSkuTable(row, 'quantity')"
                          />
                        </template>
                        <template slot-scope="{ row }" slot="cost">
                          <Input
                            clearable
                            v-model="row.cost"
                            placeholder="请输入成本价"
                            @on-change="updateSkuTable(row, 'cost')"
                          />
                        </template>
                        <template slot-scope="{ row }" slot="price">
                          <Input
                            clearable
                            v-model="row.price"
                            placeholder="请输入价格"
                            @on-change="updateSkuTable(row, 'price')"
                          />
                        </template>
                        <template slot-scope="{ row }" slot="images">
                          <Button @click="editSkuPicture(row)">编辑图片</Button>
                          <Modal
                            v-model="showSkuPicture"
                            :styles="{ top: '30px' }"
                            class-name="sku-preview-modal"
                            title="编辑图片"
                            ok-text="结束编辑"
                            @on-ok="updateSkuPicture()"
                            cancel-text="取消"
                          >
                            <div class="preview-picture">
                              <img
                                v-if="previewPicture !== ''"
                                :src="previewPicture"
                              />
                            </div>
                            <Divider />
                            <vuedraggable
                              :list="selectedSku.images"
                              :animation="200"
                              style="display: inline-block"
                            >
                              <div
                                class="sku-upload-list"
                                v-for="(img, __index) in selectedSku.images"
                                :key="__index"
                              >
                                <template>
                                  <img :src="img.url" />
                                  <div class="sku-upload-list-cover">
                                    <Icon
                                      type="md-search"
                                      @click="handleView(img.url)"
                                    ></Icon>
                                    <Icon
                                      type="md-trash"
                                      @click="handleRemove(img, __index)"
                                    ></Icon>
                                  </div>
                                </template>
                              </div>
                            </vuedraggable>
                            <Upload
                              ref="uploadSku"
                              :show-upload-list="false"
                              :on-success="handleSuccess"
                              :format="['jpg', 'jpeg', 'png']"
                              :on-format-error="handleFormatError"
                              :on-exceeded-size="handleMaxSize"
                              :max-size="1024"
                              :before-upload="handleBeforeUpload"
                              multiple
                              type="drag"
                              :action="uploadFileUrl"
                              :headers="{ ...accessToken }"
                              style="display: inline-block; width: 58px"
                            >
                              <div>
                                <Icon type="ios-camera" size="55"></Icon>
                              </div>
                            </Upload>
                          </Modal>
                        </template>
                      </Table>
                    </div>
                  </div>
                </Panel>
              </Collapse>
            </div>
          </div>
          <h4>商品详情描述</h4>
          <div class="form-item-view">
            <div class="tree-bar">
              <FormItem
                class="form-item-view-el"
                label="店内分类"
                prop="shopCategory"
              >
                <Tree
                  ref="tree"
                  style="text-align: left"
                  :data="shopCategory"
                  show-checkbox
                  @on-select-change="selectTree"
                  @on-check-change="changeSelect"
                  :check-strictly="false"
                ></Tree>
              </FormItem>
            </div>
            <FormItem class="form-item-view-el" label="商品描述" prop="intro">
              <editor eid="intro" v-model="baseInfoForm.intro"></editor>
            </FormItem>
            <FormItem
              class="form-item-view-el"
              label="移动端描述"
              prop="skuList"
            >
              <editor
                eid="mobileIntro"
                v-model="baseInfoForm.mobileIntro"
              ></editor>
            </FormItem>
          </div>
          <div v-if="baseInfoForm.goodsType != 'VIRTUAL_GOODS'">
            <h4>商品物流信息</h4>
            <div class="form-item-view">
              <FormItem
                class="form-item-view-el"
                label="物流模板"
                prop="templateId"
              >
                <Select v-model="baseInfoForm.templateId" style="width: 200px">
                  <Option
                    v-for="item in logisticsTemplate"
                    :value="item.id"
                    :key="item.id"
                    >{{ item.name }}
                  </Option>
                </Select>
              </FormItem>
            </div>
            <h4>其他信息</h4>
            <div class="form-item-view">
              <FormItem
                class="form-item-view-el"
                label="商品发布"
                prop="release"
              >
                <RadioGroup
                  type="button"
                  button-style="solid"
                  v-model="baseInfoForm.release"
                >
                  <Radio title="立即发布" :label="1">
                    <span>立即发布</span>
                  </Radio>
                  <Radio title="放入仓库" :label="0">
                    <span>放入仓库</span>
                  </Radio>
                </RadioGroup>
              </FormItem>
              <FormItem
                class="form-item-view-el"
                label="商品推荐"
                prop="skuList"
              >
                <RadioGroup
                  type="button"
                  button-style="solid"
                  v-model="baseInfoForm.recommend"
                >
                  <Radio title="推荐" :label="1">
                    <span>推荐</span>
                  </Radio>
                  <Radio title="不推荐" :label="0">
                    <span>不推荐</span>
                  </Radio>
                </RadioGroup>
              </FormItem>
            </div>
            <div class="form-item-view-bottom">
              <Collapse
                v-model="params_panel"
                v-for="(paramsGroup, groupIndex) in goodsParams"
                :title="paramsGroup.groupName"
                class="mb_10"
                style="text-align: left"
                :key="paramsGroup.groupName"
              >
                <Panel :name="paramsGroup.groupName">
                  {{ paramsGroup.groupName }}
                  <p slot="content">
                    <FormItem
                      v-for="(params, paramsIndex) in paramsGroup.params"
                      :key="paramsIndex"
                      :label="`${params.paramName}：`"
                    >
                      <Select
                        v-model="params.paramValue"
                        placeholder="请选择"
                        style="width: 200px"
                        clearable
                        @on-change="
                          selectParams(
                            paramsGroup,
                            groupIndex,
                            params,
                            paramsIndex,
                            params.paramValue
                          )
                        "
                      >
                        <Option
                          v-for="option in params.options.split(',')"
                          :label="option"
                          :value="option"
                          :key="option"
                        ></Option>
                      </Select>
                    </FormItem>
                  </p>
                </Panel>
              </Collapse>
            </div>
          </div>
        </div>
      </Form>
    </div>
    <!-- 底部按钮 -->
    <div class="footer">
      <ButtonGroup>
        <Button
          type="primary"
          @click="pre"
          v-if="!$route.query.id && !$route.query.draftId"
          >上一步
        </Button>
        <Button type="primary" @click="save" :loading="submitLoading">
          {{ this.$route.query.id ? "保存" : "保存商品" }}
        </Button>
        <Button type="primary" @click="saveToDraft">保存为模版</Button>
      </ButtonGroup>
    </div>
  </div>
</template>
<script>
import * as API_GOODS from "@/api/goods";
import * as API_Shop from "@/api/shops";
import cloneObj from "@/utils/index";
import vuedraggable from "vuedraggable";
import editor from "@/views/my-components/lili/editor";
import { uploadFile } from "@/libs/axios";
import { regular } from "@/utils";

export default {
  components: {
    editor,
    vuedraggable,
  },
  props: {
    firstData: {
      default: {},
      type: Object,
    },
  },
  data() {
    // 表单验证项，商品价格
    const checkPrice = (rule, value, callback) => {
      if (!value && value !== 0) {
        return callback(new Error("商品价格不能为空"));
      }
      setTimeout(() => {
        if (!regular.money.test(value)) {
          callback(new Error("请输入正整数或者两位小数"));
        } else if (parseFloat(value) > 99999999) {
          callback(new Error("商品价格设置超过上限值"));
        } else {
          callback();
        }
      }, 1000);
    };
    // 表单验证项，商品编号
    const checkSn = (rule, value, callback) => {
      if (!value) {
        callback(new Error("商品编号不能为空"));
      } else if (!/^[a-zA-Z0-9_\\-]+$/g.test(value)) {
        callback(new Error("请输入数字、字母、下划线或者中划线"));
      } else if (value.length > 30) {
        callback(new Error("商品编号长度不能大于30"));
      } else {
        callback();
      }
    };
    // 表单验证项，商品重量
    const checkWeight = (rule, value, callback) => {
      if (!value && typeof value !== "number") {
        callback(new Error("重量不能为空"));
      } else if (!regular.money.test(value)) {
        callback(new Error("请输入正整数或者两位小数"));
      } else if (parseFloat(value) > 99999999) {
        callback(new Error("商品重量设置超过上限值"));
      } else {
        callback();
      }
    };
    return {
      regular,
      total: 0,
      global: 0,
      accessToken: "", //令牌token
      goodsParams: "",
      categoryId: "", // 商品分类第三级id
      //提交状态
      submitLoading: false,
      //上传图片路径
      uploadFileUrl: uploadFile,
      // 预览图片路径
      previewPicture: "",
      //商品图片
      previewGoodsPicture: "",
      //展示图片层
      visible: false,
      //展示商品图片
      goodsPictureVisible: false,
      //展示sku图片
      showSkuPicture: false,
      //选择的sku
      selectedSku: {},
      /** 发布商品基本参数 */
      baseInfoForm: {
        salesModel: "RETAIL",
        /** 商品相册列表 */
        goodsGalleryFiles: [],
        /** 是否立即发布 true 立即发布 false 放入仓库 */
        release: 1,
        /** 是否为推荐商品 */
        recommend: 1,
        /** 店铺分类 */
        storeCategoryPath: "",
        brandId: 0,
        /** 计量单位 **/
        goodsUnit: "",
        /** 商品类型 **/
        goodsType: "",
        /** 分类路径 **/
        categoryPath: "",
        /** 商品卖点 **/
        sellingPoint: "",
        /** 商品详情 **/
        intro: "",
        mobileIntro: "",
        updateSku: true,
        /** 是否重新生成sku */
        regeneratorSkuFlag: false,
        /** 物流模板id **/
        templateId: "",
        /** 参数组*/
        goodsParamsDTOList: [],
        /** 商品分类中文名 */
        categoryName: [],
      },
      /** 表格头 */
      skuTableColumn: [],
      /** 表格数据 */
      skuTableData: [],
      /** 默认的规格参数 */
      skuData: [],
      /** 默认的规格值 */
      skuVals: [],
      // 某一规格名下的规格值
      skuVal: [],
      // 规格展开的项
      open_panel: [1, 2],
      /** 要提交的规格数据*/
      skuInfo: [],
      /** 物流模板 **/
      logisticsTemplate: [],

      /** 固定列校验提示内容 */
      validatatxt: "请输入0~99999999之间的数字值",
      //参数panel展示
      params_panel: [],
      /** 存储未通过校验的单元格位置  */
      validateError: [],
      baseInfoFormRule: {
        goodsName: [regular.REQUIRED, regular.WHITE_SPACE, regular.VARCHAR60],
        price: [regular.REQUIRED, { validator: checkPrice }],
        sellingPoint: [regular.REQUIRED, regular.VARCHAR60],
        goodsUnit: [{ required: true, message: "请选择计量单位" }],
        name: [regular.REQUIRED, regular.VARCHAR5],
        value: [regular.REQUIRED, regular.VARCHAR60],
        templateId: [regular.REQUIRED],
      },
      params: {
        pageNumber: 1,
        pageSize: 10,
      },
      skuInfoRules: {},
      /** 品牌列表 */
      brandList: [],
      /** 店铺分类列表 */
      shopCategory: [],
      /** 商品单位列表 */
      goodsUnitList: [],
      ignoreColumn: [
        // 添加规格时需要忽略的参数
        "_index",
        "_rowKey",
        "sn",
        "cost",
        "price",
        "weight",
        "quantity",
        "specId",
        "specValueId",
      ],
    };
  },
  methods: {
    /**
     * 选择参数
     * @paramsGroup 参数分组
     * @groupIndex 参数分组下标
     * @params 参数选项
     * @paramIndex 参数下标值
     * @value 参数选项值
     */
    selectParams(paramsGroup, groupIndex, params, paramsIndex, value) {
      if (!this.baseInfoForm.goodsParamsDTOList[groupIndex]) {
        this.baseInfoForm.goodsParamsDTOList[groupIndex] = {
          groupId: "",
          groupName: "",
          goodsParamsItemDTOList: [],
        };
      }
      //赋予分组id、分组名称
      this.baseInfoForm.goodsParamsDTOList[groupIndex].groupId =
        paramsGroup.groupId;
      this.baseInfoForm.goodsParamsDTOList[groupIndex].groupName =
        paramsGroup.groupName;

      //参数详细为空，则赋予
      if (
        !this.baseInfoForm.goodsParamsDTOList[groupIndex]
          .goodsParamsItemDTOList[paramsIndex]
      ) {
        this.baseInfoForm.goodsParamsDTOList[groupIndex].goodsParamsItemDTOList[
          paramsIndex
        ] = {
          paramName: "",
          paramValue: "",
          isIndex: "",
          // required: "",
          paramId: "",
          sort: "",
        };
      }
      this.baseInfoForm.goodsParamsDTOList[groupIndex].goodsParamsItemDTOList[
        paramsIndex
      ] = {
        paramName: params.paramName,
        paramValue: value,
        isIndex: params.isIndex,
        // required: params.required,
        paramId: params.id,
        sort: params.sort,
      };
    },
    // 编辑sku图片
    editSkuPicture(row) {
      if (row.images && row.images.length > 0) {
        this.previewPicture = row.images[0].url;
      }
      this.selectedSku = row;
      this.showSkuPicture = true;
    },
    pre() {
      // 上一步
      this.$parent.activestep--;
    },
    // 预览图片
    handleView(url) {
      this.previewPicture = url;
      this.visible = true;
    },
    // 移除已选图片
    handleRemove(item, index) {
      this.selectedSku.images = this.selectedSku.images.filter(
        (i) => i.url !== item.url
      );
      if (this.selectedSku.images.length > 0 && index === 0) {
        this.previewPicture = this.selectedSku.images[0].url;
      } else if (this.selectedSku.images.length < 0) {
        this.previewPicture = "";
      }
    },
    // 查看商品大图
    handleViewGoodsPicture(url) {
      this.previewGoodsPicture = url;
      this.goodsPictureVisible = true;
    },
    // 移除商品图片
    handleRemoveGoodsPicture(file) {
      this.baseInfoForm.goodsGalleryFiles =
        this.baseInfoForm.goodsGalleryFiles.filter((i) => i.url !== file.url);
    },
    // 更新sku图片
    updateSkuPicture() {
      this.baseInfoForm.regeneratorSkuFlag = true;
      let _index = this.selectedSku._index;
      this.skuTableData[_index] = this.selectedSku;
    },
    // sku图片上传成功
    handleSuccess(res, file) {
      if (file.response) {
        file.url = file.response.result;
        if (this.selectedSku.images) {
          this.selectedSku.images.push(file);
        } else {
          this.selectedSku.images = [file];
        }
        this.previewPicture = file.url;
      }
    },
    // 商品图片上传成功
    handleSuccessGoodsPicture(res, file) {
      if (file.response) {
        file.url = file.response.result;
        this.baseInfoForm.goodsGalleryFiles.push(file);
      }
    },
    // 图片格式不正确
    handleFormatError(file) {
      this.$Notice.warning({
        title: "文件格式不正确",
        desc: "文件 " + file.name + " 的格式不正确",
      });
    },
    // 图片大小不正确
    handleMaxSize(file) {
      this.$Notice.warning({
        title: "超过文件大小限制",
        desc: "图片大小不能超过1MB",
      });
    },
    // 图片上传前钩子
    handleBeforeUploadGoodsPicture(file) {
      const check = this.baseInfoForm.goodsGalleryFiles.length < 5;
      if (!check) {
        this.$Notice.warning({
          title: "图片数量不能大于五张",
        });
        return false;
      }
    },
    // sku图片上传前钩子
    handleBeforeUpload(file) {
      const check =
        this.selectedSku.images !== undefined &&
        this.selectedSku.images.length > 5;
      if (check) {
        this.$Notice.warning({ title: "图片数量不能大于五张" });
        return false;
      }
    },

    /** 查询商品品牌列表 */
    getGoodsBrandList() {
      API_GOODS.getCategoryBrandListDataSeller(this.categoryId).then(
        (response) => {
          this.brandList = response;
        }
      );
    },
    // 页面触底
    handleReachBottom() {
      setTimeout(() => {
        if (this.params.pageNumber * this.params.pageSize <= this.total) {
          this.params.pageNumber++;
          this.GET_GoodsUnit();
        }
      }, 1000);
    },
    // 获取商品单位
    GET_GoodsUnit() {
      API_GOODS.getGoodsUnitList(this.params).then((res) => {
        if (res.success) {
          this.goodsUnitList.push(...res.result.records.map((i) => i.name));
          this.total = res.result.total;
        }
      });
    },
    // 获取当前店铺分类
    GET_ShopGoodsLabel() {
      API_GOODS.getShopGoodsLabelListSeller().then((res) => {
        if (res.success) {
          let shopCategories = !this.baseInfoForm.storeCategoryPath
            ? []
            : this.baseInfoForm.storeCategoryPath.split(",");

          this.shopCategory = res.result.map((i) => {
            i.title = i.labelName;
            i.expand = false;
            i.checked = shopCategories.some((o) => o === i.id);
            i.children = i.children.map((j) => {
              j.title = j.labelName;
              j.expand = false;
              j.checked = shopCategories.some((o) => o === j.id);
              return j;
            });
            return i;
          });
        }
      });
    },
    // 编辑时获取商品信息
    async GET_GoodData(id, draftId) {
      let response = {};
      if (draftId) {
        response = await API_GOODS.getDraftGoodsDetail(draftId);
      } else {
        response = await API_GOODS.getGoods(id);
        this.goodsId = response.result.id;
      }

      response.result.recommend
        ? (response.result.recommend = 1)
        : (response.result.recommend = 0);
      this.baseInfoForm = { ...this.baseInfoForm, ...response.result };
      this.baseInfoForm.release = 1; //即使是被放入仓库，修改的时候也会显示会立即发布
      this.categoryId = response.result.categoryPath.split(",")[2];

      if (
        response.result.goodsGalleryList &&
        response.result.goodsGalleryList.length > 0
      ) {
        this.baseInfoForm.goodsGalleryFiles =
          response.result.goodsGalleryList.map((i) => {
            let files = { url: i };
            return files;
          });
      }

      this.Get_SkuInfoByCategory(this.categoryId);

      this.renderGoodsDetailSku(response.result.skuList);

      /** 查询品牌列表 */
      this.getGoodsBrandList();
      /** 查询商品参数 */
      this.GET_GoodsParams();
      /** 查询店铺商品分类 */
      this.GET_ShopGoodsLabel();
      this.GET_GoodsUnit();
    },
    // 渲染sku数据
    renderGoodsDetailSku(skuList) {
      let skus = [];
      let skusInfo = [];
      skuList.map((e) => {
        let sku = {
          id: e.id,
          sn: e.sn,
          price: e.price,
          cost: e.cost,
          quantity: e.quantity,
          weight: e.weight,
        };
        e.specList.forEach((u) => {
          if (u.specName === "images") {
            sku.images = u.specImage;
          } else {
            sku[u.specName] = u.specValue;
            if (
              !skusInfo.some((s) => s.name === u.specName) &&
              !this.ignoreColumn.includes(u.specName)
            ) {
              skusInfo.push({
                name: u.specName,
                spec_id: u.specNameId,
                spec_values: [
                  {
                    id: u.specValueId,
                    name: u.specName,
                    value: u.specValue || "",
                  },
                ],
              });
            } else {
              skusInfo = skusInfo.map((sk) => {
                if (
                  !sk.spec_values.some((s) => s.value === u.specValue) &&
                  sk.name === u.specName
                ) {
                  sk.spec_values.push({
                    id: u.specValueId,
                    name: u.specName,
                    value: u.specValue || "",
                  });
                }
                if (!sk.spec_id && u.specName === "specId") {
                  sk.spec_id = u.specValue;
                }
                return sk;
              });
            }
          }
        });
        skus.push(sku);
      });
      this.skuInfo = skusInfo;
      this.renderTableData(skus);
      this.skuTableData = skus;
    },

    /** 根据当前分类id查询商品应包含的参数 */
    GET_GoodsParams() {
      API_GOODS.getCategoryParamsListDataSeller(this.categoryId).then(
        (response) => {
          if (!response || response.length <= 0) {
            return;
          }
          this.goodsParams = response;

          //展开选项卡
          this.goodsParams.forEach((item) => {
            this.params_panel.push(item.groupName);
          });
          if (this.baseInfoForm.goodsParamsDTOList) {
            // 已选值集合
            const paramsArr = [];
            this.baseInfoForm.goodsParamsDTOList.forEach((group) => {
              group.goodsParamsItemDTOList.forEach((param) => {
                param.groupId = group.groupId;
                paramsArr.push(param);
              });
            });
            // 循环参数分组
            this.goodsParams.forEach((parmsGroup) => {
              parmsGroup.params.forEach((param) => {
                paramsArr.forEach((arr) => {
                  if (param.paramName == arr.paramName) {
                    param.paramValue = arr.paramValue;
                  }
                });
              });
            });
          } else {
            this.baseInfoForm.goodsParamsDTOList = [];
          }
        }
      );
    },
    /** 添加规格项 */
    addSkuItem() {
      if (this.skuInfo.length >= 5) {
        this.$Message.error("规格项不能大于5个！");
        return;
      }
      // 写入对象，下标，具体对象
      let num = this.global++;
      this.$set(this.skuInfo, this.skuInfo.length, {
        spec_values: [{ name: "规格名" + num, value: "" }],
        name: "规格名" + num,
      });
      this.renderTableData(this.skuTableData);
    },
    // 编辑规格名
    editSkuItem() {
      this.renderTableData(this.skuTableData);
    },
    // 编辑规格值
    async skuValueChange(val, index, item) {
      this.renderTableData(this.skuTableData);
    },
    // 获取焦点时，取得规格名对应的规格值
    changeSkuVals(name) {
      if (name) {
        this.skuData.forEach((e, index) => {
          if (e === name) {
            if (this.skuVal.length != this.skuVals[index].length) {
              this.skuVal = this.skuVals[index];
            }
          }
        });
      }
    },
    /** 移除当前规格项 进行数据变化*/
    handleCloseSkuItem($index, item) {
      this.skuInfo.splice($index, 1);
      this.skuTableData.forEach((e, index) => {
        delete e[item.name];
      });
      /**
       * 渲染规格详细表格
       */
      this.renderTableData(this.skuTableData);
    },
    // 添加规格值的验证
    validateEmpty(params) {
      let flag = true;
      params.forEach((item) => {
        for (var key in item) {
          if (item[key] != "0" && !item.value) {
            this.$Message.error("请必填规格项");
            flag = false;
            return false; // 终止程序
          }
        }
      });

      return flag;
    },
    /** 添加当前规格项的规格值*/
    addSpec($index, item) {
      if (this.validateEmpty(item.spec_values)) {
        if (item.spec_values.length >= 10) {
          this.$Message.error("规格值不能大于10个！");
          return;
        }

        this.$set(item.spec_values, item.spec_values.length, {
          name: item.name,
          value: "",
        });
        this.baseInfoForm.regeneratorSkuFlag = true;
        /**
         * 渲染规格详细表格
         */
        this.renderTableData(this.skuTableData);
      }
    },
    handleClearSku() {
      this.skuInfo = [];
      this.skuTableData = [];
      this.renderTableData(this.skuTableData);
    },
    /** 移除当前规格值 */
    handleCloseSkuValue(item, index) {
      // this.skuInfo[item.name].spec_values.splice(index, 1);
      this.skuInfo.forEach((i) => {
        if (i.name === item.name) {
          i.spec_values.splice(index, 1);
        }
      });
      this.skuTableData = this.skuTableData.filter(
        (e, index) => e[item.name] !== item.value
      );
      this.baseInfoForm.regeneratorSkuFlag = true;
      /**
       * 渲染规格详细表格
       */
      this.renderTableData(this.skuTableData);
    },

    /**
     * 渲染table所需要的column 和 data
     */
    renderTableData(skus) {
      this.skuTableColumn = [];
      // this.skuTableData = [];
      let pushData = [];
      //渲染头部
      this.skuInfo.forEach((sku) => {
        // !sku.name ? (sku.name = "规格名") : "";
        //列名称
        let columnName = sku.name;
        pushData.push({
          title: columnName,
          key: columnName,
        });
      });

      this.baseInfoForm.goodsType != "VIRTUAL_GOODS"
        ? pushData.push({
            title: "重量",
            slot: "weight",
          })
        : "";
      pushData.push(
        {
          title: "货号",
          slot: "sn",
        },
        {
          title: "库存",
          slot: "quantity",
        },
        {
          title: "成本价",
          slot: "cost",
        },
        {
          title: "价格",
          slot: "price",
        },
        {
          title: "图片",
          slot: "images",
        }
      );

      this.skuTableColumn = pushData;
      //克隆所有渲染的数据
      let cloneTemp = cloneObj(this.skuInfo);
      if (cloneTemp[0]) {
        //存放最终结果
        let result = [];
        //循环选中的 sku 数据
        cloneTemp[0].spec_values.forEach((specItem, index) => {
          // 如存在数据，则将数据赋值
          if (skus && skus[index] && specItem.value !== "") {
            let obj = {
              id: skus[index].id,
              sn: skus[index].sn,
              quantity: skus[index].quantity,
              cost: skus[index].cost,
              price: skus[index].price,
              [cloneTemp[0].name]: specItem.value,
              images: this.baseInfoForm.goodsGalleryFiles || [],
            };
            if (specItem.value !== "") {
              obj.id = skus[index].id;
            }
            if (skus[index].weight !== "") {
              obj.weight = skus[index].weight;
            }
            result.push(obj);
          } else {
            result.push({
              [cloneTemp[0].name]: specItem.value,
              images: this.baseInfoForm.goodsGalleryFiles || [],
            });
          }
        });
        cloneTemp.splice(0, 1);
        result = this.specIterator(result, cloneTemp, skus);
        this.skuTableData = result;
      }
    },
    /**
     * 迭代属性，形成表格
     * result 渲染的数据
     * array spec数据
     */
    specIterator(result, cloneTemp, skus) {
      // console.log("-----skus-----");
      // console.log(JSON.parse(JSON.stringify(skus)));
      // console.log("-----result-----");
      // console.log(JSON.parse(JSON.stringify(result)));
      // console.log("-----cloneTemp-----");
      // console.log(JSON.parse(JSON.stringify(cloneTemp)));
      //是否还可以循环
      if (cloneTemp.length > 0) {
        let table = [];
        // 已有数据索引
        let findIndex = 0;
        result.forEach((resItem, index) => {
          cloneTemp[0].spec_values.forEach((valItem, _index) => {
            let obj = cloneObj(resItem);
            // 判断已存在数据数量是否大于渲染数据数量（认为是在编辑原存在数据的规格值）且规格值不为空且存在已存在数据，如符合条件，则认为为修改已存在数据
            if (skus.length > result.length && valItem.value !== "" && skus[findIndex]) {
              obj = cloneObj(skus[findIndex]);
            }
            let emptyFlag = false;
            // 判断是否为规格项的第一个值，如果不为第一个值则判断为新加数据
            if (cloneTemp[0].spec_values.length > 1 && valItem.value === "") {
              delete obj.id;
              delete obj.sn;
              delete obj.quantity;
              delete obj.cost;
              delete obj.price;
              delete obj.weight;
            }

            // 判断渲染数据中是否存在空值，存在空值则判断新加数据
            for (let key in obj) {
              if (!obj[key]) {
                emptyFlag = true;
                break;
              }
            }

            // 判断当前渲染数据是否已有数据
            if (
              skus &&
              skus[findIndex] &&
              (!skus[findIndex].id || obj.id === skus[findIndex].id) &&
              valItem.value !== "" &&
              !emptyFlag
            ) {
              // 将原存在的数据放入结果中
              let originSku = skus[findIndex];
              obj = {
                sn: obj.sn || originSku.sn,
                quantity: obj.quantity || originSku.quantity,
                cost: obj.cost || originSku.cost,
                price: obj.price || originSku.price,
                weight: obj.weight || originSku.weight,
                ...obj,
              };
              if (
                originSku[valItem.name] === valItem.value ||
                (obj.id && originSku.id === obj.id)
              ) {
                obj.id = originSku.id;
              }

              // 视为处理完成已存在数据，将原数据索引后移1位
              if (skus.length == result.length) {
                findIndex++;
              }
            }

            // 如原存在数据大于渲染数据（认为是在编辑原存在数据的规格值），且存在已存在数据，且规格值不为空。则将原数据索引后移1位
            if (skus.length > result.length && (skus[findIndex] && valItem.value !== "")) {
              findIndex++;
            }

            obj[cloneTemp[0].name] = valItem.value;
            table.push(obj);
          });
        });
        result = [];
        table.forEach((t) => {
          result.push(t);
        });
        //清除当前循环的分组
        cloneTemp.splice(0, 1);
      } else {
        return result;
      }
      return this.specIterator(result, cloneTemp, skus);
    },
    /** 根据分类id获取系统设置规格信息*/
    Get_SkuInfoByCategory(categoryId) {
      if (categoryId) {
        API_GOODS.getGoodsSpecInfoSeller(categoryId).then((res) => {
          if (res.length) {
            res.forEach((e) => {
              this.skuData.push(e.specName);
              const vals = e.specValue ? e.specValue.split(",") : [];
              this.skuVals.push(Array.from(new Set(vals)));
            });
          }
        });
      }
    },
    // 判断相同数组的值
    scalarArrayEquals(array1, array2) {
      return (
        array1.length === array2.length &&
        array1.every(function (v, i) {
          return v === array2[i];
        })
      );
    },
    /** 自动完成表单所需方法*/
    filterMethod(value, option) {
      return option.toUpperCase().indexOf(value.toUpperCase()) !== -1;
    },
    /** 数据改变之后 抛出数据 */
    updateSkuTable(row, item, type = "deafult") {
      let index = row._index;
      this.baseInfoForm.regeneratorSkuFlag = true;
      /** 进行自定义校验 判断是否是数字（小数也能通过）重量 */
      if (item === "weight") {
        if (
          !/^[+]{0,1}(\d+)$|^[+]{0,1}(\d+\.\d+)$/.test(row[item]) ||
          parseInt(row[item]) < 0 ||
          parseInt(row[item]) > 99999999
        ) {
          // 校验未通过 加入错误存储列表中
          this.validateError.push([index, item]);
          this.validatatxt = "请输入0~99999999之间的数字值";
          return;
        }
      } else if (item === "quantity") {
        if (
          !/^[0-9]\d*$/.test(row[item]) ||
          parseInt(row[item]) < 0 ||
          parseInt(row[item]) > 99999999
        ) {
          // 库存
          this.validateError.push([index, item]);
          this.validatatxt = "请输入0~99999999之间的整数";
          return;
        }
      } else if (item === "cost" || item === "price") {
        if (
          !regular.money.test(row[item]) ||
          parseInt(row[item]) < 0 ||
          parseInt(row[item]) > 99999999
        ) {
          // 成本价 价格
          this.validateError.push([index, item]);
          this.validatatxt = "请输入0~99999999之间的价格";
          return;
        }
      }
      this.$nextTick(() => {
        this.skuTableData[index][item] = row[item];
      });
      // this.$set(this.skuTableData,[index][item],row[item])
    },
    // 店内分类选择
    selectTree(v) {
      if (v.length > 0) {
        // 转换null为""
        for (let attr in v[0]) {
          if (v[0][attr] == null) {
            v[0][attr] = "";
          }
        }
        let str = JSON.stringify(v[0]);
        let menu = JSON.parse(str);
        this.form = menu;
        this.editTitle = menu.title;
      }
    },
    // 店内分类选中
    changeSelect(v) {
      this.selectCount = v.length;
      let ids = "";
      v.forEach(function (e) {
        ids += e.id + ",";
      });
      ids = ids.substring(0, ids.length - 1);

      if (ids.length > 100) {
        this.$Message.error("选择了过多的店铺分类，请谨慎选择");
      }
      this.baseInfoForm.storeCategoryPath = ids;
    },
    /**  添加商品 **/
    save() {
      // this.submitLoading = true;
      this.$refs["baseInfoForm"].validate((valid) => {
        if (valid) {
          let submit = JSON.parse(JSON.stringify(this.baseInfoForm));
          if (
            submit.goodsGalleryFiles &&
            submit.goodsGalleryFiles.length <= 0
          ) {
            this.submitLoading = false;
            this.$Message.error("请上传商品图片");
            return;
          }
          if (submit.templateId === "") submit.templateId = 0;
          let flag = false;
          let paramValue = "";

          if (flag) {
            this.$Message.error(paramValue + " 参数值不能为空");
            this.submitLoading = false;
            return;
          }
          let skuInfoNames = this.skuInfo.map((n) => n.name);
          submit.skuList = [];
          this.skuTableData.map((sku) => {
            let skuCopy = {
              cost: sku.cost,
              price: sku.price,
              quantity: sku.quantity,
              sn: sku.sn,
              images: sku.images,
            };
            if (sku.weight) {
              skuCopy.weight = sku.weight;
            }
            if (sku.id) {
              skuCopy.id = sku.id;
            }
            for (let skuInfoName of skuInfoNames) {
              skuCopy[skuInfoName] = sku[skuInfoName];
            }
            submit.skuList.push(skuCopy);
          });
          if (submit.goodsGalleryFiles.length > 0) {
            submit.goodsGalleryList = submit.goodsGalleryFiles.map(
              (i) => i.url
            );
          }
          /** 参数校验 **/
          /* Object.keys(submit.goodsParamsList).forEach((item) => {
          });*/
          submit.release ? (submit.release = true) : (submit.release = false);
          submit.recommend
            ? (submit.recommend = true)
            : (submit.recommend = false);
          if (this.goodsId) {
            API_GOODS.editGoods(this.goodsId, submit).then((res) => {
              if (res.success) {
                this.submitLoading = false;
                this.$router.go(-1);
              } else {
                this.submitLoading = false;
              }
            });
          } else {
            API_GOODS.createGoods(submit).then((res) => {
              if (res.success) {
                this.submitLoading = false;
                this.$parent.activestep = 2;
                window.scrollTo(0, 0);
              } else {
                this.submitLoading = false;
              }
            });
          }
        } else {
          this.submitLoading = false;

          this.$Message.error("还有必填项未做处理，请检查表单");
        }
      });
    },
    /** 保存为模板 */
    saveToDraft() {
      this.baseInfoForm.skuList = this.skuTableData;
      if (this.baseInfoForm.goodsGalleryFiles.length > 0) {
        this.baseInfoForm.goodsGalleryList =
          this.baseInfoForm.goodsGalleryFiles.map((i) => i.url);
      }
      this.baseInfoForm.categoryName = [];
      this.baseInfoForm.saveType = "TEMPLATE";

      if (this.$route.query.draftId) {
        this.baseInfoForm.id = this.$route.query.draftId;
        this.$Modal.confirm({
          title: "当前模板已存在",
          content: "当前模板已存在，保存为新模板或替换原模板",
          okText: "保存新模板",
          cancelText: "替换旧模板",
          closable: true,
          onOk: () => {
            delete this.baseInfoForm.id;
            this.SAVE_DRAFT_GOODS();
            return;
          },
          onCancel: () => {
            this.SAVE_DRAFT_GOODS();
            return;
          },
        });
        return;
      }

      this.$Modal.confirm({
        title: "保存模板",
        content: "是否确定保存",
        okText: "保存",
        closable: true,
        onOk: () => {
          this.SAVE_DRAFT_GOODS();
        },
      });
    },
    SAVE_DRAFT_GOODS() {
      // 保存模板
      API_GOODS.saveDraftGoods(this.baseInfoForm).then((res) => {
        if (res.success) {
          this.$Message.info("保存成功！");
          this.$router.push({ name: "template-goods" });
        }
      });
    },
  },
  mounted() {
    this.accessToken = {
      accessToken: this.getStore("accessToken"),
    };
    // 获取物流模板
    API_Shop.getShipTemplate().then((res) => {
      if (res.success) {
        this.logisticsTemplate = res.result;
      }
    });
    if (this.$route.query.id || this.$route.query.draftId) {
      // 编辑商品、模板
      this.GET_GoodData(this.$route.query.id, this.$route.query.draftId);
    } else {
      // 新增商品、模板
      if (this.firstData.tempId) {
        // 选择模板
        this.GET_GoodData("", this.firstData.tempId);
      } else {
        const cateId = [];
        this.firstData.category.forEach((cate) => {
          this.baseInfoForm.categoryName.push(cate.name);
          cateId.push(cate.id);
        });
        this.categoryId = cateId[2];
        this.baseInfoForm.categoryPath = cateId.toString();
        this.baseInfoForm.goodsType = this.firstData.goodsType;
        /** 获取该商城分类下 商品参数信息 */
        this.GET_GoodsParams();
        /** 查询品牌列表 */
        this.getGoodsBrandList();
        /** 查询分类绑定的规格信息 */
        this.Get_SkuInfoByCategory(this.categoryId);
        // 获取商品单位
        this.GET_GoodsUnit();
        // 获取当前店铺分类
        this.GET_ShopGoodsLabel();
      }
    }
  },
};
</script>
<style lang="scss" scoped>
@import "./addGoods.scss";
</style>

<style>
.ivu-select .ivu-select-dropdown {
  overflow: hidden !important;
}
</style>