#!/usr/bin/env bash

liquibase --driver=org.postgresql.Driver \
      --classpath=target/postgresql-9.4.1212.jre7.jar \
      --changeLogFile=test-db.changelog.xml \
      --url="jdbc:postgresql://192.168.99.100:32771/crowdsource" \
      --username=postgres \
      --password= \
      --logLevel=debug \
      generateChangeLog