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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"

	"github.com/pkg/errors"

	liboss "github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/oss"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) ListOSS(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceOssResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
				Data: apistructs.CloudResourceOssData{List: []apistructs.CloudResourceOssBasicData{}},
			})
		}
	}()

	_ = ctx.Value("i18nPrinter").(*message.Printer)
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	tags := strutil.Split(r.URL.Query().Get("tags"), ",", true)
	prefix := r.URL.Query().Get("name")

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.Wrapf(err, "failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = errors.Wrapf(err, "failed to get ak of org:%s", i.OrgID)
		return
	}

	regionids := strutil.Split(r.URL.Query().Get("region"), ",", true)
	if len(regionids) == 0 {
		regionids = []string{"cn-hangzhou"}
	}
	// come from cloud resource, get from cloud online
	list, err := oss.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids, "", tags, prefix)
	if err != nil {
		err = errors.Wrapf(err, "failed to get oss list")
		return
	}
	resultList := []apistructs.CloudResourceOssBasicData{}
	regionBuckets := make(map[string][]string)
	// get bucket basic info
	for i, ins := range list {
		if _, ok := regionBuckets[ins.Location]; ok {
			regionBuckets[ins.Location] = append(regionBuckets[ins.Location], list[i].Name)
		} else {
			regionBuckets[ins.Location] = []string{list[i].Name}
		}
		resultList = append(resultList, apistructs.CloudResourceOssBasicData{
			Name:         ins.Name,
			Location:     ins.Location,
			CreateDate:   ins.CreationDate.String(),
			StorageClass: ins.StorageClass,
		})
	}
	// get bucket tags
	regionTags := make(map[string]*map[string]liboss.Tagging)
	for region, v := range regionBuckets {
		ak_ctx.Region = region
		result, err1 := oss.GetResourceTags(ak_ctx, v)
		if err1 != nil {
			err = errors.Wrapf(err, "failed to get resource tags")
			return
		}
		regionTags[region] = result
	}
	// fill bucket with tags
	for i, ins := range resultList {
		// check region key
		if _, ok := regionTags[ins.Location]; !ok {
			continue
		}
		// check region value
		if regionTags[ins.Location] == nil {
			continue
		}

		// fill  bucket with tags
		if v, ok := (*regionTags[ins.Location])[ins.Name]; ok {
			tags := map[string]string{}
			for _, tag := range v.Tags {
				if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixProject) {
					tags[tag.Key] = tag.Value
				}
			}
			resultList[i].Tags = tags
		}
	}

	resp, err = mkResponse(apistructs.ListCloudResourceOssResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.CloudResourceOssData{
			Total: len(resultList),
			List:  resultList,
		},
	})
	return
}

func (e *Endpoints) CreateOSS(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	req := apistructs.CreateCloudResourceOssRequest{
		CreateCloudResourceBaseInfo: &apistructs.CreateCloudResourceBaseInfo{},
	}
	if req.Vendor == "" {
		req.Vendor = aliyun_resources.CloudVendorAliCloud.String()
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		err := fmt.Errorf("failed to unmarshal create oss request: %v", err)
		content, _ := ioutil.ReadAll(r.Body)
		logrus.Errorf("%s, request:%v", err.Error(), content)
		return mkResponse(apistructs.CreateCloudResourceOssResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return resp, nil
	}
	// permission check
	err := e.PermissionCheck(i.UserID, i.OrgID, req.ProjectID, apistructs.CreateAction)
	if err != nil {
		return mkResponse(apistructs.CreateCloudResourceOssResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	req.UserID = i.UserID
	req.OrgID = i.OrgID

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return resp, nil
	}

	factory, err := resource_factory.GetResourceFactory(e.dbclient, dbclient.ResourceTypeOss)
	if err != nil {
		return mkResponse(apistructs.CreateCloudResourceOssResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	record, err := factory.CreateResource(ak_ctx, req)
	if err != nil {
		return mkResponse(apistructs.CreateCloudResourceOssResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	return mkResponse(apistructs.CreateCloudResourceRedisResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
}

// TODO demo delete
func (e *Endpoints) DeleteOSSResource(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	return mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
		Header: apistructs.Header{Success: true},
	})
}
