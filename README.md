# Test de performance d'une plateforme d'inscription universitaire avec Apache JMeter

Projet M1 Génie logiciel. On simule la **période d'inscription** d'une université : beaucoup
d'étudiants se connectent en même temps pour s'inscrire dans une filière aux places limitées.

- `src/` : l'application testée (Spring Boot 3, Java 17, base H2 en mémoire).
- `jmeter/` : le plan de test JMeter, les comptes étudiants et les scripts de lancement.

```
projet/
├── pom.xml, Dockerfile, docker-compose.yml
├── src/main/java/fr/univ/inscription/
│   ├── model/        Etudiant, Filiere, Inscription (tables de la base)
│   ├── repository/   accès à la base (Spring Data JPA)
│   ├── service/      AuthService (jetons), InscriptionService (règles + concurrence), ModeDemoLent
│   ├── web/          contrôleurs REST + gestion des erreurs
│   └── DonneesInitiales.java   5 filières + 500 étudiants créés au démarrage
├── src/main/resources/application.properties
└── jmeter/
    ├── plan_inscription.jmx    plan de test
    ├── candidats.csv           500 comptes (login, mot de passe, filière choisie)
    ├── run-charge.sh           test de charge (500 utilisateurs, 30 min)
    └── run-pic.sh              test de pic (1 500 utilisateurs en 60 s, 5 min)
```

---

## 0. Installer JMeter

JMeter est une application Java : il faut **Java 17 ou plus** puis l'archive de JMeter.

### 0.1 Vérifier / installer Java 17+

```bash
java -version          # doit afficher 17 (ou plus)
```

Si la version est plus ancienne (ex. 1.8) ou si Java est absent, installer un JDK 17
(par exemple Eclipse Temurin : https://adoptium.net/temurin/releases/?version=17).
Sous Windows, on peut prendre l'archive `.zip` et la décompresser sans droits administrateur
(ex. `C:\Users\<moi>\tools\jdk-17.0.21+9`).

### 0.2 Télécharger et vérifier JMeter

1. Aller sur https://jmeter.apache.org/download_jmeter.cgi et télécharger la dernière version
   stable (ici **5.6.3**) : `apache-jmeter-5.6.3.zip` **et** le fichier `apache-jmeter-5.6.3.zip.sha512`.
2. Vérifier l'intégrité de l'archive (la somme calculée doit être identique à celle du fichier `.sha512`) :

   ```powershell
   # Windows (PowerShell)
   Get-FileHash apache-jmeter-5.6.3.zip -Algorithm SHA512
   Get-Content apache-jmeter-5.6.3.zip.sha512
   ```
   ```bash
   # Linux / macOS
   sha512sum -c apache-jmeter-5.6.3.zip.sha512     # macOS : shasum -a 512 -c ...
   ```
3. Décompresser l'archive, par exemple dans `C:\Users\<moi>\tools\apache-jmeter-5.6.3`
   (Linux/macOS : `~/tools/apache-jmeter-5.6.3`).

### 0.3 Ajouter JMeter au PATH

- **Windows** : Paramètres → « Modifier les variables d'environnement pour votre compte » →
  `Path` → Nouveau → `C:\Users\<moi>\tools\apache-jmeter-5.6.3\bin`.
  Ou en PowerShell :
  ```powershell
  $jm = "$HOME\tools\apache-jmeter-5.6.3\bin"
  [Environment]::SetEnvironmentVariable('Path', [Environment]::GetEnvironmentVariable('Path','User') + ";$jm", 'User')
  ```
- **Linux / macOS** : ajouter à `~/.bashrc` (ou `~/.zshrc`) :
  ```bash
  export PATH="$PATH:$HOME/tools/apache-jmeter-5.6.3/bin"
  ```

**Si le Java par défaut de la machine n'est pas Java 17** (cas de cette machine : Java 8 dans le
PATH système), on indique à JMeter quel Java utiliser, sans toucher aux autres projets :

