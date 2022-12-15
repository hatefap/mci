## In order to build project, use the following command:
1. `mvn clean package -pl uri-generator -am`
   1. Then run it using: `java -jar uri-generator/target/uri-generator-0.0.1-SNAPSHOT.jar`
2. `mvn clean package -pl url-counter -am`
   1. Then run it using: `java -jar url-counter/target/url-counter-0.0.1-SNAPSHOT.jar`


## Examples:

- To get top k url: `http://localhost:8080/api/v1/topk?k=3`
- To get urls by page: `http://localhost:8080/api/v1/urls?from=3&size=10`
