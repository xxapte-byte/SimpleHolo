# SimpleHolo (Paper 1.21.4, Java 21)

## Budowanie
Wymaga JDK 21 i Maven:   mvn package
Gotowy plik: target/SimpleHolo.jar  -> wrzuc do folderu plugins/

Bez instalowania czegokolwiek: wrzuc ten folder na GitHub, zakladka Actions -> Build -> pobierz artefakt SimpleHolo.jar.

## Przyklad topki (PlaceholderAPI + ajLeaderboards)
/holo create topkille &6&lTOP ZABOJSTWA
/holo addline topkille &e1. &f%ajlb_lb_statistic_player_kills_1_name% &7- &a%ajlb_lb_statistic_player_kills_1_value%
/holo addline topkille &e2. &f%ajlb_lb_statistic_player_kills_2_name% &7- &a%ajlb_lb_statistic_player_kills_2_value%
/holo addline topkille &e3. &f%ajlb_lb_statistic_player_kills_3_name% &7- &a%ajlb_lb_statistic_player_kills_3_value%

Uprawnienie: simpleholo.admin (domyslnie OP). Interwal odswiezania: config.yml -> refresh-ticks.
Uwaga: tekst jest wspolny dla wszystkich graczy (placeholdery globalne, np. topki). Placeholdery typu %player_name% nie beda per-gracz.
