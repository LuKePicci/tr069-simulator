cd "%~dp0"
java -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=8888 -Djavax.net.ssl.trustStore=FiddlerKeystore -Djavax.net.ssl.trustStorePassword=fiddler -jar target/tr069-simulator-0.7.6-SNAPSHOT.jar server simulator.yml

rem "jre1.8.0_231\bin\keytool.exe" -import -file FiddlerRoot.cer -J-Duser.language=en -keystore FiddlerKeystore -alias Fiddler