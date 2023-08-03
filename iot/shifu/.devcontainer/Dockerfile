# See here for image contents: https://github.com/Edgenesis/devcontainer-go

ARG VARIANT="latest"
FROM edgehub/devcontainer-go:${VARIANT}

RUN echo "source /etc/bash_completion" >> "/root/.bashrc"
RUN echo "source <(kubectl completion bash)" >> "/root/.bashrc"
