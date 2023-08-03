package types

type UserMetricsResponse struct {
	IP          string      `json:"ip"`
	Source      string      `json:"source"`
	Task        string      `json:"task"`
	ClusterInfo ClusterInfo `json:"cluster_info"`
}

type ClusterInfo struct {
	KubernetesVersion string   `json:"kubernetesVersion"`
	Platform          string   `json:"platform"`
	NumPods           int      `json:"num_pods"`
	NumDeployments    int      `json:"num_deployments"`
	Pods              []string `json:"pods"`
	Deployments       []string `json:"deployments"`
}
