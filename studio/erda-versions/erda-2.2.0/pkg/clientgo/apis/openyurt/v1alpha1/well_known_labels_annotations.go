/*
Copyright 2020 The OpenYurt Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@CHANGELOG
OpenYurt Authors:
change some const value
*/

package v1alpha1

// UnitedDeployment related labels
const (
	// ControllerRevisionHashLabelKey is used to record the controller revision of current resource.
	ControllerRevisionHashLabelKey = "apps.openyurt.io/controller-revision-hash"

	// PoolNameLabelKey is used to record the name of current pool.
	PoolNameLabelKey = "apps.openyurt.io/pool-name"

	// SpecifiedDeleteKey indicates this object should be deleted, and the value could be the deletion option.
	SpecifiedDeleteKey = "apps.openyurt.io/specified-delete"
)

// NodePool related labels and annotations
const (
	// LabelDesiredNodePool indicates which nodepool the node want to join
	LabelDesiredNodePool = "apps.openyurt.io/desired-nodepool"

	// LabelCurrentNodePool indicates which nodepool the node is currently
	// belonging to
	LabelCurrentNodePool = "apps.openyurt.io/nodepool"

	AnnotationPrevAttrs = "nodepool.openyurt.io/previous-attributes"

	// Note !!!!
	// Can not change this const name , because go build -ldflags will change this values
	// @kadisi
	// LabelEdgeWorker indicates if the node is an edge node
	LabelEdgeWorker = "alibabacloud.com/is-edge-worker"

	// DefaultCloudNodePoolName defines the name of the default cloud nodepool
	DefaultCloudNodePoolName = "default-nodepool"

	// DefaultEdgeNodePoolName defines the name of the default edge nodepool
	DefaultEdgeNodePoolName = "default-edge-nodepool"

	// ServiceTopologyKey is the toplogy key that will be attached to node,
	// the value will be the name of the nodepool
	ServiceTopologyKey = "topology.kubernetes.io/zone"
)
