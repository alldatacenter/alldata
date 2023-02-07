package controllers

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/stats"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.uber.org/dig"
	"net/http"
)

var StatsController ActionController

func getStatsActions() []Action {
	statsCtx := newStatsContext()
	return []Action{
		{
			Method:      http.MethodGet,
			Path:        "/overview",
			HandlerFunc: statsCtx.getOverview,
		},
		{
			Method:      http.MethodGet,
			Path:        "/daily",
			HandlerFunc: statsCtx.getDaily,
		},
		{
			Method:      http.MethodGet,
			Path:        "/tasks",
			HandlerFunc: statsCtx.getTasks,
		},
	}
}

type statsContext struct {
	statsSvc interfaces.StatsService
}

func (svc *statsContext) getOverview(c *gin.Context) {
	data, err := svc.statsSvc.GetOverviewStats(bson.M{})
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	HandleSuccessWithData(c, data)
}

func (svc *statsContext) getDaily(c *gin.Context) {
	data, err := svc.statsSvc.GetDailyStats(bson.M{})
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	HandleSuccessWithData(c, data)
}

func (svc *statsContext) getTasks(c *gin.Context) {
	data, err := svc.statsSvc.GetTaskStats(bson.M{})
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	HandleSuccessWithData(c, data)
}

func newStatsContext() *statsContext {
	// context
	ctx := &statsContext{}

	// dependency injection
	c := dig.New()
	if err := c.Provide(service.NewService); err != nil {
		panic(err)
	}
	if err := c.Provide(stats.ProvideStatsService()); err != nil {
		panic(err)
	}
	if err := c.Invoke(func(
		statsSvc interfaces.StatsService,
	) {
		ctx.statsSvc = statsSvc
	}); err != nil {
		panic(err)
	}

	return ctx
}
