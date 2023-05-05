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

package model

import "github.com/erda-project/erda/pkg/database/dbengine"

// Certificate 证书信息模型
type Certificate struct {
	dbengine.BaseModel

	OrgID    int64
	Name     string
	Android  string
	Ios      string
	Message  string
	Type     string // IOS发布证书/Android证书/消息推送证书
	Desc     string
	Creator  string
	Operator string
}

// TableName 设置模型对应数据库表名称
func (Certificate) TableName() string {
	return "dice_certificates"
}

// AppCertificate 应用引用证书信息模型
type AppCertificate struct {
	dbengine.BaseModel

	AppID         int64
	CertificateID int64
	ApprovalID    int64
	Status        string
	Operator      string
	PushConfig    string
}

// TableName 设置模型对应数据库表名称
func (AppCertificate) TableName() string {
	return "dice_app_certificates"
}