- Windows (`jmeter.bat`) : variable d'environnement utilisateur `JM_LAUNCH` =
  `C:\Users\<moi>\tools\jdk-17.0.21+9\bin\java.exe`
- Linux / macOS / Git Bash (script `jmeter`) : `export JAVA_HOME=<dossier du JDK 17>`

Ouvrir un **nouveau** terminal puis vérifier :

```bash
jmeter --version       # affiche le logo APACHE JMETER et la version 5.6.3
```

(Les lignes `WARN StatusConsoleListener ...` affichées au démarrage sont sans gravité.)

### 0.4 Ouvrir l'interface graphique

- Windows : double-cliquer sur `bin\jmeter.bat` (ou taper `jmeter` dans un terminal).
- Linux / macOS : `jmeter.sh` (ou `jmeter`).

L'interface graphique sert à **construire et déboguer** un plan de test.
Les vrais tests de charge se lancent **en ligne de commande** (voir §4), car l'interface
consomme beaucoup de ressources et fausserait les mesures.

---

## 1. Lancer l'application à tester

### Option A : avec Maven (Java 17 requis)

```bash
mvn package                                   # compile et produit target/inscription-1.0.0.jar
java -jar target/inscription-1.0.0.jar        # démarre sur http://localhost:8080
```

> Si le `JAVA_HOME` de la machine pointe vers un Java plus ancien, le définir pour la session :
> PowerShell : `$env:JAVA_HOME="$HOME\tools\jdk-17.0.21+9"` — Bash : `export JAVA_HOME=...`

### Option B : avec Docker

```bash
docker compose up --build
```

### Vérifier que l'application répond

```bash
curl http://localhost:8080/actuator/health        # {"status":"UP"}
curl http://localhost:8080/api/filieres
```

### L'API

| Méthode | URL | Rôle | Jeton |
|---|---|---|---|
| POST | `/api/auth/login` | corps `{"login":"etudiant001","motDePasse":"test123"}` → `{"token":"...","etudiantId":1}` | non |
| GET | `/api/filieres` | liste des 5 filières et de leurs places disponibles | non |
| POST | `/api/inscriptions` | corps `{"filiereId":1}` → inscription créée | oui |
| GET | `/api/inscriptions/{id}` | consulter **sa** inscription | oui |

Le jeton s'envoie dans l'en-tête `Authorization: Bearer <jeton>`.

Données créées au démarrage : 5 filières de 120 places (600 au total) et 500 comptes
`etudiant001` à `etudiant500`, mot de passe `test123`. La base est **en mémoire** : redémarrer
l'application remet tout à zéro (à faire entre deux tests pour comparer des choses comparables).

### Comment la concurrence est gérée

Dans `InscriptionService.inscrire` :

1. la filière est lue **avec un verrou** (`SELECT ... FOR UPDATE`) : les inscriptions dans une même
   filière passent une par une, deux étudiants ne peuvent pas prendre la dernière place ensemble ;
2. si l'étudiant est déjà inscrit dans cette filière, on renvoie l'inscription existante (200) :
   une requête rejouée ne crée **pas de doublon** ; s'il est inscrit ailleurs → 409 ;
3. s'il n'y a plus de place → 409 ;
4. sinon on retire une place et on enregistre.

En plus, une **contrainte d'unicité** en base sur `etudiant_id` empêche définitivement deux
inscriptions pour le même étudiant (cas de deux requêtes simultanées vers deux filières différentes).

Vérifié : 199 étudiants qui s'inscrivent au même instant dans une filière de 120 places →
exactement 120 acceptés, 79 refus (409), 0 place négative.

### Mode démo lent (`demo.slow-mode`)

