MySQL fulltext index bug reproducer
===================================

Reproduce steps:
1. Run docker, then open a terminal, run `run-mysql.sh` to start a MySQL server.
(Alternatively, directly run a MySQL server according to [docker-compose.yml](docker-compose.yml))
2. Open another terminal, run `run-test.sh` to run the test case.
3. Test fail is observed. An error message similar to below is observed.
```
FulltextIndexTest > realTestCase() FAILED
    org.opentest4j.AssertionFailedError: expected: <50> but was: <33>
```
The number `33` may change among different runs.

All the SQLs used are also printed to the console before these lines.
