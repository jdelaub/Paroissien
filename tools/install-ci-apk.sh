#!/usr/bin/env bash
# Installe sur le téléphone l'APK de debug du dernier run CI réussi de la branche.
# Même clé de débogage qu'en local (android/keystore/debug.keystore) : la mise à jour garde user.db.
# Usage : tools/install-ci-apk.sh [branche]
set -euo pipefail

branch="${1:-$(git rev-parse --abbrev-ref HEAD)}"
artifact="${ARTIFACT:-paroissien-domainePublic-debug}"
adb="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"

run_id=$(gh run list --workflow ci.yml --branch "$branch" --status success --limit 1 --json databaseId --jq '.[0].databaseId')
if [ -z "$run_id" ]; then
    echo "Aucun run CI réussi sur la branche $branch" >&2
    exit 1
fi

dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT
gh run download "$run_id" --name "$artifact" --dir "$dir"
apk=$(find "$dir" -name '*.apk' | head -1)
echo "Installation de $(basename "$apk") (run $run_id)"
"$adb" install -r "$apk"
