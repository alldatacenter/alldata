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
	"sync"

	"github.com/erda-project/erda/pkg/discover"
)

type Conf struct {
	ListenAddr     string `default:":9528" env:"LISTEN_ADDR"`
	PoolSize       int    `default:"100" env:"POOL_SIZE"`
	SchedulerAddr  string `default:"127.0.0.1:6666" env:"SCHEDULER_ADDR"`
	MysqlAddr      string `default:"" env:"MYSQL_HOST"`
	MysqlPort      string `default:"" env:"MYSQL_PORT"`
	DbName         string `default:"dice-test" env:"MYSQL_DATABASE"`
	DbUser         string `default:"dice" env:"MYSQL_USERNAME"`
	DbPWD          string `default:"Hello1234" env:"MYSQL_PASSWORD"`
	Debug          bool   `default:"false" env:"DEBUG"`
	UseK8S         bool   `env:"DICE_CLUSTER_TYPE"`
	ErdaSystemFQDN string `env:"ERDA_SYSTEM_FQDN"`
}

var (
	C                 Conf
	ListenAddrLock    sync.Once
	PoolSizeLock      sync.Once
	SchedulerAddrLock sync.Once
	MysqlAddrLock     sync.Once
	MysqlPortLock     sync.Once
	DbNameLock        sync.Once
	DbUserLock        sync.Once
	DbPWDLock         sync.Once
	DebugLock         sync.Once
	UseK8SLock        sync.Once
)

func boolFromString(s string) (bool, error) {
	switch strings.ToLower(s) {
	case "true", "t":
		return true, nil
	case "false", "f":
		return false, nil
	}
	return false, errors.New("illegal bool tag value")
}

func ListenAddr() string {
	ListenAddrLock.Do(func() {
		e := os.Getenv("LISTEN_ADDR")
		if e == "" {
			e = ":9526"
		}

		i := e

		C.ListenAddr = i
	})
	return C.ListenAddr
}

func PoolSize() int {
	PoolSizeLock.Do(func() {
		e := os.Getenv("POOL_SIZE")
		if e == "" {
			e = "100"
		}

		i, err := strconv.Atoi(e)
		if err != nil {
			panic("conf PoolSize parse failed!")
		}

		C.PoolSize = i
	})
	return C.PoolSize
}

func SchedulerAddr() string {
	SchedulerAddrLock.Do(func() {
		e := discover.Orchestrator()
		if e == "" {
			e = "127.0.0.1:6666"
		}

		i := e

		C.SchedulerAddr = i
	})
	return C.SchedulerAddr
}

func MysqlAddr() string {
	MysqlAddrLock.Do(func() {
		e := os.Getenv("MYSQL_HOST")
		if e == "" {
			e = "rm-bp17ar40w6824r8m0o.mysql.rds.aliyuncs.com"
		}

		i := e

		C.MysqlAddr = i
	})
	return C.MysqlAddr
}

func MysqlPort() string {
	MysqlPortLock.Do(func() {
		e := os.Getenv("MYSQL_PORT")
		if e == "" {
			e = ""
		}

		i := e

		C.MysqlPort = i
	})
	return C.MysqlPort
}

func DbName() string {
	DbNameLock.Do(func() {
		e := os.Getenv("MYSQL_DATABASE")
		if e == "" {
			e = "dice-test"
		}

		i := e

		C.DbName = i
	})
	return C.DbName
}

func DbUser() string {
	DbUserLock.Do(func() {
		e := os.Getenv("MYSQL_USERNAME")
		if e == "" {
			e = "dice"
		}

		i := e

		C.DbUser = i
	})
	return C.DbUser
}

func DbPWD() string {
	DbPWDLock.Do(func() {
		e := os.Getenv("MYSQL_PASSWORD")
		if e == "" {
			e = "Hello1234"
		}

		i := e

		C.DbPWD = i
	})
	return C.DbPWD
}

func Debug() bool {
	DebugLock.Do(func() {
		e := os.Getenv("DEBUG")
		if e == "" {
			e = "true"
		}

		i, err := boolFromString(e)
		if err != nil {
			panic("conf Debug parse failed! " + err.Error())
		}

		C.Debug = i
	})
	return C.Debug
}

func UseK8S() bool {
	UseK8SLock.Do(func() {
		e := os.Getenv("DICE_CLUSTER_TYPE")
		C.UseK8S = e == "kubernetes"
	})
	return C.UseK8S
}

func SmtpHost() string {
	return os.Getenv("DICE_EMAIL_SMTP_HOST")
}

func SmtpPort() string {
	return os.Getenv("DICE_EMAIL_SMTP_PORT")
}

func SmtpUser() string {
	return os.Getenv("DICE_EMAIL_SMTP_USERNAME")
}

func SmtpPassword() string {
	return os.Getenv("DICE_EMAIL_SMTP_PASSWORD")
}

func SmtpDisplayUser() string {
	return os.Getenv("DICE_EMAIL_SMTP_DISPLAY_USER")
}

func SmtpIsSSL() string {
	return os.Getenv("DICE_EMAIL_SMTP_IS_SSL")
}

func SMTPInsecureSkipVerify() string {
	return os.Getenv("DICE_EMAIL_INSECURE_SKIP_VERIFY")
}

func AliyunAccessKeyID() string {
	return os.Getenv("ALIYUN_ACCESS_KEY_ID")
}

func AliyunAccessKeySecret() string {
	return os.Getenv("ALIYUN_ACCESS_KEY_SECRET")
}

func AliyunSmsSignName() string {
	return os.Getenv("ALIYUN_SMS_SIGN_NAME")
}

// AliyunSmsMonitorTemplateCode 监控的短信通知模版code
func AliyunSmsMonitorTemplateCode() string {
	return os.Getenv("ALIYUN_SMS_MONITOR_TEMPLATE_CODE")
}

// AliyunVmsMonitorTtsCode 监控的语音通知模版code
func AliyunVmsMonitorTtsCode() string {
	return os.Getenv("ALIYUN_VMS_MONITOR_TTSCODE")
}

// AliyunVmsMonitorCalledShowNumber 监控的语音通知被呼显示号码
func AliyunVmsMonitorCalledShowNumber() string {
	return os.Getenv("ALIYUN_VMS_MONITOR_CALLED_SHOW_NUMBER")
}

// ErdaSystemFQDN
func ErdaSystemFQDN() string {
	return os.Getenv("ERDA_SYSTEM_FQDN")
}

func Proxy() string {
	return os.Getenv("DICE_PROXY")
}

func BundleUserID() string {
	return "1101"
}
