# Linki w tekście notatki

## Potrzeba i Cel

- **Linki bez formatowania** — użytkownik wkleja lub wpisuje adres strony w zwykłym tekście notatki, bez składni Markdown.
- **Szybkie przejście** — z podglądu notatki w strumieniu link otwiera się jednym tapnięciem, bez wchodzenia w szczegóły myśli.

## Wejścia, Dane, Stany

- **Co jest linkiem** — słowo zaczynające się od `http://`, `https://` albo `www.`.
  - adres z `www.` otwiera się jako `https://…`
  - adres bez przedrostka (`onet.pl`) — nie jest linkiem
  - adres e-mail (`cos@poczta.pl`) — nie jest linkiem
- **Koniec adresu** — adres kończy się na spacji / nowej linii.
  - znaki interpunkcyjne na końcu (`.`, `,`, `;`, `:`, `!`, `?`, `)`, `]`, cudzysłów) należą do zdania, nie do adresu — *zobacz https://x.pl.* → link *https://x.pl*
- **Link Markdown** — `[opis](url)` też jest linkiem; wyświetla się *opis*, otwiera się *url*.
- **Wyświetlanie** — link jest w stonowanym niebieskim kolorze, **podkreślony**, inną czcionką niż tekst notatki i nieco mniejszy (80% rozmiaru tekstu).
  - dotyczy podglądu w strumieniu, na ekranie tworzenia myśli i w szczegółach myśli
  - w edytorze tekstu adres jest zwykłym tekstem — użytkownik edytuje dokładnie to, co wpisał

## Procesy, Opcje, Integracje

- **Tapnięcie w trybie podglądu (strumień)** — otwiera link w przeglądarce / aplikacji obsługującej adres.
  - tapnięcie poza linkiem działa jak dotąd — otwiera myśl
  - przewinięcie listy zaczęte na linku nie otwiera linku
  - link otwiera się tylko, gdy palec zostanie puszczony na tym samym linku
- **Tapnięcie w trybie edycji (tworzenie myśli, szczegóły)** — link jest wyróżniony, ale tapnięcie otwiera edytor tekstu jak dotąd, a nie link.
- **Brak aplikacji do otwarcia** — nic się nie dzieje, aplikacja się nie zamyka.

---

**Poza zakresem (stan obecny):** adresy bez `http(s)://` / `www.` nie są rozpoznawane; adres zawierający znaki formatowania Markdown (np. `__init__`) może zostać zniekształcony w podglądzie, bo tekst jest najpierw przetwarzany jako Markdown; nawias zamykający na końcu adresu (np. `…_(film)`) jest odcinany od linku.
