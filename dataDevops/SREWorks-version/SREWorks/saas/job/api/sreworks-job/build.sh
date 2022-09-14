sudo docker build . -f master-Dockerfile -t registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-job-master
sudo docker build . -f worker-Dockerfile -t registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-job-worker

sudo docker push registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-job-master
sudo docker push registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-job-worker