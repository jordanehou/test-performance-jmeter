#!/usr/bin/env bash
# Test de PIC : ouverture des inscriptions, tout le monde arrive en même temps
#   1 500 étudiants démarrés en 60 s, durée totale 5 min (300 s)
# Usage : ./run-pic.sh [api]      api = tout (défaut), connexion, filieres, inscription, consultation
#         (arguments supplémentaires transmis à JMeter, ex. ./run-pic.sh connexion -Jhost=192.168.1.10)
set -e
cd "$(dirname "$0")"

# API à tester (premier argument, "tout" = parcours complet)
API="tout"
if [ $# -gt 0 ] && [ "${1#-}" = "$1" ]; then API="$1"; shift; fi

# Commande JMeter : "jmeter" (dans le PATH) ou chemin donné par la variable JMETER
JMETER="${JMETER:-jmeter}"

# Dossier de résultats horodaté (le dossier du rapport HTML doit être vide ou inexistant)
RESULTATS="resultats/pic-$API-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTATS"

# Mêmes options que run-charge.sh (voir les commentaires de ce fichier)
"$JMETER" -n -t plan_inscription.jmx \
  -Jusers=1500 -Jrampup=60 -Jduration=300 -Japi="$API" \
  -l "$RESULTATS/resultats.jtl" \
  -e -o "$RESULTATS/rapport" \
  -j "$RESULTATS/jmeter.log" \
  "$@"

echo "Rapport HTML : $RESULTATS/rapport/index.html"
