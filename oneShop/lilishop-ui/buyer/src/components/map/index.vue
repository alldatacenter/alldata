<template>
  <div class="lili-map">
    <Modal v-model="showMap" title="选择地址" width="800">
      <div class="address">{{ addrContent.address }}</div>
      <div id="map-container"></div>

      <div class="search-con">
        <Input
          placeholder="输入关键字搜索"
          id="input-map"
          v-model="mapSearch"
        />
        <ul>
          <li
            v-for="(tip, index) in tips"
            :key="index"
            @click="selectAddr(tip.location)"
          >
            <p>{{ tip.name }}</p>
            <p>{{ tip.district + tip.address }}</p>
          </li>
        </ul>
      </div>
      <div slot="footer">
        <Button type="default" @click="showMap = false">取消</Button>
        <Button type="primary" :loading="loading" @click="ok">确定</Button>
      </div>
    </Modal>
  </div>
</template>
<script>
import AMapLoader from '@amap/amap-jsapi-loader';
import { handleRegion } from '@/api/address.js';

export default {
  name: 'map',
  props: {
    useApi: {
      default: true,
      type: Boolean
    }
  },
  data () {
    return {
      config:require('@/config'),
      showMap: false, // 展示地图
      mapSearch: '', // 地图搜索
      map: null, // 初始化地图
      autoComplete: null, // 初始化搜索方法
      geocoder: null, // 初始化地理、坐标转化
      positionPicker: null, // 地图拖拽选点
      tips: [], // 搜索关键字列表
      addrContent: {}, // 回显地址信息
      loading: false // 加载状态
    };
  },
  watch: {
    // 监听搜索框搜索地图
    mapSearch: function (val) {
      this.searchOfMap(val);
    }
  },
  methods: {
    ok () {
      // 确定选择
      this.loading = true;
      const address = this.addrContent.address;
      const township = this.addrContent.regeocode.addressComponent.township;
      const index = address.indexOf(township) + township.length;
      this.addrContent.detail = address.substring(index);
      const params = {
        cityCode: this.addrContent.regeocode.addressComponent.citycode,
        townName: this.addrContent.regeocode.addressComponent.township
      };
      if (this.useApi) {
        handleRegion(params).then((res) => {
          this.loading = false;
          if (res.success) {
            this.showMap = false;
            this.addrContent.addr = res.result.name.replace(/,/g, ' ');
            this.addrContent.addrId = res.result.id;
            this.$emit('getAddress', this.addrContent);
          }
        });
      } else {
        this.loading = false;
        this.showMap = false;
        this.$emit('getAddress', this.addrContent);
      }
    },
    init () { // 初始化地图
      AMapLoader.load({
        key: this.config.aMapKey, // 申请好的Web端开发者Key，首次调用 load 时必填
        version: '', // 指定要加载的 JSAPI 的版本，缺省时默认为 1.4.15
        plugins: [
          'AMap.ToolBar',
          'AMap.Autocomplete',
          'AMap.PlaceSearch',
          'AMap.Geolocation',
          'AMap.Geocoder'
        ], // 需要使用的的插件列表，如比例尺'AMap.Scale'等
        AMapUI: {
          // 是否加载 AMapUI，缺省不加载
          version: '1.1', // AMapUI 缺省 1.1
          plugins: ['misc/PositionPicker'] // 需要加载的 AMapUI ui插件
        }
      })
        .then((AMap) => {
          let that = this;
          this.map = new AMap.Map('map-container', {
            zoom: 12
          });
          that.map.addControl(new AMap.ToolBar());
          that.map.addControl(new AMap.Autocomplete());
          that.map.addControl(new AMap.PlaceSearch());
          that.map.addControl(new AMap.Geocoder());

          // 实例化Autocomplete
          let autoOptions = {
            city: '全国'
          };
          that.autoComplete = new AMap.Autocomplete(autoOptions); // 搜索
          that.geocoder = new AMap.Geocoder(autoOptions);

          that.positionPicker = new AMapUI.PositionPicker({
            // 拖拽选点
            mode: 'dragMap',
            map: that.map
          });
          that.positionPicker.start();
          /**
           *
           * 所有回显数据，都在positionResult里面
           * 需要字段可以查找
           *
           */
          that.positionPicker.on('success', function (positionResult) {
            // console.log(positionResult);
            that.addrContent = positionResult;
          });
        })
        .catch((e) => {});
    },
    searchOfMap (val) {
      // 地图搜索
      let that = this;
      this.autoComplete.search(val, function (status, result) {
        // 搜索成功时，result即是对应的匹配数据
        if (status === 'complete' && result.info === 'OK') {
          that.tips = result.tips;
        } else {
          that.tips = [];
        }
      });
    },
    selectAddr (location) {
      // 选择坐标
      if (!location) {
        this.$Message.warning('请选择正确点位');
        return false;
      }
      const lnglat = [location.lng, location.lat];
      this.positionPicker.start(lnglat);
    }
  },
  mounted () {
    this.init();
  }
};
</script>
<style lang="scss" scoped>
#map-container {
  width: 500px;
  height: 400px;
}

.search-con {
  position: absolute;
  right: 20px;
  top: 64px;
  width: 260px;
  ul {
    width: 260px;
    height: 400px;
    overflow: scroll;
    li {
      padding: 5px;
      p:nth-child(2) {
        color: #999;
        font-size: 12px;
      }
      &:hover {
        background-color: #eee;
        cursor: pointer;
      }
    }
  }
}

.address {
  margin-bottom: 10px;
  font-weight: bold;
}
</style>
