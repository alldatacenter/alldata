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

// Package conf 定义配置选项
package conf

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"path/filepath"
	"strings"

	"github.com/c2h5oh/datasize"
	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/envconf"
	"github.com/erda-project/erda/pkg/strutil"
)

// Conf 定义基于环境变量的配置项
type Conf struct {
	LocalMode             bool   `env:"LOCAL_MODE" default:"false"`
	Debug                 bool   `env:"DEBUG" default:"false"`
	ListenAddr            string `env:"LISTEN_ADDR" default:":9526"`
	MySQLHost             string `env:"MYSQL_HOST"`
	MySQLPort             string `env:"MYSQL_PORT"`
	MySQLUsername         string `env:"MYSQL_USERNAME"`
	MySQLPassword         string `env:"MYSQL_PASSWORD"`
	MySQLDatabase         string `env:"MYSQL_DATABASE"`
	MySQLLoc              string `env:"MYSQL_LOC" default:"Local"`
	GittarOutterURL       string `env:"GITTAR_PUBLIC_URL"`
	UCClientID            string `env:"UC_CLIENT_ID"`
	UCClientSecret        string `env:"UC_CLIENT_SECRET"`
	RootDomain            string `env:"DICE_ROOT_DOMAIN"`
	UIPublicURL           string `env:"UI_PUBLIC_URL"`
	UIDomain              string `env:"UI_PUBLIC_ADDR"`
	OpenAPIDomain         string `env:"OPENAPI_PUBLIC_ADDR"` // Deprecated: after cli refactored
	AvatarStorageURL      string `env:"AVATAR_STORAGE_URL"`  // file:///avatars or oss://appkey:appsecret@endpoint/bucket
	LicenseKey            string `env:"LICENSE_KEY"`
	RedisMasterName       string `default:"my-master" env:"REDIS_MASTER_NAME"`
	RedisSentinelAddrs    string `default:"" env:"REDIS_SENTINELS_ADDR"`
	RedisAddr             string `default:"127.0.0.1:6379" env:"REDIS_ADDR"`
	RedisPwd              string `default:"anywhere" env:"REDIS_PASSWORD"`
	ProjectStatsCacheCron string `env:"PROJECT_STATS_CACHE_CRON" default:"0 0 1 * * ?"`
	EnableProjectNS       bool   `env:"ENABLE_PROJECT_NS" default:"true"`
	LegacyUIDomain        string `env:"LEGACY_UI_PUBLIC_ADDR"`

	// subscribe config
	SubscribeLimitNum uint64 `env:"SUBSCRIBE_LIMIT_NUM" default:"6"`

	// ory/kratos config
	OryEnabled           bool   `default:"false" env:"ORY_ENABLED"`
	OryKratosPrivateAddr string `default:"kratos-admin" env:"ORY_KRATOS_ADMIN_ADDR"`

	// Allow people who are not admin to create org
	CreateOrgEnabled bool `default:"false" env:"CREATE_ORG_ENABLED"`

	// --- 文件管理 begin ---
	FileMaxUploadSizeStr string `env:"FILE_MAX_UPLOAD_SIZE" default:"300MB"` // 文件上传限制大小，默认 300MB
	FileMaxUploadSize    datasize.ByteSize
	// the size of the file parts stored in memory, the default value 32M refer to https://github.com/golang/go/blob/5c489514bc5e61ad9b5b07bd7d8ec65d66a0512a/src/net/http/request.go
	FileMaxMemorySizeStr string `env:"FILE_MAX_MEMORY_SIZE" default:"32MB"`
	FileMaxMemorySize    datasize.ByteSize

	// disable file download permission validate temporarily for multi-domain
	DisableFileDownloadPermissionValidate bool `env:"DISABLE_FILE_DOWNLOAD_PERMISSION_VALIDATE" default:"false"`

	// fs
	// 修改该值的话，注意同步修改 dice.yml 中 '<%$.Storage.MountPoint%>/dice/cmdb/files:/files:rw' 容器内挂载点的值
	StorageMountPointInContainer string `env:"STORAGE_MOUNT_POINT_IN_CONTAINER" default:"/files"`

	// oss
	OSSEndpoint     string `env:"OSS_ENDPOINT"`
	OSSAccessID     string `env:"OSS_ACCESS_ID"`
	OSSAccessSecret string `env:"OSS_ACCESS_SECRET"`
	OSSBucket       string `env:"OSS_BUCKET"`
	OSSPathPrefix   string `env:"OSS_PATH_PREFIX" default:"/dice/cmdb/files"`

	// If we allow uploaded file types that can carry active content
	FileTypeCarryActiveContentAllowed bool `env:"FILETYPE_CARRY_ACTIVE_CONTENT_ALLOWED" default:"false"`
	// File types can carry active content, separated by comma, can add more types like jsp
	FileTypesCanCarryActiveContent string `env:"FILETYPES_CAN_CARRY_ACTIVE_CONTENT" default:"html,js,xml,htm"`
	// --- 文件管理 end ---

	// audit
	AuditCleanCron           string `env:"AUDIT_CLEAN_CRON" default:"0 0 3 * * ?"`     // audit soft delete cron
	AuditArchiveCron         string `env:"AUDIT_ARCHIVE_CRON" default:"0 0 4 * * ?"`   // audit archive cron
	SysAuditCleanInterval    int    `env:"SYS_AUDIT_CLEAN_INTERVAL" default:"-7"`      // sys audit clean interval
	OrgAuditMaxRetentionDays uint64 `env:"ORG_AUDIT_MAX_RETENTION_DAYS" default:"180"` // org level audit max retention days

	// erda-configs
	ErdaConfigsBasePath string `env:"ERDA_CONFIGS_BASE_PATH" default:"common-conf/erda-configs"`
}

