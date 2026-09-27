# Mind Shaper — Tech: applicationId + flavory

## applicationId — z czego się składa

- **Sklejanie z 3 członów** — finalne `applicationId` = `base` + `flavor suffix` + `buildType suffix`, w tej kolejności.
  - `base` = `pl.hexmind.mindshaper` (z `defaultConfig`)
  - `flavor suffix` = `.sandbox` (tylko flavor `sandbox`; `standard` pusty)
  - `buildType suffix` = `.debug` (tylko debug; release pusty)
- **namespace ≠ applicationId** — dwie różne role, u nas przypadkiem ta sama wartość.
  - `namespace` — pakiet dla wygenerowanego `R`/`BuildConfig`, rzecz **kompilacji**; nie zmienia się per wariant.
  - `applicationId` — **tożsamość apki** na urządzeniu i w Google Play; to ona decyduje o współistnieniu.

## Macierz wariantów (finalne id)

- **standardDebug** → `pl.hexmind.mindshaper.debug` — obecna apka, bez zmian
- **sandboxDebug** → `pl.hexmind.mindshaper.sandbox.debug` — nowa, obok
- **standardRelease** → `pl.hexmind.mindshaper` — czyste id produkcyjne
- **sandboxRelease** → `pl.hexmind.mindshaper.sandbox`

## Flavory — dlaczego dwa

- **Wymiar wymaga flavora** — `flavorDimensions` zadeklarowane, ale bez żadnego `productFlavor` = błąd konfiguracji.
- **`standard` celowo pusty** — bez suffixu, więc bazowe id zostaje nietknięte → **backward compat** z już zainstalowaną apką (jej pakiet się nie zmienia).
- **`sandbox` = jedyna różnica** — `applicationIdSuffix = ".sandbox"`, reszta dziedziczona.
- **Każdy flavor musi mieć `dimension`** — nawet przy jednym wymiarze przypisanie jest obowiązkowe.

## Side-by-side install — mechanizm

- **Różne id = różne pakiety** — system Android traktuje je jako **osobne aplikacje**, instalują się obok siebie.
- **Izolowane dane** — każda instancja ma własny sandbox plików i **własną bazę Room** (myśli/tagi się nie współdzielą).
  - efekt uboczny: sandbox startuje z pustą bazą — pożądane przy instancji testowej.

## Podpis (klucz)

- **sandboxDebug idzie kluczem debug** — Gradle podpisuje debug automatycznie wygenerowanym kluczem → **zero keystore** do współistnienia.
- **Release wymagałby własnego keystore** — dlatego wybór padł na build debug do drugiej instancji.

## Flavor source set — override zasobów

- **`src/sandbox/` nadpisuje `src/main/`** — Gradle scala zasoby, folder flavora ma wyższy priorytet.
- **`app_name` override** — ten sam klucz w `src/sandbox/res/values/strings.xml` przykrywa wartość z `main` **tylko** dla wariantów `sandbox*`.
  - to **override**, nie drugi wpis → brak błędu „duplicate resources"
  - `standardDebug` → „MindShaper", `sandboxDebug` → „MindShaper SB"
- **Tą samą drogą można podmienić więcej** — ikona (`mipmap-*`), kolory itd., wciąż bez ruszania `main`.

## fileprovider — brak kolizji manifestu

- **Authority dynamiczne** — `${applicationId}.fileprovider` rozwija się per wariant, więc każda instancja dostaje **własne** authority.
- **Dlaczego to ważne** — hardkodowane authority są globalnie unikalne w systemie; dwie apki z tym samym authority = konflikt przy instalacji. Tu problem nie występuje.

## Backup — izolacja per flavor

