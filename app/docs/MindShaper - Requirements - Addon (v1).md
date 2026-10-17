# MindShaper - Requirements - Addon (v1)

Ustalenia: co gdzie opisywać w Requirements i dlaczego. Do scalenia z Requirements (sekcja „Jak czytać i rozwijać dokumentację?”).

## 1. Jeden dom feature'u

- **Każdy feature ma jedną kartę (dom)** — w zakładce, gdzie jest jego cel biznesowy.
- W domu jest **cały opis**: czym jest, reguły, stany, gdzie się go ustawia.
- **Miejsca użycia** (inne ekrany, Preferencje) — tylko wzmianka + link ↳ do domu, bez powtarzania reguł.
- **Dlaczego:** jedno źródło prawdy — przy zmianie feature'u poprawia się jedno miejsce, opisy się nie rozjeżdżają.

## 2. Właściciel reguły — test

**Reguła** = jedno zdanie o tym, jak coś działa (np. „zmiana skali nie przelicza wartości”).

**Problem:** feature jest używany w kilku miejscach (np. wartość myśli: lista, Uśpienie, Preferencje). Przy każdej regule pojawia się pytanie — zapisać ją w karcie feature'u czy w karcie miejsca, które go używa?

**Test — weź regułę, którą chcesz zapisać, i wyobraź sobie zmianę:**

1. **Zmieniam sam feature** (np. skale wartości, sposób nadawania). Czy ta zapisywana reguła musi się zmienić?
   → **Tak** — reguła należy do feature'u → zapisz ją w jego karcie (domu).
2. **Zmieniam tylko miejsce użycia** (np. listę myśli, Uśpienie). Czy ta zapisywana reguła musi się zmienić, a feature zostaje bez zmian?
   → **Tak** — to własna logika tego miejsca → zapisz ją w karcie miejsca + link ↳ do feature'u.

**W skrócie:** reguła mieszka tam, gdzie trzeba by ją poprawić przy zmianie.

**Jak to wygląda na przykładzie:**

- „Zmiana skali nie przelicza wartości” — zmienia się, gdy zmieniam działanie wartości, lista nie ma tu nic do rzeczy → karta **Wartościowanie myśli**.
- „Myśli poniżej progu wartości są usypiane” — wartość działa tak samo, ale ktoś może zmienić próg albo zasady usypiania → karta **Tryb „Uśpienia”** + ↳ link do wartości.

Pełny przykład — wartość myśli:

| Reguła | Miejsce |
|---|---|
| czym jest wartość, skale, nadawanie, zmiana skali nie przelicza wartości | Wartościowanie myśli (dom) |
| co lista robi z wartością | Przeglądanie myśli + ↳ link |
| próg wartości decyduje o uśpieniu | Tryb „Uśpienia” + ↳ link |
| wybór skali na ekranie | Preferencje — tylko wzmianka + ↳ link |
| wspólny Zapisz, przełączniki zapisywane od razu, wskazówki na żądanie | Preferencje (reguły ekranu) |

## 3. „* Wspólne & Podstawy” — nie worek na resztę

- Tylko **wzorce i pojęcia używane przez wiele featurów** (np. Żądanie Uprawnień, słowniczek).
- Nie trafia tam „reszta opisów” featurów.

## 4. Karta ekranu vs karty featurów w tej samej zakładce

Przykład — zakładka Preferencje:

| Karta | Rola |
|---|---|
| Zapisywanie preferencji | karta **ekranu** — mapa sekcji + działanie zapisu |
| Robienie kopii zapasowej danych, Wybór ekranu startowego | karty **featurów** z własnym celem — ich dom akurat jest w tej zakładce |

- **Mapa ekranu = wiersz na każdą sekcję**, w kolejności z aplikacji, nazwy sekcji jak w aplikacji.
- Wiersz mapy = nazwa sekcji + jedna linijka „co tu się ustawia” + link. Bez reguł feature'u.
- Pełny opis w wierszu tylko dla rzeczy **bez własnej karty** (np. imię, zapisywanie).
- Własna karta, gdy feature ma kilka funkcji albo działa też poza tym ekranem (kopia — 4 funkcje; ekran startowy — działa przy otwieraniu aplikacji, linkuje do niego też Dom).