var (
	cfg Conf
	// 存储权限配置
	permissions []model.RolePermission
	// 审计模版配置
	auditsTemplate apistructs.AuditTemplateMap
	// 域名白名单
	OrgWhiteList map[string]bool
	// legacy redirect paths
	RedirectPathList map[string]bool
)

func initPermissions() {
	permissions = getAllFiles(filepath.Join(cfg.ErdaConfigsBasePath, "permission"), permissions)
}
func initAuditTemplate() {
	auditsTemplate = genTempFromFiles(filepath.Join(cfg.ErdaConfigsBasePath, "audit/template.json"))
}

func genTempFromFiles(fileName string) apistructs.AuditTemplateMap {
	templateJSON, err := ioutil.ReadFile(fileName)
	if err != nil {
		panic(err)
	}

	var result apistructs.AuditTemplateMap
	if err := json.Unmarshal(templateJSON, &result); err != nil {
		panic(err)
	}

	for _, v := range result {
		v.ConvertContent2GoTemplateFormart()
	}

	return result
}

func getAllFiles(pathname string, perms []model.RolePermission) []model.RolePermission {
	rd, err := ioutil.ReadDir(pathname)
	if err != nil {
		panic(err)
	}
	for _, fi := range rd {
		if fi.IsDir() {
			fullDir := pathname + "/" + fi.Name()
			perms = getAllFiles(fullDir, perms)
		} else {
			fullName := pathname + "/" + fi.Name()
			yamlFile, err := ioutil.ReadFile(fullName)
			if err != nil {
				panic(err)
			}
			var per []model.RolePermission
			if err := yaml.Unmarshal(yamlFile, &per); err != nil {
				panic(err)
			}
			perms = append(perms, per...)
		}
	}
	return perms
}

// LoadForTest unit test
func LoadForTest() {
	envconf.MustLoad(&cfg)
}

