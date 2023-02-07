package services

import (
	"github.com/crawlab-team/crawlab-core/env/deps/constants"
	mongo2 "github.com/crawlab-team/crawlab-db/mongo"
)

type SettingService struct {
	parent *Service
	col    *mongo2.Col // dependency settings
}

func (svc *SettingService) Col() *mongo2.Col {
	return svc.col
}

func (svc *SettingService) Init() {
}

func NewSettingService(parent *Service) (svc *SettingService) {
	svc = &SettingService{
		parent: parent,
		col:    mongo2.GetMongoCol(constants.DependencySettingsColName),
	}

	return svc
}
