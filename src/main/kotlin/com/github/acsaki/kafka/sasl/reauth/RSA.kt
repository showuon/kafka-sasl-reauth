package com.github.acsaki.kafka.sasl.reauth

import com.nimbusds.jose.jwk.RSAKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun generateRsa(keyId: String): RSAKey {
    return generateRsaKey().let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyID(keyId)
            .build()
    }
}

fun generateRsaKey(): KeyPair =
    KeyPairGenerator.getInstance("RSA").run {
        initialize(2048)
        generateKeyPair()
    }
