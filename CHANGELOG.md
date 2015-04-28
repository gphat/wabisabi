## 2.1.2
Features:
  - Add `uriParameters` arg to `search` to expose ElasticSearch's [URI Search](http://www.elastic.co/guide/en/elasticsearch/reference/1.4/search-uri-request.html) feature, thanks [mmollaverdi](https://github.com/mmollaverdi)

## 2.1.1
Bugfixes:
  - Fix some incorrect documention, thanks [mmollaverdi](https://github.com/mmollaverdi)

Features:
  - Add `putSettings` method, thanks [mmollaverdi](https://github.com/mmollaverdi)

## 2.1.0
Bugfixes:
  - Fix broken `refresh` method, thanks [mmollaverdi](https://github.com/mmollaverdi)

Improvements:
  - Upgrade to Scala 2.11.6
  - Upgrade to Elasticsearch 1.5.0

## 2.0.19
Features:
  - Add `getSettings` method, thanks [davidbkemp](https://github.com/davidbkemp)

Bugfixes:
  - Fix that `createIndex` wasn't properly setting any `settings` specified, thanks [davidbkemp](https://github.com/davidbkemp)

Improvements:
  - Improve reuse of code in unit tests, thanks [davidbkemp](https://github.com/davidbkemp)

## 2.0.18
Features:
  - Add multi search, thanks [davidbkemp](https://github.com/davidbkemp)

## 2.0.17

Features:
  - Add `ignoreConflicts` parameter to `putMapping`, thanks [sokrahta](https://github.com/sokrahta)

Improvements:
  - Documented the `putMapping` and `getMapping` methods in the README, thanks [sokrahta](https://github.com/sokrahta)
  - Documented the `shutdown()` method in the README for curious users

## 2.0.16

Features:
  - Add methods for [warmers](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/indices-warmers.html), thanks [jfenc91](https://github.com/jfenc91)

Bugfixes:
  - Extend testing timeouts in case things get slow with the in-VM ES

## 2.0.15

Features:
  - Start keeping a damned CHANGELOG
  - Bump to Scala 2.11.5
  - Include elasticsearch dep in tests to run in-VM ES for integration tests, thanks [mmollaverdi](https://github.com/mmollaverdi)
  - Add travisci
  - Added suggest, thanks [sokrahta](https://github.com/sokrahta)
