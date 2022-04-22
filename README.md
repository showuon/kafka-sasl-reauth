# Environment to reproduce the Kafka SASL re-authentication bug

Channels failing to reauthenticate beyond the `connections.max.reauth.ms` threshold are not closed properly, producers and consumers keep doing their thing.
JWTs are issued with a short 3s expiry (set in `SingleClientRepository#tokenTtlSeconds`)

Steps to reproduce:
 - run the OAuth Authorization Server AuthServerKt class
 - `docker-compose up`
 - run the consumer in `OAuthBearerTest#consume`
 - stop AuthServerKt
 - observe producers and consumers happily keep producing even after their tokens expire (plus clock skew)