## 5. Do decyzji

- **Wiersze mapy dla featurów z kartą w tej samej zakładce** (np. kopia zapasowa):
  - **A (rekomendacja):** zostają — mapa kompletna i zgodna z ekranem; wiersz = tylko link, bez opisu zawartości, żeby nie wyglądał jak duplikat karty.
  - **B:** pomijane — mniej powtórzeń, ale mapa nie odpowiada ekranowi.
- Reguła „limit i przypomnienia dostępne tylko przy włączonych fiszkach” → dopisać do karty „Włączanie modułu fiszek” (wg punktu 2).
- Dopisać zasadę „jeden dom + test właściciela” do „Jak czytać i rozwijać dokumentację?”.
- **Zakładki per ekran czy per feature** — obserwacja: menu miesza oba podziały.
  - Per ekran (rzeczownik, jak ekran w aplikacji): Dom, Preferencje, Tagi i Domeny, Mój warsztat (karty = części ekranu: Lista, Szczegóły, Archiwum).
  - Per feature (czynność na -anie): Łapanie, Przeglądanie, Wartościowanie myśli, Kształtowanie Umysłu, Tworzenie i zarządzanie fiszkami, Powtarzanie z fiszkami.
  - Hybryda: Edycja i szczegóły myśli.
  - **Propozycja — dwa rodzaje zakładek z regułą:**
    - **Zakładka featurowa (domyślna)** — nazwa na -anie = cel biznesowy; dom featurów (pkt 1). Ekran realizujący głównie ten feature → karta ekranu z mapą sekcji w środku (pkt 4).
    - **Zakładka ekranu zbiorczego (wyjątek)** — tylko ekrany bez własnego celu, zbierające inne featury: Dom, Preferencje. Mapa + wzmianki + linki ↳; własne karty tylko dla featurów, których dom jest tutaj.
  - Skutki propozycji:
    - Tagi i Domeny → zakładka featurowa (porządkowanie tagów, konfigurowanie domen) + karta ekranu.
    - Mój warsztat → karty featurów zamiast sekcji ekranu; „Mój warsztat” jako karta ekranu.
    - Edycja i szczegóły myśli → ujednolicić w jedną stronę.
  - Konflikt z ustaleniem „zakładkę nazywać jak ekran w aplikacji” — pogodzenie: nazwa zakładki = czynność (cel), nazwa karty ekranu = dokładnie jak w aplikacji (np. „Ekran Strumień”).
  - Zmiany zakładek w Requirements — dopiero przy przejściu ekran po ekranie.
