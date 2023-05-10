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

package common

import (
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"math"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	rotatelogs "github.com/lestrrat/go-file-rotatelogs"
	"github.com/pkg/errors"
	"github.com/rifflock/lfshook"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/pkg/transport"
	erdaHttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
)

// 2016-09-27 09:38:21.541541811 +0200 CEST
// 127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700]
// "GET /apache_pb.gif HTTP/1.0" 200 2326
// "http://www.example.com/start.html"
// "Mozilla/4.08 [en] (Win98; I ;Nav)"

var timeFormat = "2006-01-02 15:04:05.000"

var AccessLog *logrus.Logger

var ErrorLog = logrus.StandardLogger()

func InitLogger() {
	AccessLog = logrus.New()
	level, err := logrus.ParseLevel(config.LogConf.AccessLevel)
	if err != nil {
		panic(err)
	}
	AccessLog.SetLevel(level)
	AccessLog.SetOutput(os.Stdout)
	if len(config.LogConf.AccessFile) > 0 {
		LogFileRotate(
			AccessLog,
			config.LogConf.AccessFile,
			time.Hour*time.Duration(config.LogConf.FileMaxAge),
			time.Hour*time.Duration(config.LogConf.FileRotateInteval),
			config.LogConf.PrettyPrint,
		)
	}

	level, err = logrus.ParseLevel(config.LogConf.ErrorLevel)
	if err != nil {
		panic(err)
	}
	ErrorLog.SetLevel(level)
	hook := NewLineHook()
	hook.Field = "line"
	ErrorLog.AddHook(hook)
	ErrorLog.SetOutput(os.Stderr)
	if len(config.LogConf.ErrorFile) > 0 {
		LogFileRotate(
			ErrorLog,
			config.LogConf.ErrorFile,
			time.Hour*time.Duration(config.LogConf.FileMaxAge),
			time.Hour*time.Duration(config.LogConf.FileRotateInteval),
			config.LogConf.PrettyPrint,
		)
	}
}

func AccessLogWrap(log *logrus.Logger) transport.ServiceOption {
	return transport.WithHTTPOptions(erdaHttp.WithHTTPInterceptor(func(h http.HandlerFunc) http.HandlerFunc {
		return func(httpResp http.ResponseWriter, httpReq *http.Request) {
			recorder := httptest.NewRecorder()
			recorder.Code = 502
			path := httpReq.URL.RequestURI()
			start := time.Now()
			reqBody, err := ioutil.ReadAll(httpReq.Body)
			if err != nil {
				return
			}
			httpReq.Body = io.NopCloser(bytes.NewReader(reqBody))
			defer func() {
				util.DoRecover()
				stop := time.Since(start)
				latency := int(math.Ceil(float64(stop.Nanoseconds()) / 1000000.0))
				clientUserAgent := httpReq.UserAgent()
				referer := httpReq.Referer()
				hostname, err := os.Hostname()
				if err != nil {
					hostname = "unknow"
				}
				result := recorder.Result()
				headers := httpResp.Header()
				for key, values := range result.Header {
					headers[textproto.CanonicalMIMEHeaderKey(key)] = values
				}
				httpResp.WriteHeader(result.StatusCode)
				io.Copy(httpResp, result.Body)
				entry := logrus.NewEntry(log).WithFields(logrus.Fields{
					"hostname":   hostname,
					"statusCode": result.StatusCode,
					"latency":    latency, // time to process
					"method":     httpReq.Method,
					"path":       path,
					"referer":    referer,
					"userAgent":  clientUserAgent,
				})
				msg := fmt.Sprintf(`[%s %s] [%s] [%s]`, httpReq.Method, path, reqBody, recorder.Body.Bytes())
				entry.Info(msg)
			}()
			h(recorder, httpReq)
		}

	}))
}

// Logger is the logrus logger handler
func AccessWrap(log *logrus.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		// other handler can change c.Path so:
		path := c.Request.URL.RequestURI()
		start := time.Now()
		c.Next()
		stop := time.Since(start)
		latency := int(math.Ceil(float64(stop.Nanoseconds()) / 1000000.0))
		statusCode := c.Writer.Status()
		clientIP := c.ClientIP()
		clientUserAgent := c.Request.UserAgent()
		referer := c.Request.Referer()
		reqBody := c.GetString("reqBody")
		respBody := c.GetString("respBody")
		hostname, err := os.Hostname()
		if err != nil {
			hostname = "unknow"
		}
		dataLength := c.Writer.Size()
		if dataLength < 0 {
			dataLength = 0
		}

		entry := logrus.NewEntry(log).WithFields(logrus.Fields{
			"hostname":   hostname,
			"statusCode": statusCode,
			"latency":    latency, // time to process
			"clientIP":   clientIP,
			"method":     c.Request.Method,
			"path":       path,
			"referer":    referer,
			"dataLength": dataLength,
			"userAgent":  clientUserAgent,
		})

		if len(c.Errors) > 0 {
			entry.Error(c.Errors.ByType(gin.ErrorTypePrivate).String())
		} else {
			msg := fmt.Sprintf(`[%s %s] [%s] [%d] [%s]`, c.Request.Method, path, reqBody, statusCode, respBody)
			if statusCode > 499 {
				entry.Error(msg)
			} else if statusCode > 399 {
				entry.Warn(msg)
			} else {
				entry.Info(msg)
			}
		}
	}
}

func LogFileRotate(logger *logrus.Logger, logPath string, maxAge time.Duration, rotationTime time.Duration, prettyPrint bool) {
	writer, err := rotatelogs.New(
		logPath+".%Y%m%d%H",
		rotatelogs.WithMaxAge(maxAge),             // 文件最大保存时间
		rotatelogs.WithRotationTime(rotationTime), // 日志切割时间间隔
	)
	if err != nil {
		logger.Errorf("config local file system logger error. %+v", errors.WithStack(err))
	}
	lfHook := lfshook.NewHook(lfshook.WriterMap{
		logrus.DebugLevel: writer, // 为不同级别设置不同的输出目的
		logrus.InfoLevel:  writer,
		logrus.WarnLevel:  writer,
		logrus.ErrorLevel: writer,
		logrus.FatalLevel: writer,
		logrus.PanicLevel: writer,
	}, &logrus.JSONFormatter{
		TimestampFormat: timeFormat,
		PrettyPrint:     prettyPrint,
	})
	logger.AddHook(lfHook)
}
