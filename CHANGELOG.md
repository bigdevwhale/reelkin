# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [2.9.0] - 2026-11-25

### Added
- **Watched a while ago:** a stub takes the time as well as the day – just now, an hour or two ago, or any time from the clock – and the days now go back to the day before yesterday. The night and morning achievements count the time you set.
- **An episode watched earlier:** on a series' card, «watched earlier…» dates the next episode – an hour ago, last night, any day. A season's stub takes the date of its last episode.

### Fixed
- **−1 after an earlier episode:** taking back an episode finds it by its number, so one dated before the last no longer stays behind.

<!-- tg
Просмотр можно записать задним числом: не только день, но и время – час назад, два, вчера вечером
Серию тоже можно отметить позже: в карточке сериала «смотрел раньше…»
-->

## [2.8.0] - 2026-11-25

### Added
- **Pause the projector (#21):** an interval – the lights go up and the film waits on its frame. «Go on» plays it on.
- **From any episode (#24):** a series can start from the episode you're on, not only from a new season. Episodes seen before don't feed the pets.

### Fixed
- **Kinopoisk links Wikipedia doesn't know (#22):** a series now comes with its seasons, a film with its poster and name.
- **A paw too many (#23):** the cat, hamster, red panda, kitsune, maneki-neko and pug no longer hold something in a fifth paw.

<!-- tg
Проектор можно поставить на паузу: фильм подождёт на том же кадре
Сериал можно отметить с любой серии, а не только с нового сезона
Ссылки с Кинопоиска, которых нет в Википедии, теперь добавляются: сериал с сезонами, фильм с постером
У питомцев больше нет лишних лап
-->

## [2.7.0] - 2026-11-22

### Added
- **What next after a season (#16):** the season's stub asks – watch the next season, it's already seen (noted, and on to the one after), or no more for now (into the cupboard, to bring back later). Before, both buttons only led away and the next season had to be fetched from «taken off» by hand.

### Changed
- **Episodes read plainly (#11):** «season 3 · 4 of 12 watched», and «· 8 left» on the series card.
- **Links the catalogue doesn't know (#18):** a Kinopoisk link missing from Wikidata is searched by the words shared with it; if there are none, the search says so and «Add by hand» opens a series for a /series/ link.

### Fixed
- A season can have up to 999 episodes when added by hand (#18); it stopped at 99.

<!-- tg
После последней серии сезона можно сразу выбрать: смотрю следующий, уже посмотрен или продолжения нет
Серии теперь подписаны понятно: «просмотрено 4 из 12 · осталось 8»
В сезоне можно указать до 999 серий, а для ссылок, которых нет в базе, приложение прямо предлагает добавить вручную
-->

## [2.6.1] - 2026-11-19

### Fixed
- **Links that found nothing:** a link from the Wikipedia app or a mobile browser is no longer cut short in the bill's search, so the film is found by it.
- **Anime and web series in the search:** a series whose type the search didn't know, like an original net animation (Cyberpunk: Edgerunners), is now found by its name and by a link.

<!-- tg
Поиск по ссылке теперь понимает длинные ссылки из приложения Википедии и мобильного браузера.
Аниме и веб-сериалы, которые раньше не находились (например, «Киберпанк: Бегущие по краю»), теперь находятся и по названию, и по ссылке.
-->

## [2.6.0] - 2026-11-18

### Added
- **A backup of the whole cinema:** Settings → General → Backup saves everything into one file: the bill, the stubs, the episodes, the pets, the goals, the posters, your own backdrop and the settings. On another phone, load the file and the cinema is as you left it. Loading replaces what is on that phone, and asks first.
- **Moving through Lingvale:** the bridge to Lingvale can now hand it that same backup file and take one back, so Lingvale can move all the apps to a new phone at once. It still answers only Lingvale signed with our key, and Lingvale asks before anything is replaced.

### Fixed
- **The unit stays in sight:** in a film's card the length fields keep «h» and «min» beside the number once you start typing.

<!-- tg
Весь кинотеатр теперь можно сохранить в один файл и загрузить на другом телефоне: Настройки → Основное → Копия.
В файле афиша, корешки, питомцы, цели, постеры и настройки.
Lingvale теперь может перевезти на новый телефон все приложения сразу.
-->

## [2.5.0] - 2026-11-16

### Added
- **Add by a link:** paste a link from IMDb, Kinopoisk, Letterboxd, TMDB, TVmaze, MyAnimeList or Wikipedia into the search, or share it to Reelkin from a browser or another app, and the film is found by it (#13).

### Fixed
- A film's length can be typed in hours and minutes, like 1 h 42 min, not only in minutes (#12).

<!-- tg
Фильм теперь можно добавить по ссылке: вставь в поиск ссылку с Кинопоиска, IMDb, Letterboxd или TMDB или нажми «Поделиться → Reelkin» прямо в браузере.
Длину фильма можно указать в часах и минутах, например 1 ч 42 мин.
-->

## [2.4.0] - 2026-11-16

### Added
- **Lingvale, the village of the pets from all our apps:** Reelkin now has a small read-only bridge, so in Lingvale you can build the cinema and its residents move in with their days. The bridge answers only Lingvale signed with our key; it changes nothing and nothing leaves the phone.

<!-- tg
Питомцы Reelkin теперь могут жить в Lingvale – общей деревне наших приложений.
В Lingvale для них можно построить кинотеатр, и жильцы переедут туда сами вместе со своими днями.
Данные остаются в Reelkin: деревня их только читает.
-->

## [2.3.0] - 2026-11-14

### Added
- More genres besides the main one. The main genre still sets the poster, the spectator and the pet's form and feeds a pet that loves it half again; a loved genre among the others feeds it a quarter more. The bill's genre filter finds a title by any of its genres. Plain genre names from the search and the imports fill them in.

<!-- tg
Теперь у фильма может быть несколько жанров: главный и дополнительные.
Если питомец любит главный жанр – сытнее в полтора раза, если один из дополнительных – на четверть.
Фильтр в афише находит фильм по любому из его жанров.
-->

## [2.2.0] - 2026-11-14

### Added
- Subgenres under each of the 12 genres: cyberpunk, slasher, black comedy, space opera, 81 in all, or one of your own. They come with a search, an import from Kinopoisk or MyShows, show on the cards and stubs and are found by the search; the pets still go by the genre.
- A series started from a later season can have the seasons before it marked as watched, when it's added or later from its card. They stay in the diary as «seen before» and don't feed the pets or count in the statistics.

<!-- tg
Поджанры: киберпанк, слэшер, чёрная комедия, космоопера… всего 81, можно добавить и свой. Подтягиваются из поиска и импорта, видны на карточках и ищутся; питомцы по-прежнему смотрят на жанр.
Сериал с середины: прошлые сезоны можно отметить просмотренными. Они остаются в дневнике как «смотрел раньше» и не идут питомцам и в статистику.
-->

## [2.1.0] - 2026-11-14

### Added
- 17 anime pets, 49 in all: a tanuki, a kappa, a nekomata, a mecha, a maneki-neko, a kirin and more. Each loves anime (animation from Japan) in its own way, and its can opens the same way.
- An «Anime» shelf of 12 achievements and 20 more on the other shelves: 88 in all.

### Fixed
- A pet's closing eyelid no longer takes the colour of a blush.
- Films imported from MyShows keep their country.

<!-- tg
17 аниме-питомцев: тануки, каппа, нэкомата, меха-робот, манэки-нэко, кирин и другие – у каждого своя любовь к аниме.
Новая полка достижений «Аниме» и ещё 20 достижений на других полках – всего 88.
Исправлено: закрытые глаза питомцев больше не красятся в цвет румянца.
-->

## [2.0.2] - 2026-11-13

### Fixed
- «What's on tonight?»: the poster changes with the film after «Something else» – it used to keep showing the first pick's cover. The same fix applies to every poster that changes in place.

<!-- tg
Исправлено: в «Что ставим?» после «Другое» постер теперь меняется вместе с фильмом, а не остаётся от первого.
-->

## [2.0.1] - 2026-11-13

### Fixed
- The «Running» tab counts only the series you're watching; shelved ones still sit in the tab but no longer add to the number (#8).
- The Kinopoisk import says up front that its ratings come 50 to a page and every page needs «Next page» – only the first page used to be read (#7).

<!-- tg
Исправлено: в «Идут» число показывает только сериалы, которые смотришь сейчас, без снятых с проката.
При переносе с Кинопоиска теперь видно, что фильмы идут по 50 на страницу и нужно пролистать все страницы.
-->

## [2.0.0] - 2026-11-12

- 32 pets instead of 5, fairy-tale ones too (a little dragon, a unicorn, a phoenix, a kitsune, a back-row ghost…):
  three cans are open from the start, the rest open by the way you watch.
- Pets work as in Songling: egg (a film can), little one, half-grown, grown-up, old-timer; favourite films feed
  one and a half times as much and pay popcorn; a nature and, when grown, a form for good; only the pet in the
  booth eats, the others rest and come back when called. The pet of 1.x comes along with all it ate.
- A pet's card and a can's card in blocks: growing, favourite films, what they give, favourites eaten so far,
  nature, form, what it ate. Pets blink and close their eyes when asleep or happy, and breathe.
- The pets, the booth, its things and the hall's stage drawn anew (Codex image generation: tools/art_prompts.py,
  tools/art_gen.py, cut by tools/cut.py and tools/scene.py), three times finer than before. A new launcher icon.
- New navigation: the booth, the bill, the reel in the middle, the saga and the pets.
- The booth: the pet's card, «Tonight» with a mood to pick, a poster to swap to and the pet's word on it, the
  week's tasks, the running series. The buffet sells wallpapers, a rug, a plant and a clock, with a live try-on.
- The saga: the numbers for a week, a month, a year or all time, the pet's word, fact tiles that open into their
  whole story, goals, achievements, the calendar, the hall, the awards and postcards.
- Goals picked from 22, weekly tasks, and 56 achievements in steps, each step paying popcorn.
- Postcards for a feed or a story: a stub's review, an achievement, the booth, the pet, the numbers, films,
  series, genres, rhythm, the year, the map and the menagerie, with a QR code to Google Play if you like.
- The bill: a wall of stubs by month, search, filters by directors and cast (from Wikidata), people on the cards.
- Posters are shown as they are, no longer pixelated.
- Tips on the first visit to each screen.
- Traits and the cinema's sign are gone: the pets' own pictures carry their looks now.

<!-- tg
32 питомца вместо пяти, среди них дракончик, единорог, феникс и кицунэ – коробки открываются тем, как ты смотришь.
Питомцы и будка нарисованы заново, в буфете обои, ковёр, цветок и часы с примеркой.
Новая вкладка «Сага»: статистика, цели, 56 достижений и открытки для соцсетей.
«Сегодня вечером» подбирает фильм под настроение.
-->

## [1.4.0] - 2026-11-08

- A goal for series too: seasons to finish this year, with its own bar on the year card (#6).
- The year's goals move one at a time; held down, the number runs (#6).

<!-- tg
Цель на год теперь и для сериалов – сколько сезонов досмотреть, со своей полоской в афише.
«+» и «−» меняют цель по одному, а если держать – быстро прокручивают.
-->

## [1.3.2]

- **Support Reelkin:** Settings → More → StillCraft has a fifth line, «Поддержать Reelkin · на новые
  сеансы и тёплый попкорн», which opens the StillCraft support page in the browser.
- **The app is called Reelkin in Russian too**, as on Google Play: under the icon, on the first screen,
  in Settings, in the README and in the privacy policy.

<!-- tg
Новое: в Настройках, во вкладке «Ещё», появилась строчка «Поддержать Reelkin» — на новые сеансы и тёплый попкорн.
Приложение теперь и по-русски называется Reelkin, как в Google Play.
-->

## [1.3.1]

- **A big collection no longer stalls the app.** The stubs page lays out only the stubs on screen
  instead of all of them at once, posters are read and decoded off the main thread, only the latest
  hundred stay in memory, and posters found after an import are written in batches instead of one by
  one. On a Mi 9 with 1385 stubs: janky frames went from 91% to 2.5%, and opening the stubs page no
  longer freezes for 3.6 s.
- **Stats open on all time**, with a chip for every year that has stubs: films brought over from
  Kinopoisk or MyShows are dated the day they were watched, mostly in years past, so the current year
  alone hid them.
- **Posters for Kinopoisk films come from Kinopoisk itself** by the film's id, then from Wikipedia,
  then by name; a failed download is tried again and placeholders are turned away. Films brought over
  before this version get their missing posters once, in the background.
- Year bars in the stats show three-digit counts in full.
- A collection brought over before the film can opened no longer says «−1384 more and it opens».

<!-- tg
Большая коллекция больше не тормозит: список корешков и обложки грузятся по мере прокрутки.
Статистика открывается «за всё время» и сразу учитывает фильмы, перенесённые с Кинопоиска и MyShows, а ещё по ней можно смотреть любой год.
Обложки для фильмов с Кинопоиска берутся с самого Кинопоиска, а недостающие у уже перенесённых подтянутся сами.
-->

## [1.3.0]

- **Bring your history over** (Settings → Bring your history):
  - **MyShows** by a public profile's login, no password (#2). A finished series tears a stub for each
    season; one being watched tears its finished seasons and goes to Running at the episode reached;
    «will watch» goes to Waiting. MyShows doesn't show when an episode was watched, so a season's stub
    is dated by its last episode.
  - **Kinopoisk** (#4) has no export, so its own pages are opened in a browser inside the app and the
    films are picked up from every ratings page you go through, with the rating and the date. Wikidata
    fills in the year, the country, the genre and the length by the Kinopoisk id. A saved ratings page
    or any list of Kinopoisk links works too.
  - Nothing comes in twice, the posters follow in the background, and an import can be taken back.
- **When the next season comes out** (#1): a series' card, the bill and the booth say «season 4 comes
  out on…», «isn't out yet» or «no season 4 yet», and how many seasons are out. The seasons come from
  MyShows (TVmaze when MyShows doesn't know the show) and are checked again every couple of days. A
  season finished while the next isn't out puts the series in the cupboard, and each season takes its
  own number of episodes.
- **The season field says what it is:** «on season», with a note when it skips seasons that they won't
  count as watched (#1).
- Seasons marked as already watched now count their minutes.
- **Google Play:** the release also builds an AAB with every language; the store listing, graphics and a
  privacy policy are in `docs/google-play` and `PRIVACY.md`.

<!-- tg
Историю можно перенести: сериалы из MyShows по логину, оценки с Кинопоиска прямо с его страниц (Настройки → Перенести историю).
В карточке сериала видно, сколько сезонов вышло и когда выйдет следующий.
Поле «смотрю сезон» больше не путается с числом сезонов, а сезоны, отмеченные как уже просмотренные, теперь считаются в минутах.
-->

## [1.2.1]

- **A series already watched:** adding a series now offers «Уже посмотрел сезон N» next to «Смотрю».
  It tears a stub for that season, with popcorn, three words and the date, and puts the series in the
  cupboard at the next season, ready to bring back when it comes out.

<!-- tg
Сериал теперь тоже можно отметить как уже просмотренный: кнопка «Уже посмотрел сезон N» при добавлении.
Корешок за сезон отрывается сразу, а сериал ждёт в шкафу следующего сезона.
-->

## [1.2.0]

- **The day you watched it:** tearing a stub asks when you watched the film — today, yesterday or any
  earlier day from a calendar; a stub's date can be changed later on its card. The stats, the hall and
  the bill follow the date (#3).
- **Mystery/thriller:** the noir genre is now called «детектив/триллер» («mystery/thriller»), and
  thrillers, mysteries, suspense and spy films land in it instead of drama.

<!-- tg
Теперь можно указать, когда смотрел фильм: сегодня, вчера или любой день в календаре. Дату можно поменять и потом, в корешке.
Жанр «нуар» стал «детектив/триллер», и триллеры больше не попадают в драму.
-->

## [1.1.0]

- **The season you're on:** a series can start from any season, and the season can be changed later;
  a different season starts from its first episode.
- **The bill in three pages:** Waiting, Running and Stubs, with counts. The search field narrows what's
  yours as you type; a filter sheet narrows by films or series, genres, popcorn, the year watched and
  who you watched with; the stubs can be ordered by date, popcorn, title, length or release year.
- **Stats:** films, hours, average popcorn, episodes and seasons, countries; month by month or year by
  year, genres, popcorn, lengths, decades, countries, company and weekdays. Tap the year card.
- **A background picture** behind every screen, dimmed so the text stays clear: a public-domain photo
  for each genre or a red curtain, the genre of what's on now, or a picture of your own
  (Settings → Look). Credits in `assets/backdrops/CREDITS.txt`.
- **Real posters by default;** pixels are one tap away in Settings.
- **A new genre, reality shows,** with a studio-camera poster and a spectator filming on a phone.
- **Tidier screens:** the hall and the settings are split into pages; the booth has tiles for the
  buffet, the sign, the stats and the awards.
- «Cliffhanger!» reads naturally in Russian now; country stamps no longer break a word in two.

<!-- tg
Сериалы теперь можно начинать с любого сезона, а сезон — поменять в «Изменить».
Афиша разложена на «Ждут», «Идут» и «Корешки», есть фильтр по жанру, попкорну, году и компании, и сортировка.
Появилась статистика, фон за экранами (фото по жанру или своя картинка) и жанр «реалити-шоу».
Обложки по умолчанию теперь настоящие, а не пиксельные.
-->

## [1.0.0]

The first version.

- A pet hatches from a film can after the first three films; their genres decide who it is.
- The projectionist's booth: put a film on the projector and the pet watches it with you, then tear a stub
  (popcorn rating, three words, company, post-credits scene, rewatch).
- Minutes watched grow the pet; the third film of a genre gives it a trait.
- A hundred-seat hall of spectators dressed by genre, the sign built from first letters, the time machine
  of decades, country stamps.
- Series on season tickets: episodes, cliffhangers, finales, taking a series off without guilt.
- «What's on?» by time and mood, double bills, the year's awards, the buffet.
- Film search through Wikidata with Wikipedia posters, in pixels or as they are.

<!-- tg
Новое приложение «Бобина»: фильмы и сериалы растят зверька в будке киномеханика.
Ставишь фильм на проектор — питомец смотрит вместе с тобой, а на титрах отрываешь корешок.
Зал на сто мест заполняется зрителями, вывеска собирается из первых букв, сериалы идут по абонементу.
-->