Quand `demo.slow-mode=true`, chaque `POST /api/inscriptions` passe par un « service de validation »
simulé qui prend 300 ms et ne traite que 10 demandes à la fois (`demo.slow-delay-ms`,
`demo.slow-concurrency`). Avec peu d'utilisateurs on ne voit qu'un léger surcoût ; avec beaucoup,
les demandes font la queue et les temps de réponse s'envolent.

Activation :
```bash
java -jar target/inscription-1.0.0.jar --demo.slow-mode=true
# ou avec Docker : mettre DEMO_SLOWMODE: "true" dans docker-compose.yml
```

### Suivre le CPU et la mémoire pendant un test (Actuator)

```bash
curl http://localhost:8080/actuator/metrics/process.cpu.usage      # CPU du processus (0 à 1)
curl http://localhost:8080/actuator/metrics/system.cpu.usage       # CPU de la machine
curl http://localhost:8080/actuator/metrics/jvm.memory.used        # mémoire utilisée (octets)
curl http://localhost:8080/actuator/metrics/jvm.threads.live       # nombre de threads
curl http://localhost:8080/actuator/metrics/tomcat.threads.busy    # requêtes en cours de traitement
```

---

## 2. Le plan de test `jmeter/plan_inscription.jmx`

Parcours d'un étudiant virtuel, répété en boucle pendant toute la durée du test :
**connexion → liste des filières → inscription → consultation de son inscription**.

