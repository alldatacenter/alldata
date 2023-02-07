package config

import (
	"encoding/json"
	"github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/utils"
	"github.com/crawlab-team/go-trace"
	"github.com/spf13/viper"
	"io/ioutil"
	"os"
	"path"
)

type Service struct {
	cfg  *Config
	path string
}

func (svc *Service) Init() (err error) {
	// check config directory path
	configDirPath := path.Dir(svc.path)
	if !utils.Exists(configDirPath) {
		if err := os.MkdirAll(configDirPath, os.FileMode(0766)); err != nil {
			return trace.TraceError(err)
		}
	}

	if !utils.Exists(svc.path) {
		// not exists, set to default config
		// and create a config file for persistence
		svc.cfg = NewConfig(nil)
		data, err := json.Marshal(svc.cfg)
		if err != nil {
			return trace.TraceError(err)
		}
		if err := ioutil.WriteFile(svc.path, data, os.FileMode(0766)); err != nil {
			return trace.TraceError(err)
		}
	} else {
		// exists, read and set to config
		data, err := ioutil.ReadFile(svc.path)
		if err != nil {
			return trace.TraceError(err)
		}
		if err := json.Unmarshal(data, svc.cfg); err != nil {
			return trace.TraceError(err)
		}
	}

	return nil
}

func (svc *Service) Reload() (err error) {
	return svc.Init()
}

func (svc *Service) GetBasicNodeInfo() (res interfaces.Entity) {
	return &entity.NodeInfo{
		Key:        svc.GetNodeKey(),
		Name:       svc.GetNodeName(),
		IsMaster:   svc.IsMaster(),
		AuthKey:    svc.GetAuthKey(),
		MaxRunners: svc.GetMaxRunners(),
	}
}

func (svc *Service) GetNodeKey() (res string) {
	return svc.cfg.Key
}

func (svc *Service) GetNodeName() (res string) {
	return svc.cfg.Name
}

func (svc *Service) IsMaster() (res bool) {
	return svc.cfg.IsMaster
}

func (svc *Service) GetAuthKey() (res string) {
	return svc.cfg.AuthKey
}

func (svc *Service) GetMaxRunners() (res int) {
	return svc.cfg.MaxRunners
}

func (svc *Service) GetConfigPath() (path string) {
	return svc.path
}

func (svc *Service) SetConfigPath(path string) {
	svc.path = path
}

func NewNodeConfigService(opts ...Option) (svc2 interfaces.NodeConfigService, err error) {
	// cfg
	cfg := NewConfig(nil)

	// config service
	svc := &Service{
		cfg: cfg,
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// normalize config path
	cfgPath := svc.GetConfigPath()
	if cfgPath == "" || cfgPath == config.DefaultConfigPath {
		if viper.GetString("config.path") != "" {
			cfgPath = viper.GetString("config.path")
		} else {
			cfgPath = config.DefaultConfigPath
		}
	}
	svc.SetConfigPath(cfgPath)

	// init
	if err := svc.Init(); err != nil {
		return nil, err
	}

	return svc, nil
}

func ProvideConfigService(path string) func() (interfaces.NodeConfigService, error) {
	return func() (interfaces.NodeConfigService, error) {
		return NewNodeConfigService(WithConfigPath(path))
	}
}
