package com.github.acsaki.kafka.sasl.reauth

import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.servlet.config.annotation.EnableWebMvc

const val issuer = "http://localhost"
const val tokenEndpoint = "/oauth2/token"
const val jwksEndpoint = "/oauth2/jwks"

@SpringBootApplication
@Configuration
@Import(OAuth2AuthorizationServerConfiguration::class)
@ComponentScan(basePackageClasses = [AuthServer::class])
@EnableWebSecurity
@EnableWebMvc
class AuthServer {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val keyId = "123"
    private val rsaJwk by lazy { generateRsa(keyId) }

    @Bean
    fun registeredClientRepository(): RegisteredClientRepository = SingleClientRepository

    @Bean
    fun jwkSource(): JWKSource<SecurityContext?> =
        JWKSource<SecurityContext?> { jwkSelector: JWKSelector, _: SecurityContext? ->
            JWKSet(rsaJwk).let { jwkSelector.select(it) }
        }

    @Bean
    fun providerSettings(): ProviderSettings =
        ProviderSettings.builder()
            .issuer(issuer)
            .tokenEndpoint(tokenEndpoint)
            .jwkSetEndpoint(jwksEndpoint)
            .build()
            .also {
                logger.info("token endpoint URL: {}", it.tokenEndpoint)
                logger.info("JWKS endpoint: {}", it.jwkSetEndpoint)
            }

    @Bean
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http.run {
        authorizeRequests().anyRequest().permitAll().and().csrf().disable()
        build()
    }
}

@Controller
internal class ClientEndpoints {

    @GetMapping(value = ["/client/{id}"])
    fun getClient(@PathVariable id: String): ResponseEntity<RegisteredClient> =
        ok(SingleClientRepository.findByClientId(id))

    @GetMapping(value = ["/client/{id}/clientSecret"])
    fun getSecret(@PathVariable id: String): ResponseEntity<String> = ok(SingleClientRepository.secret)

    @PostMapping(value = ["/client/{id}/clientSecret"])
    fun changeClientSecret(@PathVariable id: String, @RequestBody newSecret: String): ResponseEntity<String> =
        ok("OK").also {
            SingleClientRepository.secret = newSecret
        }
}

fun main(args: Array<String>) {
    runApplication<AuthServer>(*args)
}