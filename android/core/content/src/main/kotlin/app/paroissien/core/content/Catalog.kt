package app.paroissien.core.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Format de paquet que cette version de l'app sait lire (ADR 003). */
object PackFormat {
    const val APPLICATION_ID = 0x5052534E
    const val MAJOR = 1
    const val MINOR = 0
    const val USER_VERSION = MAJOR * 100 + MINOR

    /** Vrai si un paquet qui demande [minAppFormat] (« majeur.mineur ») peut s'afficher correctement. */
    fun supports(minAppFormat: String): Boolean {
        val (major, minor) = minAppFormat.split('.').map { it.toIntOrNull() ?: Int.MAX_VALUE }.let { it[0] to it.getOrElse(1) { 0 } }
        return major < MAJOR || (major == MAJOR && minor <= MINOR)
    }
}

@Serializable
data class Catalog(val catalogFormat: Int, val generated: Long, val packs: List<CatalogPack>) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(bytes: ByteArray): Catalog = json.decodeFromString(serializer(), bytes.decodeToString())
    }
}

@Serializable
data class CatalogPack(
    val packageId: String,
    val packageVersion: Long,
    val file: String,
    val manifest: String,
    val sha256: String,
    val size: Long,
    val rights: String,
    val minAppFormat: String,
    val title: Map<String, String> = emptyMap(),
    val publications: List<String> = emptyList(),
)

/** Signature ECDSA P-256 détachée de catalog.json (SHA256withECDSA, DER), produite par openssl (ADR 003). */
object CatalogVerifier {

    fun publicKey(pem: String): PublicKey {
        val der = Base64.getDecoder().decode(pem.lines().filterNot { it.startsWith("-----") }.joinToString(""))
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
    }

    /** Vrai si [signature] signe exactement [catalog] avec l'une des [keys]. */
    fun verify(catalog: ByteArray, signature: ByteArray, keys: List<PublicKey>): Boolean = keys.any { key ->
        runCatching {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key)
                update(catalog)
                verify(signature)
            }
        }.getOrDefault(false)
    }
}
