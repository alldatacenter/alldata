rm -rf ./tis-scala-compiler-dependencies*

mvn package -Dappname=tis-scala-compiler-dependencies

tar xvf tis-scala-compiler-dependencies.tar.gz