// Load 加载配置项.
func Load() {
	envconf.MustLoad(&cfg)

	initPermissions()
	initAuditTemplate()

	// parse FileMaxUploadSize
	var fileMaxUploadByte datasize.ByteSize
	if err := fileMaxUploadByte.UnmarshalText([]byte(cfg.FileMaxUploadSizeStr)); err != nil {
		panic(fmt.Sprintf("failed to parse FILE_MAX_UPLOAD_SIZE, err: %v", err))
	}
	fmt.Println(fileMaxUploadByte.String())
	cfg.FileMaxUploadSize = fileMaxUploadByte

	// parse FileMaxMemorySize
	var fileMaxMemoryByte datasize.ByteSize
	if err := fileMaxMemoryByte.UnmarshalText([]byte(cfg.FileMaxMemorySizeStr)); err != nil {
		panic(fmt.Sprintf("failed to parse FILE_MAX_MEMORY_SIZE, err: %v", err))
	}
	cfg.FileMaxMemorySize = fileMaxMemoryByte

	OrgWhiteList = map[string]bool{
		UIDomain():                          true,
		LegacyUIDomain():                    true,
		OpenAPIDomain():                     true,
		"openapi.default.svc.cluster.local": true,
	}

	RedirectPathList = map[string]bool{
		"/microService": true,
		"/workBench":    true,
		"/dataCenter":   true,
		"/orgCenter":    true,
		"/edge":         true,
		"/sysAdmin":     true,
		"/org-list":     true,
		"/noAuth":       true,
		"/freshMan":     true,
		"/inviteToOrg":  true,
		"/perm":         true,
	}
}

// AuditTemplate 返回权限列表
func AuditTemplate() apistructs.AuditTemplateMap {
	return auditsTemplate
}

// Permissions 获取权限配置
func Permissions() map[string]model.RolePermission {
	pm := make(map[string]model.RolePermission, len(permissions))
	for _, v := range permissions {
		k := strutil.Concat(v.Scope, v.Resource, v.Action)
		pm[k] = v
	}
	return pm
}

// RolePermissions 获取角色对应的权限配置
func RolePermissions(roles []string) (map[string]model.RolePermission, []model.RolePermission) {
	pm := make(map[string]model.RolePermission, len(permissions))
	resourceRoles := make([]model.RolePermission, 0)
	for _, v := range permissions {
		for _, role := range roles {
			confRoles := strings.SplitN(v.Role, ",", -1)
			for _, cR := range confRoles {
				if role == cR {
					k := strutil.Concat(v.Scope, v.Resource, v.Action)
					pm[k] = v
				}
			}

			if v.ResourceRole != "" {
				resourceRoles = append(resourceRoles, v)
			}
		}
	}
	return pm, resourceRoles
}

// ListenAddr 返回 ListenAddr 选项.
func ListenAddr() string {
	return cfg.ListenAddr
}

// LocalMode 本地调试模式
func LocalMode() bool {
	return cfg.LocalMode
}

// Debug 返回 Debug 选项.
func Debug() bool {
	return cfg.Debug
}

// MySQLHost 返回 MySQLHost 选项.
func MySQLHost() string {
	return cfg.MySQLHost
}

// MySQLPort 返回 MySQLPort 选项.
func MySQLPort() string {
	return cfg.MySQLPort
}

// MySQLUsername 返回 MySQLUsername 选项.
func MySQLUsername() string {
	return cfg.MySQLUsername
}

// MySQLPassword 返回 MySQLPassword 选项.
func MySQLPassword() string {
	return cfg.MySQLPassword
}

// MySQLDatabase 返回 MySQLDatabase 选项.
func MySQLDatabase() string {
	return cfg.MySQLDatabase
}

// MySQLLoc 返回 MySQLLoc 选项.
func MySQLLoc() string {
	return cfg.MySQLLoc
}

// GittarOutterURL 返回 GittarOutterURL 选项.
func GittarOutterURL() string {
	return cfg.GittarOutterURL
}

// UCClientID 返回 UCClientID 选项.
func UCClientID() string {
	return cfg.UCClientID
}

// UCClientSecret 返回 UCClientSecret 选项.
func UCClientSecret() string {
	return cfg.UCClientSecret
}

// RootDomain 返回 RootDomain 选项
func RootDomain() string {
	return RootDomainList()[0]
}

// Multiple domain
func RootDomainList() []string {
	return strutil.Split(cfg.RootDomain, ",")
}

// UIPublicURL 返回 UIPublicURL 选项
func UIPublicURL() string {
	return cfg.UIPublicURL
}

