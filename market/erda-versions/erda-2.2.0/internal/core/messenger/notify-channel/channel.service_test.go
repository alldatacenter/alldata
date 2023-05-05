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

package notify_channel

import (
	"context"
	"encoding/base64"
	"errors"
	"reflect"
	"strconv"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/jinzhu/copier"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/messenger/notifychannel/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/notify-channel/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/ucauth"
)

func Test_notifyChannelService_CreateNotifyChannel(t *testing.T) {

	type args struct {
		ctx context.Context
		req *pb.CreateNotifyChannelRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.CreateNotifyChannelRequest{Name: "", Type: "aliyun_sms", Config: nil}}, true},
		{"case2", args{req: &pb.CreateNotifyChannelRequest{Name: "test", Type: "", Config: map[string]*structpb.Value{}}}, true},
		{"case3", args{req: &pb.CreateNotifyChannelRequest{Name: "test", Type: "error", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case4", args{req: &pb.CreateNotifyChannelRequest{Name: "create_error", Type: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case5", args{req: &pb.CreateNotifyChannelRequest{Name: "test", Type: "aliyun_sms", Config: nil}}, true},
		{"case6", args{req: &pb.CreateNotifyChannelRequest{Name: "create", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var ncs *notifyChannelService
			ConfigValidate := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "ConfigValidate", func(ncs *notifyChannelService, channelType string, c map[string]*structpb.Value) (map[string]*structpb.Value, error) {
				if channelType == "error" {
					return nil, errors.New("not support")
				}
				return c, nil
			})
			defer ConfigValidate.Unpatch()
			CovertToPbNotifyChannel := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "CovertToPbNotifyChannel", func(ncs *notifyChannelService, lang i18n.LanguageCodes, channel *db.NotifyChannel, needConfig bool) *pb.NotifyChannel {
				return &pb.NotifyChannel{Name: "test", Type: &pb.NotifyChannelType{Name: "test", DisplayName: "test"}, ChannelProviderType: &pb.NotifyChannelProviderType{Name: "test", DisplayName: "test"}}
			})
			defer CovertToPbNotifyChannel.Unpatch()
			var ncdb *db.NotifyChannelDB
			GetByName := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetByName", func(ncdb *db.NotifyChannelDB, name string) (*db.NotifyChannel, error) {
				if name == "" {
					return nil, errors.New("err")
				}
				if name == "test" {
					return &db.NotifyChannel{Id: "xx", Name: name}, nil
				}
				return nil, nil
			})
			defer GetByName.Unpatch()
			Create := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "Create", func(ncdb *db.NotifyChannelDB, notifyChannel *db.NotifyChannel) (*db.NotifyChannel, error) {
				if notifyChannel.Name == "create_error" {
					return nil, errors.New("create_error")
				}
				return notifyChannel, nil
			})
			defer Create.Unpatch()
			var b *bundle.Bundle
			KMSCreateKey := monkey.PatchInstanceMethod(reflect.TypeOf(b), "KMSCreateKey", func(b *bundle.Bundle, req apistructs.KMSCreateKeyRequest) (*kmstypes.CreateKeyResponse, error) {
				return &kmstypes.CreateKeyResponse{KeyMetadata: kmstypes.KeyMetadata{KeyID: "test"}}, nil
			})
			defer KMSCreateKey.Unpatch()
			KMSEncrypt := monkey.PatchInstanceMethod(reflect.TypeOf(b), "KMSEncrypt", func(b *bundle.Bundle, req apistructs.KMSEncryptRequest) (*kmstypes.EncryptResponse, error) {
				return &kmstypes.EncryptResponse{KeyID: req.KeyID, CiphertextBase64: "test"}, nil
			})
			defer KMSEncrypt.Unpatch()
			var uc *ucauth.UCClient
			GetUser := monkey.PatchInstanceMethod(reflect.TypeOf(uc), "GetUser", func(uc *ucauth.UCClient, userID string) (*ucauth.User, error) {
				return &ucauth.User{ID: "test"}, nil
			})
			defer GetUser.Unpatch()

			GetUserID := monkey.Patch(apis.GetUserID, func(ctx context.Context) string {
				return "1"
			})
			defer GetUserID.Unpatch()
			GetOrgID := monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
				return "1"
			})
			defer GetOrgID.Unpatch()
			Language := monkey.Patch(apis.Language, func(ctx context.Context) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			defer Language.Unpatch()

			s := &notifyChannelService{
				p: &provider{bdl: bundle.New(), uc: ucauth.NewUCClient("test", "ucClientId", "ucClientSecret")},
			}
			_, err := s.CreateNotifyChannel(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("CreateNotifyChannel() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

		})
	}
}

