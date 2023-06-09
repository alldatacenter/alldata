After finish building ByConity in development environment, in the build directory execute this command to run unit test
```
src/unit_tests_dbms --output-on-failure
```

You can run individual test case by passing in filter, for example

```
src/unit_tests_dbms --output-on-failure --gtest_filter='backgroundjob*'
```


To run CI test, after starting ByConity in development env, look at the below [script](https://github.com/ByConity/ByConity/tree/master/ci_scripts/run_ci_in_development_env.sh), edit it so that the enviroment variables in the script are correctly set. Then run it,
```
ci_scripts/run_ci_in_development_env.sh
```

You can run individual test case by following the instruction in the comments inside the script

