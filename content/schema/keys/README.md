# Clés de test (non secrètes)

Clé ECDSA P-256 **de test** pour signer `catalog.json` en debug, en benchmark et en CI (ADR 003). Elle est publique par construction : n'importe qui peut signer avec. La clé de production, introduite avec les paquets téléchargeables (V1), restera hors du dépôt, dans `~/.android-keys/`.

- `test-p256.pem` : privée, format SEC1, pour `openssl dgst -sign` (chaîne Python).
- `test-p256.pk8.pem` : la même en PKCS#8, pour les tests Kotlin qui signent.
- `test-p256.pub.pem` : publique, embarquée dans l'app.
- `test-vector.txt` et `.sig` : signature produite par openssl, vérifiée par Java (compatibilité DER).
