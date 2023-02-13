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

package conf

import (
	"strings"

	"github.com/erda-project/erda/pkg/envconf"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
)

// Conf define envs
type Conf struct {
	Debug          bool   `env:"DEBUG" default:"false"`
	ListenAddr     string `env:"LISTEN_ADDR" default:":9527"`
	UCClientID     string `default:"dice" env:"UC_CLIENT_ID"`
	UCClientSecret string `default:"secret" env:"UC_CLIENT_SECRET"`
	WildDomain     string `default:"dev.terminus.io" env:"DICE_ROOT_DOMAIN"`

	MonitorAddr      string `env:"MONITOR_ADDR"`
	GittarAddr       string `env:"GITTAR_ADDR"`
	BundleTimeoutSec int    `env:"BUNDLE_TIMEOUT_SECOND" default:"30"`

	ConsumerNum       int    `env:"CONSUMER_NUM" default:"5"`
	DiceClusterName   string `env:"DICE_CLUSTER_NAME" required:"true"`
	EventboxAddr      string `env:"EVENTBOX_ADDR"`
	CMDBAddr          string `env:"CMDB_ADDR"`
	PipelineAddr      string `env:"PIPELINE_ADDR"`
	NexusAddr         string `env:"NEXUS_ADDR" required:"true"`
	NexusUsername     string `env:"NEXUS_USERNAME" required:"false"`
	NexusPassword     string `env:"NEXUS_PASSWORD" required:"false"`
	SonarAddr         string `env:"SONAR_ADDR" required:"true"`
	SonarPublicURL    string `env:"SONAR_PUBLIC_URL" required:"true"`
	SonarAdminToken   string `env:"SONAR_ADMIN_TOKEN" required:"true"` // dice.yml 里依赖了 sonar，由工具链注入 SONAR_ADMIN_TOKEN
	GolangCILintImage string `env:"GOLANGCI_LINT_IMAGE" default:"registry.cn-hangzhou.aliyuncs.com/terminus/terminus-golangci-lint:1.27"`
	UIPublicURL       string `env:"UI_PUBLIC_URL" required:"true"`
	UIDomain          string `env:"UI_PUBLIC_ADDR" required:"true"`

	// ory/kratos config
	OryEnabled           bool   `default:"false" env:"ORY_ENABLED"`
	OryKratosPrivateAddr string `default:"kratos-admin" env:"ORY_KRATOS_ADMIN_ADDR"`

	CentralNexusPublicURL string `env:"NEXUS_PUBLIC_URL" required:"true"`
	CentralNexusAddr      string `env:"NEXUS_ADDR" required:"true"`
	CentralNexusUsername  string `env:"NEXUS_USERNAME" required:"false"`
	CentralNexusPassword  string `env:"NEXUS_PASSWORD" required:"false"`

	// rsa
	Base64EncodedRsaPublicKey  string `env:"BASE64_ENCODED_RSA_PUBLIC_KEY" default:"LS0tLS1CRUdJTiBwdWJsaWMga2V5LS0tLS0KTUlJQ0lqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FnOEFNSUlDQ2dLQ0FnRUFrOCtVK3QyeHhoM1hpREJnRjM2dApxWU5UZmN2NDA4aTdsZnFZRG9TRHMxbDA5bitsLzFOZTQ5b0xxZ0h1ZTQ5MmJHNFI0T0ZHZW1IMktIZmUya3BnCjZpd2tFM0xrZW5KMm56NFdPQWNnOUhiWlA0TFpReGxoeUVwNlE2aHQyekgxZ25Uc2p0QUlzMEZxbXJXZmlVVkQKdFdib1lmSDMvNWZReSs3V00yWkU3bzdnWWxIM1RLR2M5amEvWmgwOTBUZXdULzV3TVhPb1llcFRsWVBmTDVoTwo0em9GeGFpbzltanhpQmVveDNrUkM5RlZsSFM4ZDVlYWRHNkttR2cydjlTaE96SThDaGErRkJHSm83b3E4UEZEClRFMUFuZnBjZml5ckVxVVpzbDZTckl1TjVZUTREM3h1clZnY1RkcG9MV1dpallJbVZ0bytJU3FScW9QemxqVWQKTzdDa2NVRXUvVno2UCt2Vjc4b1JWRktYM0E0aG9vYlFFSkphNlFISmlzN1JQRW5TTjZXS2k4RXkzSlFhT3hXWAppejR3aDk3VmIyZDU4c3l1M0pJSTFOWVlyemtqTitEd1RLV1dqcjVYaVhHSGVCRDFtMmpaMytxV1RCTW1oNC9QCmtWc2M0T29lOG40ZXFoYVc1d2QyaU5jUlRHUS9sUmY4ekNSRlhCN1lvbWJrVlQwc1hVcllXQWFkWURFUEFmazUKTncvUjJaTXkyNGVhd0ZCcTVmYVB6VVJWRUY4WC9uUm5kL1YwUFZBSGgySG9CeFJaZzFkSGJrSWQ3SUo5R2cxbwpKVzJZOTlobzRpK0QvTDl2cWNPOVRyOXN0dStWcG1UQ1BRdFZqWHlpY0FuZmN4MWxhOEI0Q2Y4azhWN1RBSmJWCm14SjdaUTJEbGs3TTdBYzNTamVEUmJrQ0F3RUFBUT09Ci0tLS0tRU5EIHB1YmxpYyBrZXktLS0tLQo="`
	Base64EncodedRsaPrivateKey string `env:"BASE64_ENCODED_RSA_PRIVATE_KEY" default:"LS0tLS1CRUdJTiBwcml2YXRlIGtleS0tLS0tCk1JSUpLUUlCQUFLQ0FnRUFrOCtVK3QyeHhoM1hpREJnRjM2dHFZTlRmY3Y0MDhpN2xmcVlEb1NEczFsMDluK2wKLzFOZTQ5b0xxZ0h1ZTQ5MmJHNFI0T0ZHZW1IMktIZmUya3BnNml3a0UzTGtlbkoybno0V09BY2c5SGJaUDRMWgpReGxoeUVwNlE2aHQyekgxZ25Uc2p0QUlzMEZxbXJXZmlVVkR0V2JvWWZIMy81ZlF5KzdXTTJaRTdvN2dZbEgzClRLR2M5amEvWmgwOTBUZXdULzV3TVhPb1llcFRsWVBmTDVoTzR6b0Z4YWlvOW1qeGlCZW94M2tSQzlGVmxIUzgKZDVlYWRHNkttR2cydjlTaE96SThDaGErRkJHSm83b3E4UEZEVEUxQW5mcGNmaXlyRXFVWnNsNlNySXVONVlRNApEM3h1clZnY1RkcG9MV1dpallJbVZ0bytJU3FScW9QemxqVWRPN0NrY1VFdS9WejZQK3ZWNzhvUlZGS1gzQTRoCm9vYlFFSkphNlFISmlzN1JQRW5TTjZXS2k4RXkzSlFhT3hXWGl6NHdoOTdWYjJkNThzeXUzSklJMU5ZWXJ6a2oKTitEd1RLV1dqcjVYaVhHSGVCRDFtMmpaMytxV1RCTW1oNC9Qa1ZzYzRPb2U4bjRlcWhhVzV3ZDJpTmNSVEdRLwpsUmY4ekNSRlhCN1lvbWJrVlQwc1hVcllXQWFkWURFUEFmazVOdy9SMlpNeTI0ZWF3RkJxNWZhUHpVUlZFRjhYCi9uUm5kL1YwUFZBSGgySG9CeFJaZzFkSGJrSWQ3SUo5R2cxb0pXMlk5OWhvNGkrRC9MOXZxY085VHI5c3R1K1YKcG1UQ1BRdFZqWHlpY0FuZmN4MWxhOEI0Q2Y4azhWN1RBSmJWbXhKN1pRMkRsazdNN0FjM1NqZURSYmtDQXdFQQpBUUtDQWdCSlhxbngyS2ZNMHJWUTJjcG8veTJPeml4Y2Jpb21YaWFYTE52Ym9QV0t5aVhmMGI4QlBVNEZ4Zzh5CkpXRk9uZ2pIaTk5K0EvU3EvUU5tVlJJZXd2cldZbkRKNHFiOURPSks2MU8ySGZ2Q3ZWZmJTY1UwcEYzQVFRL3QKazZac1BxRkNUMjI0K2hUSGZmby9yMVh3bXB3Z2FHT0Rjc3VLYUw1dzdDNFJOM3VSK3dQd2FnVmFXWUtEU091Ngo4VnJsQmtLVGdwWUlSZ1BZRHF2TXRMZk5kVW43U3FyZzBYYUZVZFJLbkl2ZjcvMkJJempheHhOaVBiT2loZGh3CkRKTFlwK0FjZFRRT1FmbTZGblorK2dNa3RHMldhMlpleEk2eTV0TklId0hoWTBabE5hU0t3Qlhmd2dGaU5ERmcKaDhCY2dHMnUxbUxYaTk5NU1SczdTK0pXdGlpNjNqYUxsMmN0eXIxYVJPNlJRdmtYMlgzbTU4MFRKODVwZ0dBbgpYY3hENW1HNTRsRFlqdTllbVlRUUZlRDNrQXQzV3pGWmhkWXFtUEU4VEk1clBCbGtxNzkwVzF3K3BveUpHc0VoCjJIZXlMekQ5WGNSdXBoVE1QaEFXUTlBMzVHbnBOM0NrUXA0c1lBSnlNck5FQWhHQ285cXVGZW5rRDMxQmFPSjMKQWN1MDBISVpyUy9JTkRZNGJ4MmExd3RFbnd4dDkxSnJ4R1I5eFFtZE5DejFLZkR2Z2laSldTR2ZPYXhkbmtoMgp0WHkrYkl1WER0ZVhsK3dDN0wySG9PTmZKUjM5RDNPTjVLdzRvbTdlMjVSM3dKcEN2UGFFR01ELzZ0YWt6N1dCCnVEbmVBTHRSb1ZkSzhsK1RuaGVKcmtQRG1tUnkwL1BPRzgxWXp3VVlEVCs1VmtuTnNRS0NBUUVBd3paQ0JWTk4KaGlza3psTmdRTTRZTkVLT3lLUkcwL05LUlBIM1lzVUZyeDJEQjhESzVITWJCSVNqQ0ZrQUhJRXI1R1VlVVUwRgpCMndURFFScnQ0KzZYcG92aE5SZmdOQWZodXZDdXZNajk2eG05N3hpa01GeDI3ZW9VcVQ0amlKSU5NTG12MXh1CnY1UTNGbnhhQVgxdE05UHpEbHJQN3ZXaWsrWWprZFRZZlFLL0dHdU90d2RwdC96V2xXREtaSmp6RmozamtVNUoKK3lNcjVlbDBSdVJFQlZkaHdHTkRqWHRGNXg0UkQ2YWpXRXFtNEJ4VXBHeGM3UEZ2NU96d0NMNm50NkxTUGwzMwo0OGpOVVhsYWhESk0rUStQeHBBR2UxTXVUM2dTdmNaVk5hMG83eXNuZHE2b3VaSUx5VFZ3S0xoZG1kbnpZY1BWCmpKL0lFOTdZR0RBblZRS0NBUUVBd2RhbENoWDlacFVGalFOWG1KcnUyaGwxck9UYmE1V3FCWGh1ME91bnNHaG4KUkN2YjgyckQ4dnR4VVNLV1drSTd3cFFEakVxOVQyVmRRbmswR3ZVK3lIUW1WVE9MenNTcVlBRk0rdnUrWnJ0UApMcWxSa05lYll4UGx0TkwyVWJQYUMrOVpWblNOYUJDeEpOKzBwcTJnZG5IRWd5YWRJQ1FUVE41OGFDSVN4SGg5ClRqQ2N6Njd1RDMrU3FTVHVEZHFnQUhmeDlNTytjb3JQaW1Zc2c4d2FwZ3JtR0paMkpRWC9QdllLUlkrVXFoSngKd2VBaGlYVTFDY2Y3eWpMUEdWZURrZTQwTktEcGFNZjJnYW9qakgxei8yaERuU3A2enc4Wnc1VWg3dlA2azBkeQpvYzVzQ0FxYURWNkZEMG4xNWF5Q1RVWG1EQU1VTWU1Qlh5SjVCQkJjMVFLQ0FRRUFwNVcrMjkrRjREYk5wQ3RECnFKN0ZmS2ZlK0RTL2NWbWRXczcyOTkzNFlUdE9yNnM5QXg0bUJaendjVXdtb2xIcUltc0V1ZnNLNURKTnNKRXAKQUM3dGFpV253YnFvT21keGlWeUFrZ29GeUt4Q3dVOEN0dzY2OWtzV3Y4eE1iWWpVd0NiSi9XSVcyWFVlVGJsMwpjMndBQWN4bER0KzdQb08xakk2MzNvd0JSbURET08ydFdVZU41SnUwaEF6Ujg4YXllVmVzTTZRb011Y2cyb0d1Cmh1V1QxNW9LbXlVY2F5dDIrVkNBaVJVZmliNmN3Q3pTSlUyNkFOZk1uWlVqQS83WThQZGcwcFhOSjhuTktiS3EKbUc2dVVlcWdIWENyZjlnTEc4SVRKTVJOaG9VZmJTTjQvNVExMlFtZUFLQlZweitQYTNNR1U5blJUS1luRjVmcApuK3BHK1FLQ0FRRUF3UTMrb2VUMDFFNW5rT0piUStwTEtYMWg3aWloUUsxM0FLdko4dHBCMFRpcVlRTXR0V29JCmJ1QnZJOWZHMTI1UUJxTlVSVTNLN21DT1diNU5YdXdTODZKNjZ6RERkZFA1dkZTUFR3bWJ3TVdkUDJQemtNYXMKUkNsMUJudDJTRGxRV2NLd3Y2S2xrNWZNVm1WWGp3b3VYc2xBWno3MkR5VGU5QmhDMzVQUURVM1R2eVE3aWIwMwo3TWVxVWp3dHZDNmFYTjBaWmlYdWNEWkFMaDlGQnA4cGkyWWZkUzJsellvRGhibVcwV0VITjd2WEFMa3hyYTNHCmZVOW9QeUlMa2JuUG1IQWVIcXlFeTQ4Y3ZGZXZ3Q1RTZXZabElRdEY5U09kRFdaaXZaTFJaZzRxNVd5cHUvaVQKSmUyVnFIeUpJNDZFMkdGZGxXa2JtLzhuckpDdzVwTkZZUUtDQVFCWTNjKzVnY0dkcGJoYkRjenlHNngrcHJVbwpWclVlanNJTUlIZEh1MUUzWW5rWVdrL2hMWEh4anYrbW14RTVOb0hpMmJSTG53VXZhelhReW1CV2l2cW9FRGtmClZsMGR1Tk5xZmpWcG1JY092U1Rkb0wwRHlsZVExT2JEUjBZMEhMZmNvUnJLSUhzOE13b3BWWGtIZjFzamdxVEIKMjNycHd6aDVBTHdaMXJNVDJGNC9CL3RRdlBBMzRTeGlhanQ0RlBHU3pwUE50b0trU1ZOTnM3TS96SXFmY1JtZAppNjNhcW1Tc2pJWEdrZWY1cGpZZTV2WHM2ODZkVldMVGZ4SllMa0N6TkZ4c1dZdXBFaWhZQVJQbnViUGpNd21QCkVWcDBiQTdJNlpnQUQ3Vmw1TkR3UGg4bFZYUlYzWHVyV2dFVXUrcytqMHNTMEZyS2tUeldsRmF1aUhmawotLS0tLUVORCBwcml2YXRlIGtleS0tLS0tCg=="`

	// issue management
	MetricsIssueCron string `env:"METRICS_ISSUE_CRON" default:"0 0 0 1/7 * ?"` // metrics issue report monitor execution cycle
	MaxIssueFilterBm int    `env:"MAX_ISSUE_FILTER_BM" default:"10"`

	AvatarStorageURL string `env:"AVATAR_STORAGE_URL"` // file:///avatars or oss://appkey:appsecret@endpoint/bucket

	TestFilePollingIntervalSec  int `env:"TEST_FILE_POLLING_INTERVAL_SEC" default:"30"`
	TestSetSyncCopyMaxNum       int `env:"TEST_SET_SYNC_COPY_MAX_NUM" default:"300"`
	TestFileRecordPurgeCycleDay int `env:"TEST_FILE_RECORD_PURGE_CYCLE_DAY" default:"7"`

	ProjectStatsCacheCron       string `env:"PROJECT_STATS_CACHE_CRON" default:"0 0 1 * * ?"`
	UpdateIssueExpiryStatusCron string `env:"UPDATE_ISSUE_EXPIRY_STATUS_CRON" default:"0 0 * * *"`
	UpdateMemberActiveRankCron  string `env:"UPDATE_MEMBER_ACTIVE_RANK_CRON" default:"0 0 * * *"`
	ExportIssueFileStoreDay     int    `env:"EXPORT_ISSUE_FILE_STORE_DAY" default:"7"`
	UpdateGuideExpiryStatusCron string `env:"UPDATE_GUIDE_EXPIRY_STATUS_CRON" default:"0 0/5 * * * ?"`

	GittarPublicURL string `env:"GITTAR_PUBLIC_URL"`
}

