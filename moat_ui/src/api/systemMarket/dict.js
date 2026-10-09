import request from '@/utils/request'

/**
 * 按字典编码查询字典明细（数据字典）
 *
 * 后端：datax-service-system  cn.datax.service.system.controller.DictController#getDictItems
 *       GET /dicts/code/{code}
 * 网关：/data/system/**  →  lb://service-data-system  (StripPrefix=2)
 *       ⇒ 前端 URL 为 /data/system/dicts/code/{code}
 *
 * 返回：{"success":true,"code":200,"msg":"操作成功",
 *        "data":[{"id":"...","dictId":"...","itemText":"1","itemValue":"运行"}, ...]}
 *
 * 注意 data 元素的字段是 itemText / itemValue（对应库表 sys_market_dict_item 的
 * item_text / item_value），与 utils/data-process.js 里的 selectDictLabel 保持一致。
 *
 * 典型用法：
 *   this.getDicts('sys_job_status').then(res => { if (res.success) this.statusOptions = res.data })
 */
export function getDicts(dictCode) {
  return request({
    url: '/data/system/dicts/code/' + dictCode,
    method: 'get'
  })
}
