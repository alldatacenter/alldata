// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package publish_item

import (
	"encoding/json"
	"net/url"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/metrics"
	"github.com/erda-project/erda/internal/pkg/metrics/query"
)

// GetCertificationlist 获取验证列表
func (i *PublishItem) GetCertificationlist(req *apistructs.PublishItemCertificationListRequest,
	mk *apistructs.MonitorKeys) ([]*apistructs.PublishItemCertificationResponse, error) {
	return GetUserList(mk.AK, mk.AI, req)
}

func GetUserList(ak, ai string, req *apistructs.PublishItemCertificationListRequest) ([]*apistructs.PublishItemCertificationResponse, error) {
	logrus.Infof("start time: %d, end time: %d", req.StartTime, req.EndTime)
	endTime := time.Unix(int64(req.EndTime/1000), 0)
	startTime := time.Unix(int64(req.StartTime/1000), 0)

	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	deviceReq := query.CreateQueryRequest("ta_metric_mobile_metrics")
	deviceReq = deviceReq.
		StartFrom(startTime).
		EndWith(endTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("un", "?*").
		GroupBy([]string{"tags.uid"}).
		Apply("last", "tags.uid").
		Apply("last", "tags.cid").
		Apply("last", "tags.un").
		Apply("max", ".timestamp").
		Apply("sort", "max_.timestamp").
		Apply("limit", "1000")

	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }
	logrus.Infof("monitor request params 获取认证列表数据: %s", deviceReq.ConstructParam().Encode())
	// resp, err := client.QueryMetric(deviceReq)
	// if err != nil {
	// 	return nil, err
	// }
	resp, err := metrics.Client.QueryMetric(deviceReq)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != 200 {
		logrus.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
		return nil, errors.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
	}
	var respBody apistructs.PublishItemMetricsCardinalityInterfaceResp
	logrus.Infof("monitor single resp data: %s", string(resp.Body))
	if err := json.Unmarshal(resp.Body, &respBody); err != nil {
		return nil, err
	}
	if len(respBody.Data.Results[0].Data) == 0 {
		return nil, nil
	}
	logrus.Infof("monitor single resp data respBody.Data.Results data len: %d", len(respBody.Data.Results[0].Data))
	result := make([]*apistructs.PublishItemCertificationResponse, 0, len(respBody.Data.Results[0].Data))
	for _, value := range respBody.Data.Results[0].Data {
		item := &apistructs.PublishItemCertificationResponse{}
		for k, v := range value {
			if strings.Contains(k, "tags.cid") {
				item.DeviceNo = v.Data.(string)
			}
			if strings.Contains(k, "tags.uid") {
				if v.Data == nil {
					item.UserID = ""
				} else {
					item.UserID = v.Data.(string)
				}
			}
			if strings.Contains(k, "tags.un") {
				if v.Data == nil {
					item.UserName = ""
				} else {
					m, err := url.ParseQuery(v.Data.(string))
					if err != nil {
						return nil, err
					}
					for k := range m {
						item.UserName = k
					}

				}
			}
			if strings.Contains(k, "max.timestamp") {
				a := int64(v.Data.(float64))
				item.LastLoginTime = time.Unix(a/int64(time.Second), a%int64(time.Second))
			}
		}
		if item.UserID == "" {
			continue
		}
		result = append(result, item)
	}

	return result, nil
}
