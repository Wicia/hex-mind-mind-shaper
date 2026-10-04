# Fiszki i powtórki

## Potrzeba i Cel

- **Osobny system, nie forma myśli** — fiszki żyją w nazwanych *zestawach*, niezależnie od myśli; mają własny widok *Utrwalanie*.
  - inny rytm niż myśli (powtórki vs tryb uśpienia), więc nie mieszają się ze *Strumieniem*
- **Trwałe zapamiętywanie** — fiszki wracają w rosnących odstępach czasu, tak by powtórka wypadała tuż przed zapomnieniem; użytkownik ocenia, jak mu poszło, i widzi, ile już opanował.
- **Prosty, przewidywalny system powtórek** — stała skala interwałów zamiast algorytmu; użytkownik rozumie, co dzieje się z kartą.

## Wejścia, Dane, Stany

- **Włączenie funkcji** — fiszki są domyślnie wyłączone; włącza je przełącznik w *Ustawieniach*.
  - funkcja wyłączona → pozycja *Utrwalanie* znika z menu, a parametry powtórek są wyszarzone
  - wyłączenie funkcji nie usuwa zapisanych zestawów
- **Parametry powtórek** (*Ustawienia*, pod przełącznikiem; zapisywane przyciskiem zapisu ustawień):
  - **limit nowych kart dziennie** — domyślnie 10, zakres 1–50
  - **próg zaległości** wstrzymujący nowe karty — domyślnie 30, zakres 5–200, co 5
- **Zestaw** — nazwa (wymagana) + uporządkowana lista fiszek; kolejność = kolejność wpisania.
- **Fiszka** — awers (pytanie / hasło) + rewers (odpowiedź), oba wymagane; należy do zestawu, niezależna od myśli.
- **Poziomy i interwały** — poziomy 1–5 z interwałami **1 → 3 → 7 → 14 → 30 dni**; najniższy poziom to 1.
- **Statusy karty**:
  - **Nowa** — jeszcze nigdy nie powtarzana, czeka w puli
  - **Aktywna** — bierze udział w planowych powtórkach
  - **Zamrożona** — po wpadce czeka na drugą szansę; poziom „w zawieszeniu”
  - **Opanowana** — nie jest już pokazywana w powtórkach
- **Postęp karty** — status, poziom, termin kolejnej powtórki, koniec zamrożenia, dzień pierwszej powtórki (do dziennego limitu nowych).
- **Zaległości** — wszystkie powtórki dostępne teraz: planowe z terminem ≤ dziś + odblokowane drugie szanse.
- **Granica dnia** — północ czasu lokalnego.
- **Wygląd** — jeden kolor kart (pomarańczowy, nie szary — szary oznacza w aplikacji element nieaktywny).
  - tekst fiszki wyróżniony krojem (Alegreya); na karcie końcowej wszystkie teksty tej samej wielkości, nagłówek pogrubiony
  - znak usuwania X zawsze po lewej
  - do ocen służą kciuki i tylda — ikony „ptaszek” / X oznaczają w aplikacji „zapisz” / „usuń”
  - rakieta (start sesji) = ta sama ikona co *Home* (ukośna) — w widżecie *Dzisiejsza sesja*, na kartach zestawów i w podglądzie zestawu

## Widok *Utrwalanie*

- **Miejsce** — w menu między *Strumieniem* a *Warsztatem*.
- **Widżet *„Dzisiejsza sesja”*** na górze — jedna sesja ze wszystkich zestawów: *„Do powtórzenia: N · Nowe: M”* albo *„Na dziś wszystko powtórzone :)”*; brak fiszek w ogóle = brak widżetu (stany: zob. *Stany widżetu Dzisiejsza sesja*).
  - *Do powtórzenia* = karty już znane, dostępne teraz (planowe z terminem ≤ dziś + odblokowane drugie szanse); *Nowe* = nowe karty, które wejdą do tej sesji
  - przy wstrzymanych nowych fiszkach druga linia: *„Nowe fiszki wstrzymane - do nadrobienia: N”*
  - nie wygląda jak przycisk: bez wypełnienia i bez reakcji na tapnięcie, tylko świecąca (rozmyta) ramka w kolorze akcentu fiszek
  - linia ramki równo z krawędziami kart zestawów poniżej
  - jedyna akcja to większa rakieta (start sesji), ukryta, gdy nie ma nic do powtórki
  - linia ramki faluje jak powierzchnia wody = „jest coś do zrobienia”; po powtórzeniu wszystkiego fale płynnie się wygładzają (zostaje prosta ramka)
  - wyłączone animacje w systemie = fale nieruchome
  - przewija się razem z listą
