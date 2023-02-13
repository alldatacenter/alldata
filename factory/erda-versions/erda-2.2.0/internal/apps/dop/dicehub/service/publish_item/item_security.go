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
	"math"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
)

// GetBlacklists 获取黑名单列表，分页
func (i *PublishItem) GetBlacklists(req *apistructs.PublishItemUserlistRequest) (*apistructs.PublishItemUserlistData, error) {
	_, err := i.checkPublishItemExsit(int64(req.PublishItemID))
	if err != nil {
		return nil, err
	}
	total, blacklists, err := i.db.GetBlacklists(req.PageNo, req.PageSize, req.PublishItemID)
	if err != nil {
		return nil, err
	}

	var resultData apistructs.PublishItemUserlistData
	if len(*blacklists) == 0 {
		return &resultData, nil
	}

	resultData.Total = total
	for _, v := range *blacklists {
		resultData.List = append(resultData.List, &apistructs.PublishItemUserListResponse{
			ID:        v.ID,
			UserID:    v.UserID,
			UserName:  v.UserName,
			DeviceNo:  v.DeviceNo,
			CreatedAt: v.CreatedAt,
		})
	}
	return &resultData, nil
}

// GetEraselists 获取数据擦除列表，分页
func (i *PublishItem) GetEraselists(req *apistructs.PublishItemUserlistRequest) (*apistructs.PublishItemUserlistData, error) {
	_, err := i.checkPublishItemExsit(int64(req.PublishItemID))
	if err != nil {
		return nil, err
	}
	total, blacklists, err := i.db.GetErases(req.PageNo, req.PageSize, req.PublishItemID)
	if err != nil {
		return nil, err
	}

	var resultData apistructs.PublishItemUserlistData
	if len(*blacklists) == 0 {
		return &resultData, nil
	}

	resultData.Total = total
	for _, v := range *blacklists {
		resultData.List = append(resultData.List, &apistructs.PublishItemUserListResponse{
			EraseStatus: v.EraseStatus,
			DeviceNo:    v.DeviceNo,
			CreatedAt:   v.UpdatedAt,
		})
	}
	return &resultData, nil
}

// AddBlacklist 添加黑名单
func (i *PublishItem) AddBlacklist(req *apistructs.PublishItemUserlistRequest) (error, *dbclient.PublishItem) {
	artifact, err := i.checkPublishItemExsit(int64(req.PublishItemID))
	if err != nil {
		return err, nil
	}
	if req.UserID != "" {
		blacklist, err := i.db.GetBlacklistByUserID(req.UserID, req.PublishItemID)
		if err != nil {
			return err, nil
		}
		if blacklist != nil {
			for _, v := range blacklist {
				if v.DeviceNo == req.DeviceNo {
					return errors.New("请勿重复添加"), nil
				}
			}
		}
	}
	if req.DeviceNo != "" {
		blacklist, err := i.db.GetBlacklistByDeviceNo(req.PublishItemID, req.DeviceNo)
		if err != nil {
			return err, nil
		}
		if blacklist != nil {
			for _, v := range blacklist {
				if v.UserID == req.UserID {
					return errors.New("请勿重复添加"), nil
				}
			}
		}
	}
	if err := i.db.CreateBlacklist(&dbclient.PublishItemBlackList{
		PublishItemID:  req.PublishItemID,
		PublishItemKey: artifact.AK,
		DeviceNo:       req.DeviceNo,
		UserID:         req.UserID,
		UserName:       req.UserName,
		Operator:       req.Operator,
	}); err != nil {
		return err, nil
	}
	return nil, artifact
}

// RemoveBlacklist 移除黑名单
func (i *PublishItem) RemoveBlacklist(blacklistID, publishItemID uint64) (error, *dbclient.PublishItemBlackList, *dbclient.PublishItem) {
	publishItem, err := i.checkPublishItemExsit(int64(publishItemID))
	if err != nil {
		return err, nil, nil
	}
	blacklist, err := i.db.GetBlacklistByID(blacklistID)
	if err != nil {
		return err, nil, nil
	}
	if blacklist != nil {
		if err := i.db.DeleteBlacklist(blacklist); err != nil {
			return err, nil, nil
		}
	}
	return nil, blacklist, publishItem
}

// AddErase 添加数据擦除
func (i *PublishItem) AddErase(req *apistructs.PublishItemUserlistRequest) (error, *dbclient.PublishItem) {
	artifact, err := i.checkPublishItemExsit(int64(req.PublishItemID))
	if err != nil {
		return err, nil

	}
	erase, err := i.db.GetEraseByDeviceNo(req.PublishItemID, req.DeviceNo)
	if err != nil {
		return err, nil
	}
	if erase == nil {
		if err := i.db.CreateErase(&dbclient.PublishItemErase{
			PublishItemID:  req.PublishItemID,
			PublishItemKey: artifact.AK,
			DeviceNo:       req.DeviceNo,
			EraseStatus:    apistructs.Erasing,
			Operator:       req.Operator,
		}); err != nil {
			return err, nil
		}
		return nil, artifact
	}
	if erase.EraseStatus == apistructs.Erasing {
		return errors.Errorf("请勿重复添加"), nil
	}

	erase.EraseStatus = apistructs.Erasing
	if err := i.db.UpdateErase(erase); err != nil {
		return err, nil
	}
	return nil, artifact
}

