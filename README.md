# Environment to reproduce the Kafka SASL re-authentication bug

Channels failing to reauthenticate beyond the `connections.max.reauth.ms` threshold are not closed properly, producers and consumers keep doing their thing.
JWTs are issued with a short 3s expiry (set in `SingleClientRepository#tokenTtlSeconds`)

Steps to reproduce:
* run the OAuth Authorization Server AuthServerKt class
   - run with the attached jar file: (I'm built with jdk 17)
   ```
   java -jar luke-kafka-sasl-reauth.jar
   ```
   - or build the jar file from source:
   ```
   ./gradlew build
   java -jar PATH_TO_OUTPUT_LIB/kafka-sasl-reauth.jar
   ```
   If the tests failed, ignore it
* run ZK and kafka:
  - run with docker images:
  ```
  docker-compose up
  ```
  - or run with local ZK and Kafka, follow the kafka quickstart guide: https://kafka.apache.org/quickstart
      - replace the config/server.properties in Kafka with the attached config/server.properties
  
* run the producer test in `OAuthBearerTest#test`
  ```
  ./gradlew test -d 
  ```
* stop AuthServerKt
* observe producers and consumers happily keep producing even after their tokens expire (plus clock skew)
