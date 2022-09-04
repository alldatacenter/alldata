/**
 * Created by caoshuaibiao on 2019/3/16.
 */
import httpClient from '../../utils/httpClient';

class FormElementAPI {
    //TODO getOrgType2 getSubType getChangefreeProductList 需要从老主站服务迁移至工单服务,重新对接cf
    getOrgType2(id) {
        return httpClient.get("changefree", {
            params: {
                dept_id: id,
                action: "query_second_layer_dept"
            }
        })
    }

    getSubType(id) {
        return httpClient.get("changefree", {
            params: {
                dept_id: id,
                action: "query_change_type"
            }
        })
    }

    getChangefreeProductList(str) {
        return httpClient.get("changefree", {
            params: {
                key: str,
                action: "query_product_app_info"
            }
        })
    }

    getItemData(url, params) {
        return httpClient.get(url, {
            params: params
        })
    }

    getBucUserInfo = (k) => {
        return httpClient.get("gateway/v2/foundation/connector/buc/user/getUserListByKeywordNew", {
            params: {
                keyword: k
            }
        })
    }

}

const formElementAPI = new FormElementAPI();

export default formElementAPI