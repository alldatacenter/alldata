package controllers

import (
	"github.com/gin-gonic/gin"
	"net/http"
)

func getI18n(c *gin.Context) {
	// TODO: implement me
	panic("not implemented")
}

func getI18nActions() []Action {
	return []Action{
		{
			Path:        "",
			Method:      http.MethodGet,
			HandlerFunc: getI18n,
		},
	}
}

var I18nController ActionController
