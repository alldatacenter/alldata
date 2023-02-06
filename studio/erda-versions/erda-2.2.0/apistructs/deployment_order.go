package apistructs

import "time"

const (
	TypeApplicationRelease = "APPLICATION_RELEASE"
	TypeProjectRelease     = "PROJECT_RELEASE"

	SourceDeployCenter   = "DEPLOY_CENTER"
	SourceDeployPipeline = "PIPELINE"

	DeployStatusWaitDeploy = "WAITDEPLOY"
)

type DeploymentOrderStatus string

type DeploymentOrderCreateRequest struct {
	Workspace string `json:"workspace"` // target workspace
	Id        string `json:"id"`        // auto generate if empty

	// deploy center or pipeline build
	ReleaseId string   `json:"releaseId"`
	Modes     []string `json:"modes"`

	// pipeline, application or project
	Type            string `json:"type"` // application_release or project_release
	ReleaseName     string `json:"releaseName"`
	ProjectId       uint64 `json:"projectId"`
	ApplicationName string `json:"applicationName"`

	Source              string `json:"source"` // default: DEPLOY_CENTER; value: PIPELINE
	AutoRun             bool   `json:"autoRun"`
	DeployWithoutBranch bool   `json:"deployWithoutBranch"`
	Operator            string
}

type DeploymentOrderCreateResponse struct {
	Id              string                                  `json:"id"`
	Name            string                                  `json:"name"`
	Type            string                                  `json:"type"`
	ReleaseId       string                                  `json:"releaseId"`
	ProjectId       uint64                                  `json:"projectId"`
	ProjectName     string                                  `json:"projectName"`
	ApplicationId   int64                                   `json:"applicationId"`
	ApplicationName string                                  `json:"applicationName"`
	Status          DeploymentOrderStatus                   `json:"status"`
	Deployments     map[string]*DeploymentCreateResponseDTO `json:"deployments,omitempty"`
	DeployList      string                                  `json:"deployList,omitempty"`
}

type DeploymentOrderDeployRequest struct {
	DeploymentOrderId string
	Operator          string
}

type DeploymentOrderListConditions struct {
	ProjectId        uint64
	MyApplicationIds []uint64
	Workspace        string
	Query            string
}

type DeploymentOrderDetail struct {
	DeploymentOrderItem
	ApplicationsInfo [][]*ApplicationInfo `json:"applicationsInfo"`
}

type ApplicationInfo struct {
	Id             uint64                `json:"id"`
	Name           string                `json:"name"`
	DeploymentId   uint64                `json:"deploymentId,omitempty"`
	Params         *DeploymentOrderParam `json:"params"`
	ReleaseId      string                `json:"releaseId,omitempty"`
	ReleaseVersion string                `json:"releaseVersion,omitempty"`
	Branch         string                `json:"branch,omitempty"`
	CommitId       string                `json:"commitId,omitempty"`
	PreCheckResult *PreCheckResult       `json:"preCheckResult,omitempty"`
	DiceYaml       string                `json:"diceYaml,omitempty"`
	Status         DeploymentStatus      `json:"status,omitempty"`
}

type PreCheckResult struct {
	Success     bool     `json:"success"`
	FailReasons []string `json:"failReasons,omitempty"`
}

type DeploymentOrderListData struct {
	Total int                    `json:"total"`
	List  []*DeploymentOrderItem `json:"list"`
}

type DeploymentOrderItem struct {
	ID                string                `json:"id"`
	Name              string                `json:"name"`
	ReleaseInfo       *ReleaseInfo          `json:"releaseInfo"`
	Type              string                `json:"type,omitempty"`
	ApplicationStatus string                `json:"applicationStatus,omitempty"`
	Workspace         string                `json:"workspace"`
	BatchSize         uint64                `json:"batchSize"`
	CurrentBatch      uint64                `json:"currentBatch"`
	Status            DeploymentOrderStatus `json:"status,omitempty"`
	Operator          string                `json:"operator,omitempty"`
	DeployList        string                `json:"deployList,omitempty"`
	CreatedAt         time.Time             `json:"createdAt"`
	UpdatedAt         time.Time             `json:"updatedAt"`
	StartedAt         *time.Time            `json:"startedAt,omitempty"`
}

type ReleaseInfo struct {
	Id        string    `json:"id"`
	Version   string    `json:"version,omitempty"`
	Type      string    `json:"type"`
	Creator   string    `json:"creator,omitempty"`
	CreatedAt time.Time `json:"createdAt,omitempty"`
	UpdatedAt time.Time `json:"updatedAt,omitempty"`
}

type DeploymentOrderParam []*DeploymentOrderParamData

type DeploymentOrderParamData struct {
	Key     string `json:"key"`
	Value   string `json:"value"`
	Encrypt bool   `json:"encrypt"`
	Type    string `json:"type,omitempty"`
	Comment string `json:"comment"`
}

type DeploymentOrderStatusMap map[string]DeploymentOrderStatusItem

type DeploymentOrderStatusItem struct {
	AppID            uint64           `json:"appId"`
	DeploymentID     uint64           `json:"deploymentId"`
	DeploymentStatus DeploymentStatus `json:"deploymentStatus"`
	RuntimeID        uint64           `json:"runtimeId"`
}

type DeploymentOrderCancelRequest struct {
	DeploymentOrderId string
	Operator          string
	Force             bool `json:"force"`
}
