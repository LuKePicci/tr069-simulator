package com.paraam.cpeagent.core;

import java.util.ArrayList;

import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.ID;
import org.dslforum.cwmp_1_0.EventStruct;

public class CPEWorker implements Runnable {

    String ip = "192.168.1.50";
    int port = 8030;
    String acsurl;
    String requrl;
    int informperiod = 1800;
    String dumploc;
    String username = null;
    String passwd = null;
    String authtype = null;
    String useragent = null;
    String serialNumberFormat = "CPE%08d";
    int serialNumber = 0;
    XmlFormatter xmlFmt = null;
    boolean strangeAcs = false;
    CpeDBReader confdb = null;

    public static void main(String args[]) {
        String mgmturl = "http://192.168.1.50:8085/ws?wsdl";
        CPEWorker worker = new CPEWorker("192.168.1.50", 8030, mgmturl, "/wsdl", 1800,
                "/dump/microcell/", null, null, null, "TR069 Simulator/0.7.0",
                "", "%08d", 0, false);
        Thread cpthread = new Thread(worker, "WorkerThread");
        cpthread.start();
    }

    public CPEWorker(String ip, int port, String acsurl, String requrl, int informperiod, String dumploc,
            String username, String passwd, String authtype, String useragent, String xmlformatter,
            String serialNumberFormat, int serialNumber, boolean strangeAcs) {
        this.ip = ip;
        this.port = port;
        this.acsurl = acsurl;
        this.requrl = requrl;
        this.dumploc = dumploc;
        this.username = username;
        this.passwd = passwd;
        this.authtype = authtype;
        this.useragent = useragent;
        this.informperiod = informperiod;
        this.serialNumberFormat = serialNumberFormat;
        this.serialNumber = serialNumber;
        this.strangeAcs = strangeAcs;

        String connreqURL = "http://" + ip + ":" + port + requrl;
        //System.out.println("ConnectionRequestURL >>>>>> " + connreqURL);
        this.confdb = CpeDBReader.readFromGetMessages(dumploc);

        //final CpeActions cpeActions = new CpeActions(confdb);
        //System.out.println("Loaded Properties >>>>>>>>> "  + confdb.props.toString());
        String serialNo = String.format(this.serialNumberFormat, this.serialNumber);
        ((ConfParameter) confdb.confs.get(confdb.props.getProperty("PeriodicInformInterval"))).value = "" + informperiod;
        ((ConfParameter) confdb.confs.get(confdb.props.getProperty("ConnectionRequestURL"))).value = connreqURL; // "/wsdl";
        ((ConfParameter) confdb.confs.get(confdb.props.getProperty("SerialNumber"))).value = serialNo;
        ((ConfParameter) confdb.confs.get(confdb.props.getProperty("ExternalIPAddress"))).value = ip;
        //((ConfParameter)confdb.confs.get("InternetGatewayDevice.ManagementServer.PeriodicInformInterval")).value = "1800";

        if (xmlformatter != null) {
            this.xmlFmt = new XmlFormatter(xmlformatter);
        }

        ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_URL"))).value = acsurl; // "http://192.168.1.50:8085/ws?wsdl";
        //((ConfParameter)confdb.confs.get("InternetGatewayDevice.ManagementServer.URL")).value = "http://192.168.1.50:8085/ws?wsdl";
        //((ConfParameter)confdb.confs.get("InternetGatewayDevice.DeviceInfo.SerialNumber")).value = "CP0816NT029";
        //((ConfParameter)confdb.confs.get("InternetGatewayDevice.DeviceInfo.SoftwareVersion")).value = "GroovyCPE_FW.1";

        try {
            ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_Username"))).value = username;
            ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_Password"))).value = passwd;
        } catch (NullPointerException missingProps) {
            System.out.println(" WARNING: ACS credential switch not abailable, missing property for MgmtServer_Username or MgmtServer_Password mapping in " + confdb.props.toString());
        }
    }

    public CPEHttpServer getNewHttpServer() {
        return new CPEHttpServer(confdb, acsurl, username, passwd, authtype, useragent, xmlFmt, strangeAcs);
    }

    public CPEPeriodicInform getNewPeriodicInform() {
        return new CPEPeriodicInform(confdb, acsurl, username, passwd, authtype, useragent, xmlFmt, strangeAcs);
    }

    @Override
    public void run() {
        boolean bootstrapped = false;
        while (!bootstrapped) {
            String bootstrapAcs = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_URL"))).value;
            ArrayList<EventStruct> eventKeyList = new ArrayList<EventStruct>();
            EventStruct eventStruct = new EventStruct();
            eventStruct.setEventCode("0 BOOTSTRAP");
            eventKeyList.add(eventStruct);
            eventStruct = new EventStruct();
            eventStruct.setEventCode("1 BOOT");
            eventKeyList.add(eventStruct);
            CpeActions cpeactions = new CpeActions(confdb);
            Envelope informMessage = cpeactions.doInform(eventKeyList);

            // boolean strangeAcs = false;
            if (!this.strangeAcs) {
                ID id = new ID();
                id.setMustUnderstand(true);
                String pk = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("ParameterKey"))).value;
                id.setString(pk.equals("") ? String.format("%d_%s", this.serialNumber, "SIM_TR69_ID") : pk);
                informMessage.getHeader().getObjects().add(id);
            }

            try {
                this.acsurl = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_URL"))).value;
                this.username = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_Username"))).value;
                this.passwd = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_Password"))).value;
            } catch (NullPointerException missingProps) {
                System.out.println(" WARNING: ACS credential switch not abailable, missing property for MgmtServer_Username or MgmtServer_Password mapping in " + confdb.props.toString());
            }

            CPEClientSession session = new CPEClientSession(cpeactions, acsurl, username, passwd, authtype, useragent, xmlFmt, strangeAcs);
            session.sendInform(informMessage);

            bootstrapped = bootstrapAcs.equals(((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_URL"))).value);
        }
    }

}
