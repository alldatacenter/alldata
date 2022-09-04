apiVersion: v1
kind: Pod
metadata:
  name: {{ POD_NAME }}
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
    image: {{ KANIKO_IMAGE }}
    args: [{%- if SNAPSHOTMODE %}
            "--snapshotMode={{ SNAPSHOTMODE }}",
            {%- endif %}
            "--context=s3://{{ CONTEXT }}",
            {%- if BUILD_ARGS %}
                {%- for ARG in BUILD_ARGS %}
                        "{{ ARG }}",
                {%- endfor %}
            {%- endif %}
            {%- if CONFIG_KANIKO_FLAGS %}
                {%- for FLAG in CONFIG_KANIKO_FLAGS %}
                        "{{ FLAG }}",
                {%- endfor %}
            {%- endif %}
            {%- if DESTINATIONS %}
                {%- for DESTINATION in DESTINATIONS %}
                        "--destination={{ DESTINATION }}",
                {%- endfor %}
            {%- endif %}
            "--dockerfile={{ DOCKERFILE }}"
            ]
    env:
      - name: AWS_ACCESS_KEY_ID
        value: {{ STORAGE_ACCESS_KEY }}
      - name: AWS_SECRET_ACCESS_KEY
        value: {{ STORAGE_SECRET_KEY }}
      - name: AWS_REGION
        value: oss-cn-zhangjiakou
      - name: S3_ENDPOINT
        value: {{ STORAGE_ENDPOINT }}
      - name: S3_FORCE_PATH_STYLE
        value: "true"
    volumeMounts:
      - name: kaniko-secret
        mountPath: /kaniko/.docker
  restartPolicy: Never
  volumes:
    - name: kaniko-secret
      secret:
        secretName: {{ K8S_DOCKER_SECRET }}
        items:
          - key: .dockerconfigjson
            path: config.json