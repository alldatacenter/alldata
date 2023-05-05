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

package release

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"reflect"
	"strings"

	"github.com/coreos/etcd/clientv3"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	gormV2 "gorm.io/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	_ "github.com/erda-project/erda-infra/providers/mysql/v2"
	gallerypb "github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	extensiondb "github.com/erda-project/erda/internal/apps/dop/dicehub/extension/db"
	imagedb "github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/release/db"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/release_rule"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

type config struct {
	MaxTimeReserved string `file:"max_time_reserved" env:"RELEASE_MAX_TIME_RESERVED"`
	GCSwitch        bool   `file:"gc_switch" env:"RELEASE_GC_SWITCH"`
}

// +provider
type provider struct {
	Cfg                   *config
	Log                   logs.Logger
	Register              transport.Register      `autowired:"service-register" required:"true"`
	DB                    *gorm.DB                `autowired:"mysql-client"`
	DBv2                  *gormV2.DB              `autowired:"mysql-gorm.v2-client"`
	Etcd                  *clientv3.Client        `autowired:"etcd"`
	GallerySvc            gallerypb.GalleryServer `autowired:"erda.apps.gallery.Gallery"`
	releaseService        *ReleaseService
	releaseGetDiceService *releaseGetDiceService
	opusService           pb.OpusServer
	bdl                   *bundle.Bundle
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())

	p.opusService = &opus{d: &db.OpusDB{DB: p.DBv2}}
	p.releaseService = &ReleaseService{
		p:               p,
		db:              &db.ReleaseConfigDB{DB: p.DB},
		labelRelationDB: &db.LabelRelationConfigDB{DB: p.DB},
		imageDB:         &imagedb.ImageConfigDB{DB: p.DB},
		extensionDB:     &extensiondb.ExtensionConfigDB{DB: p.DB},
		bdl:             p.bdl,
		//Etcd:    p.Etcd,
		Config: &releaseConfig{
			MaxTimeReserved: p.Cfg.MaxTimeReserved,
		},
		ReleaseRule: release_rule.New(release_rule.WithDBClient(&dbclient.DBClient{
			DBEngine: &dbengine.DBEngine{DB: p.DB},
		})),
		opus:    p.opusService,
		gallery: p.GallerySvc,
	}
	p.releaseGetDiceService = &releaseGetDiceService{
		p:  p,
		db: &db.ReleaseConfigDB{DB: p.DB},
	}

	if p.Register != nil {
		pb.RegisterReleaseServiceImp(p.Register, p.releaseService, apis.Options(),
			transport.WithHTTPOptions(
				transhttp.WithEncoder(func(rw http.ResponseWriter, r *http.Request, data interface{}) error {
					// TODO because some bug, r.Context() is nil，use r.URL.path temporary
					//info := transport.ContextServiceInfo(r.Context())
					//if info != nil {
					//	if info.Service() == "GetIosPlist" && info.Method() == "GET" {
					//		if resp, ok := data.(*apis.Response); ok && resp != nil {
					//			fmt.Println(reflect.TypeOf(resp.Data))
					//			if dt, ok := resp.Data.(string); ok {
					//				rw.Write([]byte(dt))
					//			}
					//		}
					//	}
					//}
					if strutil.HasPrefixes(r.URL.Path, "/api/releases") && strutil.HasSuffixes(r.URL.Path, "/actions/get-plist") {
						if resp, ok := data.(*apis.Response); ok && resp != nil {
							if dt, ok := resp.Data.(string); ok {
								rw.Write([]byte(dt))
								data = nil
							}
						}
					}

					logrus.Debugf("enter encoder")
					if resp, ok := data.(*apis.Response); ok && resp != nil {
						logrus.Debugf("enter encoder, type of data: %v", reflect.TypeOf(resp.Data))
						switch data := resp.Data.(type) {
						case *pb.ReleaseGetResponseData:
							if !data.IsProjectRelease {
								break
							}
							modes := make(map[string]apistructs.ReleaseDeployModeSummary)
							for name, mode := range data.Modes {
								list := make([][]*apistructs.ApplicationReleaseSummary, len(data.Modes[name].ApplicationReleaseList))
								for i, array := range mode.ApplicationReleaseList {
									list[i] = make([]*apistructs.ApplicationReleaseSummary, len(array.List))
									for j, summary := range array.List {
										list[i][j] = &apistructs.ApplicationReleaseSummary{
											ReleaseID:       summary.ReleaseID,
											ReleaseName:     summary.ReleaseName,
											Version:         summary.Version,
											ApplicationID:   summary.ApplicationID,
											ApplicationName: summary.ApplicationName,
											CreatedAt:       summary.CreatedAt,
											DiceYml:         summary.DiceYml,
										}
									}
								}
								modes[name] = apistructs.ReleaseDeployModeSummary{
									DependOn:               mode.DependOn,
									Expose:                 mode.Expose,
									ApplicationReleaseList: list,
								}
							}

							m, err := marshal(data)
							if err != nil {
								logrus.Errorf("failed to marshal releaseGetResponseData, %v", err)
								return err
							}
							m["modes"] = modes
							resp.Data = m
						}
					}
					if err := encoding.EncodeResponse(rw, r, data); err != nil {
						logrus.Errorf("failed to encodeResponse, %v", err)
						return err
					}
					return nil
				}),
				transhttp.WithDecoder(func(r *http.Request, out interface{}) error {
					logrus.Debugf("enter decoder, type of out: %v", reflect.TypeOf(out))
					switch out.(type) {
					// decode for api POST /api/releases
					case *pb.ReleaseCreateRequest:
						m := make(map[string]interface{})

						var body []byte
						if r.Body != nil {
							body, _ = ioutil.ReadAll(r.Body)
						} else {
							return nil
						}
						if err := json.Unmarshal(body, &m); err != nil {
							logrus.Errorf("failed to unmarshal ReleaseCreateRequest req body, %v", err)
							return err
						}

						isProjectRelease, ok := m["isProjectRelease"].(bool)
						if !ok || !isProjectRelease {
							logrus.Debugf("Decoder of ReleaseCreateRequest: not a project release, skip")
							r.Body = ioutil.NopCloser(bytes.NewBuffer(body))
							break
						}

						modes, ok := m["modes"].(map[string]interface{})
						if !ok {
							logrus.Errorf("invalid type of modes: %v", reflect.TypeOf(m["applicationReleaseList"]))
							return errors.Errorf("modes is invalid")
						}

						m["modes"] = convertToPbModes(modes)

						if err := unmarshal(m, out); err != nil {
							return err
						}
						return nil
					case *pb.ReleaseUpdateRequest:
						m := make(map[string]interface{})

						var body []byte
						if r.Body != nil {
							body, _ = ioutil.ReadAll(r.Body)
						} else {
							return nil
						}
						if err := json.Unmarshal(body, &m); err != nil {
							logrus.Errorf("failed to unmarshal ReleaseUpdateRequest req body, %v", err)
							return err
						}

						modes, ok := m["modes"].(map[string]interface{})
						if !ok {
							logrus.Debugf("Decoder of ReleaseUpdateRequest: not a project release, skip")
							r.Body = ioutil.NopCloser(bytes.NewBuffer(body))
							break
						}

						m["modes"] = convertToPbModes(modes)

						if err := unmarshal(m, out); err != nil {
							logrus.Errorf("failed to unmarshal, %v", err)
							return err
						}
						return nil
					}
					if err := encoding.DecodeRequest(r, out); err != nil {
						logrus.Errorf("failed to decodeRequest, %v", err)
						return err
					}
					return nil
				}),
			))

		pb.RegisterReleaseGetDiceServiceImp(p.Register, p.releaseGetDiceService, apis.Options(),
			transport.WithHTTPOptions(
				transhttp.WithEncoder(func(rw http.ResponseWriter, r *http.Request, data interface{}) error {
					if resp, ok := data.(*apis.Response); ok && resp != nil {
						if diceYAML, ok := resp.Data.(string); ok {
							if strings.Contains(r.Header.Get("Accept"), "application/x-yaml") {
								rw.Header().Set("Content-Type", "application/x-yaml; charset=utf-8")
								rw.Write([]byte(diceYAML))
							} else { // default: application/json
								yaml, err := diceyml.New([]byte(diceYAML), false)
								if err != nil {
									logrus.Errorf("diceyml new error: %v", err)
									return errors.Errorf("Parse diceyml error.")
								}
								diceJSON, err := yaml.JSON()
								if err != nil {
									logrus.Errorf("diceyml marshal error: %v", err)
									return errors.Errorf("Parse diceyml error.")
								}
								rw.Header().Set("Content-Type", "application/json; charset=utf-8")
								rw.Write([]byte(diceJSON))
							}
							data = nil
						}
					}
					return encoding.EncodeResponse(rw, r, data)
				}),
			))
	}
	// TODO endpoint open GC now. open it after refactor Dicehub
	// Do release Scheduled cleaning tasks
	//if err := p.ReleaseGC(); err != nil {
	//	return err
	//}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.dicehub.release.ReleaseService" || ctx.Type() == pb.ReleaseServiceServerType() || ctx.Type() == pb.ReleaseServiceHandlerType():
		return p.releaseService
	case ctx.Service() == "erda.core.dicehub.release.ReleaseGetDiceService" || ctx.Type() == pb.ReleaseGetDiceServiceServerType() || ctx.Type() == pb.ReleaseGetDiceServiceHandlerType():
		return p.releaseGetDiceService
	case ctx.Service() == "erda.core.dicehub.release.Opus" || ctx.Type() == pb.OpusServerType() || ctx.Type() == pb.OpusHandlerType():
		return p.opusService
	}
	return p
}