func Test_notifyChannelService_GetNotifyChannels(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetNotifyChannelsRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.GetNotifyChannelsRequest{PageNo: 1, PageSize: 10, Type: "sms"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			GetOrgID := monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
				return "1"
			})
			defer GetOrgID.Unpatch()
			Language := monkey.Patch(apis.Language, func(ctx context.Context) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			defer Language.Unpatch()

			var base *base64.Encoding
			DecodeString := monkey.PatchInstanceMethod(reflect.TypeOf(base), "DecodeString", func(base *base64.Encoding, s string) ([]byte, error) {
				return []byte("test"), nil
			})
			defer DecodeString.Unpatch()

			var ncs *notifyChannelService
			CovertToPbNotifyChannel := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "CovertToPbNotifyChannel", func(ncs *notifyChannelService, lang i18n.LanguageCodes, channel *db.NotifyChannel, needConfig bool) *pb.NotifyChannel {
				return &pb.NotifyChannel{Type: &pb.NotifyChannelType{Name: "test"}, ChannelProviderType: &pb.NotifyChannelProviderType{Name: "test"}, Config: map[string]*structpb.Value{}}
			})
			defer CovertToPbNotifyChannel.Unpatch()
			ConfigValidate := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "ConfigValidate", func(ncs *notifyChannelService, channelType string, c map[string]*structpb.Value) (map[string]*structpb.Value, error) {
				if channelType == "error" {
					return nil, errors.New("not support")
				}
				return c, nil
			})
			defer ConfigValidate.Unpatch()

			var ncdb *db.NotifyChannelDB
			ListByPage := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "ListByPage", func(ncdb *db.NotifyChannelDB, offset, pageSize int64, scopeId, scopeType, channelType string) (int64, []db.NotifyChannel, error) {
				var channels []db.NotifyChannel
				for i := 0; i < 10; i++ {
					channels = append(channels, db.NotifyChannel{Id: strconv.Itoa(i), Name: strconv.Itoa(i), Type: strconv.Itoa(i), ChannelProvider: strconv.Itoa(i)})
				}
				return 10, channels, nil
			})
			defer ListByPage.Unpatch()
			var b *bundle.Bundle
			KMSDecrypt := monkey.PatchInstanceMethod(reflect.TypeOf(b), "KMSDecrypt", func(b *bundle.Bundle, req apistructs.KMSDecryptRequest) (*kmstypes.DecryptResponse, error) {
				return &kmstypes.DecryptResponse{PlaintextBase64: "test"}, nil
			})
			defer KMSDecrypt.Unpatch()

			s := &notifyChannelService{
				p: &provider{bdl: bundle.New(), uc: ucauth.NewUCClient("test", "ucClientId", "ucClientSecret")},
			}
			_, err := s.GetNotifyChannels(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyChannels() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_UpdateNotifyChannel(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.UpdateNotifyChannelRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.UpdateNotifyChannelRequest{Name: "create_error", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case2", args{req: &pb.UpdateNotifyChannelRequest{Id: "error", Name: "create_error", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case3", args{req: &pb.UpdateNotifyChannelRequest{Id: "nil", Name: "create_error", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case4", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, false},
		{"case5", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "error", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case6", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "exist", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case7", args{req: &pb.UpdateNotifyChannelRequest{Id: "not_same", Name: "current", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
		{"case8", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "current", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, false},
		{"case9", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "test", Type: "", ChannelProviderType: "aliyun_sms", Config: nil}}, false},
		{"case10", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "test", Type: "sms", ChannelProviderType: "aliyun_sms", Config: nil}}, false},
		{"case11", args{req: &pb.UpdateNotifyChannelRequest{Id: "test", Name: "test", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, false},
		{"case12", args{req: &pb.UpdateNotifyChannelRequest{Id: "error", Name: "test", Type: "sms", ChannelProviderType: "aliyun_sms", Config: map[string]*structpb.Value{"AccessKeyId": structpb.NewStringValue("xx"), "AccessKeySecret": structpb.NewStringValue("xx"), "SignName": structpb.NewStringValue("xx"), "TemplateCode": structpb.NewStringValue("xx")}}}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			Language := monkey.Patch(apis.Language, func(ctx context.Context) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			defer Language.Unpatch()
			var ncdb *db.NotifyChannelDB
			GetById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetById", func(ncdb *db.NotifyChannelDB, id string) (*db.NotifyChannel, error) {
				if id == "error" {
					return nil, errors.New("error")
				}
				if id == "nil" {
					return nil, nil
				}
				return &db.NotifyChannel{Id: id}, nil
			})
			defer GetById.Unpatch()
			UpdateById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "UpdateById", func(ncdb *db.NotifyChannelDB, notifyChannel *db.NotifyChannel) (*db.NotifyChannel, error) {
				if notifyChannel.Id == "error" {
					return nil, errors.New("error")
				}
				return notifyChannel, nil
			})
			defer UpdateById.Unpatch()
			GetCountByName := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetCountByName", func(ncdb *db.NotifyChannelDB, name string) (int64, error) {
				if name == "error" {
					return 0, errors.New("error")
				}
				if name == "exist" {
					return 2, nil
				}
				if name == "current" {
					return 1, nil
				}
				return 0, nil
			})
			defer GetCountByName.Unpatch()
			GetByName := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetByName", func(ncdb *db.NotifyChannelDB, name string) (*db.NotifyChannel, error) {
				return &db.NotifyChannel{Id: "test", Name: name}, nil
			})
			defer GetByName.Unpatch()

			var ncs *notifyChannelService
			CovertToPbNotifyChannel := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "CovertToPbNotifyChannel", func(ncs *notifyChannelService, lang i18n.LanguageCodes, channel *db.NotifyChannel, needConfig bool) *pb.NotifyChannel {
				return &pb.NotifyChannel{Type: &pb.NotifyChannelType{Name: "test"}, ChannelProviderType: &pb.NotifyChannelProviderType{Name: "test"}, Config: map[string]*structpb.Value{}}
			})
			defer CovertToPbNotifyChannel.Unpatch()

			var b *bundle.Bundle
			KMSEncrypt := monkey.PatchInstanceMethod(reflect.TypeOf(b), "KMSEncrypt", func(b *bundle.Bundle, req apistructs.KMSEncryptRequest) (*kmstypes.EncryptResponse, error) {
				return &kmstypes.EncryptResponse{KeyID: req.KeyID, CiphertextBase64: "test"}, nil
			})
			defer KMSEncrypt.Unpatch()

			s := &notifyChannelService{
				p: &provider{bdl: bundle.New(), uc: ucauth.NewUCClient("test", "ucClientId", "ucClientSecret")},
			}
			_, err := s.UpdateNotifyChannel(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("UpdateNotifyChannel() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_GetNotifyChannel(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetNotifyChannelRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.GetNotifyChannelRequest{Id: ""}}, true},
		{"case2", args{req: &pb.GetNotifyChannelRequest{Id: "error"}}, true},
		{"case3", args{req: &pb.GetNotifyChannelRequest{Id: "test"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			Language := monkey.Patch(apis.Language, func(ctx context.Context) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			defer Language.Unpatch()
			var ncdb *db.NotifyChannelDB
			GetById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetById", func(ncdb *db.NotifyChannelDB, id string) (*db.NotifyChannel, error) {
				if id == "error" {
					return nil, errors.New("error")
				}
				if id == "nil" {
					return nil, nil
				}
				return &db.NotifyChannel{Id: id, Config: "{\"xx\": \"xx\"}"}, nil
			})
			defer GetById.Unpatch()

			var base *base64.Encoding
			DecodeString := monkey.PatchInstanceMethod(reflect.TypeOf(base), "DecodeString", func(base *base64.Encoding, s string) ([]byte, error) {
				return []byte("test"), nil
			})
			defer DecodeString.Unpatch()
			var ncs *notifyChannelService
			CovertToPbNotifyChannel := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "CovertToPbNotifyChannel", func(ncs *notifyChannelService, lang i18n.LanguageCodes, channel *db.NotifyChannel, needConfig bool) *pb.NotifyChannel {
				return &pb.NotifyChannel{Type: &pb.NotifyChannelType{Name: "test"}, ChannelProviderType: &pb.NotifyChannelProviderType{Name: "test"}, Config: map[string]*structpb.Value{}}
			})
			defer CovertToPbNotifyChannel.Unpatch()
			ConfigValidate := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "ConfigValidate", func(ncs *notifyChannelService, channelType string, c map[string]*structpb.Value) (map[string]*structpb.Value, error) {
				if channelType == "error" {
					return nil, errors.New("not support")
				}
				return c, nil
			})
			defer ConfigValidate.Unpatch()
			var b *bundle.Bundle
			KMSDecrypt := monkey.PatchInstanceMethod(reflect.TypeOf(b), "KMSDecrypt", func(b *bundle.Bundle, req apistructs.KMSDecryptRequest) (*kmstypes.DecryptResponse, error) {
				return &kmstypes.DecryptResponse{PlaintextBase64: "test"}, nil
			})
			defer KMSDecrypt.Unpatch()
			s := &notifyChannelService{
				p: &provider{bdl: bundle.New(), uc: ucauth.NewUCClient("test", "ucClientId", "ucClientSecret")},
			}
			_, err := s.GetNotifyChannel(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyChannel() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_DeleteNotifyChannel(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.DeleteNotifyChannelRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.DeleteNotifyChannelRequest{Id: ""}}, true},
		{"case1", args{req: &pb.DeleteNotifyChannelRequest{Id: "error"}}, true},
		{"case1", args{req: &pb.DeleteNotifyChannelRequest{Id: "test"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var ncdb *db.NotifyChannelDB
			DeleteById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "DeleteById", func(ncdb *db.NotifyChannelDB, id string) (*db.NotifyChannel, error) {
				if id == "error" {
					return nil, errors.New("error")
				}
				if id == "nil" {
					return nil, nil
				}
				return &db.NotifyChannel{Id: id}, nil
			})
			defer DeleteById.Unpatch()

			s := &notifyChannelService{}
			_, err := s.DeleteNotifyChannel(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("DeleteNotifyChannel() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_ConfigValidate(t *testing.T) {
	type args struct {
		channelType string
		c           map[string]*structpb.Value
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{channelType: "aliyun_sms", c: map[string]*structpb.Value{"accessKeyId": structpb.NewStringValue("xx"), "accessKeySecret": structpb.NewStringValue("xx"), "signName": structpb.NewStringValue("xx"), "templateCode": structpb.NewStringValue("xx")}}, false},
		{"case2", args{channelType: "dingtalk", c: map[string]*structpb.Value{"agentId": structpb.NewNumberValue(33), "appKey": structpb.NewStringValue("xx"), "appSecret": structpb.NewStringValue("xx")}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &notifyChannelService{}
			_, err := s.ConfigValidate(tt.args.channelType, tt.args.c)
			if (err != nil) != tt.wantErr {
				t.Errorf("ConfigValidate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_notifyChannelService_GetNotifyChannelEnabled(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetNotifyChannelEnabledRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.GetNotifyChannelEnabledRequest{ScopeId: "", ScopeType: "", Type: "xx"}}, true},
		{"case2", args{req: &pb.GetNotifyChannelEnabledRequest{ScopeId: "xx", ScopeType: "", Type: ""}}, true},
		{"case3", args{req: &pb.GetNotifyChannelEnabledRequest{ScopeId: "error", ScopeType: "xx", Type: "xx"}}, true},
		{"case4", args{req: &pb.GetNotifyChannelEnabledRequest{ScopeId: "xx", ScopeType: "xx", Type: "xx"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			Language := monkey.Patch(apis.Language, func(ctx context.Context) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			defer Language.Unpatch()
			var ncdb *db.NotifyChannelDB
			monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetByScopeAndType", func(ncdb *db.NotifyChannelDB, scopeId, scopeType, channelType string) (*db.NotifyChannel, error) {
				if scopeId == "error" {
					return nil, errors.New("error")
				}
				return &db.NotifyChannel{Id: "test", Config: "{\"xx\": \"xx\"}"}, nil
			})
			var ncs *notifyChannelService
			CovertToPbNotifyChannel := monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "CovertToPbNotifyChannel", func(ncs *notifyChannelService, lang i18n.LanguageCodes, channel *db.NotifyChannel, needConfig bool) *pb.NotifyChannel {
				nc := pb.NotifyChannel{}
				err := copier.CopyWithOption(&nc, channel, copier.Option{IgnoreEmpty: true, DeepCopy: true})
				if err != nil {
					return nil
				}
				return &nc
			})
			defer CovertToPbNotifyChannel.Unpatch()
			monkey.PatchInstanceMethod(reflect.TypeOf(ncs), "ConfigValidate", func(ncs *notifyChannelService, channelType string, c map[string]*structpb.Value) (map[string]*structpb.Value, error) {
				if channelType == "error" {
					return nil, errors.New("not support")
				}
				return c, nil
			})
			var base *base64.Encoding
			DecodeString := monkey.PatchInstanceMethod(reflect.TypeOf(base), "DecodeString", func(base *base64.Encoding, s string) ([]byte, error) {
				return []byte("test"), nil
			})
			defer DecodeString.Unpatch()
			var b *bundle.Bundle
			KMSDecrypt := monkey.PatchInstanceMethod(reflect.TypeOf(b), "KMSDecrypt", func(b *bundle.Bundle, req apistructs.KMSDecryptRequest) (*kmstypes.DecryptResponse, error) {
				return &kmstypes.DecryptResponse{PlaintextBase64: "test"}, nil
			})
			defer KMSDecrypt.Unpatch()
			s := &notifyChannelService{
				p: &provider{bdl: bundle.New(), uc: ucauth.NewUCClient("test", "ucClientId", "ucClientSecret")},
			}
			_, err := s.GetNotifyChannelEnabled(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyChannelEnabled() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_UpdateNotifyChannelEnabled(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.UpdateNotifyChannelEnabledRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: ""}}, true},
		{"case2", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: "error", Enable: true}}, true},
		{"case3", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: "nil", Enable: false}}, true},
		{"case4", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: "type_error", Enable: true}}, true},
		{"case5", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: "type_exist", Enable: true}}, false},
		{"case6", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: "update_error", Enable: true}}, false},
		{"case7", args{req: &pb.UpdateNotifyChannelEnabledRequest{Id: "test", Enable: false}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			Language := monkey.Patch(apis.Language, func(ctx context.Context) i18n.LanguageCodes {
				return i18n.LanguageCodes{{Code: "zh"}}
			})
			defer Language.Unpatch()
			var ncdb *db.NotifyChannelDB
			GetById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetById", func(ncdb *db.NotifyChannelDB, id string) (*db.NotifyChannel, error) {
				if id == "error" {
					return nil, errors.New("error")
				}
				if id == "nil" {
					return nil, nil
				}
				if id == "type_error" {
					return &db.NotifyChannel{ScopeId: "test", ScopeType: "test", Type: "type_error", IsEnabled: false}, nil
				}
				if id == "type_exist" {
					return &db.NotifyChannel{Id: "type_exist", ScopeId: "test", ScopeType: "test", Type: "type_exist", IsEnabled: false}, nil
				}

				if id == "update_error" {
					return &db.NotifyChannel{ScopeId: "test", ScopeType: "test", Type: "update_error", IsEnabled: false}, nil
				}
				return &db.NotifyChannel{ScopeId: "test", ScopeType: "test", Type: "test", IsEnabled: false}, nil
			})
			defer GetById.Unpatch()

			GetByScopeAndType := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetByScopeAndType", func(ncdb *db.NotifyChannelDB, scopeId, scopeType, channelType string) (*db.NotifyChannel, error) {
				if channelType == "type_error" {
					return nil, errors.New("error")
				}
				if channelType == "type_exist" {
					return &db.NotifyChannel{Id: "type_exist"}, nil
				}
				return &db.NotifyChannel{Id: channelType}, nil
			})
			defer GetByScopeAndType.Unpatch()
			SwitchEnable := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "SwitchEnable", func(ncdb *db.NotifyChannelDB, currentNotifyChannel, switchNotifyChannel *db.NotifyChannel) error {
				if currentNotifyChannel.Type == "type_error" {
					return errors.New("error")
				}
				return nil
			})
			defer SwitchEnable.Unpatch()

			mt := &MockTran{}
			Text := monkey.PatchInstanceMethod(reflect.TypeOf(mt), "Text", func(mt *MockTran, lang i18n.LanguageCodes, key string) string {
				return ""
			})
			defer Text.Unpatch()

			UpdateById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "UpdateById", func(ncdb *db.NotifyChannelDB, notifyChannel *db.NotifyChannel) (*db.NotifyChannel, error) {
				if notifyChannel.Type == "update_error" {
					return nil, errors.New("error")
				}
				return notifyChannel, nil
			})
			defer UpdateById.Unpatch()

			s := &notifyChannelService{
				p: &provider{I18n: &MockTran{}},
			}
			_, err := s.UpdateNotifyChannelEnabled(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("UpdateNotifyChannelEnabled() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func Test_notifyChannelService_GetNotifyChannelEnabledStatus(t *testing.T) {

	type args struct {
		ctx context.Context
		req *pb.GetNotifyChannelEnabledStatusRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.GetNotifyChannelEnabledStatusRequest{}}, true},
		{"case2", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "error"}}, true},
		{"case3", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "nil"}}, true},
		{"case4", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "hasEnabled"}}, true},
		{"case5", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "scopeError"}}, true},
		{"case5", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "enableNil"}}, false},
		{"case6", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "test"}}, false},
		{"case7", args{req: &pb.GetNotifyChannelEnabledStatusRequest{Id: "testNotSame"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var ncdb *db.NotifyChannelDB
			GetById := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetById", func(ncdb *db.NotifyChannelDB, id string) (*db.NotifyChannel, error) {
				if id == "error" {
					return nil, errors.New("error")
				}
				if id == "nil" {
					return nil, nil
				}
				if id == "hasEnabled" {
					return &db.NotifyChannel{Id: id, IsEnabled: true}, nil
				}
				if id == "scopeError" {
					return &db.NotifyChannel{Id: id, ScopeId: "error"}, nil
				}
				if id == "enableNil" {
					return &db.NotifyChannel{Id: id, ScopeId: "enableNil"}, nil
				}
				if id == "testNotSame" {
					return &db.NotifyChannel{Id: "testNotSame"}, nil
				}
				return &db.NotifyChannel{Id: id}, nil
			})
			defer GetById.Unpatch()
			GetByScopeAndType := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "GetByScopeAndType", func(ncdb *db.NotifyChannelDB, scopeId, scopeType, channelType string) (*db.NotifyChannel, error) {
				if scopeId == "error" {
					return nil, errors.New("error")
				}
				if scopeId == "enableNil" {
					return nil, nil
				}
				return &db.NotifyChannel{Id: "test", Config: "{\"xx\": \"xx\"}"}, nil
			})
			defer GetByScopeAndType.Unpatch()

			s := &notifyChannelService{}
			_, err := s.GetNotifyChannelEnabledStatus(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyChannelEnabledStatus() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_GetNotifyChannelsEnabled(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetNotifyChannelsEnabledRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "case1",
			args: args{
				req: &pb.GetNotifyChannelsEnabledRequest{},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			GetOrgID := monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
				return "1"
			})
			defer GetOrgID.Unpatch()
			var ncdb *db.NotifyChannelDB
			EnabledChannelList := monkey.PatchInstanceMethod(reflect.TypeOf(ncdb), "EnabledChannelList", func(ncdb *db.NotifyChannelDB, scopeId, scopeType string) ([]db.NotifyChannel, error) {
				return []db.NotifyChannel{
					{
						Id:              "21",
						Name:            "ss",
						Type:            "sms",
						Config:          "ss",
						ScopeType:       "ss",
						ScopeId:         "2",
						CreatorId:       "23",
						ChannelProvider: "sdds",
						IsEnabled:       false,
						KmsKey:          "saa",
						CreatedAt:       time.Time{},
						UpdatedAt:       time.Time{},
						IsDeleted:       false,
					},
				}, nil
			})
			defer EnabledChannelList.Unpatch()

			GetUserID := monkey.Patch(apis.GetUserID, func(ctx context.Context) string {
				return "1"
			})
			defer GetUserID.Unpatch()
			var bund *bundle.Bundle
			GetNotifyConfigMS := monkey.PatchInstanceMethod(reflect.TypeOf(bund), "GetNotifyConfigMS", func(bdl *bundle.Bundle, userId, orgId string) (bool, error) {
				return true, nil
			})
			defer GetNotifyConfigMS.Unpatch()
			pro := provider{
				Cfg:                 nil,
				Log:                 nil,
				Register:            nil,
				uc:                  nil,
				notifyChanelService: nil,
				bdl:                 &bundle.Bundle{},
				DB:                  nil,
				I18n:                nil,
			}
			s := &notifyChannelService{
				p: &pro,
			}
			_, err := s.GetNotifyChannelsEnabled(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyChannelsEnabled() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyChannelService_CovertToPbNotifyChannel(t *testing.T) {
	type args struct {
		lang       i18n.LanguageCodes
		channel    *db.NotifyChannel
		needConfig bool
	}
	tests := []struct {
		name string
		args args
		want *pb.NotifyChannel
	}{
		{
			name: "case1",
			args: args{channel: &db.NotifyChannel{Id: "case", Name: "case", Type: "case"}},
			want: &pb.NotifyChannel{
				Id:                  "case",
				Name:                "case",
				Type:                &pb.NotifyChannelType{Name: "case"},
				ChannelProviderType: &pb.NotifyChannelProviderType{},
				CreatorName:         "test_nick",
				CreateAt:            "0001-01-01 00:00:00",
				UpdateAt:            "0001-01-01 00:00:00",
			}},
		{
			name: "case2",
			args: args{channel: &db.NotifyChannel{Id: "case", Name: "case", Type: "case", ChannelProvider: "case"}},
			want: &pb.NotifyChannel{
				Id:                  "case",
				Name:                "case",
				Type:                &pb.NotifyChannelType{Name: "case"},
				ChannelProviderType: &pb.NotifyChannelProviderType{Name: "case"},
				CreatorName:         "test_nick",
				CreateAt:            "0001-01-01 00:00:00",
				UpdateAt:            "0001-01-01 00:00:00",
			}},
		{
			name: "case3",
			args: args{channel: &db.NotifyChannel{Id: "case", Name: "case", CreatorId: "not_nick"}},
			want: &pb.NotifyChannel{
				Id:                  "case",
				Name:                "case",
				CreatorName:         "test",
				Type:                &pb.NotifyChannelType{},
				ChannelProviderType: &pb.NotifyChannelProviderType{},
				CreateAt:            "0001-01-01 00:00:00",
				UpdateAt:            "0001-01-01 00:00:00",
			}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var uc *ucauth.UCClient
			GetUser := monkey.PatchInstanceMethod(reflect.TypeOf(uc), "GetUser", func(uc *ucauth.UCClient, userID string) (*ucauth.User, error) {
				if userID == "not_nick" {
					return &ucauth.User{Name: "test"}, nil
				}
				return &ucauth.User{Name: "test", Nick: "test_nick"}, nil
			})
			defer GetUser.Unpatch()
			s := &notifyChannelService{
				p: &provider{I18n: &MockTran{}, uc: &ucauth.UCClient{}},
			}
			got := s.CovertToPbNotifyChannel(tt.args.lang, tt.args.channel, tt.args.needConfig)
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("CovertToPbNotifyChannel() = %v, want %v", got, tt.want)
			}
		})
	}
}
