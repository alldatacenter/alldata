# flink-fraud

# start-up
    cd ./flink-fraud 
    and run:
        docker build -t fraud-app:latest -f webapp/webapp.Dockerfile webapp/
        docker build -t fraud-job:latest -f fraud-job/Dockerfile fraud-job/
        docker-compose -f docker-compose-local-job.yaml up
    

