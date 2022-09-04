# KIND

An example of configuration for deploying Elasticsearch chart on [Kind][].

You can use `make install` to deploy it.

Note that this configuration should be used for test only and isn't recommended
for production.

## Current issue

There is currently an [kind issue][] with mount points created from PVCs not writeable by non-root users.
[kubernetes-sigs/kind#1157][] should fix it in a future release.

Meanwhile, the workaround is to install manually [Rancher Local Path Provisioner][] and use `local-path` storage class for Elasticsearch volumes (see [Makefile][] instructions).

[Kind]: https://kind.sigs.k8s.io/
[Kind issue]: https://github.com/kubernetes-sigs/kind/issues/830
[Kubernetes-sigs/kind#1157]: https://github.com/kubernetes-sigs/kind/pull/1157
[Rancher Local Path Provisioner]: https://github.com/rancher/local-path-provisioner
[Makefile]: ./Makefile#L5