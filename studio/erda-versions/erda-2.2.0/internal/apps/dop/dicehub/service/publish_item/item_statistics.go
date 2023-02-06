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
	"github.com/gogap/errors"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
)

func (i *PublishItem) GetMonitorkeys(req *apistructs.QueryAppPublishItemRelationRequest) ([]apistructs.MonitorKeys, error) {
	publishItem, err := i.db.GetPublishItem(req.PublishItemID)
	if err != nil {
		return nil, err
	}
	mks := []apistructs.MonitorKeys{{
		AK:    publishItem.AK,
		AI:    publishItem.AI,
		Env:   "OFFLINE",
		AppID: 0,
	}}

	resp, err := i.bdl.QueryAppPublishItemRelations(req)
	if err != nil {
		return nil, err
	}

	for _, relation := range resp.Data {
		if relation.AK == "" || relation.AI == "" {
			continue
		}
		mks = append(mks, apistructs.MonitorKeys{
			AK:    relation.AK,
			AI:    relation.AI,
			Env:   relation.Env,
			AppID: relation.AppID,
		})
	}

	return mks, nil
}

func (i *PublishItem) GetPublishItemByAKAI(AK, AI string) (*dbclient.PublishItem, error) {
	if AK == "" || AI == "" {
		return nil, errors.New("empty ak or ai")
	}

	var publishItemID int64
	item, err := i.db.GetPublishItemByAKAI(AK, AI)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			resp, err := i.bdl.QueryAppPublishItemRelations(&apistructs.QueryAppPublishItemRelationRequest{AK: AK, AI: AI})
			if err != nil {
				return nil, err
			}
			if len(resp.Data) != 1 {
				return nil, errors.Errorf("invalid ak or ai: %s, %s", AK, AI)
			}
			publishItemID = resp.Data[0].PublishItemID
		} else {
			return nil, err
		}
	} else {
		publishItemID = int64(item.ID)
	}

	return i.db.GetPublishItem(publishItemID)
}
