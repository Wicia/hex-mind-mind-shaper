# Metadane — listy tagów (Osoby / Projekty)

## Potrzeba i Cel

- **Przegląd tagów w jednym miejscu** — użytkownik widzi wszystkie swoje tagi wraz z tym, jak często ich używa.
- **Porządkowanie nazewnictwa** — możliwość poprawienia nazwy tagu bez wchodzenia w pojedyncze myśli.

## Wejścia, Dane, Stany

- **Dwa rodzaje tagów** — *Osoby* i *Projekty*; każdy ma własną, niezależną listę.
  - w danym momencie widoczna jest **tylko jedna** lista
  - **Osoby** — ludzie, o których myśl mówi lub których dotyczy
  - **Projekty** — sprawy i przedsięwzięcia, do których myśl się odnosi
- **Kafelek tagu** — nazwa tagu + licznik myśli, które go używają.
  - kolejność: od najczęściej używanych
  - układ: po dwa kafelki w rzędzie
  - tag bez powiązanych myśli zostaje na liście z licznikiem `0`
- **Tag „ja" na liście Osób** — tag własnej osoby jest **zawsze pierwszy**, niezależnie od liczby użyć.
  - pojawia się na liście dopiero, gdy zostanie użyty — nie jest tworzony z góry
- **Zapis małymi literami** — wielkość liter nie odróżnia tagów: *Ania* i *ania* to ten sam tag.
- **Przełącznik zakładek** — jeden sklejony pasek z zakładkami; aktywna zakładka jest wyróżniona i wskazuje dzióbkiem listę pod spodem.
- **Stan pusty** — gdy dla danego rodzaju nie ma żadnego tagu, zamiast listy pojawia się komunikat.
  - *Brak osób w tagach* / *Brak projektów w tagach* — zależnie od wybranej zakładki
- **Zapamiętany wybór** — po powrocie z innego ekranu widoczna jest ostatnio oglądana zakładka.

## Procesy, Opcje, Integracje

- **Przełączanie zakładek** — dotknięcie zakładki podmienia listę na tagi tego rodzaju.
- **Zmiana nazwy tagu** — dotknięcie kafelka otwiera edycję nazwy; zatwierdzenie zmienia nazwę **we wszystkich myślach** korzystających z tego tagu.
  - pusta nazwa → *Podaj nazwę*, zmiana nie zostaje zapisana
  - nazwa zajęta przez inny tag tego samego rodzaju → *Taki tag już istnieje*, zmiana nie zostaje zapisana
  - nazwy nie kolidują między rodzajami — ten sam tekst może być osobno Osobą i Projektem
- **Kopia zapasowa** — tagi i ich powiązania z myślami wchodzą do kopii zapasowej i wracają przy jej wczytaniu.

---

**Poza zakresem (stan obecny):** usuwanie tagów z poziomu listy; tag, który przestał być używany, pozostaje widoczny z licznikiem `0`.
