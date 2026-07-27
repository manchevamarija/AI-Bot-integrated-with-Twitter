# X integration

## Поддржани targets

| Тип | Пример | X навигација |
|---|---|---|
| `KEYWORD` | `Скопје` | live search за текстот |
| `HASHTAG` | `Македонија` | live search за `#Македонија` |
| `PROFILE` | `@username` | јавниот X профил |
| `FEED_URL` | X URL | директна навигација |

Македонскиот јазик не се филтрира со неподдржан `lang:mk` X оператор.
Ботот ги чита реалните резултати и потоа локалниот `LanguageDetector` го
пресметува confidence score-от.

## Live browser режим

```dotenv
X_EXTRACTION_MODE=LIVE_BROWSER
X_BROWSER_AUTO_LOGIN=false
BOT_HEADLESS=false
BOT_PROFILE_DIRECTORY=C:/Users/Marija/.aibot-x-chrome-profile
```

Workflow-от е read-only: не објавува, не лајкува, не следи профили и не праќа
пораки. Дозволени се само навигација, чекање, скролување и извлекување јавни
резултати.

## Опционален Claude

Claude decision-making е исклучен по default:

```dotenv
ANTHROPIC_ENABLED=false
ANTHROPIC_API_KEY=
```

Во оваа состојба `LlmClient` го задржува template contract-от, но користи
детерминистички read-only одлуки. Со валиден key може да се активира Claude
без промена на кодот.

## Trace и дедупликација

Trace-от чува:

- секоја browser акција и нејзината успешност;
- нови и дупликат објави по target;
- вкупни уникатни објави;
- број објави со медиуми;
- број објави над македонскиот праг.

Дедупликацијата прво користи source URL, потоа external X ID, а како последна
опција комбинација од автор и содржина.
