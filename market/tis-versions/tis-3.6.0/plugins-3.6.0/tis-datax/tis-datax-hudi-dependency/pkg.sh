rm -rf ./tis-datax-hudi-dependency

mvn clean package -Dappname=all -Dmaven.test.skip=true

tar xvf tis-datax-hudi-dependency.tar.gz
rm -rf ./tis-datax-hudi-dependency.tar.gz
