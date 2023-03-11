<template>
  <div class="upgrade-table">
    <div class="nav-bar">
      <left-outlined @click="goBack" />
      <span class="title g-ml-8">{{$t('upgradeHiveTable')}}</span>
    </div>
    <div class="content">
      <div class="table-attrs">
        <a-form
          name="fields"
          class="label-120"
        >
          <a-form-item
            :label="$t('field')"
            name="field"
          >
            <schema-field :loading="loading" :fields="field" ref="schemaFieldRef"></schema-field>
          </a-form-item>
          <a-form-item
            :label="$t('partitonField')"
            name="partitonField"
          >
            <partition-field :loading="loading" :partitionFields="partitionFields"></partition-field>
          </a-form-item>
          <a-form-item
            :label="$t('otherProperties')"
            name="otherProperties"
          >
            <other-properties :propertiesObj="propertiesObj" ref="propertiesRef" />
          </a-form-item>
        </a-form>
      </div>
      <div class="footer-btn">
        <a-button type="primary" @click="onCofirm" :loading="loading" class="btn g-mr-12">{{$t('ok')}}</a-button>
        <a-button type="ghost" @click="cancel" class="btn">{{$t('cancel')}}</a-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LeftOutlined } from '@ant-design/icons-vue'
import schemaField from './components/Field.vue'
import partitionField from './components/Partition.vue'
import otherProperties from './components/Properties.vue'
import { DetailColumnItem, IMap } from '@/types/common.type'
import { getHiveTableDetail, upgradeHiveTable } from '@/services/table.service'

const loading = ref<boolean>(false)
const field = reactive<DetailColumnItem[]>([])
const partitionFields = reactive<DetailColumnItem[]>([])
const propertiesObj = reactive<IMap<string>>({})
const pkName = reactive<IMap<string>[]>([])

const emit = defineEmits<{
 (e: 'goBack'): void
 (e: 'refresh'): void
}>()

const router = useRouter()
const route = useRoute()

const params = computed(() => {
  return {
    ...route.query
  }
})
const schemaFieldRef = ref()
const propertiesRef = ref()

async function getDetails() {
  try {
    const { catalog, db, table } = params.value
    if (!catalog || !db || !table) {
      return
    }
    loading.value = true
    partitionFields.length = 0
    field.length = 0
    const result = await getHiveTableDetail({
      ...params.value
    })
    const { partitionColumnList = [], schema, properties } = result;
    (partitionColumnList || []).forEach((ele: DetailColumnItem) => {
      partitionFields.push(ele)
    });
    (schema || []).forEach((ele: DetailColumnItem) => {
      field.push(ele)
    })
    Object.assign(propertiesObj, properties)
  } catch (error) {
  } finally {
    loading.value = false
  }
}

function onCofirm() {
  getParams()
}

async function getParams() {
  pkName.length = 0
  const pkList = schemaFieldRef.value.getPkname()
  pkList.forEach((ele: DetailColumnItem) => {
    pkName.push(ele)
  })
  propertiesRef.value.getProperties().then((res) => {
    if (res) {
      Object.assign(propertiesObj, res)
      upgradeTable()
    }
  })
}

async function upgradeTable() {
  try {
    const { catalog, db, table } = params.value
    if (!catalog || !db || !table) {
      return
    }
    loading.value = true
    await upgradeHiveTable({
      ...params.value,
      pkList: pkName,
      properties: propertiesObj
    })
    goBack()
    emit('refresh')
  } catch (error) {
    // failed
    goBack()
  } finally {
    loading.value = false
  }
}
function goBack() {
  emit('goBack')
}

function cancel() {
  goBack()
}

onMounted(() => {
  getDetails()
})
</script>

<style lang="less" scoped>
.upgrade-table {
  height: 100%;
  display: flex;
  flex: 1;
  flex-direction: column;
  .nav-bar {
    padding-left: 12px;
    height: 20px;
    flex-shrink: 0;
  }
  .content {
    padding: 24px 24px 0;
    display: flex;
    flex: 1;
    flex-direction: column;
    width: 66%;
    justify-content: space-between;
    height: calc(100% - 32px);
    .table-attrs {
      display: flex;
      flex: 1;
      overflow-y: auto;
      padding-bottom: 48px;
    }
    .footer-btn {
      height: 32px;
      margin-top: 24px;
      .btn {
        min-width: 78px;
      }
    }
  }
}
</style>