var cfg Conf

// Load loads envs
func Load() {
	envconf.MustLoad(&cfg)
}

func Debug() bool {
	return cfg.Debug
}

func ListenAddr() string {
	return cfg.ListenAddr
}

func UCClientID() string {
	return cfg.UCClientID
}

func UCClientSecret() string {
	return cfg.UCClientSecret
}

func WildDomain() string {
	return cfg.WildDomain
}

func SuperUserID() string {
	return "1100"
}

func MonitorAddr() string {
	return cfg.MonitorAddr
}

func GittarAddr() string {
	return cfg.GittarAddr
}

func BundleTimeoutSecond() int {
	return cfg.BundleTimeoutSec
}

func ConsumerNum() int {
	return cfg.ConsumerNum
}

func NexusAddr() string {
	return cfg.NexusAddr
}

func NexusUsername() string {
	return cfg.NexusUsername
}

func NexusPassword() string {
	return cfg.NexusPassword
}

func DiceClusterName() string {
	return cfg.DiceClusterName
}

func SonarAddr() string {
	return cfg.SonarAddr
}

func SonarPublicURL() string {
	return cfg.SonarPublicURL
}

func SonarAdminToken() string {
	return cfg.SonarAdminToken
}

func EventboxAddr() string {
	return cfg.EventboxAddr
}

