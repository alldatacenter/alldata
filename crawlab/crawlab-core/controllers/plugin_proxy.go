package controllers

import (
	"errors"
	"fmt"
	"github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/constants"
	errors2 "github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/grpc/client"
	"github.com/crawlab-team/crawlab-core/grpc/server"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/gin-gonic/gin"
	"github.com/imroc/req"
	"go.uber.org/dig"
	"io/ioutil"
	"net/http"
	"strings"
)

var PluginProxyController ActionController

func getPluginProxyActions() []Action {
	pluginDoCtx := newPluginProxyContext()
	return []Action{
		{
			Path:        "/:name/*path",
			Method:      http.MethodGet,
			HandlerFunc: pluginDoCtx.do,
		},
		{
			Path:        "/:name",
			Method:      http.MethodPost,
			HandlerFunc: pluginDoCtx.do,
		},
		{
			Path:        "/:name/*path",
			Method:      http.MethodPost,
			HandlerFunc: pluginDoCtx.do,
		},
	}
}

var _pluginProxyCtx *pluginProxyContext

type pluginProxyContext struct {
	c        interfaces.GrpcClient
	modelSvc service.ModelService
	server   interfaces.GrpcServer
}

func (ctx *pluginProxyContext) do(c *gin.Context) {
	// plugin name
	name := c.Param("name")

	// plugin
	p, err := ctx.modelSvc.GetPluginByName(name)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// body data
	var data []byte
	if c.Request.Method != http.MethodGet {
		data, err = ioutil.ReadAll(c.Request.Body)
		if err != nil {
			HandleErrorInternalServerError(c, err)
			return
		}
	}

	switch p.Proto {
	case constants.PluginProtoHttp:
		ctx._doHttp(c, p, data)
	case constants.PluginProtoGrpc:
		ctx._doGrpc(c, p, data)
	default:
		HandleErrorInternalServerError(c, errors.New(fmt.Sprintf("%s is not implemented", p.Proto)))
	}
}

func (ctx *pluginProxyContext) _doHttp(c *gin.Context, p *models.Plugin, data []byte) {
	path := c.Param("path")
	query := ""
	if c.Request.URL.RawQuery == "" {
		query = ""
	} else {
		query = "?" + c.Request.URL.RawQuery
	}
	var url string
	if !strings.HasPrefix(p.Endpoint, "http://") && !strings.HasPrefix(p.Endpoint, "https://") {
		url = fmt.Sprintf("http://%s%s%s", p.Endpoint, path, query)
	} else {
		url = fmt.Sprintf("%s%s%s", p.Endpoint, path, query)
	}
	res, err := req.Do(c.Request.Method, url, req.HeaderFromStruct(c.Request.Header), data)
	if err != nil {
		if res == nil {
			HandleErrorInternalServerError(c, errors2.ErrorControllerEmptyResponse)
			return
		}
		HandleError(res.Response().StatusCode, c, err)
		return
	}
	contentType := res.Response().Header.Get("Content-Type")
	c.Data(res.Response().StatusCode, contentType, res.Bytes())
	//c.Data(http.StatusOK, constants.HttpContentTypeApplicationJson, res.Bytes())
}

func (ctx *pluginProxyContext) _doGrpc(c *gin.Context, p *models.Plugin, data []byte) {
	// content
	data, err := ioutil.ReadAll(c.Request.Body)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// subscribe
	// TODO: node key
	sub, err := ctx.server.GetSubscribe("plugin:" + p.Name)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// stream message
	msg := &grpc.StreamMessage{Data: data}

	// send
	_ = sub.GetStream().Send(msg)
}

func newPluginProxyContext() *pluginProxyContext {
	if _pluginProxyCtx != nil {
		return _pluginProxyCtx
	}

	ctx := &pluginProxyContext{}

	// dependency injection
	c := dig.New()
	if err := c.Provide(client.ProvideGetClient(config.DefaultConfigPath)); err != nil {
		panic(err)
	}
	if err := c.Provide(service.NewService); err != nil {
		panic(err)
	}
	if err := c.Provide(server.ProvideGetServer(config.DefaultConfigPath)); err != nil {
		panic(err)
	}
	if err := c.Invoke(func(
		c interfaces.GrpcClient,
		modelSvc service.ModelService,
		server interfaces.GrpcServer,
	) {
		ctx.c = c
		ctx.modelSvc = modelSvc
		ctx.server = server
	}); err != nil {
		panic(err)
	}

	_pluginProxyCtx = ctx

	return ctx
}
