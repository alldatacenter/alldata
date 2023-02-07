package controllers

import (
	"github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/grpc/server"
	"github.com/crawlab-team/crawlab-core/interfaces"
	delegate2 "github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/plugin"
	"github.com/crawlab-team/crawlab-db/mongo"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	"github.com/gin-gonic/gin"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	mongo2 "go.mongodb.org/mongo-driver/mongo"
	"go.uber.org/dig"
	"net/http"
)

var PluginController *pluginController

func getPluginActions() []Action {
	pluginCtx := newPluginContext()
	return []Action{
		{
			Method:      http.MethodPost,
			Path:        "/:id/start",
			HandlerFunc: pluginCtx.start,
		},
		{
			Method:      http.MethodPost,
			Path:        "/:id/stop",
			HandlerFunc: pluginCtx.stop,
		},
		{
			Method:      http.MethodGet,
			Path:        "/public",
			HandlerFunc: pluginCtx.getPublicPluginList,
		},
		{
			Method:      http.MethodGet,
			Path:        "/public/info",
			HandlerFunc: pluginCtx.getPublicPluginInfo,
		},
	}
}

type pluginController struct {
	ListActionControllerDelegate
	d   ListActionControllerDelegate
	ctx *pluginContext
}

func (ctr *pluginController) Post(c *gin.Context) {
	s, err := ctr.ctx.post(c)
	if err != nil {
		return
	}
	HandleSuccessWithData(c, s)
}

func (ctr *pluginController) Delete(c *gin.Context) {
	_, err := ctr.ctx.delete(c)
	if err != nil {
		return
	}
	HandleSuccess(c)
}

func (ctr *pluginController) GetList(c *gin.Context) {
	withStatus := c.Query("status")
	if withStatus == "" {
		ctr.d.GetList(c)
		return
	}
	ctr.ctx.getListWithStatus(c)
}

type pluginContext struct {
	modelSvc             service.ModelService
	modelPluginSvc       interfaces.ModelBaseService
	modelPluginStatusSvc interfaces.ModelBaseService
	pluginSvc            interfaces.PluginService
	svr                  interfaces.GrpcServer
}

var _pluginCtx *pluginContext

func (ctx *pluginContext) start(c *gin.Context) {
	// id
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// plugin
	p, err := ctx.modelSvc.GetPluginById(id)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// start (master)
	go func() {
		if err := ctx.pluginSvc.StartPlugin(p.GetId()); err != nil {
			trace.PrintError(err)
			return
		}
	}()

	// start (workers)
	if p.DeployMode == constants.PluginDeployModeAll {
		go func() {
			// active worker nodes
			nodes, err := ctx._getWorkerNodes()
			if err != nil {
				return
			}

			// start on each worker node
			for _, n := range nodes {
				_ = ctx.svr.SendStreamMessageWithData("node:"+n.Key, grpc.StreamMessageCode_START_PLUGIN, p)
			}
		}()
	}

	HandleSuccess(c)
}

func (ctx *pluginContext) stop(c *gin.Context) {
	// id
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// plugin
	p, err := ctx.modelSvc.GetPluginById(id)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// stop (master)
	go func() {
		if err := ctx.pluginSvc.StopPlugin(p.GetId()); err != nil {
			trace.PrintError(err)
			return
		}
	}()

	// stop (workers)
	if p.DeployMode == constants.PluginDeployModeAll {
		go func() {
			// active worker nodes
			nodes, err := ctx._getWorkerNodes()
			if err != nil {
				return
			}

			// stop on each worker node
			for _, n := range nodes {
				_ = ctx.svr.SendStreamMessageWithData("node:"+n.Key, grpc.StreamMessageCode_STOP_PLUGIN, p)
			}
		}()
	}

	HandleSuccess(c)
}

