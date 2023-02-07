package entity

import (
	"encoding/json"
	"github.com/crawlab-team/crawlab-core/constants"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type PluginUIComponent struct {
	Name        string   `json:"name" bson:"name"`
	Title       string   `json:"title" bson:"title"`
	Src         string   `json:"src" bson:"src"`
	Type        string   `json:"type" bson:"type"`
	Path        string   `json:"path" bson:"path"`
	ParentPaths []string `json:"parent_paths" bson:"parent_paths"`
}

type PluginUINav struct {
	Path     string        `json:"path" bson:"path"`
	Title    string        `json:"title" bson:"title"`
	Icon     []string      `json:"icon" bson:"icon"`
	Children []PluginUINav `json:"children,omitempty" bson:"children,omitempty"`
}

type PluginUIAsset struct {
	Path string `json:"path" bson:"path"`
	Type string `json:"type" bson:"type"`
}

type PluginEventKey struct {
	Include string `json:"include" bson:"include"`
	Exclude string `json:"exclude" bson:"exclude"`
}

type PluginSubStatus struct {
	NodeId primitive.ObjectID `json:"node_id" bson:"node_id"`
	Status string             `json:"status" bson:"status"`
	Pid    int                `json:"pid" bson:"pid"`
	Error  string             `json:"error" bson:"error"`
}

type PluginSetting struct {
	PluginBaseUrl   string `json:"plugin_base_url"`
	GithubPublicOrg string `json:"github_public_org"`
	RepoPrefix      string `json:"repo_prefix"`
	GoProxy         string `json:"go_proxy"`
}

func (ps *PluginSetting) Value() (value bson.M) {
	data, _ := json.Marshal(ps)
	_ = json.Unmarshal(data, &value)
	return value
}

func NewPluginSetting(value bson.M) (ps PluginSetting) {
	res, ok := value[constants.SettingPluginBaseUrl]
	if ok {
		ps.PluginBaseUrl, _ = res.(string)
	}
	res, ok = value[constants.SettingPluginGithubPublicOrg]
	if ok {
		ps.GithubPublicOrg, _ = res.(string)
	}
	res, ok = value[constants.SettingPluginRepoPrefix]
	if ok {
		ps.RepoPrefix, _ = res.(string)
	}
	return ps
}
