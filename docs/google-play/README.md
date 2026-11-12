# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Reelkin-v<версия>.aab`.
  Первая версия для Play — **v1.3.0**. Секреты `SIGNING_*` в репозитории заданы; без них сборка
  подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
  Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.reelkin`.
  2. **Select key** → ключ с SHA-256 `1cd5490d77490a396b3d12e0015daa488df3658bc8c6571f8af58037f4452985`
     (сертификат CN=Reelkin, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.reelkin` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/reelkin/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Reelkin: Movie Diary & Pet` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из снимков README (`docs/screenshots/<язык>/*.jpg`):
Play не берёт картинки длиннее двух ширин, а снимки 1080×2341, поэтому экран уходит за нижний край
под подписью. Иконка — `docs/logo.png` (`tools/icon.py`). Нужен `pip install pillow`. Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderScreens*' && python tools/shots.py
python tools/play.py
```

На скриншотах — названия настоящих фильмов из демо-кинотеатра, но обложки пиксельные, нарисованные
приложением, а не постеры студий: Play к такому обычно не придирается. Если всё же придерётся,
переснимите демо с вымышленными названиями.

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Entertainment** |
| Tags (до 5) | Movie, TV, Entertainment, TV guide, Movie tickets, reviews & listings |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/reelkin/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Дневники фильмов (Letterboxd, TV Time, MyShows) живут в **Entertainment**. Игра
с питомцем здесь — оформление, а не механика ради механики, поэтому не Games. Категорию можно сменить позже.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Перенос с Кинопоиска требует аккаунта Кинопоиска, но это необязательная функция стороннего сервиса.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- Насилия, страха, сексуального контента, ненормативной лексики, наркотиков, азартных игр — **No**.
  Попкорн — внутриигровая валюта, которую только зарабатывают просмотрами; за деньги её не купить,
  покупок нет.
- **Users interact / share content** — **No**: общения между пользователями нет.
- **Web browser / unrestricted internet** — **No**: встроенный браузер открывает только Кинопоиск
  и вход Яндекса (`Kinopoisk.allowed`), остальные адреса не грузятся.
- **Location** — No.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
  Младше 13 не отмечать: иначе действует политика Families с отдельными требованиями.
- *Could your store listing unintentionally appeal to children?* → **No**: это дневник фильмов и
  сериалов со статистикой и переносом истории; питомец — оформление дневника. Если Google не согласится
  из-за пиксельного питомца, ответ меняется на Yes без последствий для 13+.

**News apps** — No.

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**

Нам ничего не приходит, но Play считает «сбором» любую передачу с телефона, в том числе в чужие
сервисы. Честный минимум:

- *Does your app collect or share any of the required user data types?* → **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (всё по HTTPS).
- *Do you provide a way for users to request that their data is deleted?* → **No** (аккаунтов нет,
  на серверах ничего не хранится; данные удаляются вместе с приложением).
- Типы данных:
  - **App activity → In-app search history** — названия, которые ищут (уходят в Wikidata, а название
    сериала — в MyShows/TVmaze за сезонами). Collected: **Yes**. Shared: **No** (передача по действию
    пользователя, которую он ожидает, — исключение из «sharing»). **Processed ephemerally: Yes**.
    Required: **No** (можно добавлять вручную). Purpose: **App functionality**.
  - **Personal info → User IDs** — логин MyShows при переносе истории. Collected: **Yes**. Shared: **No**.
    Processed ephemerally: **Yes**. Required: **No**. Purpose: **App functionality**.
- Ничего больше: аналитики, рекламы, идентификаторов устройства нет. Вход на Кинопоиск идёт на странице
  самого Яндекса во встроенном браузере, приложение его не читает. Из страниц оценок берутся только
  id фильмов, а в Wikidata уходят id, а не данные пользователя.
- Итог в карточке: «Data isn't shared with third parties», «Data is encrypted in transit».

Вариант строже — «No data collected» — формально неверен: в Play это понимают как «ничего не уходит
с телефона», а поиск уходит.

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — No.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Permissions** — особых деклараций не нужно: единственное разрешение — `INTERNET`. Картинки выбираются
через системный Photo Picker, файлы — через системный диалог. Foreground-сервисов, будильников
и уведомлений нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Reelkin-v1.3.0.aab` из GitHub Release.
3. Release name: `1.3.0`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; поиск фильма находит афишу; перенос из MyShows по логину и страница
   Кинопоиска во встроенном браузере открываются.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.reelkin`.
