# Penstock [![Build Status](https://travis-ci.com/woodenmill/penstock.svg?branch=master)](https://travis-ci.com/woodenmill/penstock) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/d80fdff9f48e456c88845dce16a594e6)](https://www.codacy.com/project/wojda/penstock/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=woodenmill/penstock&amp;utm_campaign=Badge_Grade_Dashboard)

Penstock (/ˈpɛnstɒk/) is a set of building blocks that transforms your favourite testing framework into powerful load test tool for streaming applications.


> A penstock (fr. conduite forcée) is a sluice or gate or intake structure that controls water flow, or an enclosed pipe that delivers water to hydro turbines and sewerage systems. The term is inherited from the earlier technology of mill ponds and watermills.
>
> &mdash; [Wikipedia](https://en.wikipedia.org/wiki/Penstock)

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
