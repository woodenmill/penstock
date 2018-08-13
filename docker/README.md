# Run locally

This directory allows you test your specifications locally. In order to run Zookeeper, Kafka, Prometheus and Grafana, type following command:

`docker-compose up -d --build`

| Component  | Url            |
|------------|----------------|
| Grafana    | localhost:3000 |
| Prometheus | localhost:9090 |
| Kafka      | localhost:9092 |


## Credits
The docker setup was made based on fantastic [vegasbrianc/prometheus](https://github.com/vegasbrianc/prometheus) repository.