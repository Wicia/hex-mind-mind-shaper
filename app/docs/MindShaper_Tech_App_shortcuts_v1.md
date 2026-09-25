# Tech — Skróty aplikacji (launcher shortcuts)

## Model rozwiązania

- **Skrót = intencja z parametrem, nie osobna ścieżka** — każdy skrót otwiera to samo `CaptureActivity`, różni się tylko dodatkowym extra `EXTRA_CAPTURE_FORM` niosącym nazwę wartości `DefaultCaptureForm`
  - dzięki temu nie powstała druga implementacja otwierania formy — cała logika została w `autoOpenDefaultFormIfNeeded()`
- **Nadpisanie, nie rozgałęzienie** — `autoOpenDefaultFormIfNeeded()` czyta extra i używa go *zamiast* ustawienia; brak extra = poprzednie zachowanie (forma z `AppSettingsStorage.getDefaultCaptureForm()`)
- **Odporność na złą wartość** — `runCatching { DefaultCaptureForm.valueOf(name) }.getOrNull()`, więc nieznana/uszkodzona wartość cicho spada do ustawienia domyślnego zamiast rzucić `IllegalArgumentException`

## Rejestracja skrótów

- **Dynamiczne, nie `res/xml/shortcuts.xml`** — utrzymana wcześniejsza decyzja: statyczny XML nie rozwija `${applicationId}`, więc gubi wariant `.debug`
- **`setDynamicShortcuts(list)` zamiast `pushDynamicShortcut`** (zmiana)
  - podmienia **cały** zestaw → gwarantuje kolejność z listy
  - usuwa nieistniejący już skrót `new_thought` na urządzeniach, które uruchamiały wcześniejszy build; `push` zostawiłby go na stałe
- **Fabryka `buildCaptureShortcut(id, labelRes, iconRes, captureForm)`** — cztery pozycje różnią się tylko danymi, więc budowa poszła do jednej metody zamiast powielania buildera
- **`@StringRes` / `@DrawableRes`** na parametrach — Lint pilnuje, by nie wpadł tam dowolny `Int`

## Stos intencji

- `setIntents(arrayOf(homeIntent, captureIntent))` — **Home pierwszy, Capture ostatni**
  - launcher układa wcześniejsze intencje pod ostatnią, więc BACK z zimnego startu ląduje na `HomeActivity` zamiast wyrzucać z aplikacji
  - zachowane bez zmian z poprzedniej implementacji

## Etykiety

- **Jeden string na skrót** — `shortLabel` i `longLabel` dostają tę samą wartość
  - API wymaga obu, ale nic nie każe im się różnić; przy krótkich etykietach („Złap notatkę") dłuższy wariant nic nie wnosił
  - zejście z 8 stringów do 3

## Ikony

- **Trzy nowe vectory** — `ic_shortcut_new_text` / `_voice` / `_photo`
  - konstrukcja z istniejącego wzorca: pomarańczowy dysk (`#FFA86B`) + biały glif w `<group>` ze skalą
  - glify **reużyte** z `ic_thought_type_rich_text` / `_recording` / `_photo` (ten sam viewport 960) — brak nowych ścieżek do utrzymania
  - skala `0.58` zamiast `0.62` z pierwowzoru, bo te glify są wizualnie szersze
- **Kolor wpisany wprost, nie `?attr/...`** — launcher renderuje ikony skrótów we własnym motywie, gdzie atrybuty motywu rozwiązują się nieprzewidywalnie (uwaga przeniesiona z istniejącej ikony)

## Zachowane za darmo

- **Feature-flagi** — `autoOpenVoiceRecording()` / `autoOpenPhoto()` już sprawdzały `isVoiceRecordingEnabled()` / `isPhotoFeatureEnabled()` i pokazywały dialog zachęty
  - skrót do wyłączonej funkcji trafia w tę samą ścieżkę, bez dodatkowego kodu

## Do weryfikacji na urządzeniu

- kolejność skrótów w launcherze (część nakładek producenckich ma własne zdanie)
- czytelność glifów w małym rozmiarze ikony skrótu
- zniknięcie starego skrótu `new_thought` po aktualizacji z poprzedniego builda
- skrót do wyłączonej funkcji (głosówka/zdjęcie) → czy dialog zachęty pojawia się poprawnie przy zimnym starcie
