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
	"errors"
	"os"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/i18n"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/envconf"
	"github.com/erda-project/erda/pkg/strutil"
)

type Conf struct {
	ListenAddr string `default:":9529" env:"LISTEN_ADDR"`

	RedisMasterName    string `default:"my-master" env:"REDIS_MASTER_NAME"`
	RedisSentinelAddrs string `default:"" env:"REDIS_SENTINELS_ADDR"`
	RedisAddr          string `default:"127.0.0.1:6379" env:"REDIS_ADDR"`
	RedisPwd           string `default:"anywhere" env:"REDIS_PASSWORD"`

	UCAddrFront        string `default:"" env:"UC_PUBLIC_ADDR"`
	UCRedirectHost     string `default:"openapi.test.terminus.io" env:"SELF_PUBLIC_ADDR"`
	UCClientID         string `default:"dice" env:"UC_CLIENT_ID"`
	UCClientSecret     string `default:"secret" env:"UC_CLIENT_SECRET"`
	RedirectAfterLogin string `default:"//dice.test.terminus.io/" env:"UI_PUBLIC_ADDR"`
	CookieDomain       string `default:".terminus.io,.erda.cloud" env:"COOKIE_DOMAIN"`
	OldCookieDomain    string `default:"" env:"OLD_COOKIE_DOMAIN"`
	SessionCookieName  string `default:"OPENAPISESSION" env:"SESSION_COOKIE_NAME"`
	CSRFCookieDomain   string `default:"" env:"CSRF_COOKIE_DOMAIN"`

	UseK8S             string `env:"DICE_CLUSTER_TYPE"`
	SurveyDingding     string `env:"SURVEY_DINGDING"`
	DiceProtocol       string `env:"DICE_PROTOCOL"`
	CustomNamespace    string `env:"CUSTOM_NAMESPACE"`
	SelfPublicURL      string `env:"SELF_PUBLIC_URL"`
	ExportUserWithRole string `default:"false" env:"EXPORT_USER_WITH_ROLE"`
	ErdaSystemFQDN     string `env:"ERDA_SYSTEM_FQDN"`

	CustomSvcHostPortMapping map[string]ServiceHostPort

	// 修改该值的话，注意同步修改 dice.yml 中 '<%$.Storage.MountPoint%>/dice/openapi/oauth2/:/oauth2/:rw' 容器内挂载点的值
	OAuth2NetdataDir string `env:"OAUTH2_NETDATA_DIR" default:"/oauth2/"`

	CSRFWhiteList string `env:"CSRF_WHITE_LIST"`

	// ory/kratos config
	OryEnabled           bool   `default:"false" env:"ORY_ENABLED"`
	OryKratosAddr        string `default:"kratos-public" env:"ORY_KRATOS_ADDR"`
	OryKratosPrivateAddr string `default:"kratos-admin" env:"ORY_KRATOS_ADMIN_ADDR"`

	// Allow people who are not admin to create org
	CreateOrgEnabled bool `default:"false" env:"CREATE_ORG_ENABLED"`

	MySQLHost     string `env:"MYSQL_HOST"`
	MySQLPort     string `env:"MYSQL_PORT"`
	MySQLUsername string `env:"MYSQL_USERNAME"`
	MySQLPassword string `env:"MYSQL_PASSWORD"`
	MySQLDatabase string `env:"MYSQL_DATABASE"`
	MySQLLoc      string `env:"MYSQL_LOC" default:"Local"`
	Debug         bool   `env:"DEBUG" default:"false"`
}

var cfg Conf

func init() {
	envconf.MustLoad(&cfg)
}

// Load 加载环境变量配置
func Load() {
	envconf.MustLoad(&cfg)
	i18n.InitI18N()
	cfg.CustomSvcHostPortMapping = initCustomSvcHostPortMapping()
}

// ListenAddr return LISTEN_ADDR
func ListenAddr() string {
	return cfg.ListenAddr
}

// RedisMasterName
func RedisMasterName() string {
	return cfg.RedisMasterName
}

func RedisSentinelAddrs() string {
	return cfg.RedisSentinelAddrs
}

func RedisAddr() string {
	return cfg.RedisAddr
}

func RedisPwd() string {
	return cfg.RedisPwd
}

func UCAddrFront() string {
	return cfg.UCAddrFront
}

func UCRedirectHost() string {
	return cfg.UCRedirectHost
}

func UCClientID() string {
	return cfg.UCClientID
}

func UCClientSecret() string {
	return cfg.UCClientSecret
}

func RedirectAfterLogin() string {
	return cfg.RedirectAfterLogin
}

func CookieDomain() string {
	return cfg.CookieDomain
}

func OldCookieDomain() string {
	return cfg.OldCookieDomain
}

func SessionCookieName() string {
	return cfg.SessionCookieName
}

func CSRFCookieDomain() string {
	return cfg.CSRFCookieDomain
}

func UseK8S() bool {
	return cfg.UseK8S == "kubernetes"
}

