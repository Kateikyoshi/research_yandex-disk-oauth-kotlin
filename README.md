Investigation project with the following goals:
1) Get OAuth2 token from Yandex
2) Get files from Yandex disk
3) Get better with Maven
4) Refresh Java skills

Unfortunately trying out R2DBC connection with H2 for R2dbcReactiveOAuth2AuthorizedClientService is impossible.
https://github.com/r2dbc/r2dbc-h2/issues/127 - this.
Clob iterator is blocking. If you use it reactive context, you are dead.
Only solution is to start with containerized Postgre.
Having Postgre allows us to persist our OAuth2AuthorizedClient when app restarts.

I fully understand that my WebClient calls are blocking, and thus I can't access yandex disk from the endpoint flow.
In other words, controller methods which return Mono or Flux can't block at any point. Whatever.
I don't have any will to fix that since I am only interested in Yandex itself, not Reactive libs at the moment.