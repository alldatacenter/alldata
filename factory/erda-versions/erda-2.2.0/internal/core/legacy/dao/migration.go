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

package dao

import (
	"strconv"

	"github.com/erda-project/erda/internal/core/legacy/model"
)

var joinMap = "LEFT OUTER JOIN kratos_uc_userid_mapping on kratos_uc_userid_mapping.id = uc_user.id"

const noPass = "no pass"

func (client *DBClient) GetUcUserList() ([]model.User, error) {
	var users []model.User
	sql := client.Table("uc_user").Joins(joinMap)
	if err := sql.Where("password IS NOT NULL AND kratos_uc_userid_mapping.id IS NULL").Find(&users).Error; err != nil {
		return nil, err
	}
	return users, nil
}

func (client *DBClient) InsertMapping(userID, uuid string) error {
	return client.Table("kratos_uc_userid_mapping").Create(&model.UserIDMapping{ID: userID, UserID: uuid}).Error
}

func (client *DBClient) GetUcUserID(uuid string) (string, error) {
	var users []model.User
	if err := client.Table("kratos_uc_userid_mapping").Select("id").Where("user_id = ?", uuid).Find(&users).Error; err != nil {
		return "", err
	}
	if len(users) == 0 {
		return "", nil
	}
	return strconv.FormatInt(users[0].ID, 10), nil
}