func (ctx *pluginContext) post(c *gin.Context) (p *models.Plugin, err error) {
	// bind
	p = &models.Plugin{}
	if err := c.ShouldBindJSON(&p); err != nil {
		HandleErrorBadRequest(c, err)
		return nil, err
	}

	// TODO: check if exists

	// add
	if err := delegate2.NewModelDelegate(p, GetUserFromContext(c)).Add(); err != nil {
		HandleErrorInternalServerError(c, err)
		return nil, err
	}

	// install
	go func() {
		// install (master)
		if err := ctx.pluginSvc.InstallPlugin(p.GetId()); err != nil {
			trace.PrintError(err)
			return
		}

		// updated plugin
		p, err = ctx.modelSvc.GetPluginById(p.GetId())
		if err != nil {
			trace.PrintError(err)
			return
		}

		// install on worker nodes if deploy mode is "all"
		//if p.DeployMode == constants.PluginDeployModeAll {
		//	// active worker nodes
		//	nodes, err := ctx._getWorkerNodes()
		//	if err != nil {
		//		return
		//	}
		//
		//	// install on each worker node
		//	for _, n := range nodes {
		//		if err := ctx.svr.SendStreamMessageWithData("node:"+n.Key, grpc.StreamMessageCode_INSTALL_PLUGIN, p); err != nil {
		//			trace.PrintError(err)
		//		}
		//	}
		//}
	}()

	return p, nil
}

func (ctx *pluginContext) delete(c *gin.Context) (p *models.Plugin, err error) {
	// id
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// plugin
	p, err = ctx.modelSvc.GetPluginById(id)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// uninstall (master)
	go func() {
		if err := ctx.pluginSvc.UninstallPlugin(p.GetId()); err != nil {
			trace.PrintError(err)
			return
		}
	}()

	// uninstall (workers)
	if p.DeployMode == constants.PluginDeployModeAll {
		go func() {
			// active worker nodes
			nodes, err := ctx._getWorkerNodes()
			if err != nil {
				return
			}

			// uninstall on each worker node
			for _, n := range nodes {
				_ = ctx.svr.SendStreamMessageWithData("node:"+n.Key, grpc.StreamMessageCode_UNINSTALL_PLUGIN, p)
			}
		}()
	}

	// delete plugin
	if err := delegate2.NewModelDelegate(p, GetUserFromContext(c)).Delete(); err != nil {
		HandleErrorInternalServerError(c, err)
		return nil, err
	}

	// delete plugin status
	if err := ctx.modelPluginStatusSvc.Delete(bson.M{"plugin_id": p.Id}); err != nil {
		HandleErrorInternalServerError(c, err)
		return nil, err
	}

	return p, nil
}