- **Alternatywa: tagowanie reguł** (zamiast „jednego domu” i testu właściciela).
  - Obserwacja: test właściciela pomaga piszącemu, a czytelnik pyta „co się dzieje na tym ekranie?” albo „jak działa ta rzecz?”. Naturalny dla niego podział: feature = konfiguracja (Preferencje) + użycie (per ekran).
  - Każda reguła biznesowa = osobny wiersz (jedno zdanie) z tagami:
    - **miejsce** — ekran, Preferencje albo „Ogólne” (reguła bez ekranu, np. wyznaczanie stanu myśli); może być kilka,
    - **proces** — np. Wartościowanie myśli, Tryb „Uśpienia”; może być kilka.
  - Każda reguła zapisana raz → bez duplikatów, bez linków „po definicję”; widoki generowane z tagów.
  - **Układ:** czarne menu po lewej (jak w Requirements) — u góry zakładki „Procesy” / „Miejsca”, pod nimi wygenerowana lista dla wybranego wymiaru, podzielona na sekcje (proces → reguły pogrupowane wg funkcji; miejsce → reguły pogrupowane wg procesu).
  - Treść w tabeli jak w Requirements (Funkcja | Wejścia, Dane, Stany | Procesy, Opcje, Integracje) — reguła ma dodatkowo przypisaną kolumnę; wiersz = funkcja, punkty = reguły.
  - Tagi reguły nie są widoczne w tekście — pod każdym punktem-regułą mały zwijany znacznik „Powiązania”: dwie kolumny, Miejsca | Procesy (klik przełącza widok).
  - Koszt: reguły jako atomowe wiersze + dyscyplina tagowania; filtrowanie w JS.
  - **Pilot:** osobny plik `MindShaper - Requirements - Tagging Pilot (v1).html` — Wartościowanie myśli + Tryb „Uśpienia” (dotykają Preferencji, Strumienia i Szczegółów myśli).
  - Jeśli przejdzie → przebudować punkty 1–2 (i 4) Addona.
  - **Kierunek przyjęty (2026-10-17):** jedno miejsce zapisu wszystkich wymagań, widok/perspektywa się zmienia.
  - **Ustalenia przed migracją:**
    1. Status i data (🟡 Robocze / 🟢 Gotowe / 🔴 TBD, #close, Changelog) — **per funkcja** (wiersz tabeli).
    2. Potrzeba i Cel, Pojęcia — **per proces**, nad tabelą w widoku Procesy (widok Miejsca — na razie bez).
    3. Opis miejsca (np. mapa sekcji Preferencji, „Ekran Dom”) — krótkie intro miejsca nad tabelami w widoku Miejsca.
    4. Linki ↳ w treści — znikają, zastępują je „Powiązania”; zostają tylko odwołania w treści reguły (np. „wg Filtrowanie myśli”).
    5. Dane — na razie tablica w JS w HTML, edytuje Claude; później osobny plik danych wczytywany do HTML.

## 6. Nowy kierunek — tagowane wymagania (alternatywa dla punktów 1–4)

**Zasada:** jedno miejsce zapisu wszystkich wymagań, widok/perspektywa się zmienia (Procesy / Miejsca). Szczegóły formatu i ustalenia — sekcja 5, punkt „Alternatywa: tagowanie reguł”; pilot: `MindShaper - Requirements - Tagging Pilot (v1).html`.

**Przebieg pracy — ekran po ekranie (np. Preferencje):**

1. **Funkcje tego miejsca** — z kodu ekranu; głównie konfiguracja (np. Włączanie trybu, Wybieranie skali). Reguły z tagiem miejsca + procesu.
2. **Pierwsze spotkanie z procesem** (np. Tryb „Uśpienia”) — od razu:
   - wpis procesu: Potrzeba i Cel (+ Pojęcia),
   - logika procesu z kodu → reguły „Ogólne” (np. wyznaczanie stanu myśli) — nie należą do żadnego ekranu, więc przechodząc po ekranach nie trafilibyśmy na nie.
3. **Kolejne ekrany** — tylko użycie: reguły z tagiem tego miejsca i istniejącego procesu (np. oznaczenie uśpionych w Strumieniu, Obudź myśl w Szczegółach).
4. **Akceptacja** — przy każdym ekranie lista reguł nowych / zmienionych / do usunięcia → akceptacja → edycja (znaczniki ✳️/✴️).

Proces kompletuje się stopniowo — po przejściu wszystkich ekranów widok Procesy pokazuje go w całości.

**Ustalenia:**

- **Nazwy funkcji bez miejsca** — miejsce wynika z tagów (np. „Wyświetlanie stanu myśli” zamiast „Wyświetlanie w Strumieniu” + „Wyświetlanie w Szczegółach myśli”). Status funkcji obejmuje wszystkie jej miejsca — 🟡, dopóki nie sprawdzono każdego.
- **Migracja razem z synchronizacją z kodem** — nowy plik `MindShaper - Requirements New (v1).html` w formacie tagowanym, wypełniany ekran po ekranie (start: Preferencje). v12 zostaje źródłem do końca przejścia, potem → `app/docs/legacy`. Bez przepisywania treści dwa razy.
