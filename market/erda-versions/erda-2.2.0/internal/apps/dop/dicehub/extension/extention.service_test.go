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

package extension

import (
	"context"
	"reflect"
	"testing"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/apistructs"
)

func Test_extensionService_SearchExtensions(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ExtensionSearchRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ExtensionSearchResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.ExtensionSearchRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.ExtensionSearchResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.SearchExtensions(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.SearchExtensions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.SearchExtensions() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_extensionService_CreateExtension(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ExtensionCreateRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ExtensionCreateResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.ExtensionCreateRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.ExtensionCreateResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.CreateExtension(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.CreateExtension() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.CreateExtension() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_extensionService_QueryExtensions(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.QueryExtensionsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.QueryExtensionsResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.QueryExtensionsRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.QueryExtensionsResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.QueryExtensions(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.QueryExtensions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.QueryExtensions() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_extensionService_QueryExtensionsMenu(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.QueryExtensionsMenuRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.QueryExtensionsMenuResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.QueryExtensionsMenuRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.QueryExtensionsMenuResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.QueryExtensionsMenu(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.QueryExtensionsMenu() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.QueryExtensionsMenu() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_extensionService_CreateExtensionVersion222(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ExtensionVersionCreateRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ExtensionVersionCreateResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.ExtensionVersionCreateRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.ExtensionVersionCreateResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.CreateExtensionVersion(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.CreateExtensionVersion222() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.CreateExtensionVersion222() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_extensionService_GetExtensionVersion(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetExtensionVersionRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetExtensionVersionResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetExtensionVersionRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetExtensionVersionResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.GetExtensionVersion(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.GetExtensionVersion() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.GetExtensionVersion() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_extensionService_QueryExtensionVersions(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ExtensionVersionQueryRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ExtensionVersionQueryResponse
		wantErr  bool
	}{
		//		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.dicehub.extension.ExtensionService",
		//			`
		//erda.core.dicehub.extension:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.ExtensionVersionQueryRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.ExtensionVersionQueryResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.ExtensionServiceServer)
			got, err := srv.QueryExtensionVersions(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("extensionService.QueryExtensionVersions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("extensionService.QueryExtensionVersions() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func TestConvertFieldI18n(t *testing.T) {
	var key = "${{ i18n.displayName }}"
	var localeMap = map[string]map[string]string{
		"zh-CN": {
			"desc":                           "Erda MySQL Migration 工具",
			"displayName":                    "Erda MySQL 数据迁移",
			"outputs.success.desc":           "是否成功",
			"params.database.desc":           "要执行 migration 的库名",
			"params.lint_config.desc":        "Erda MySQL Migration Lint 的配置文件",
			"params.migrationdir.desc":       "脚本存放目录",
			"params.modules.desc":            "要进行数据迁移的模块, 为空时对 migrationDir 目录下的所有模块进行数据迁移",
			"params.mysql_host.desc":         "mysql 服务地址",
			"params.mysql_password.desc":     "mysql 密码",
			"params.mysql_port.desc":         "mysql 服务端口",
			"params.mysql_username.desc":     "mysql 用户名",
			"params.retry_timeout.desc":      "连接数据库最长超时时间",
			"params.skip_lint.desc":          "跳过 Erda MySQL 规约检查. 注意标明 \"MIGRATION_BASE\" 的脚本总是不会被检查.",
			"params.skip_migration.desc":     "跳过预执行和正式执行",
			"params.skip_pre_migration.desc": "跳过预执行",
			"params.skip_sandbox.desc":       "跳过沙盒预演",
			"params.workdir.desc":            "工作目录, 如 ${git-checkout}",
		},
		"en-US": {
			"desc":                           "Erda MySQL Migration tool",
			"displayName":                    "Erda MySQL data migration",
			"outputs.success.desc":           "whether succeed",
			"params.database.desc":           "The name of the library to execute migration",
			"params.lint_config.desc":        "Configuration file of Erda MySQL migration lint",
			"params.migrationdir.desc":       "Script directory",
			"params.modules.desc":            "The module to be migrated, or all modules in the migrationDir directory if empty.",
			"params.mysql_host.desc":         "Mysql host",
			"params.mysql_password.desc":     "Mysql password",
			"params.mysql_port.desc":         "Mysql port",
			"params.mysql_username.desc":     "Mysql user name",
			"params.retry_timeout.desc":      "The maximum timeout for connecting to the database",
			"params.skip_lint.desc":          "Skip Erda MySQL protocol check. Note that scripts marked \"MIGRATION_BASE\" will not be checked.",
			"params.skip_migration.desc":     "Skip pre-execution and formal execution",
			"params.skip_pre_migration.desc": "Skip pre-execution",
			"params.skip_sandbox.desc":       "Skip the sandbox preview",
			"params.workdir.desc":            "Working directory, such as ${git-checkout}",
		},
	}
	data := ConvertFieldI18n(key, localeMap)
	t.Log(data)
}

func TestIsExtensionPublic(t *testing.T) {
	tests := []struct {
		name string
		spec *apistructs.Spec
		want bool
	}{
		{
			name: "public extension",
			spec: &apistructs.Spec{Public: true},
			want: true,
		},
		{
			name: "private extension",
			spec: &apistructs.Spec{Public: false},
			want: false,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			res := isExtensionPublic(test.spec)
			if res != test.want {
				t.Errorf("got %t, want %t", res, test.want)
			}
		})
	}
}
