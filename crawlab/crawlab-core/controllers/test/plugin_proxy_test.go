package test

import (
	"context"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/controllers"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/require"
	"net/http"
	"testing"
	"time"
)

func TestPluginProxyController_Http(t *testing.T) {
	T.Setup(t)
	e := T.NewExpect(t)

	// plugin model
	p := &models.Plugin{
		Name:     "test-plugin",
		Proto:    constants.PluginProtoHttp,
		Endpoint: "http://localhost:19999",
	}
	err := delegate.NewModelDelegate(p).Add()
	require.Nil(t, err)

	// plugin app
	app := gin.New()
	app.POST("*path", func(c *gin.Context) {
		path := c.Param("path")
		var p models.Plugin
		if err := c.ShouldBindJSON(&p); err != nil {
			controllers.HandleErrorInternalServerError(c, err)
			return
		}
		controllers.HandleSuccessWithData(c, map[string]interface{}{
			"path": path,
			"p":    p,
		})
	})
	svr := &http.Server{
		Handler: app,
		Addr:    "0.0.0.0:19999",
	}
	go svr.ListenAndServe()
	time.Sleep(1 * time.Second)

	req := T.WithAuth(e.POST("/plugin-proxy/test-plugin").WithJSON(p))
	res := req.Expect().Status(http.StatusOK).JSON().Object()
	res.Path("$.data").NotNull()
	res.Path("$.data.p").NotNull()
	res.Path("$.data.p.name").Equal("test-plugin")
	res.Path("$.data.p.proto").Equal(constants.PluginProtoHttp)
	res.Path("$.data.p.endpoint").Equal("http://localhost:19999")
	require.Nil(t, err)

	req = T.WithAuth(e.POST("/plugin-proxy/test-plugin/test-path").WithJSON(p))
	res = req.Expect().Status(http.StatusOK).JSON().Object()
	res.Path("$.data.path").Equal("/test-path")
	require.Nil(t, err)

	// shutdown app
	_ = svr.Shutdown(context.Background())
}
