## About Dashboard
This is a dashboard console for us to use the [Apache InLong incubator](https://github.com/apache/incubator-inlong).

## Build

* Use mvn
    ```
    mvn package -DskipTests -Pdocker -pl inlong-dashboard
    ```
* Use nodejs

    ```
    npm run build
    ```

## Run

* Use docker
    ```
    docker run -d --name dashboard -e MANAGER_API_ADDRESS=127.0.0.1:8083 -p 80:80 inlong/dashboard
    ```

## Dev

* Use nodejs
    ```
    npm run dev
    ```
