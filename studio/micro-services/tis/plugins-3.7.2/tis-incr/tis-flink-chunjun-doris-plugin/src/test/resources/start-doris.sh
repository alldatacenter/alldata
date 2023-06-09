# docker-compose   -f ./compose-doris-test.yml stop
docker-compose   -f ./compose-doris-test.yml up -d
sleep 10
mysql -h127.0.0.1 -P9031 -uroot < ./doris-init.sql
