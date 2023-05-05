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
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ecs"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ons"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/oss"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/overview"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/rds"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/redis"
	libregion "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/region"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	libzone "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/zone"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/strutil"
)

// only set/unset/filter tag with dice-cluster or dice-project prefix
func (e *Endpoints) CloudResourceSetTag(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {

	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CreateCloudResourceGatewayResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()

	// decode request
	req := apistructs.CloudResourceSetTagRequest{}
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = errors.Wrapf(err, "failed to decode cloud resource set tag request:%s", r.Body)
		return
	}

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return
	}

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}

	regionItems := make(map[string][]apistructs.CloudResourceTagItem)
	for k, v := range req.Items {
		if v.Region == "" {
			logrus.Errorf("tag item with empty region, item:%+v", v)
			continue
		}
		if _, ok := regionItems[v.Region]; ok {
			regionItems[v.Region] = append(regionItems[v.Region], req.Items[k])
		} else {
			regionItems[v.Region] = []apistructs.CloudResourceTagItem{req.Items[k]}
		}
	}
	// resource type convert to upper case
	req.ResourceType = strings.ToUpper(req.ResourceType)
	for region, items := range regionItems {
		ak_ctx.Region = region
		switch req.ResourceType {
		// vpc
		case aliyun_resources.TagResourceTypeVpc.String():
			err = vpc.OverwriteTags(ak_ctx, items, req.Tags, aliyun_resources.TagResourceTypeVpc)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag vpc, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeVsw.String():
			err = vpc.OverwriteTags(ak_ctx, items, req.Tags, aliyun_resources.TagResourceTypeVsw)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag vswitch, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeRedis.String():
			err = redis.OverwriteTags(ak_ctx, items, req.Tags)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag redis, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeOss.String():
			err = oss.OverwriteTags(ak_ctx, items, req.Tags)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag oss, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeOnsInstance.String():
			err = ons.OverwriteTags(ak_ctx, items, req.Tags, aliyun_resources.TagResourceTypeOnsInstanceTag, req.InstanceID)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag ons, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeOnsTopic.String():
			err = ons.OverwriteTags(ak_ctx, items, req.Tags, aliyun_resources.TagResourceTypeOnsTopicTag, req.InstanceID)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag ons topic, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeOnsGroup.String():
			err = ons.OverwriteTags(ak_ctx, items, req.Tags, aliyun_resources.TagResourceTypeOnsGroupTag, req.InstanceID)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag ons group, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeECS.String():
			err = ecs.OverwriteTags(ak_ctx, items, req.Tags)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag redis, request:%+v", req)
				return
			}
		case aliyun_resources.TagResourceTypeRDS.String():
			err = rds.OverwriteTags(ak_ctx, items, req.Tags)
			if err != nil {
				err = errors.Wrapf(err, "failed to tag redis, request:%+v", req)
				return
			}
		default:
			err = fmt.Errorf("tag vpc related resource failed, invalide resource type: %s", req.ResourceType)
			return
		}
	}

	resp, err = mkResponse(apistructs.TagCloudResourceVPCResponse{
		Header: apistructs.Header{Success: true},
	})
	return
}

func (e *Endpoints) CloudResourceOverview(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	i18nPrinter := ctx.Value("i18nPrinter").(*message.Printer)
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)

	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return resp, nil
	}
	allResource, err := overview.GetCloudResourceOverView(ak_ctx, i18nPrinter)
	if err != nil {
		errstr := fmt.Sprintf("get cloud resource overview failed, error: %v", err)
		return mkResponse(apistructs.CreateCloudAccountResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	return mkResponse(apistructs.CloudResourceOverviewResponse{
		Header: apistructs.Header{Success: true},
		Data:   allResource,
	})
}
func (e *Endpoints) ListRegion(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return mkResponse(apistructs.ListCloudResourceRegionResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: fmt.Sprintf("failed to get ak of org: %s", orgid)},
			},
			Data: []apistructs.ListCloudResourceRegion{},
		})
	}
	regions, err := libregion.List(ak_ctx)
	if err != nil {
		errstr := fmt.Sprintf("failed to list regions: %v", err)
		return mkResponse(apistructs.ListCloudResourceRegionResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
			Data: []apistructs.ListCloudResourceRegion{},
		})
	}
	resultlist := []apistructs.ListCloudResourceRegion{}
	for _, reg := range regions {
		resultlist = append(resultlist, apistructs.ListCloudResourceRegion{
			RegionID:  reg.RegionId,
			LocalName: reg.LocalName,
		})
	}
	return mkResponse(apistructs.ListCloudResourceRegionResponse{
		Header: apistructs.Header{Success: true},
		Data:   resultlist,
	})
}

