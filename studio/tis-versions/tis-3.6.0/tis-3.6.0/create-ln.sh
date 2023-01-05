# https://blog.csdn.net/qq_38425719/article/details/102515854
# relevant java code in CreateSoftLink.java
rm -rf /opt/data/tis/libs/plugins/*
#rm -f /opt/data/tis/libs/plugins/*.tpi


for f in `find /Users/mozhenghua/j2ee_solution/project/plugins  -name '*.tpi' -print`
do
   echo " ln -s $f "
   ln -s $f /opt/data/tis/libs/plugins/${f##*/}
done ;

#for tis-scala-compiler-dependencies
#rm -f /opt/data/tis/libs/tis-scala-compiler-dependencies/*
#cd ./tis-scala-compiler-dependencies
#mvn dependency:copy-dependencies
#mkdir -p /opt/data/tis/libs/tis-scala-compiler-dependencies
#ln -s /Users/mozhenghua/j2ee_solution/project/tis-solr/tis-scala-compiler-dependencies/target/dependency/* /opt/data/tis/libs/tis-scala-compiler-dependencies


#/Users/mozhenghua/Desktop/j2ee_solution/project/tis-ibatis/target/dependency
#rm -f /opt/data/tis/libs/tis-ibatis/*
#cd /Users/mozhenghua/Desktop/j2ee_solution/project/tis-ibatis
#mvn clean package -Dmaven.test.skip=true
#mvn dependency:copy-dependencies
#mkdir -p /opt/data/tis/libs/tis-ibatis
#ln -s /Users/mozhenghua/Desktop/j2ee_solution/project/tis-ibatis/target/dependency/* /opt/data/tis/libs/tis-ibatis
#ln -s /Users/mozhenghua/Desktop/j2ee_solution/project/tis-ibatis/target/*.jar /opt/data/tis/libs/tis-ibatis