- **Lista zestawów** — niżej etykieta *„Zestawy”* i lista zestawów, ostatnio edytowane na górze.
  - karta zestawu: *„Nazwa (liczba fiszek)”* i *„Powtórki: N · Nowe: M · Opanowane: K”*
  - tapnięcie karty = podgląd zestawu
  - rakieta = sesja tylko z tego zestawu; widoczna, gdy jest w nim coś do powtórki teraz
  - przycisk „+” = nowy zestaw — jeden dialog: nazwa (*„Utwórz”*), potem pole nazwy znika i pojawiają się awers / rewers do dodawania serią (*„Dodaj kolejną”* / *„Zakończ”*); *„Zakończ”* otwiera podgląd nowego zestawu
    - pole nazwy tej samej wysokości co pola awersu i rewersu
    - *„Anuluj”* przy nazwie = zestaw nie powstaje
  - liczniki odświeżają się po powrocie na ekran (druga szansa mogła się odblokować, mógł zacząć się nowy dzień)
  - brak sortowania, filtrów i wyszukiwania (na razie)
- **Podgląd zestawu** — nazwa zestawu w nagłówku (mniejsza niż tytuły innych ekranów — nazwy bywają dłuższe), lista fiszek w kolejności wpisania: awers (pogrubiony), pod nim rewers; bez numeru fiszki.
  - X (lewo) = usunięcie zestawu (po potwierdzeniu)
  - ołówek = edycja zestawu, tapnięcie fiszki = jej edycja, przytrzymanie = usunięcie — zawsze dostępne
  - rakieta = sesja z tego zestawu (jak na liście); gdy nie ma nic do powtórki, znika bez zostawiania pustego miejsca
- **Poziom fiszki w podglądzie** — po prawej pionowy pasek: szare tło, wypełnienie w kolorze akcentu fiszek rosnące od dołu (poziom / 5), nad nim liczba = poziom.
  - *nowa* = „0” i pusty pasek
  - *zamrożona* = cała karta półprzezroczysta (czeka na drugą szansę), pasek i liczba jak dla jej poziomu
  - *opanowana* = pełny pasek, zamiast liczby większa gwiazda ★ w kolorze akcentu; co kilka sekund (losowo, gwiazdy nie w jednym rytmie) „nadaje sygnał” — rozchodzący się i zanikający pierścień; wyłączone animacje w systemie = bez sygnału
- **Tworzenie i edycja zestawu** — dialog z nazwą zestawu i listą wierszy (awers + rewers), przyciskiem dodania wiersza i X usuwania wiersza.
  - brak nazwy blokuje zapis z komunikatem
  - wiersz całkiem pusty jest pomijany; wiersz wypełniony do połowy blokuje zapis z komunikatem
  - długa lista przewija się wewnątrz dialogu, przycisk dodania wiersza zostaje widoczny
- **Edycja karty** — dozwolona zawsze i nie zmienia postępu (poziom i termin zostają).
  - zmiana treści karty już powtarzanej (nie *nowej*) → toast *„Postęp nauki zostaje, ale zmiana treści może wpłynąć na naukę”*
  - nowa fiszka startuje jako *nowa*

## Sesja powtórek

- **Sesja** — *Dzisiejsza sesja* = jedna sesja ze wszystkich zestawów naraz; kolejka pokazywana po kolei na jednej karcie.
  - rakieta zestawu = te same reguły zawężone do zestawu; limit nowych i próg zaległości liczone zawsze globalnie
  - kolejność: drugie szanse → planowe powtórki (najniższy poziom najpierw) → nowe karty (do dziennego limitu, starsze zestawy najpierw)
  - stan sesji nie jest zapisywany — kolejka liczona za każdym razem z postępu kart, każda ocena zapisywana od razu, więc przerwanie sesji niczego nie gubi
  - kolejka ustalana na starcie sesji — karty, które staną się dostępne w jej trakcie, wejdą do następnej
- **Scrim** — ekran się przyciemnia, a z dołu wjeżdża karta z pierwszą fiszką.
  - karta pokazuje tylko tekst fiszki — bez nazwy zestawu
  - tapnięcie scrimu (poza kartą) albo *wstecz* = przerwanie sesji; ocenione fiszki są już zapisane, następna sesja bierze resztę