func (e *Endpoints) ListZone(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	region := r.URL.Query().Get("region")
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return mkResponse(apistructs.ListCloudResourceZoneResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: fmt.Sprintf("failed to get ak of org: %s", orgid)},
			},
			Data: []apistructs.ListCloudResourceZone{},
		})
	}
	zones, err := libzone.List(ak_ctx, []string{region})
	if err != nil {
		errstr := fmt.Sprintf("failed to list zones: %v", err)
		return mkResponse(apistructs.ListCloudResourceZoneResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
			Data: []apistructs.ListCloudResourceZone{},
		})
	}
	resultlist := []apistructs.ListCloudResourceZone{}
	for _, zone := range zones {
		resultlist = append(resultlist, apistructs.ListCloudResourceZone{
			ZoneID:    zone.ZoneId,
			LocalName: zone.LocalName,
		})
	}
	return mkResponse(apistructs.ListCloudResourceZoneResponse{
		Header: apistructs.Header{Success: true},
		Data:   resultlist,
	})
}

func (e *Endpoints) mkCtx(ctx context.Context, orgid string) (aliyun_resources.Context, httpserver.Responser) {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	akreader := e.dbclient.OrgAKReader()
	ak, err := akreader.ByVendors("aliyun").ByOrgID(orgid).Do()
	if err != nil || len(ak) == 0 {
		errstr := fmt.Sprintf(
			i18n.Sprintf("No cloud resource account is configured under the current enterprise(%s)", orgid))
		return aliyun_resources.Context{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: errstr},
					},
					Data: apistructs.ListCloudResourceECSData{List: []apistructs.ListCloudResourceECS{}},
				},
			}
	}
	key, err := e.CloudAccount.KmsKey()
	if err != nil {
		errstr := fmt.Sprintf("failed to get kms key: %v", err)
		return aliyun_resources.Context{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: errstr},
					},
				},
			}
	}
	decryptData, err := e.CloudAccount.Kmsbundle.KMSDecrypt(apistructs.KMSDecryptRequest{
		DecryptRequest: kmstypes.DecryptRequest{
			KeyID:            key.KeyMetadata.KeyID,
			CiphertextBase64: ak[0].SecretKey,
		}})
	if err != nil {
		errstr := fmt.Sprintf("failed to decrypt data: %v", err)
		return aliyun_resources.Context{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: errstr},
					},
				},
			}
	}
	rawSecret, err := base64.StdEncoding.DecodeString(decryptData.PlaintextBase64)
	if err != nil {
		errstr := fmt.Sprintf("failed to decode(base64) data: %v", err)
		return aliyun_resources.Context{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: errstr},
					},
				},
			}
	}
	var vendor string
	vendor = string(ak[0].Vendor)
	if vendor == "" || vendor == "aliyun" {
		vendor = aliyun_resources.CloudVendorAliCloud.String()
	}
	ak_ctx := aliyun_resources.Context{
		AccessKeySecret: aliyun_resources.AccessKeySecret{
			OrgID:        orgid,
			Vendor:       vendor,
			AccessKeyID:  ak[0].AccessKey,
			AccessSecret: string(rawSecret),
		},
		DB:       e.dbclient,
		Bdl:      e.bdl,
		JS:       e.JS,
		CachedJs: e.CachedJS,
	}
	return ak_ctx, nil
}

func (e *Endpoints) getAvailableRegions(ak_ctx aliyun_resources.Context, r *http.Request) aliyun_resources.CachedRegionIDs {
	regionList := strutil.Split(r.URL.Query().Get("region"), ",", true)
	regionids := aliyun_resources.CachedRegionIDs{
		ECS: regionList,
		VPC: regionList,
	}
	if len(regionList) == 0 {
		regionids = aliyun_resources.ActiveRegionIDs(ak_ctx)
	}
	return regionids
}
