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

package notify

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/i18n"
)

func (o *NotifyGroup) GetNotifyItemsByNotifyID(notifyID int64) ([]*apistructs.NotifyItem, error) {
	return o.db.GetNotifyItemsByNotifyID(notifyID)
}

func (o *NotifyGroup) QueryNotifyItems(locale *i18n.LocaleResource, request *apistructs.QueryNotifyItemRequest) (*apistructs.QueryNotifyItemData, error) {
	itemData, err := o.db.QueryNotifyItems(request)
	if err != nil {
		return nil, err
	}
	for _, item := range itemData.List {
		o.LocaleItem(locale, item)
	}
	return itemData, nil
}

func (o *NotifyGroup) UpdateNotifyItem(request *apistructs.UpdateNotifyItemRequest) error {
	return o.db.UpdateNotifyItem(request)
}

func (o *NotifyGroup) LocaleItem(locale *i18n.LocaleResource, item *apistructs.NotifyItem) {
	item.DisplayName = locale.Get("notify." + item.Category + "." + item.Name)
	item.MarkdownTemplate = locale.Get("notify."+item.Category+"."+item.Name+".markdown_template", "")
	item.EmailTemplate = locale.Get("notify."+item.Category+"."+item.Name+".email", "")
	item.DingdingTemplate = locale.Get("notify."+item.Category+"."+item.Name+".dingding", "")
	item.MBoxTemplate = locale.Get("notify."+item.Category+"."+item.Name+".mbox", "")
}
