package utils

import (
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/models/models"
	vcs "github.com/crawlab-team/crawlab-vcs"
)

func InitGitClientAuth(g *models.Git, gitClient *vcs.GitClient) {
	// set auth
	switch g.AuthType {
	case constants.GitAuthTypeHttp:
		gitClient.SetAuthType(vcs.GitAuthTypeHTTP)
		gitClient.SetUsername(g.Username)
		gitClient.SetPassword(g.Password)
	case constants.GitAuthTypeSsh:
		gitClient.SetAuthType(vcs.GitAuthTypeSSH)
		gitClient.SetUsername(g.Username)
		gitClient.SetPrivateKey(g.Password)
	}
}
