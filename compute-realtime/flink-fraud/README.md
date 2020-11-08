# flink-fraud

# start-up
    git clone https://github.com/afedulov/fraud-detection-demo
    docker build -t fraud-app:latest -f webapp/webapp.Dockerfile webapp/
    docker build -t fraud-job:latest -f fraud-job/Dockerfile fraud-job/
    docker-compose -f docker-compose-local-job.yaml up
    