func SurveyDingding() string {
	return cfg.SurveyDingding
}

func DiceProtocol() string {
	return cfg.DiceProtocol
}

func OAuth2NetdataDir() string {
	return cfg.OAuth2NetdataDir
}

func CSRFWhiteList() []string {
	return strutil.Split(cfg.CSRFWhiteList, ",", true)
}

func OryEnabled() bool {
	return cfg.OryEnabled
}

func OryKratosAddr() string {
	return cfg.OryKratosAddr
}

func OryKratosPrivateAddr() string {
	return cfg.OryKratosPrivateAddr
}

func OryLoginURL() string {
	return "/uc/login"
}

func OryLogoutURL() string {
	return "/.ory/kratos/public/self-service/browser/flows/logout"
}

func OryCompatibleClientID() string {
	return "kratos"
}

func OryCompatibleClientSecret() string {
	return ""
}

func CustomNamespace() string {
	return cfg.CustomNamespace
}

func SelfPublicURL() string {
	return cfg.SelfPublicURL
}

func ExportUserWithRole() bool {
	return cfg.ExportUserWithRole == "true"
}

func ErdaSystemFQDN() string {
	return cfg.ErdaSystemFQDN
}

func CreateOrgEnabled() bool {
	return cfg.CreateOrgEnabled
}

func MySQLHost() string {
	return cfg.MySQLHost
}

func MySQLPort() string {
	return cfg.MySQLPort
}

func MySQLUsername() string {
	return cfg.MySQLUsername
}

func MySQLPassword() string {
	return cfg.MySQLPassword
}

func MySQLDatabase() string {
	return cfg.MySQLDatabase
}

func MySQLLoc() string {
	return cfg.MySQLLoc
}

func Debug() bool {
	return cfg.Debug
}

// GetDomain get a domain by request host
func GetDomain(host, confDomain string) (string, error) {
	if strings.Contains(host, ":") {
		host = strings.SplitN(host, ":", -1)[0]
	}
	err := errors.New("invalid domain")
	domainSlice := strings.SplitN(host, ".", -1)
	l := len(domainSlice)
	if l < 2 {
		return "", err
	}
	domain := "." + domainSlice[l-2] + "." + domainSlice[l-1]
	logrus.Infof("domain is: %s", domain)

	confDomains := strings.SplitN(confDomain, ",", -1)
	for _, v := range confDomains {
		if strings.Contains(v, domain) {
			return v, nil
		}
	}

	return "", err
}

// GetUCRedirectHost get a uc redirect host by referer
func GetUCRedirectHost(referer string) string {
	rh := strings.SplitN(UCRedirectHost(), ",", -1)
	for _, v := range rh {
		domainSlice := strings.SplitN(v, ".", -1)
		l := len(domainSlice)
		if l < 2 {
			return ""
		}
		if strings.Contains(domainSlice[l-1], ":") {
			domainSlice[l-1] = strings.SplitN(domainSlice[l-1], ":", -1)[0]
		}
		domain := domainSlice[l-2] + "." + domainSlice[l-1]
		logrus.Infof("redirect domain is: %s", domain)
		if strings.Contains(referer, domain) {
			return v
		}
	}

	return ""
}

type ServiceHostPort struct {
	Host string
	Port uint16
}

// initCustomSvcHostPortMapping init mapping when openapi initialize only once, not get from env per request.
func initCustomSvcHostPortMapping() map[string]ServiceHostPort {
	customSvcHostPortMapping := make(map[string]ServiceHostPort)
	for svc, envKey := range discover.ServicesEnvKeys {
		svcAddr, ok := os.LookupEnv(envKey)
		if !ok {
			continue
		}
		svcHost, svcPort, ok := getSvcHostPortFromAddr(svcAddr)
		if !ok {
			continue
		}
		logrus.Infof("find custom service addr, svc: %s, host: %s, port: %d", svc, svcHost, svcPort)
		customSvcHostPortMapping[svc] = ServiceHostPort{Host: svcHost, Port: svcPort}
	}
	return customSvcHostPortMapping
}

func getSvcHostPortFromAddr(svcAddr string) (host string, port uint16, ok bool) {
	colonIndex := strings.Index(svcAddr, ":")
	if colonIndex == -1 {
		return svcAddr, 80, true
	}
	host = svcAddr[:colonIndex]
	port64, err := strconv.ParseUint(svcAddr[colonIndex+1:], 10, 16)
	if err != nil {
		logrus.Warnf("failed to get svc host & port from addr, skip, addr: %s, err: %v", svcAddr, err)
		return "", 0, false
	}
	return host, uint16(port64), true
}

func GetCustomSvcHostPort(svc string) (string, uint16, bool) {
	if len(cfg.CustomSvcHostPortMapping) == 0 {
		return "", 0, false
	}
	hostPort, ok := cfg.CustomSvcHostPortMapping[svc]
	return hostPort.Host, hostPort.Port, ok
}
