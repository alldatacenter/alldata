package config

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/spf13/viper"
)

type PathService struct {
	cfgPath string
}

func (svc *PathService) GetConfigPath() (path string) {
	return svc.cfgPath
}

func (svc *PathService) SetConfigPath(path string) {
	svc.cfgPath = path
}

func NewConfigPathService() (svc interfaces.WithConfigPath) {
	svc = &PathService{}
	if viper.GetString("config.path") != "" {
		svc.SetConfigPath(viper.GetString("config.path"))
	} else {
		svc.SetConfigPath(DefaultConfigPath)
	}
	return svc
}
