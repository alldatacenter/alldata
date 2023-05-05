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

package dto

type RuleRegion int

const (
	PACKAGE_RULE RuleRegion = iota
	API_RULE
)

type RuleCategory string

const (
	ACL_RULE   RuleCategory = "acl"
	AUTH_RULE  RuleCategory = "auth"
	LIMIT_RULE RuleCategory = "limit"
)

var RULE_PRIORITY = map[RuleCategory]int{
	AUTH_RULE:  1000,
	ACL_RULE:   999,
	LIMIT_RULE: 998,
}

var KEYAUTH_CONFIG map[string]interface{}
var OAUTH2_CONFIG map[string]interface{}
var SIGNAUTH_CONFIG map[string]interface{}
var HMACAUTH_CONFIG map[string]interface{}

type OpenapiRule struct {
	Region          RuleRegion
	PackageApiId    string
	PackageId       string
	PluginId        string
	PluginName      string
	Category        RuleCategory
	Config          map[string]interface{}
	Enabled         bool
	PackageZoneNeed bool
	NotKongPlugin   bool
	ConsumerId      string
}

type OpenapiRuleInfo struct {
	Id string
	OpenapiRule
}

type SortByRuleList []OpenapiRuleInfo

func (list SortByRuleList) Len() int { return len(list) }

func (list SortByRuleList) Swap(i, j int) { list[i], list[j] = list[j], list[i] }

func (list SortByRuleList) Less(i, j int) bool {
	ip := RULE_PRIORITY[list[i].Category]
	jp := RULE_PRIORITY[list[j].Category]
	return ip > jp
}

func init() {
	KEYAUTH_CONFIG = map[string]interface{}{}
	KEYAUTH_CONFIG["key_names"] = []string{"appKey", "x-app-key", "Access-Token"}
	OAUTH2_CONFIG = map[string]interface{}{}
	OAUTH2_CONFIG["token_expiration"] = 3600
	OAUTH2_CONFIG["enable_client_credentials"] = true
	OAUTH2_CONFIG["accept_http_if_already_terminated"] = true
	OAUTH2_CONFIG["global_credentials"] = true
	SIGNAUTH_CONFIG = map[string]interface{}{}
	SIGNAUTH_CONFIG["key_name"] = "appKey"
	SIGNAUTH_CONFIG["sign_name"] = "sign"
	HMACAUTH_CONFIG = map[string]interface{}{}
	HMACAUTH_CONFIG["hide_credentials"] = true
	HMACAUTH_CONFIG["validate_request_body"] = true
	HMACAUTH_CONFIG["enforce_headers"] = []string{"date", "request-line"}
	HMACAUTH_CONFIG["algorithms"] = []string{"hmac-sha256", "hmac-sha384", "hmac-sha512"}
}