- **Problem** — backup ląduje we **wspólnym** `Downloads/mindshaper_backup/`; przy stałej nazwie pliku dwie instancje **nadpisywałyby sobie** snapshoty (i restore ciągnąłby cudze dane).
- **Rozwiązanie: sufiks na FOLDERZE, nie na nazwie** — zmiana wyłącznie w `getBackupDirectory()`, przez który idą wszystkie trzy operacje (`createSnapshot`, `restoreSnapshot`, `getSnapshotStats`).
  - jeden punkt → izolacja spójna dla zapisu, listy i odczytu; **cross-restore znika sam** (każda instancja widzi tylko swój folder)
  - nazwa pliku zostaje prosta (`mindshaper_v{ver}_{data}.json`)
- **Wariant „standard-legacy"** — `standard` zostaje na starym folderze, sufiks dostają tylko nie-standardowe flavory:
  - warunek `flavor.isNotBlank() && flavor != "standard"` → `_$flavor`
  - `standard` → `mindshaper_backup/` (**widzi stare backupy, zero migracji**)
  - `sandbox` → `mindshaper_backup_sandbox/`
- **Dlaczego działa bez dodatkowej roboty**
  - `createSnapshot` woła `backupDir.mkdirs()` → nowy folder powstaje przy pierwszym backupie
  - `getSnapshotStats` ma `?: emptyArray()` → pusty/nieistniejący folder nie wywala listy
- **Gotcha: magiczny string `"standard"`** — warunek jest sprzężony z nazwą flavora; przemianowanie `standard` w przyszłości „zgubi" stare backupy (dostanie sufiksowy folder). Oznaczone `TODO / core /` w kodzie jako ostrzeżenie.

## Taski testów a flavory

- **Problem** — hook „odpalaj unit testy przy każdym `assemble`" wiązał się sztywno do nazwy `testDebugUnitTest`. Flavory **rozbiły** tę nazwę na per-wariant (`testStandardDebugUnitTest`, `testSandboxDebugUnitTest`), więc `testDebugUnitTest` przestał istnieć → `assembleSandboxDebug` sypał `Task with path 'testDebugUnitTest' not found`.
- **Dlaczego stary hook był kruchy**
  - **`whenTaskAdded` + `dependsOn("string")`** — twarde wiązanie po nazwie, wykonywane eagerly przy dodaniu **każdego** taska; jedna zmiana w nazewnictwie (flavory) je rozwala
  - to samo API to źródło ostrzeżenia **„Deprecated Gradle features… incompatible with Gradle 9.0"**
- **Rozwiązanie: nazwa taska liczona z wariantu**
  - `tasks.matching { it.name.startsWith("assemble") }.configureEach { … }`
  - `"test${name.removePrefix("assemble")}UnitTest"` → `assembleSandboxDebug` mapuje na `testSandboxDebugUnitTest`; pasuje niezależnie od liczby flavorów
  - **`matching{}.configureEach{}`** = leniwe, nowoczesne API → znika deprecation
- **Efekt do świadomości** — `assemble<Variant>` odpala teraz testy **swojego** wariantu (wcześniej zawsze `testDebugUnitTest`). Bardziej poprawne; jeśli kiedyś chcesz jeden wspólny zestaw, wiąż na stałe do `testStandardDebugUnitTest`.

## APK output naming

- **`${name}` w nazwie pliku** — `MindShaper-${name}-v${versionCode}-${date}.apk`; `name` = pełna nazwa wariantu (np. `sandboxDebug`).
- **Po co** — bez `${name}` wszystkie warianty celowały w jedną nazwę i **nadpisywały** sobie APK; teraz każdy wariant ma osobny plik.

## Gotchas do zapamiętania

- **Kolejność suffixów jest stała** — base → flavor → buildType; nie da się jej odwrócić bez ręcznego `applicationId` w bloku.
- **`versionNameSuffix` niezależny od id** — debug dokłada `-DEBUG` do `versionName`, co nie ma wpływu na `applicationId`.
- **Zmiana `applicationId` po publikacji = nowa apka na Play** — dlatego produkcyjne id (`standardRelease`) trzymamy czyste, bez suffixów.
