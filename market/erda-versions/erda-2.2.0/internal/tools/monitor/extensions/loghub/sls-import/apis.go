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

package slsimport

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
)

// 1、调用ops的/api/cloud-account接口获取阿里云账号的ak+sk，查询数据库获取日志清洗的规则列表。
// 2、获取每个阿里云账号的sls下面的 Project 列表。https://help.aliyun.com/document_detail/29007.html?spm=a2c4g.11186623.6.1086.53797ad8lExjb8
// 3、获取每个 Project的 LogStore
// 4、获取每个 LogStore下的 Shard
// 5、启动 goroutine，通过sls sdk 消费每个 Shard。https://github.com/aliyun/aliyun-log-go-sdk?spm=a2c4g.11186623.2.21.5f602e89FmWksQ
// 6、根据规则，将日志数据清洗为指标

// Endpoints sls endpoints
var Endpoints = []string{
	"cn-hangzhou.log.aliyuncs.com",           // 华东1（杭州）
	"cn-hangzhou-finance.log.aliyuncs.com",   // 华东1（杭州-金融云）
	"cn-shanghai.log.aliyuncs.com",           // 华东2（上海）
	"cn-shanghai-finance-1.log.aliyuncs.com", // 华东2（上海-金融云）
	"cn-qingdao.log.aliyuncs.com",            // 华北1（青岛）
	"cn-beijing.log.aliyuncs.com",            // 华北2（北京）
	"cn-zhangjiakou.log.aliyuncs.com",        // 华北3（张家口）
	"cn-huhehaote.log.aliyuncs.com",          // 华北5（呼和浩特）
	"cn-shenzhen.log.aliyuncs.com",           // 华南1（深圳）
	"cn-shenzhen-finance.log.aliyuncs.com",   // 华南1（深圳-金融云）
	"cn-chengdu.log.aliyuncs.com",            // 西南1（成都）
	"cn-hongkong.log.aliyuncs.com",           // 中国（香港）
	"ap-northeast-1.log.aliyuncs.com",        // 日本（东京）
	"ap-southeast-1.log.aliyuncs.com",        // 新加坡
	"ap-southeast-2.log.aliyuncs.com",        // 澳大利亚（悉尼）
	"ap-southeast-3.log.aliyuncs.com",        // 马来西亚（吉隆坡）
	"ap-southeast-5.log.aliyuncs.com",        // 印度尼西亚（雅加达）
	"me-east-1.log.aliyuncs.com",             // 阿联酋（迪拜）
	"us-west-1.log.aliyuncs.com",             // 美国（硅谷）
	"eu-central-1.log.aliyuncs.com",          // 德国（法兰克福）
	"us-east-1.log.aliyuncs.com",             // 美国（弗吉尼亚）
	"ap-south-1.log.aliyuncs.com",            // 印度（孟买）
	"eu-west-1.log.aliyuncs.com",             // 英国（伦敦）
}

// AccountInfo .
type AccountInfo struct {
	OrgID           string
	OrgName         string
	AccessKey       string
	AccessSecretKey string
	Endpoints       []string
}

func (p *provider) getAccountInfo() (map[string]*AccountInfo, error) {
	if len(p.C.Account.AccessKey) > 0 {
		return map[string]*AccountInfo{
			p.C.Account.OrgID + "/" + p.C.Account.AccessKey: {
				OrgID:           p.C.Account.OrgID,
				OrgName:         p.C.Account.OrgName,
				AccessKey:       p.C.Account.AccessKey,
				AccessSecretKey: p.C.Account.AccessSecretKey,
				Endpoints:       Endpoints,
			},
		}, nil
	}
	pageNo, pageSize := 1, 30
	orgs := make(map[uint64]string)
	for {
		resp, err := p.bdl.ListOrgs(&apistructs.OrgSearchRequest{
			PageNo:   pageNo,
			PageSize: pageSize,
		}, "")
		if err != nil {
			return nil, err
		}
		for _, item := range resp.List {
			orgs[item.ID] = item.Name
		}
		if len(orgs) >= resp.Total || pageNo > ((resp.Total+pageSize-1)/pageSize) {
			break
		}
		pageNo++
	}
	accounts := make(map[string]*AccountInfo)
	for orgID, name := range orgs {
		orgid := strconv.FormatUint(orgID, 10)
		resp, err := p.bdl.GetOrgAccount(orgid, "aliyun")
		if err != nil {
			p.L.Warnf("fail to get org %d accounts, err: %s", orgID, err)
			continue
		}
		if resp == nil || resp.AccessKeyID == "" {
			continue
		}
		accounts[orgid+"/"+resp.AccessKeyID] = &AccountInfo{
			OrgID:           orgid,
			OrgName:         name,
			AccessKey:       resp.AccessKeyID,
			AccessSecretKey: resp.AccessSecret,
			Endpoints:       Endpoints,
		}
	}
	return accounts, nil
}
