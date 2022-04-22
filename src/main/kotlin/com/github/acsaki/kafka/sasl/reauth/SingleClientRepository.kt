package com.github.acsaki.kafka.sasl.reauth

import org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS
import org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC
import org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.TokenSettings
import java.time.Duration.ofSeconds

object SingleClientRepository : RegisteredClientRepository {

    const val clientId = "client1"
    const val initialSecret = "client_secret1"
    const val tokenTtlSeconds = 3L

    var secret = initialSecret
        set(value) {
            field = value
            client = RegisteredClient.from(client)
                .clientSecret("{noop}$value")
                .build()
        }

    @Volatile
    private var client: RegisteredClient = RegisteredClient.withId("1")
        .clientId(clientId)
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(CLIENT_SECRET_POST)
        .authorizationGrantType(CLIENT_CREDENTIALS)
        .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(ofSeconds(tokenTtlSeconds)).build())
        .scope("kafka")
        .build()

    override fun save(registeredClient: RegisteredClient) = error("not supported")

    override fun findById(id: String?): RegisteredClient = client

    override fun findByClientId(clientId: String?): RegisteredClient = client
}