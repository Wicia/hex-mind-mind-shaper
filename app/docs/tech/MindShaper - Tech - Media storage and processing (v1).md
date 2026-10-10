# TECH: Zdjęcia i nagrania — jak to działa

Notatka o architekturze media w myślach: jak zapisujemy i jak wczytujemy zdjęcia oraz nagrania głosowe.

## Gdzie trzymane

- Oba media (zdjęcie, nagranie) trzymane **w bazie**, bezpośrednio przy myśli — jako dane binarne.
- Obok danych binarnych trzymane są **metadane**: rozmiar zdjęcia oraz długość nagrania. To one, nie same bajty, decydują o tym, czy myśl „ma zdjęcie / ma nagranie".

## Zapis

**Zdjęcie — kompresowane przed zapisem:**
- najpierw korekta orientacji (poziomo/pionowo wg danych EXIF),
- skalowanie do rozsądnego maksimum,
- zapis jako JPEG.

**Nagranie — zapisywane bez kompresji:**
- plik audio ląduje w bazie w oryginalnej postaci,
- rozmiar ograniczony pośrednio przez limit długości nagrania.

## Wczytywanie

Wspólna zasada: **leniwie, na żądanie**.

- Listy i widok szczegółów pobierają **tylko metadane**, nie same media. Dzięki temu wiadomo, czy pokazać ikonę/miniaturkę, bez ładowania ciężkich danych.
- Właściwe bajty wczytywane są **osobno, dopiero gdy trzeba** je pokazać lub odtworzyć:
  - **zdjęcie** — dekodowane do miniaturki (podgląd) lub pełnego rozmiaru (po kliknięciu),
  - **nagranie** — zapisywane chwilowo do pliku tymczasowego, z którego gra odtwarzacz.

## Zasada nadrzędna

**Widoki, które pokazują listę lub podgląd, nigdy nie ładują binarnych media razem z myślą.** Media wczytuje się zawsze osobno, per pojedynczy rekord, w momencie faktycznej potrzeby wyświetlenia lub odtworzenia.
