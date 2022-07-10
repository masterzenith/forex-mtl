# A local proxy for Forex rates

Build a local proxy for getting Currency Exchange Rates

**Requirements**

Forex is a simple application that acts as a local proxy for getting exchange rates.
It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies,
so they don't have to worry about the specifics of third-party providers.

* The service returns an exchange rate when provided with 2 supported currencies
* The rate should not be older than 5 minutes
* The service should support at least 10,000 successful requests per day with 1 API token

In practice, this should require the following 2 points:

1. Create a `live` interpreter for the `oneframe` service. This should consume the [one-frame API](https://hub.docker.com/r/paidyinc/one-frame).
2. Adapt the `rates` processes (if necessary) to make sure you cover the requirements of the use case, and work around possible limitations of the third-party provider.
3. Make sure the service's own API gets updated to reflect the changes made in points 1 & 2.

## Getting started

Build and publish the docker image of this project.
```bash
sbt docker:publishLocal
```
Once the image is published, start all required images.
```bash
docker-compose up
```
Try to get the conversion rate between Indonesian Rupiah and Japanese Yen.
```bash
curl 'localhost:9090/rates?from=USD&to=JPY'
```
What about, asking for all possible permutations of exchange rates?
```bash
curl 'localhost:9090/rates'
```
Firing up unit testing and integration testing.
```bash
sbt test
sbt it:test # make sure docker-compose up has executed beforehand
```

## Technology used

- [Cats-effect](https://typelevel.org/cats-effect/)
- [fs2](https://fs2.io/index.html)
- [http4s](https://fs2.io/index.html)
- [Weaver Test](https://disneystreaming.github.io/weaver-test/docs/multiple_suites_logging)
- [circe](https://circe.github.io/circe/parsing.html)

## Approach

`oneframe` service supports multiple pairs of queries in one `GET` request. Instead of asking for only one exchange rate, we can also ask for other rates at once like GBP to USD, JPY to AUD, etc.
To get the most benefits out of this, Forex will literally take every permutation of our supported currencies and caches all the rate results taken from `oneframe`.
Of course, this will only work if our supported currencies are minimal. Querying all of 22350 currency combinations in the world in a single GET to `oneframe` doesn't sound like a good plan,
but given that our server only supports 14 currencies, the permutation is only 182 and luckily still in the acceptable range of the `oneframe` server.

The main goals of Forex are two-fold:
- Overcome the limitations of 1000 invocations per day that the `oneframe` server gives.
- If local cache is used, it must be no older than 5 minutes.

If we call `oneframe` every 86.4 seconds starting early in the day, the 1000th call will be at the very end of the day. That is, Forex tries to call `oneframe` 1000 times in a day by waiting 86.4 seconds
between each call, hence the cache age wouldn't be older than 86.4 seconds. And yes, we call `oneframe` greedily to update every currency combinations within each call :)

The scheduler to update the cache is implemented using `fs2` [src/main/scala/forex/app/stream/updater/package.scala]


## Code practices and structures

The initiator of this project used typelevel stacks and aimed to be more using scala in functional way. Aligned with this initiative, this project tries to follow functional programming principles
by avoiding side effects and impurity. Every impure expression will be wrapped inside an `IO` construct.

Forex structures the packages based on hexagonal architecture.

```
                               +------+
                               | boot | (boot knows everything, arrows are not shown)
                               +------+

                      +----------+   +------------+
                      | app/http |   | app/stream |
                      +---+------+   +--+---------+
                          |             |     
                          |     +-------+
                          |     |
   +--------------+  +----v-----v---+ +------------------+  +---------------+
   | interps/http |  | app/programs | | interps/inmemory |  | interps/dummy |
   +-----------+--+  +------------+-+ +--------+---------+  +-------+-------+
               |                  |            |                    | 
               |                  |            |                    | 
               |                +-v----+       |                    | 
               +--------------->| core |<------+--------------------+
                                +------+   
```
- `core` is the business logic. This layer must not depend on any infrastructure logic (Kafka, Cassandra, etc). Every
  [aggregate](https://dolittle.io/docs/reference/cli/runtime/aggregates/) in core has an algebra which will be implemented in the interpreter layer.
  Since our domain is small, the only algebra `core` exposed is [src/main/scala/forex/core/rates/algebra.scala].
  Examples of business logic are: generating new employee code, validating items, as long as it doesn't require infrastructure involvements.
- `interps` or interpreter, or sometimes also known as infrastructure layer from DDD world, implements the algebra exposed by `core`. For example, `dummy` implementation generates a dummy
  rate which might be handy for unit testing, but `interps/http` contains a HTTP client to get the rate from another server.
- `programs` acts as an intermediary between `app` and `core` by re-exposing another algebra. Internally it calls `core`'s algebra. Please note that we shouldn't
  write business logic here.
- `app` is the application layer. Forex has two `app`, one is for updating the cache every 86.4 seconds, and the other one is for serving user rate queries.
- `boot` is the highest layer and knows everything underneath it. Since components are modularized, `boot` will wire them together and start the application.
- `commons` is everything that doesn't contain Forex specific logic, and can be pretty much reusable on other projects. Note that `commons` is not even under the `forex` package.

## Room for improvements

- The discussed approach is only working if the supported currencies are minimal.
- Retry logic when calling `oneframe` API.
- More testing, or if possible, apply testing based on algebraic laws.
- OpenAPI specifications, probably using [tAPIr](https://tapir.softwaremill.com/en/latest/).
- Midstream the initialization process. Currently, we call `.compile.drain` twice. We can simplify it to become one.
- Simulation testing will verify that Forex never exceeds 1000 invocations (a `oneframe` constraint). A fake full day test ought to be implemented by supplying fake timer.