- **Awers** — tekst pytania; oko (lewy dół) = odkrycie fiszki (obrót karty); *odśwież* (prawy dół) = pominięcie.
  - pominięta fiszka trafia na koniec kolejki i wróci po pozostałych
  - gdy w kolejce została jedna fiszka, *odśwież* jest ukryty
- **Rewers** — tekst odpowiedzi; kciuk w górę = *Dobrze* (lewy dół), tylda = *Średnio* (środek), kciuk w dół = *Kiepsko* (prawy dół).
  - ocena przelicza poziom i termin fiszki (zob. *Reguły powtórek*), zapis od razu, następna fiszka
- **Pasek postępu** — na dolnej krawędzi karty; rośnie po każdej ocenionej fiszce (ocenione / wszystkie w sesji).
- **Karta końcowa** — nie należy do zestawu; po ostatniej ocenie pokazuje na scrimie *„Wszystkie fiszki zostały ogarnięte ;)”*, *„Dobrze: X · Średnio: Y · Kiepsko: Z”* i pełny pasek.
  - gdy coś zostało zamrożone: *„Zamrożone wrócą na drugą szansę za godzinę”*
  - „ptaszek” w prawym dolnym rogu = zamknięcie scrimu
  - pokazywana raz, zaraz po sesji
- **Ocena to nie edycja** — przebieg sesji nie zmienia daty modyfikacji zestawu, więc zestaw nie przeskakuje na liście; edycja zestawu ją zmienia.

## Reguły powtórek

- **Ocena przypomnienia** — po odkryciu rewersu trzy przyciski: kciuk w górę **[Dobrze]**, tylda **[Średnio]**, kciuk w dół **[Kiepsko]**.
- **Skutki oceny**:

  | Karta | [Dobrze] | [Średnio] | [Kiepsko] |
  |---|---|---|---|
  | **nowa** (pierwsza powtórka) | poziom 1, powtórka jutro | poziom 1, powtórka jutro | poziom 1 + zamrożenie |
  | **planowa powtórka** | poziom +1 (na poziomie 5 = opanowana) | poziom bez zmian | zamrożenie, poziom w zawieszeniu |
  | **druga szansa** | poziom bez zmian | poziom −1 | poziom −1 + ponowne zamrożenie |

  - jedyna droga w górę to [Dobrze] przy planowej powtórce; poprawka przy drugiej szansie ratuje poziom, ale go nie podnosi
  - poziom nie spada poniżej 1
- **Druga szansa** — zamrożona karta wraca najwcześniej 1 godzinę po ocenie.
  - niewykorzystana do końca dnia czeka dalej, bez spadku poziomu; następnego dnia wraca jako druga szansa
- **Termin kolejnej powtórki** — dzień faktycznej powtórki (nie planowanej) + interwał poziomu; karta jest dostępna przez cały ten dzień; spóźnienie nie obniża poziomu.
- **Opanowanie** — [Dobrze] przy planowej powtórce na poziomie 5 (30 dni).
- **Wstrzymanie nowych kart** — gdy zaległości przekraczają próg, nowe karty są odraczane (bez utraty); wracają po nadrobieniu zaległości.
  - komunikat z liczbą powtórek do nadrobienia, po których nowe wrócą

## Stany widżetu *Dzisiejsza sesja*

Treść wynika z kolejki wyliczonej z postępu kart (*R* = powtórki w kolejce, *N* = nowe w kolejce, *P* = zaległości ponad próg).

| Stan | Warunek | Tekst | Rakieta | Ramka |
|---|---|---|---|---|
| **A** | brak jakiejkolwiek fiszki (brak zestawów albo same puste) | widżet ukryty | – | – |
| **B** | kolejka niepusta, nowe nie są wstrzymane | *„Do powtórzenia: R · Nowe: N”* | ✅ | faluje |
| **C** | zaległości > próg, a nowe czekają | *„Do powtórzenia: R · Nowe: 0”*<br>*„Nowe fiszki wstrzymane - do nadrobienia: P”* | ✅ | faluje |
| **D** | kolejka pusta | *„Na dziś wszystko powtórzone :)”* | ukryta | prosta |

- stan „wstrzymane i pusto” nie występuje — wstrzymanie oznacza zaległości ponad próg, a zaległości są w kolejce, więc w stanie C rakieta jest zawsze
- odświeżanie: po każdej zmianie danych (ocena w sesji, edycja) i po powrocie na ekran; widżet nie odświeża się sam, gdy druga szansa odblokuje się, kiedy użytkownik jest na ekranie

### Scenariusze

