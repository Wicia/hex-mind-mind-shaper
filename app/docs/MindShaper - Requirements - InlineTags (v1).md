# Tagi w tekście notatki (@osoba / #projekt)

## Potrzeba i Cel

- **Tagowanie w trakcie pisania** — użytkownik oznacza osobę lub projekt w zwykłym tekście notatki, bez przechodzenia do osobnego pola tagów.
- **Czytelny podgląd** — w podglądzie notatki tagi są wyróżnione, a znaki markerów nie zaśmiecają tekstu.

## Wejścia, Dane, Stany

- **Dwa markery** — `@` oznacza *Osobę*, `#` oznacza *Projekt*.
  - tagiem jest **jedno słowo** tuż za markerem: litery (także polskie), cyfry, `_` i `-`
  - słowo kończy się na pierwszym innym znaku — `@vdv,` daje tag *vdv*
  - przykład: *spotkanie z @Michałem o #ProjektX wczoraj* → Osoba *Michałem*, Projekt *ProjektX*
- **Marker na początku słowa** — marker działa tylko na początku tekstu albo po spacji / nowej linii.
  - `cos@poczta.pl`, `abc#123`, adres z `#` w środku — nie tworzą tagu
  - sam marker bez słowa za nim (`@`, `@ vdv`) — nie tworzy tagu
- **Nagłówek to nie tag** — `# Tytuł` (ze spacją) pozostaje nagłówkiem Markdown; `#projekt` (bez spacji) jest tagiem.
- **Wyświetlanie** — w podglądzie notatki tag jest **pogrubiony**, w kolorze lekko jaśniejszym od tekstu, **bez znaku markera**.
  - dotyczy podglądu w strumieniu, na ekranie tworzenia myśli i w szczegółach myśli
  - w edytorze tekstu markery są widoczne — użytkownik edytuje dokładnie to, co wpisał

## Procesy, Opcje, Integracje

- **Tagi dopiero po zapisie** — w trakcie pisania nic się nie dzieje: tekst nie jest sprawdzany, tagi myśli ani listy tagów się nie zmieniają.
  - dopiero zapis uruchamia sprawdzenie, czy w tekście są tagi, i dopisuje nowe do myśli
  - dopiero wtedy nowe tagi pojawiają się na listach *Osoby* / *Projekty*
  - wyjście bez zapisu = żadnych nowych tagów
- **Tworzenie myśli** — tekst jest sprawdzany przy zapisie **całej myśli**; tagi z tekstu są dokładane do tagów z pola tagów.
  - samo zatwierdzenie notatki w edytorze zmienia tylko szkic — tagi jeszcze nie powstają
- **Edycja notatki w szczegółach** — tekst jest sprawdzany przy zatwierdzeniu notatki w edytorze; nowe tagi od razu trafiają do myśli.
- **Bez duplikatów** — tag, który myśl już ma, nie jest dodawany drugi raz; wielkość liter nie ma znaczenia (*Ania* i *ania* to ten sam tag).
- **Lista tagów** — tagi dodane z tekstu trafiają na listy *Osoby* / *Projekty* w Metadanych tak samo jak tagi z pola tagów.

---

**Poza zakresem (stan obecny):** usunięcie tagu z tekstu nie zdejmuje go z myśli — tagi z tekstu są tylko dokładane; tag wewnątrz formatowania Markdown (np. `**@vdv**`) jest wyróżniony w podglądzie, ale nie zostaje zapisany jako tag (zob. TODO).