| Élément | Rôle |
|---|---|
| **HTTP Request Defaults** | serveur (`${__P(host,localhost)}`), port (`${__P(port,8080)}`), encodage UTF-8, délais max. Toutes les requêtes en héritent. |
| **HTTP Header Manager (JSON)** | ajoute `Content-Type: application/json` et `Accept: application/json` à toutes les requêtes. |
| **CSV Data Set Config** | lit `candidats.csv` : chaque itération prend la ligne suivante et crée `${login}`, `${motDePasse}`, `${filiereId}`. Fichier partagé par tous les utilisateurs, relu depuis le début quand il est fini. |
| **Thread Group « Étudiants »** | `${__P(users,50)}` utilisateurs, montée en `${__P(rampup,30)}` s, durée `${__P(duration,120)}` s. `__P(nom,défaut)` lit une propriété passée avec `-Jnom=valeur`, sinon prend la valeur par défaut. En cas d'erreur, l'utilisateur recommence un nouveau parcours. |
| **Uniform Random Timer** | « temps de réflexion » : pause de 3 à 5 s (3000 ms + aléa de 0 à 2000 ms) avant chaque requête, comme un vrai étudiant. |
| **Response Assertion** | la requête est en erreur si le code HTTP n'est pas 200. |
| **Duration Assertion** | la requête est en erreur si elle dure plus de 3000 ms (notre exigence de performance). |
| **1 - Connexion** + **JSON Extractor** | `POST /api/auth/login` ; l'extracteur lit `$.token` dans la réponse et le range dans `${token}`. |
| **2 - Liste des filières** | `GET /api/filieres`. |
| **3 - Inscription** | `POST /api/inscriptions` avec `{"filiereId":${filiereId}}` et l'en-tête `Authorization: Bearer ${token}` ; un 2ᵉ JSON Extractor récupère `$.id` → `${inscriptionId}`. |
| **4 - Consultation inscription** | `GET /api/inscriptions/${inscriptionId}` avec le jeton. |
| **Summary Report** | tableau de synthèse (dans l'interface graphique). |
| **View Results Tree** | détail de chaque requête/réponse, **désactivé** par défaut (très coûteux) ; l'activer seulement pour déboguer avec 1 ou 2 utilisateurs. |

Dans `candidats.csv`, les étudiants 001–100 choisissent la filière 1, 101–200 la filière 2, etc.
(100 étudiants par filière de 120 places) : toutes les inscriptions du scénario peuvent aboutir,
donc toute erreur mesurée vient de la **performance** et non d'une filière pleine.
Quand le fichier est relu, un étudiant déjà inscrit renvoie la même requête : l'application
répond 200 avec son inscription existante.

### Ouvrir le plan dans l'interface graphique

1. Lancer l'application (§1), puis `jmeter` (ou `jmeter.bat` / `jmeter.sh`).
2. *File → Open* → `jmeter/plan_inscription.jmx`.
3. Pour un essai rapide : clic droit sur *View Results Tree* → *Enable*, puis bouton vert ▶ (*Start*).
   Les valeurs par défaut (50 utilisateurs, 120 s) s'appliquent puisqu'aucun `-J` n'est passé.
4. Cliquer sur *Summary Report* / *View Results Tree* pour voir les résultats.
   *Run → Clear All* pour effacer, bouton ■ pour arrêter.

---

## 3. Lancer un test en ligne de commande

Depuis le dossier `jmeter/` :

```bash
jmeter -n -t plan_inscription.jmx -Jusers=10 -Jrampup=10 -Jduration=30 \
       -l resultats/essai/resultats.jtl -e -o resultats/essai/rapport
```

| Option | Signification |
|---|---|
| `-n` | *non-GUI* : mode ligne de commande, sans interface graphique (indispensable pour mesurer correctement) |
| `-t fichier.jmx` | le plan de test à exécuter |
| `-J nom=valeur` | définit une propriété lue dans le plan par `${__P(nom,défaut)}` (ici users, rampup, duration ; aussi host et port) |
| `-l fichier.jtl` | fichier de résultats bruts : une ligne par requête (heure, durée, code, succès...) |
| `-e` | génère le rapport HTML à la fin du test |
| `-o dossier` | dossier du rapport HTML (doit être **vide ou inexistant**) |

Pendant le test, JMeter affiche toutes les 30 s une ligne `summary` : nombre de requêtes,
débit (`/s`), temps moyen/min/max (ms), erreurs, utilisateurs actifs.

### Tester une seule API : `-Japi=...`

Chaque requête du plan est placée dans un *If Controller* qui ne l'exécute que pour certaines
valeurs de la propriété `api` :

| `-Japi=` | Requêtes exécutées | Remarque |
|---|---|---|
| `tout` (défaut) | 1 → 2 → 3 → 4 | parcours complet |
| `connexion` | 1 | |
| `filieres` | 2 | pas besoin de jeton |
| `inscription` | 1 → 3 | la connexion fournit le jeton |
| `consultation` | 1 → 3 → 4 | il faut le jeton et le numéro d'inscription |

Dans le rapport, chaque requête a sa propre ligne : on lit celle de l'API testée.

```powershell
mkdir resultats\connexion-500
jmeter -n -t plan_inscription.jmx -Japi=connexion -Jusers=500 -Jrampup=300 -Jduration=1800 -l resultats\connexion-500\resultats.jtl -e -o resultats\connexion-500\rapport
```

Avec les scripts : `./run-charge.sh connexion`, `./run-pic.sh filieres`, etc.
Une valeur inconnue (faute de frappe) arrête le test tout de suite, avec 0 requête (`summary = 0`).

On peut aussi générer le rapport plus tard à partir d'un `.jtl` existant :
`jmeter -g resultats.jtl -o rapport`.

### Les deux scénarios prêts à l'emploi

| Script | Type de test | Utilisateurs | Montée | Durée |
|---|---|---|---|---|
| `run-charge.sh` | **charge** : affluence forte mais attendue | 500 | 5 min (300 s) | 30 min |
| `run-pic.sh` | **pic** : ouverture des inscriptions, tout le monde arrive en même temps | 1 500 | 60 s | 5 min |

```bash
cd jmeter
./run-charge.sh
./run-pic.sh
./run-pic.sh -Jhost=192.168.1.20    # les options en plus sont transmises à JMeter
```

Les résultats sont rangés dans `jmeter/resultats/<type>-<date>/` (`resultats.jtl`, `jmeter.log`,
`rapport/index.html`).
Sous Windows, ces scripts se lancent dans **Git Bash** ; il faut alors que `jmeter` soit dans le
PATH de Git Bash et que `JAVA_HOME` pointe vers Java 17
(`export JAVA_HOME="C:/Users/<moi>/tools/jdk-17.0.21+9"`).

> Conseil : lancer JMeter et l'application sur deux machines différentes si possible, sinon ils se
> partagent le CPU et les mesures sont faussées. Redémarrer l'application entre deux tests.

---

## 4. Lire le rapport HTML

Ouvrir `rapport/index.html` dans un navigateur.

**Dashboard** (page d'accueil) :
- **APDEX** : indice de satisfaction de 0 à 1 (1 = toutes les réponses rapides). Seuils par défaut :
  satisfaisant < 500 ms, tolérable < 1 500 ms.
- **Requests Summary** : camembert succès / échecs.
- **Statistics** : le tableau le plus important, une ligne par requête :
  - `#Samples` : nombre de requêtes ; `FAIL` / `Error %` : **taux d'erreur** ;
  - `Average`, `Min`, `Max` : temps de réponse en ms ;
  - `Median`, `90th pct`, `95th pct`, `99th pct` : **percentiles**. « 95th pct = 800 ms » signifie
    que 95 % des requêtes ont répondu en moins de 800 ms. Les percentiles sont plus fiables que la
    moyenne, qui cache les requêtes très lentes ;
  - `Transactions/s` : **débit**, nombre de requêtes traitées par seconde ;
  - `Received / Sent KB/sec` : volume réseau.
- **Errors** et **Top 5 Errors by sampler** : nature des erreurs (ex. `Code HTTP différent de 200`,
  `The operation lasted too long` = Duration Assertion dépassée, `409`, `Connection refused`...).

**Charts** :
- *Over Time → Response Times Over Time* : évolution des temps de réponse ; on voit à quel moment
  ils se dégradent pendant la montée en charge.
- *Over Time → Active Threads Over Time* : nombre d'utilisateurs actifs (forme de la montée en charge).
- *Throughput → Transactions Per Second* : débit ; s'il stagne alors que les utilisateurs augmentent,
  le serveur est saturé.
- *Response Times → Response Time Percentiles* et *Response Time vs Request* (temps en fonction de la charge).

Ce qu'on cherche : jusqu'à combien d'utilisateurs les temps restent sous 3 s avec moins de 1 %
d'erreurs, et quelle requête se dégrade en premier.

---

## 5. Démo : comparer mode normal et mode lent

1. **Mode normal** : démarrer l'application normalement, puis
   ```bash
   cd jmeter && ./run-charge.sh          # ou run-pic.sh pour une démo plus courte (5 min)
   ```
2. Arrêter l'application, la relancer avec `--demo.slow-mode=true`
   (Docker : `DEMO_SLOWMODE: "true"`), relancer **le même** script.
3. Ouvrir les deux `rapport/index.html` côte à côte et comparer, pour **3 - Inscription** et pour le total :
   moyenne, 95ᵉ percentile, débit, taux d'erreur, et la courbe *Response Times Over Time*.

Ce qu'on doit observer en mode lent : avec peu d'utilisateurs, l'inscription prend simplement
~300 ms de plus ; quand la charge dépasse la capacité du « service de validation »
(10 demandes / 300 ms ≈ 33 inscriptions par seconde), une file d'attente se forme, les temps de
réponse augmentent sans cesse, la Duration Assertion (3 s) échoue et le taux d'erreur monte.
Pendant ce temps on peut suivre `/actuator/metrics/tomcat.threads.busy` qui grimpe.

Exemple mesuré sur cette machine (10 utilisateurs, 30 s) :

| Requête | Mode normal (moyenne) | Mode lent (moyenne) |
|---|---|---|
| 3 - Inscription | 50 ms | 357 ms |
| Taux d'erreur total | 0 % | 0 % |
