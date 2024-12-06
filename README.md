tr069-simulator
===============

This is a Java based TR069 Simulator for CPE devices. Currently this simulator supports <a href="http://www.broadband-forum.org/cwmp/cwmp-1-0.xsd">cwmp-1-0.xsd</a>
schema. Jibx tool has been used to bind the schema file into java classes.

This simulator can support different type of CPE devices. Also you can simulate hundreds of devices using the same instance.

<b>How to install (without java SDK):</b><br>
To run this simulator,<br> 
1. Download the project and unzip it to a suitable location.<br>
2. Modify the agent.csv configuration file available in the ./conf directory.<br>

<b>To run:</b><br>
<div class="highlight"><pre>
java -jar target/tr069-x.y.z-SNAPSHOT.jar server simulator.yml<br>
</pre></div>
Or simply doubleclick the batch launcher (for Windows).
<b>Note:</b> Java 8 must be available in your system.

### agent.csv Configuration

<b>agent.csv</b> file is the csv configuration file that contains agent configurations. General summary:

- Start IP and End IP define a range of IP addresses, and the system runs a simulator thread for each
- The HTTP Port defines the port on which a simulator listens for connection requests
- The simulator reports a ConnectionRequestURL constructed from the IP address, port, and the path configured in agent.csv
- The Dump Location is a relative path to the folder containing the device configuration data
- User name, password, and authentication type are used to authenticate the device with ACS
- User agent for CPE SOAP HTTP requests
- SOAP XML format processors to adapt flaky ACS implementations
- The CPE serial number or a template to be auto incremented over the IP range

 Some examples:<br>
<div class="highlight"><pre>
<span class="c1">startip, endip, acs_url, conn_req_url, http_port, periodic_inform, dump_location, username, password, authtype, useragent, xmlformat, sn_format, sn_numeric</span>
<span class="kd">192.168.1.11, 192.168.1.11, http://tr069.me/tr069/ws?wsdl&probe=257ebf, /wsdl, 8035, 300, /dump/microcell/, user1, passwd1, basic, TR069 Simulator, normal, CPE_A</span>
<span class="kd">192.168.1.51, 192.168.1.120, http://tr069.me/tr069/ws?wsdl&probe=257ebf, /wsdl, 8035, 300, /dump/microcell/, nouser, nopass, none, TR069 Simulator, stripdec, CPE_%00d, 51</span>
<span class="kd">192.168.2.211, 192.168.2.220, http://tr069.me/tr069/ws?wsdl&probe=257ebf, /wsdl, 8035, 300, /dump/microcell/, user1, passwd1, digest, "MySIM, CWMP/1.0", stripdec+faketypes</span>
</pre></div>
First CPE IP Address <br> 
Last CPE IP Address<br>
Initial ACS Management Server URL<br>
Connection Request URL (HTTP GET query string)<br>
Inbound Connection Request HTTP Port<br>
Initial Period Inform Interval (in seconds)<br>
Dump Location Path (relative)<br>
ACS Management Server Username<br>
ACS Management Server Password<br>
ACS Management Server HTTP Auth Type (basic, digest, none)<br>
CPE HTTP User Agent<br>
XML Formatter Options (normal, stripdec, faketypes)<br>
Serial Number Format (fixed or printf style to accomodate numeric auto-increment)<br>
Serial Number Numeric (integer, incremented for each agent in IP interval)<br>

You can modify these parameters according to your requirements. To simulate multiple CPE devices, provide the start and<br>
end ipaddress. Periodic Inform Interval is in seconds. Simulator will send Inform request based on this parameter.<br><br> 

Dump Location Path is the directory path where simulator will read and load the CPE data model template.<br>
Simulator will check for two set of files.<br>
- getvalues.txt
- getnames.txt

getvalues.txt contains Name/Value data as XML Nodes. Simulator will respond to the ACS Server based on this Name/Value Pair.<br>
getnames.txt contains ParameterInfoStruct XML Nodes. Access detail about the parameters are retrieved from this file.
<br>
Currently, microcell device data model dump is being included with this repo and bundled in the JAR. If you wish to simulate a different CPE, either they need to create these two XML files manually, or they need to take a dump from the real CPE device by reading the GetParameterValuesResponse
and GetParameterNamesResponse for root node (e.g. InternetGatewayDevice, Device, ...).<br>

If the ACS Server supports HTTP Authentication, provide the username, password and authentication type. basic and digest 
methods are currently supported. If authentication is not supported, leave auth type empty or use none, these fileds will be ignopred.<br><br>

### Connection Request Testing

1. The connection request URL reported by the device as a TR-069 parameter is determined by the agent.csv IP address configuration, not the actual IP address
2. For connection request testing, you can put connection requests in the JMS queue using Postman with the test harness. In this case enter a URL that corresponds with the actual visible IP address and port of the simulator
3. For functional tests, the agent.csv configuration will have to be set up so the device reports a valid connection request URL

### SDK Requirements

- JDK 1.8, newer JDK do not include necessary JIBX libriaries and tooling

<br>

<b>License</b><br>
MIT License.<br>