// UpdateErase 更新数据擦除
func (i *PublishItem) UpdateErase(request *apistructs.PublishItemEraseRequest) error {
	publishItem, err := i.GetPublishItemByAKAI(request.Ak, request.Ai)
	if err != nil {
		return err
	}
	if publishItem == nil {
		return nil
	}
	erase, err := i.db.GetEraseByDeviceNo(publishItem.ID, request.DeviceNo)
	if err != nil {
		return err
	}
	if erase == nil {
		return nil
	}
	if erase.EraseStatus != apistructs.EraseSuccess {
		erase.EraseStatus = request.EraseStatus
		if err := i.db.UpdateErase(erase); err != nil {
			logrus.Errorf("update erase status failure, deviceNo: %s, publishItemKey key: %s", request.DeviceNo, request.Ai)
			return err
		}
	}

	return nil
}

// GetSecurityStatus 获取客户安全信息状态
func (i *PublishItem) GetSecurityStatus(request apistructs.PublishItemSecurityStatusRequest) (*apistructs.PublishItemSecurityStatusResponse, error) {
	publishItem, err := i.GetPublishItemByAKAI(request.Ak, request.Ai)
	if err != nil {
		return nil, err
	}
	if publishItem == nil {
		return nil, errors.Errorf("not exist publish item")
	}
	inBlacklist := false
	if request.UserID != "" {
		blacklist, err := i.db.GetBlacklistByUserID(request.UserID, publishItem.ID)
		if err != nil {
			return nil, err
		}

		if len(blacklist) > 0 {
			for _, i := range blacklist {
				if i.DeviceNo == "" {
					inBlacklist = true
					break
				}
				if i.DeviceNo == request.DeviceNo {
					inBlacklist = true
					break
				}
			}
		}
	}

	if !inBlacklist {
		blacklist, err := i.db.GetBlacklistByDeviceNo(publishItem.ID, request.DeviceNo)
		if err != nil {
			return nil, err
		}
		if len(blacklist) > 0 {
			for _, i := range blacklist {
				if i.UserID == "" {
					inBlacklist = true
					break
				}
				if i.UserID == request.UserID {
					inBlacklist = true
				}
			}
		}
	}

	erase, err := i.db.GetEraseByDeviceNo(publishItem.ID, request.DeviceNo)
	if err != nil {
		return nil, err
	}
	eraseStatus := ""
	if erase != nil {
		eraseStatus = erase.EraseStatus
	}
	result := apistructs.PublishItemSecurityStatusResponse{
		NoJailbreak: publishItem.NoJailbreak,
		InBlacklist: inBlacklist,
		InEraseList: erase != nil,
		EraseStatus: eraseStatus,
	}
	if publishItem.GeofenceLon == 0.0 || publishItem.GeofenceLat == 0.0 {
		result.WithinGeofence = true
	} else {
		if request.Lon == 0.0 || request.Lat == 0.0 {
			result.WithinGeofence = false
		} else {
			dist := geoDistance(publishItem.GeofenceLon, publishItem.GeofenceLat, request.Lon, request.Lat)
			result.WithinGeofence = Smaller(dist*1000.0, publishItem.GeofenceRadius)
		}
	}

	return &result, nil
}

// checkPublishItemExsit 查看artifact是否存在
func (i *PublishItem) checkPublishItemExsit(publishItemID int64) (*dbclient.PublishItem, error) {
	publishItem, err := i.db.GetPublishItem(publishItemID)
	if err != nil {
		return nil, err
	}
	if publishItem == nil {
		return nil, errors.Errorf("non-existing publishItem information，publishItem id: %d", publishItemID)
	}
	return publishItem, nil
}

// GeoDistance 计算地理距离，依次为两个坐标的纬度、经度、单位（默认：英里，K => 公里，N => 海里）
func geoDistance(lng1 float64, lat1 float64, lng2 float64, lat2 float64) float64 {
	const PI float64 = 3.141592653589793

	radlat1 := float64(PI * lat1 / 180)
	radlat2 := float64(PI * lat2 / 180)

	theta := float64(lng1 - lng2)
	radtheta := float64(PI * theta / 180)

	dist := math.Sin(radlat1)*math.Sin(radlat2) + math.Cos(radlat1)*math.Cos(radlat2)*math.Cos(radtheta)

	if dist > 1 {
		dist = 1
	}

	dist = math.Acos(dist)
	dist = dist * 180 / PI
	dist = dist * 60 * 1.1515
	dist = dist * 1.609344

	return dist
}

func Smaller(a, b float64) bool {
	return math.Max(a, b) == b && math.Abs(a-b) > 0.00001
}
