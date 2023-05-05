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

// Package core_services
package legacy

import (
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/go-redis/redis"
	"github.com/gorilla/schema"
	"github.com/sirupsen/logrus"
	"gopkg.in/igm/sockjs-go.v2/sockjs"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	projectCache "github.com/erda-project/erda/internal/core/legacy/cache/project"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/endpoints"
	"github.com/erda-project/erda/internal/core/legacy/services/activity"
	"github.com/erda-project/erda/internal/core/legacy/services/application"
	"github.com/erda-project/erda/internal/core/legacy/services/approve"
	"github.com/erda-project/erda/internal/core/legacy/services/audit"
	"github.com/erda-project/erda/internal/core/legacy/services/errorbox"
	"github.com/erda-project/erda/internal/core/legacy/services/filesvc"
	"github.com/erda-project/erda/internal/core/legacy/services/label"
	"github.com/erda-project/erda/internal/core/legacy/services/manual_review"
	"github.com/erda-project/erda/internal/core/legacy/services/mbox"
	"github.com/erda-project/erda/internal/core/legacy/services/member"
	"github.com/erda-project/erda/internal/core/legacy/services/notice"
	"github.com/erda-project/erda/internal/core/legacy/services/notify"
	"github.com/erda-project/erda/internal/core/legacy/services/org"
	"github.com/erda-project/erda/internal/core/legacy/services/permission"
	"github.com/erda-project/erda/internal/core/legacy/services/project"
	"github.com/erda-project/erda/internal/core/legacy/services/subscribe"
	"github.com/erda-project/erda/internal/core/legacy/services/user"
	"github.com/erda-project/erda/internal/core/legacy/utils"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/websocket"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/license"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Initialize 初始化应用启动服务.
func (p *provider) Initialize() error {
	conf.Load()
	if conf.Debug() {
		logrus.SetLevel(logrus.DebugLevel)
	}

	// init endpoints
	ep, err := p.initEndpoints()
	if err != nil {
		return err
	}

	bdl := bundle.New(
		bundle.WithCollector(),
	)

	go ep.UserSvc().UcUserMigration()

	server := httpserver.New(conf.ListenAddr())
	server.RegisterEndpoint(ep.Routes())
	server.WithLocaleLoader(bdl.GetLocaleLoader())
	// Add auth middleware
	// server.Router().Path("/metrics").Methods(http.MethodGet).Handler(promxp.Handler("cmdb"))
	server.Router().Path("/api/images/{imageName}").Methods(http.MethodGet).HandlerFunc(endpoints.GetImage)
	logrus.Infof("start the service and listen on address: \"%s\"", conf.ListenAddr())

	wsi, err := websocket.New()
	go func() {
		wsi.Start(nil)
	}()
	server.Router().PathPrefix("/api/dice/eventbox").Path("/ws/{any:.*}").
		Handler(sockjs.NewHandler("/api/dice/eventbox/ws", sockjs.DefaultOptions, wsi.HTTPHandle))
	p.Router.Any("/**", server.Router().ServeHTTP)
	return nil
}

// 初始化 Endpoints
func (p *provider) initEndpoints() (*endpoints.Endpoints, error) {
	var (
		etcdStore *etcd.Store
		store     jsonstore.JsonStore
		ossClient *oss.Client
		db        *dao.DBClient
		redisCli  *redis.Client
		err       error
	)

	store, err = jsonstore.New()
	if err != nil {
		return nil, err
	}

	etcdStore, err = etcd.New()
	if err != nil {
		return nil, err
	}

	if utils.IsOSS(conf.AvatarStorageURL()) {
		url, err := url.Parse(conf.AvatarStorageURL())
		if err != nil {
			return nil, err
		}
		appSecret, _ := url.User.Password()
		ossClient, err = oss.New(url.Host, url.User.Username(), appSecret)
		if err != nil {
			return nil, err
		}
	}

	db, err = dao.NewDBClient()
	if err != nil {
		return nil, err
	}

	if conf.LocalMode() {
		redisCli = redis.NewClient(&redis.Options{
			Addr:     conf.RedisAddr(),
			Password: conf.RedisPwd(),
		})
	} else {
		redisCli = redis.NewFailoverClient(&redis.FailoverOptions{
			MasterName:    conf.RedisMasterName(),
			SentinelAddrs: strings.Split(conf.RedisSentinelAddrs(), ","),
			Password:      conf.RedisPwd(),
		})
	}
	if _, err := redisCli.Ping().Result(); err != nil {
		return nil, err
	}

	// 初始化UC Client
	uc := ucauth.NewUCClient(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
	if conf.OryEnabled() {
		uc = ucauth.NewUCClient(conf.OryKratosPrivateAddr(), conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
		uc.SetDBClient(db.DB)
	}

	// init bundle
	bundleOpts := []bundle.Option{
		bundle.WithAddOnPlatform(),
		bundle.WithGittar(),
		bundle.WithGittarAdaptor(),
		bundle.WithCoreServices(),
		bundle.WithMonitor(),
		bundle.WithScheduler(),
		bundle.WithDiceHub(),
		bundle.WithPipeline(),
		bundle.WithOrchestrator(),
		bundle.WithQA(),
		bundle.WithHTTPClient(httpclient.New(
			httpclient.WithTimeout(time.Second, time.Second*30),
		)),
		bundle.WithKMS(),
		bundle.WithHepa(),
		bundle.WithCollector(),
		bundle.WithClusterManager(),
	}
	bdl := bundle.New(bundleOpts...)

	// init org service
	o := org.New(
		org.WithDBClient(db),
		org.WithUCClient(uc),
		org.WithBundle(bdl),
		org.WithRedisClient(redisCli),
		org.WithI18n(p.Tran),
		org.WithTokenSvc(p.TokenService),
	)

	// init project service
	proj := project.New(
		project.WithDBClient(db),
		project.WithUCClient(uc),
		project.WithBundle(bdl),
		project.WithI18n(p.Tran),
	)

	// init app service
	app := application.New(
		application.WithDBClient(db),
		application.WithUCClient(uc),
		application.WithBundle(bdl),
	)

	// init member service
	m := member.New(
		member.WithDBClient(db),
		member.WithUCClient(uc),
		member.WithRedisClient(redisCli),
		member.WithTranslator(p.Tran),
		member.WithTokenSvc(p.TokenService),
	)
	mr := manual_review.New(
		manual_review.WithDBClient(db),
	)
	notifyService := notify.New(
		notify.WithDBClient(db),
	)

	// init activity service
	a := activity.New(
		activity.WithDBClient(db),
		activity.WithBundle(bdl),
	)

	pm := permission.New(
		permission.WithDBClient(db),
	)

	mboxService := mbox.New(
		mbox.WithDBClient(db),
		mbox.WithBundle(bdl),
	)

	approve := approve.New(
		approve.WithDBClient(db),
		approve.WithBundle(bdl),
		approve.WithUCClient(uc),
		approve.WithMember(m),
	)

	// 通过ui显示错误,不影响启动
	license, _ := license.ParseLicense(conf.LicenseKey())

	// init label
	l := label.New(
		label.WithDBClient(db),
	)

	notice := notice.New(
		notice.WithDBClient(db),
	)

	audit := audit.New(
		audit.WithDBClient(db),
		audit.WithUCClient(uc),
		audit.WithTrans(p.Tran),
	)

	errorBox := errorbox.New(
		errorbox.WithDBClient(db),
	)

	fileSvc := filesvc.New(
		filesvc.WithDBClient(db),
		filesvc.WithBundle(bdl),
		filesvc.WithEtcdClient(etcdStore),
	)

	user := user.New(
		user.WithDBClient(db),
		user.WithUCClient(uc),
	)

	sub := subscribe.New(
		subscribe.WithDBClient(db),
	)

	// queryStringDecoder
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)

	// cache setting
	projectCache.New(db)

	// compose endpoints
	ep := endpoints.New(
		endpoints.WithJSONStore(store),
		endpoints.WithEtcdStore(etcdStore),
		endpoints.WithOSSClient(ossClient),
		endpoints.WithDBClient(db),
		endpoints.WithUCClient(uc),
		endpoints.WithBundle(bdl),
		endpoints.WithOrg(o),
		endpoints.WithManualReview(mr),
		endpoints.WithProject(proj),
		endpoints.WithApp(app),
		endpoints.WithMember(m),
		endpoints.WithActivity(a),
		endpoints.WithPermission(pm),
		endpoints.WithNotify(notifyService),
		endpoints.WithLicense(license),
		endpoints.WithLabel(l),
		endpoints.WithMBox(mboxService),
		endpoints.WithNotice(notice),
		endpoints.WithApprove(approve),
		endpoints.WithQueryStringDecoder(queryStringDecoder),
		endpoints.WithAudit(audit),
		endpoints.WithErrorBox(errorBox),
		endpoints.WithFileSvc(fileSvc),
		endpoints.WithUserSvc(user),
		endpoints.WithSubscribe(sub),
		endpoints.WithTokenSvc(p.TokenService),
	)
	return ep, nil
}

type ExposedInterface interface {
	CheckPermission(req *apistructs.PermissionCheckRequest) (bool, error)
}

func (p *provider) CheckPermission(req *apistructs.PermissionCheckRequest) (bool, error) {
	return p.perm.CheckPermission(req)
}