func (ctx *pluginContext) getListWithStatus(c *gin.Context) {
	// params
	all := MustGetFilterAll(c)
	pagination := MustGetPagination(c)
	query := MustGetFilterQuery(c)
	sort := MustGetSortOption(c)

	// options
	opts := &mongo.FindOptions{
		Sort: sort,
	}
	if !all {
		opts.Skip = pagination.Size * (pagination.Page - 1)
		opts.Limit = pagination.Size
	}

	// get list
	list, err := ctx.modelPluginSvc.GetList(query, opts)
	if err != nil {
		if err == mongo2.ErrNoDocuments {
			HandleErrorNotFound(c, err)
		} else {
			HandleErrorInternalServerError(c, err)
		}
		return
	}

	// check empty list
	if len(list.GetModels()) == 0 {
		HandleSuccessWithListData(c, nil, 0)
		return
	}

	// ids
	var ids []primitive.ObjectID
	for _, d := range list.GetModels() {
		p := d.(interfaces.Model)
		ids = append(ids, p.GetId())
	}

	// total count
	total, err := ctx.modelPluginSvc.Count(query)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// plugin status list
	query = bson.M{
		"plugin_id": bson.M{
			"$in": ids,
		},
	}
	psList, err := ctx.modelSvc.GetPluginStatusList(query, nil)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// node ids
	var nodeIds []primitive.ObjectID
	for _, ps := range psList {
		nodeIds = append(nodeIds, ps.NodeId)
	}

	// nodes
	var nodes []models.Node
	if nodeIds != nil {
		// nodes
		query = bson.M{
			"_id": bson.M{
				"$in": nodeIds,
			},
		}
		nodes, err = ctx.modelSvc.GetNodeList(query, nil)
		if err != nil {
			HandleErrorInternalServerError(c, err)
			return
		}
	}

	// nodes dict
	nodesDict := map[primitive.ObjectID]models.Node{}
	for _, n := range nodes {
		nodesDict[n.Id] = n
	}

	// plugin status dict
	dict := map[primitive.ObjectID][]models.PluginStatus{}
	for _, ps := range psList {
		n, ok := nodesDict[ps.NodeId]
		if ok {
			ps.Node = &n
		}
		dict[ps.GetPluginId()] = append(dict[ps.GetPluginId()], ps)
	}

	// data
	var data []interface{}
	for _, d := range list.GetModels() {
		p := d.(*models.Plugin)
		s, ok := dict[p.GetId()]
		if ok {
			p.Status = s
		}
		data = append(data, *p)
	}

	// response
	HandleSuccessWithListData(c, data, total)
}

func (ctx *pluginContext) getPublicPluginList(c *gin.Context) {
	data, err := ctx.pluginSvc.GetPublicPluginList()
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	HandleSuccessWithData(c, data)
}

func (ctx *pluginContext) getPublicPluginInfo(c *gin.Context) {
	fullName := c.Query("full_name")
	data, err := ctx.pluginSvc.GetPublicPluginInfo(fullName)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	HandleSuccessWithData(c, data)
}

func (ctx *pluginContext) _getWorkerNodes() (nodes []models.Node, err error) {
	// active worker nodes
	return ctx.modelSvc.GetNodeList(bson.M{
		"is_master": false,
		"active":    true,
	}, nil)
}

func newPluginContext() *pluginContext {
	if _pluginCtx != nil {
		return _pluginCtx
	}

	// context
	ctx := &pluginContext{}

	// config path
	configPath := viper.GetString("config.path")
	if configPath == "" {
		configPath = config.DefaultConfigPath
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(service.GetService); err != nil {
		panic(err)
	}
	if err := c.Provide(plugin.ProvideGetPluginService(configPath)); err != nil {
		panic(err)
	}
	if err := c.Provide(server.ProvideGetServer(configPath)); err != nil {
		panic(err)
	}
	if err := c.Invoke(func(
		modelSvc service.ModelService,
		pluginSvc interfaces.PluginService,
		svr interfaces.GrpcServer,
	) {
		ctx.modelSvc = modelSvc
		ctx.pluginSvc = pluginSvc
		ctx.svr = svr
	}); err != nil {
		panic(err)
	}

	// model plugin service
	ctx.modelPluginSvc = ctx.modelSvc.GetBaseService(interfaces.ModelIdPlugin)

	// model plugin status service
	ctx.modelPluginStatusSvc = ctx.modelSvc.GetBaseService(interfaces.ModelIdPluginStatus)

	_pluginCtx = ctx

	return ctx
}

func newPluginController() *pluginController {
	actions := getPluginActions()
	modelSvc, err := service.GetService()
	if err != nil {
		panic(err)
	}

	ctr := NewListPostActionControllerDelegate(ControllerIdPlugin, modelSvc.GetBaseService(interfaces.ModelIdPlugin), actions)
	d := NewListPostActionControllerDelegate(ControllerIdPlugin, modelSvc.GetBaseService(interfaces.ModelIdPlugin), actions)
	ctx := newPluginContext()

	return &pluginController{
		ListActionControllerDelegate: *ctr,
		d:                            *d,
		ctx:                          ctx,
	}
}
