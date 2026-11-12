#!/usr/bin/env bash
# Test de CHARGE : période d'inscription "normale mais chargée"
#   500 étudiants, montée progressive en 5 min (300 s), durée totale 30 min (1800 s)
# Usage : ./run-charge.sh [api]      api = tout (défaut), connexion, filieres, inscription, consultation
#         (arguments supplémentaires transmis à JMeter, ex. ./run-charge.sh connexion -Jhost=192.168.1.10)
set -e
cd "$(dirname "$0")"

# API à tester (premier argument, "tout" = parcours complet)
API="tout"
if [ $# -gt 0 ] && [ "${1#-}" = "$1" ]; then API="$1"; shift; fi

# Commande JMeter : "jmeter" (dans le PATH) ou chemin donné par la variable JMETER
JMETER="${JMETER:-jmeter}"

# Dossier de résultats horodaté (le dossier du rapport HTML doit être vide ou inexistant)
RESULTATS="resultats/charge-$API-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTATS"

# -n : mode ligne de commande (sans interface graphique)
# -t : plan de test à exécuter
# -J : propriétés lues dans le plan (users, rampup, duration, api)
# -l : fichier où enregistrer chaque résultat (.jtl)
# -e -o : générer le rapport HTML à la fin, dans ce dossier
# -j : fichier de log de JMeter
"$JMETER" -n -t plan_inscription.jmx \
  -Jusers=500 -Jrampup=300 -Jduration=1800 -Japi="$API" \
  -l "$RESULTATS/resultats.jtl" \
  -e -o "$RESULTATS/rapport" \
  -j "$RESULTATS/jmeter.log" \
  "$@"

echo "Rapport HTML : $RESULTATS/rapport/index.html"
