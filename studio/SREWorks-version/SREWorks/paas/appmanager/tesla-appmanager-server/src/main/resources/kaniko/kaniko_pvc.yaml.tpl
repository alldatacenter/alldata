apiVersion: v1
kind: Pod
metadata:
  name: {{ podName }}
spec:
  tolerations:
    - effect: NoSchedule
      key: sigma.ali/is-ecs
      operator: Exists
    - effect: NoSchedule
      key: sigma.ali/resource-pool
      operator: Exists
  containers:
  - name: kaniko
    image: reg.docker.alibaba-inc.com/sw/kaniko:1.5.1
    args: ["--dockerfile={{ dockerfile }}",
            "--context={{ context }}",
            {%- if targetFilePath %}
            "{{ targetFilePath }}",
            {%- endif %}
            {%- if buildArgs %}
            {%- for arg in buildArgs %}
                    "{{ arg }}",
            {%- endfor %}
            {%- endif %}
            "--destination={{ destination }}",
            {%- if pushArgs %}
            "{{ pushArgs }}"
            {%- endif %}
            ]
    volumeMounts:
      - name: kaniko-secret
        mountPath: /kaniko/.docker
      - name: dockerfile-storage
        mountPath: /workspace
  restartPolicy: Never
  volumes:
    - name: kaniko-secret
      secret:
        secretName: server-docker-secret
        items:
          - key: .dockerconfigjson
            path: config.json
    - name: dockerfile-storage
      persistentVolumeClaim:
        claimName: appmanager-kaniko