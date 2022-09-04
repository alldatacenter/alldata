package lib

import (
	"context"
	"crypto/tls"
	"fmt"
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"golang.org/x/oauth2"
	"net/http"
	"net/url"
)

// NewClient 创建一个与 appmanager 交互的 client
func NewClient(endpoint, clientId, clientSecret, username, password string) *http.Client {
	if clientId == "" && clientSecret == "" && username == "" && password == "" {
		return http.DefaultClient
	}

	conf := &oauth2.Config{
		ClientID:     clientId,
		ClientSecret: clientSecret,
		RedirectURL:  "http://appmanager",
		Scopes:       []string{"all"},
		Endpoint: oauth2.Endpoint{
			AuthURL:  endpoint + "/oauth/auth",
			TokenURL: endpoint + "/oauth/token",
		},
	}
	token, err := conf.PasswordCredentialsToken(context.Background(), username, password)
	cobra.CheckErr(err)
	if !token.Valid() {
		fmt.Printf("auth token invalid. got: %#v", token)
	}
	log.Debug().Str("token", token.AccessToken).Msgf("access token has generated")
	return conf.Client(context.Background(), token)
}

// NewProxyClient 创建一个与 appmanager 交互的 client（Proxy）
func NewProxyClient(endpoint, clientId, clientSecret, username, password, proxy string) *http.Client {
	conf := &oauth2.Config{
		ClientID:     clientId,
		ClientSecret: clientSecret,
		RedirectURL:  "http://appmanager",
		Scopes:       []string{"all"},
		Endpoint: oauth2.Endpoint{
			AuthURL:  endpoint + "/oauth/auth",
			TokenURL: endpoint + "/oauth/token",
		},
	}
	token, err := conf.PasswordCredentialsToken(context.Background(), username, password)
	cobra.CheckErr(err)
	if !token.Valid() {
		fmt.Printf("auth token invalid. got: %#v", token)
	}
	log.Debug().Str("token", token.AccessToken).Msgf("access token has generated")

	// 设置代理并初始化 Client
	proxyUrl, err := url.Parse(proxy)
	cobra.CheckErr(err)
	tr := &http.Transport{
		Proxy: http.ProxyURL(proxyUrl),
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	return &http.Client{
		Transport: &oauth2.Transport{
			Base:   tr,
			Source: oauth2.ReuseTokenSource(nil, conf.TokenSource(context.Background(), token)),
		},
	}
}
