cd "%~dp0"
java -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=8888 -jar target/tr069-simulator-0.7.6-SNAPSHOT.jar server simulator.yml