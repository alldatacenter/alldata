/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package extension

import corev1 "k8s.io/api/core/v1"

type QoSClass string

const (
	QoSLSE    QoSClass = "LSE"
	QoSLSR    QoSClass = "LSR"
	QoSLS     QoSClass = "LS"
	QoSBE     QoSClass = "BE"
	QoSSystem QoSClass = "SYSTEM"
	QoSNone   QoSClass = ""
)

func GetPodQoSClass(pod *corev1.Pod) QoSClass {
	if pod == nil || pod.Labels == nil {
		return QoSNone
	}
	return GetQoSClassByAttrs(pod.Labels, pod.Annotations)
}

func GetQoSClassByAttrs(labels, annotations map[string]string) QoSClass {
	// annotations are for old format adaption reason
	if q, exist := labels[LabelPodQoS]; exist {
		return GetPodQoSClassByName(q)
	}
	return QoSNone
}

func GetPodQoSClassByName(qos string) QoSClass {
	q := QoSClass(qos)

	switch q {
	case QoSLSE, QoSLSR, QoSLS, QoSBE, QoSSystem:
		return q
	}

	return QoSNone
}