func GolangCILintImage() string {
	return cfg.GolangCILintImage
}

func CMDBAddr() string {
	return cfg.CMDBAddr
}

func UIPublicURL() string {
	return cfg.UIPublicURL
}

func UIDomain() string {
	return cfg.UIDomain
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

// CentralNexusPublicURL 返回 中心集群 nexus 公网地址
func CentralNexusPublicURL() string {
	return cfg.CentralNexusPublicURL
}

// CentralNexusAddr 返回 中心集群 nexus 地址
func CentralNexusAddr() string {
	return cfg.CentralNexusAddr
}

// CentralNexusComponentName 返回 中心集群 nexus 组件名
func CentralNexusComponentName() string {
	return strings.Split(httpclientutil.RmProto(cfg.CentralNexusAddr), ".")[0]
}

// CentralNexusUsername 返回 中心集群 nexus 用户名
func CentralNexusUsername() string {
	return cfg.CentralNexusUsername
}

// CentralNexusPassword 返回 中心集群 nexus 密码
func CentralNexusPassword() string {
	return cfg.CentralNexusPassword
}

// Base64EncodedRsaPublicKey 返回 rsa 公钥
func Base64EncodedRsaPublicKey() string {
	return cfg.Base64EncodedRsaPublicKey
}

// Base64EncodedRsaPrivateKey 返回 rsa 私钥
func Base64EncodedRsaPrivateKey() string {
	return cfg.Base64EncodedRsaPrivateKey
}

func MetricsIssueCron() string {
	return cfg.MetricsIssueCron
}

// MaxIssueFilterBm returns maximum limitation of IssueFilterBookmarks
func MaxIssueFilterBm() int {
	return cfg.MaxIssueFilterBm
}

// AvatarStorageURL 返回 OSSUsage 选项
func AvatarStorageURL() string {
	return cfg.AvatarStorageURL
}

func TestFileIntervalSec() int {
	return cfg.TestFilePollingIntervalSec
}

// ProjectStatsCacheCron 项目状态缓存刷新周期
func ProjectStatsCacheCron() string {
	return cfg.ProjectStatsCacheCron
}

func TestSetSyncCopyMaxNum() int {
	return cfg.TestSetSyncCopyMaxNum
}

func TestFileRecordPurgeCycleDay() int {
	return cfg.TestFileRecordPurgeCycleDay
}

func UpdateIssueExpiryStatusCron() string {
	return cfg.UpdateIssueExpiryStatusCron
}

func UpdateMemberActiveRankCron() string {
	return cfg.UpdateMemberActiveRankCron
}

func ExportIssueFileStoreDay() int {
	return cfg.ExportIssueFileStoreDay
}

func UpdateGuideExpiryStatusCron() string {
	return cfg.UpdateGuideExpiryStatusCron
}

func GittarPublicURL() string {
	return cfg.GittarPublicURL
}