1. **Pierwszy dzień** — 20 nowych, limit 10 → **B** *„Do powtórzenia: 0 · Nowe: 10”*
2. **Po sesji** (8× [Dobrze], 2× [Kiepsko]) — 2 karty zamrożone na godzinę, limit nowych wykorzystany → **D** *„Na dziś wszystko powtórzone :)”* ⚠️ nieprawda — za godzinę te 2 wrócą
3. **Godzinę później**, po powrocie na ekran → **B** *„Do powtórzenia: 2 · Nowe: 0”*
4. **Następny dzień** — 8 kart poziomu 1 do powtórki + 10 nowych → **B** *„Do powtórzenia: 8 · Nowe: 10”*
5. **Powrót po 2 tygodniach** — 40 zaległych, próg 30 → **C** *„Do powtórzenia: 40 · Nowe: 0”* / *„do nadrobienia: 10”*
6. **Przerwanie sesji po 10 ocenach** — zaległości spadają do 30 (nie przekraczają progu), nowe wracają → **B** *„Do powtórzenia: 30 · Nowe: 10”*
   - nowe nie dochodzą do trwającej sesji — kolejka ustalana na jej starcie; pojawią się w następnej
7. **Wszystko opanowane** → **D** *„Na dziś wszystko powtórzone :)”* ⚠️ też nieprawda — te karty już nigdy nie wrócą

### Znany problem: stan D skleja 4 sytuacje

- **D1** — naprawdę koniec na dziś, następna powtórka np. jutro
- **D2** — limit nowych wykorzystany, nowe czekają na kolejne dni
- **D3** — są zamrożone karty, które wrócą za < 1h (scenariusz 2)
- **D4** — wszystko opanowane (scenariusz 7)

Propozycja rozbicia (do decyzji, zob. *Do ustalenia*):

| Stan | Tekst |
|---|---|
| D3 (priorytet) | *„Drugie szanse: 2 - wrócą o 14:35”* |
| D1 / D2 | *„Na dziś wszystko powtórzone :)”* + *„Następna powtórka: jutro (8)”* |
| D4 | *„Wszystkie fiszki opanowane 🎉”* |

- opcjonalnie: odświeżenie widżetu w chwili odblokowania najbliższej drugiej szansy

## Dane, migracje, kopie zapasowe

- **Migracja bazy (v20 → v21)** — fiszki odpięte od myśli; każda myśl z fiszkami staje się zestawem:
  - nazwa zestawu = tytuł myśli; bez tytułu → *„Zestaw fiszek #<id>”*
  - daty zestawu = daty myśli
  - myśli, które miały **tylko** fiszki (bez tekstu, nagrania i zdjęcia), są usuwane
  - myśli nie mają już formy fiszek: znika przycisk dodawania, widżet w *Tworzeniu*, *Szczegółach* i *Strumieniu*
- **Migracja bazy (v21 → v22)** — system powtórek: dotychczasowe liczniki zaliczeń / oblań i stan sesji per zestaw przepadają; wszystkie karty startują jako *nowe*.
- **Kopia zapasowa** — zestawy i fiszki (z postępem powtórek) są w kopii zapasowej.
  - kopie sprzed systemu powtórek (baza ≤ v21) odtwarzają fiszki jako *nowe*
  - kopie z czasów fiszek w myślach (baza v19–v20) odtwarzają się jako zestawy — po jednym na myśl; myśli zostają (te z samymi fiszkami wracają puste)
  - kopie sprzed fiszek odtwarzają się bez nich

## Do ustalenia

- **Stan D widżetu *Dzisiejsza sesja*** — rozbicie na D1–D4 (zob. *Stany widżetu*).
- **Przypomnienie o zamrożonych kartach** — przed końcem dnia, jeśli są jeszcze karty czekające na drugą szansę; do ustalenia godzina „końca dnia” i mechanizm powiadomień (kolejny krok).
- **Opanowane karty** — podgląd i przywracanie do nauki (kolejny krok).
- **Przeglądanie wszystkich fiszek** — wyszukiwanie i filtry (zestaw / status) w *Utrwalaniu*.
- **Treść** — tylko tekst czy też obraz / audio; karty dwukierunkowe.
- **Import kart**.

---

**Poza zakresem (stan obecny):** przypomnienie o zamrożonych kartach przed końcem dnia; podgląd i przywracanie opanowanych kart; przeglądanie wszystkich fiszek z wyszukiwaniem / filtrami; karty ścieżek w *Warsztacie* nie korzystają jeszcze ze wspólnego widżetu fiszki (krok D); brak sortowania / filtrów / wyszukiwania zestawów.
