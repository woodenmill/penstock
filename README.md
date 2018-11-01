# Penstock [![Build Status](https://travis-ci.com/woodenmill/penstock.svg?branch=master)](https://travis-ci.com/woodenmill/penstock) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d80fdff9f48e456c88845dce16a594e6)](https://www.codacy.com/project/wojda/penstock/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=woodenmill/penstock&amp;utm_campaign=Badge_Grade_Dashboard)

Penstock (/ˈpɛnstɒk/) is a set of building blocks that transforms your favourite testing framework into powerful load test tool for streaming applications.

## Concept

```
XXXXX Penstock XXXXXX            XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
X                   X            X                                                                  X
X                   X            X                                                                  X
X                   X            X                      +--------------------+                      X
X  +-------------+  X            X  +-------------+     |                    |     +-------------+  X
X  | Load Runner +-----messages-----> Kafka Topic +-----> Your Streaming App +-----> Kafka Topic |  X
X  +-------------+  X            X  +-------------+     |                    |     +-------------+  X
X                   X            X                      +---------+----------+                      X
X                   X            X                                |                                 X
X                   X            X                                |                                 X
X                   X            X                                |metrics                          X
X                   X            X                                |                                 X
X                   X            X                                |                                 X
X  +------------+   X            X                       +--------v--------+                        X
X  | Assertions <------metrics---------------------------+ Your Metrics DB |                        X
X  +------------+   X            X                       +-----------------+                        X
X                   X            X                                                                  X
X                   X            X                                                                  X
XXXXXXXXXXXXXXXXXXXXX            XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

```

## Quickstart with sbt

Add following Bintray repository:
```
Resolver.bintrayRepo("woodenmill", "oss-maven")
```

Then add following dependency:
```
"io.woodenmill" %% "penstock" % "0.0.3"
```

## Running locally
1. Clone the repository
2. Start Kafka, Prometheus and Grafana locally by running:
```bash
 docker-compose -f ./docker/docker-compose.yml up -d --build
```
3. Run example load test - [GettingStartedSpec](./src/it/scala/io/woodenmill/penstock/examples/GettingStartedSpec.scala)
```bash
sbt it:test
```
4. Open Grafana dashboards (login: admin, pass: admin) [http://localhost:3000](http://localhost:3000)

To stop it:
```bash
docker-compose -f ./docker/docker-compose.yml down
```

## Q&A
Q: Why 'Penstock'?
> A penstock (fr. conduite forcée) is a sluice or gate or intake structure that controls water flow, or an enclosed pipe that delivers water to hydro turbines and sewerage systems. The term is inherited from the earlier technology of mill ponds and watermills.
>
> &mdash; [Wikipedia](https://en.wikipedia.org/wiki/Penstock)