// UIDomain 返回 UIDomain 选项
func UIDomain() string {
	return cfg.UIDomain
}

// LegacyUIDomain
func LegacyUIDomain() string {
	return cfg.LegacyUIDomain
}

// OpenAPIDomain 返回 OpenAPIDomain 选项
func OpenAPIDomain() string {
	return cfg.OpenAPIDomain
}

// AvatarStorageURL 返回 OSSUsage 选项
func AvatarStorageURL() string {
	return cfg.AvatarStorageURL
}

// LicenseKey 返回 LicenseKey 选项.
func LicenseKey() string {
	return cfg.LicenseKey
}

// AuditCleanCron 返回审计事件软删除周期
func AuditCleanCron() string {
	return cfg.AuditCleanCron
}

// AuditArchiveCron 返回审计事件归档周期
func AuditArchiveCron() string {
	return cfg.AuditArchiveCron
}

// SysAuditCleanInterval 返回 sys scope 审计事件软删除周期
func SysAuditCleanInterval() int {
	return cfg.SysAuditCleanInterval
}

// RedisMasterName 返回redis master name
func RedisMasterName() string {
	return cfg.RedisMasterName
}

// RedisSentinelAddrs 返回 redis 哨兵地址
func RedisSentinelAddrs() string {
	return cfg.RedisSentinelAddrs
}

// RedisAddr 返回 redis 地址
func RedisAddr() string {
	return cfg.RedisAddr
}

// RedisPwd 返回 redis 密码
func RedisPwd() string {
	return cfg.RedisPwd
}

// ProjectStatsCacheCron 项目状态缓存刷新周期
func ProjectStatsCacheCron() string {
	return cfg.ProjectStatsCacheCron
}

// EnableNS 是否打开项目级命名空间
func EnableNS() bool {
	return cfg.EnableProjectNS
}

func OryEnabled() bool {
	return cfg.OryEnabled
}

func OryKratosPrivateAddr() string {
	return cfg.OryKratosPrivateAddr
}

func OryCompatibleClientID() string {
	return "kratos"
}

func OryCompatibleClientSecret() string {
	return ""
}

func CreateOrgEnabled() bool {
	return cfg.CreateOrgEnabled
}

// FileMaxUploadSize 返回 文件上传的大小限制.
func FileMaxUploadSize() datasize.ByteSize {
	return cfg.FileMaxUploadSize
}

// FileMaxMemorySize return the size of the file parts stored in memory
func FileMaxMemorySize() datasize.ByteSize {
	return cfg.FileMaxMemorySize
}

// OSSEndpoint 返回 oss endpoint.
func OSSEndpoint() string {
	return cfg.OSSEndpoint
}

// OSSAccessID 返回 oss access id.
func OSSAccessID() string {
	return cfg.OSSAccessID
}

// OSSAccessSecret 返回 oss access secret
func OSSAccessSecret() string {
	return cfg.OSSAccessSecret
}

// OSSBucket 返回 oss bucket.
func OSSBucket() string {
	return cfg.OSSBucket
}

// OSSPathPrefix 返回 文件在指定 bucket 下的路径前缀.
func OSSPathPrefix() string {
	return cfg.OSSPathPrefix
}

// StorageMountPointInContainer 返回 files 在容器内的挂载点.
func StorageMountPointInContainer() string {
	return cfg.StorageMountPointInContainer
}

// DisableFileDownloadPermissionValidate return switch for file download permission check.
func DisableFileDownloadPermissionValidate() bool {
	return cfg.DisableFileDownloadPermissionValidate
}

func FileTypeCarryActiveContentAllowed() bool {
	return cfg.FileTypeCarryActiveContentAllowed
}

func FileTypesCanCarryActiveContent() []string {
	return strutil.Split(cfg.FileTypesCanCarryActiveContent, ",")
}

func OrgAuditMaxRetentionDays() uint64 {
	return cfg.OrgAuditMaxRetentionDays
}

func SubscribeLimitNum() uint64 {
	return cfg.SubscribeLimitNum
}
