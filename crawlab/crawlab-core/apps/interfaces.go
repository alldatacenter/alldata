package apps

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/gin-gonic/gin"
	"net/http"
)

type App interface {
	Init()
	Start()
	Wait()
	Stop()
}

type ApiApp interface {
	App
	GetGinEngine() (engine *gin.Engine)
	GetHttpServer() (svr *http.Server)
	Ready() (ok bool)
}

type NodeApp interface {
	App
	interfaces.WithConfigPath
	SetGrpcAddress(address interfaces.Address)
}

type ServerApp interface {
	NodeApp
	GetApi() (api *Api)
	GetNodeService() (masterSvc interfaces.NodeService)
}

type DockerApp interface {
	App
	GetParent() (parent ServerApp)
	SetParent(parent ServerApp)
	Ready() (ok bool)
}
