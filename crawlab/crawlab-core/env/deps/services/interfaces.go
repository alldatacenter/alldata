package services

import (
	entity2 "github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/env/deps/entity"
	"github.com/crawlab-team/crawlab-core/env/deps/models"
)

type DependencyService interface {
	Init()
	GetRepoList(query string, pagination *entity2.Pagination) (deps []models.Dependency, total int, err error)
	GetDependencies(params entity.UpdateParams) (deps []models.Dependency, err error)
	InstallDependencies(params entity.InstallParams) (err error)
	UninstallDependencies(params entity.UninstallParams) (err error)
	GetLatestVersion(dep models.Dependency) (v string, err error)
}