// ReleaseGC Do release gc Scheduled cleaning tasks
func (p *provider) ReleaseGC() error {
	if p.Cfg.GCSwitch {
		p.ImageGCCron(p.Etcd)
	}
	return nil
}

func init() {
	servicehub.Register("erda.core.dicehub.release", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}

func convertToPbModes(modes map[string]interface{}) map[string]*pb.Mode {
	pdModes := make(map[string]*pb.Mode)
	for name, m := range modes {
		mode, ok := m.(map[string]interface{})
		if !ok {
			logrus.Debugf("invalid tpye of mode %s, skip", name)
			continue
		}
		appReleaseList, ok := mode["applicationReleaseList"].([]interface{})
		if !ok {
			logrus.Debugf("invalid type of appReleaseList: %v, skip", reflect.TypeOf(mode["applicationReleaseList"]))
			continue
		}
		list := make([]*pb.ReleaseList, len(appReleaseList))
		for i := 0; i < len(appReleaseList); i++ {
			l, ok := appReleaseList[i].([]interface{})
			if !ok {
				logrus.Debugf("invalid type of appReleaseList element: %v, skip", reflect.TypeOf(appReleaseList[i]))
				continue
			}

			var group pb.ReleaseList
			for j := 0; j < len(l); j++ {
				s, ok := l[j].(string)
				if !ok {
					logrus.Debugf("invalid type of release id: %v, skip", reflect.TypeOf(l[j]))
					continue
				}
				group.List = append(group.List, s)
			}
			list[i] = &group
		}
		s, _ := mode["dependOn"].([]interface{})
		var dependOn []string
		for _, id := range s {
			releaseID, ok := id.(string)
			if !ok {
				continue
			}
			dependOn = append(dependOn, releaseID)
		}
		expose, _ := mode["expose"].(bool)
		pdModes[name] = &pb.Mode{
			DependOn:               dependOn,
			Expose:                 expose,
			ApplicationReleaseList: list,
		}
	}
	return pdModes
}

func marshal(in interface{}) (map[string]interface{}, error) {
	data, err := json.Marshal(in)
	if err != nil {
		return nil, err
	}

	out := make(map[string]interface{})
	if err := json.Unmarshal(data, &out); err != nil {
		return nil, err
	}
	return out, nil
}

func unmarshal(in map[string]interface{}, out interface{}) error {
	data, err := json.Marshal(in)
	if err != nil {
		return err
	}

	return json.Unmarshal(data, out)
}
