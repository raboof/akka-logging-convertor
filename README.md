# akka-logging-converter

Parses Akka logging and converts it to various other formats

## Running

The main class accepts multiple input files and 1 output file.

It auto-detects the input format and determines the desired output format based
on the file extension:

    sbt "run in1.txt in2.txt out.json"

    sbt "run in1.txt in2.txt out.shiviz"

## Kibana

Output of type json can be consumed by Kibana:

* Get docker-elk: `git clone git@github.com:raboof/docker-elk`
* Start kibana: `docker-compose up`
* Post the json to Kibana: `nc localhost 5000 < out.logs`
* Go to http://localhost:5601/app/kibana#/management/kibana/index
* Choose pattern `logstash-*`, Time Filter field name `@timestamp`
* Browse and filter the logs at http://localhost:5601/app/kibana#/discover

To clear kibana remove the images with 'docker-compose rm'

## ShiViz

Upload the ShiViz file to https://bestchai.bitbucket.io/shiviz/
