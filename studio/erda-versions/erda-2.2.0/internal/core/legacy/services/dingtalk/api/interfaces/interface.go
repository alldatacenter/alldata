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

package interfaces

import "time"

type DingtalkAccessTokenManager interface {
	GetAccessToken(appKey string) (string, error)
}

type DingtalkUserInfoManager interface {
	GetUserIdsByPhones(accessToken string, agentId int64, phones []string) (userIds []string, err error)
}

type DingtalkApiClient interface {
	SendWorkNotice(phones []string, title, content string) error
}

type DingTalkApiClientFactory interface {
	GetClient(appKey, appSecret string, agentId int64) DingtalkApiClient
}

type KvCache interface {
	Get(key string) (string, error)
	Set(key string, value string, expire time.Duration) (string, error)
}
