# Déployer et tester l'application

Guide rapide. Toutes les explications détaillées sont dans le [README](README.md).

## Prérequis

- **Docker Desktop** (pour lancer l'application), ou à défaut **Java 17** et **Maven**
- **Apache JMeter 5.6.3** avec **Java 17** (installation : README, section 0)

## 1. Lancer l'application

Depuis le dossier du projet :

```bash
docker compose up --build
```

Sans Docker :

```bash
mvn package
java -jar target/inscription-1.0.0.jar
```

L'application est prête quand le message `Données initiales : 5 filières, 500 étudiants` s'affiche.
Elle écoute sur **http://localhost:8080**.

Vérification :

```bash
curl http://localhost:8080/actuator/health      # {"status":"UP"}
curl http://localhost:8080/api/filieres         # liste des 5 filières
```

## 2. Lancer un test de performance

Dans un second terminal, depuis le dossier `jmeter/` :

```bash
mkdir -p resultats/essai
jmeter -n -t plan_inscription.jmx -Jusers=50 -Jrampup=30 -Jduration=120 -l resultats/essai/resultats.jtl -e -o resultats/essai/rapport
```

| Option | Rôle |
|---|---|
| `-Jusers` | nombre d'étudiants simulés |
| `-Jrampup` | durée de leur arrivée progressive (s) |
| `-Jduration` | durée totale du test (s) |
| `-Japi` | API testée : `tout` (défaut), `connexion`, `filieres`, `inscription`, `consultation` |

Scénarios prévus :

| Test | Paramètres |
|---|---|
| Charge | `-Jusers=500 -Jrampup=300 -Jduration=1800` |
| Pic | `-Jusers=1500 -Jrampup=60 -Jduration=300` |

## 3. Consulter les résultats

Ouvrir `resultats/essai/rapport/index.html` dans un navigateur. Le tableau **Statistics** donne,
pour chaque API, les temps de réponse (moyenne, percentiles), le débit et le taux d'erreur.

Les rapports des tests déjà réalisés se trouvent dans `jmeter/resultats/<test>/rapport/index.html`.

## 4. Option : mode lent

Pour montrer la dégradation des temps de réponse, relancer l'application en mode lent
(`DEMO_SLOWMODE: "true"` dans `docker-compose.yml`, ou `--demo.slow-mode=true` avec `java -jar`),
puis refaire le même test et comparer les deux rapports.
