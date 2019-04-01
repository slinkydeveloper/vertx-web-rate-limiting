# Vertx Web Rate limiting experiments

## Rate Limiting & Quota
Rate limiting is a technical requirement for a backend: It ensures that the architecture behind the backend won’t be flooded with more requests that is able to fulfill. It is also useful to avoid DDoS attacks. To solve this issues api calls can be statically or dynamically limited using rate limiting algorithms

On public APIs landscape, APIs are usually sold with a specified calls limit in a time window: this limit is called quota

In this repo I'm experimenting both algorithms

## Quota
`vertx-web-rate-limiting-common` contains primitives and test utils for `QuotaHandler`

`vertx-web-rate-limiting-local` contains a `Buckets` implementation using local Vert.x maps

## Rate limiter
`vertx-web-rate-limiting-concurrency-limits` uses Netflix's `concurrency-limits` to implement a dynamic rate limiter using AIMD or Vegas algorithm.

I wrote an experiment for it in `vertx-web-rate-limiting-experiment`
