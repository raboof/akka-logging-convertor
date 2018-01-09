To view logs in Kibana:

* Get docker-elk: `git clone git@github.com:deviantony/docker-elk`
* Add to `logstash/pipeline/logstash.conf`: 

```
input {
        tcp {
                port => 5000
                codec => json
        }
}
```

* Start kibana: `docker-compose up`
* Convert the logging to json (`sbt run`)
* Post it to Kibana: `nc localhost 5000 < out.logs`
* Go to http://localhost:5601/app/kibana#/management/kibana/index
* Choose pattern `logstash-*`, Time Filter field name `@timestamp`
* Browse and filter the logs at http://localhost:5601/app/kibana#/discover

To clear kibana remove the images with 'docker-compose rm'
