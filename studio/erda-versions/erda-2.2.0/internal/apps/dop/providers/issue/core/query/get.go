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

package query

import (
	"github.com/jinzhu/gorm"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (p *provider) GetIssue(id int64, identityInfo *commonpb.IdentityInfo) (*pb.Issue, error) {
	// 请求校验
	if id == 0 {
		return nil, apierrors.ErrGetIssue.MissingParameter("id")
	}
	// 查询事件
	model, err := p.db.GetIssue(id)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, apierrors.ErrGetIssue.NotFound()
		}
		return nil, apierrors.ErrGetIssue.InternalError(err)
	}
	issue, err := p.Convert(model, identityInfo)
	if err != nil {
		return nil, apierrors.ErrGetIssue.InternalError(err)
	}
	return issue, nil
}
