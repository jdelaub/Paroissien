#!/usr/bin/env bash
# Relevé du téléphone de référence pour l'ADR 002 (version d'Android, écran, GPU, fréquences).
# Usage : tools/device-inventory.sh > /tmp/appareil.md   (téléphone branché ou appairé en adb Wi-Fi)
set -euo pipefail
adb="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"

prop() { "$adb" shell getprop "$1" | tr -d '\r'; }

echo "| Propriété | Valeur |"
echo "|---|---|"
echo "| Modèle | $(prop ro.product.model) ($(prop ro.product.device)) |"
echo "| Android | $(prop ro.build.version.release) (API $(prop ro.build.version.sdk)) |"
echo "| One UI | $(prop ro.build.version.oneui) |"
echo "| Correctif de sécurité | $(prop ro.build.version.security_patch) |"
echo "| SoC | $(prop ro.soc.model) |"
echo "| Pilote EGL | $(prop ro.hardware.egl) |"
echo "| GPU | $("$adb" shell dumpsys SurfaceFlinger | tr -d '\r' | grep -m1 -E '^GLES:' | sed 's/^GLES: //') |"
echo "| Taille d'écran | $("$adb" shell wm size | tr -d '\r' | tr '\n' ' ') |"
echo "| Densité | $("$adb" shell wm density | tr -d '\r' | tr '\n' ' ') |"
echo "| Fréquences | $("$adb" shell dumpsys display | tr -d '\r' | grep -oE 'fps=[0-9.]+' | sort -u | tr '\n' ' ') |"
