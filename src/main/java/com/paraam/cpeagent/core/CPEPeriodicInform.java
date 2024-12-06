package com.paraam.cpeagent.core;

import java.util.ArrayList;
import java.util.Date;

import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;

public class CPEPeriodicInform implements Runnable {

    CpeActions cpeActions;
    CpeDBReader confdb;
    String acsurl = null;
    String username = null;
    String passwd = null;
    String authtype = null;
    String useragent = null;
    XmlFormatter xmlFmt = null;
    boolean strangeAcs = false;

    public CPEPeriodicInform(CpeDBReader confdb, String acsurl, String username, String passwd, String authtype, String useragent, XmlFormatter xmlFmt, boolean strangeAcs) {
        this.confdb = confdb;
        this.acsurl = acsurl;
        this.username = username;
        this.passwd = passwd;
        this.authtype = authtype;
        this.useragent = useragent;
        this.xmlFmt = xmlFmt;
        this.strangeAcs = strangeAcs;
    }

    public void run() {
        try {
            while (true) {
                int sleepInterval = Integer.parseInt(((ConfParameter) confdb.confs.get(confdb.props.getProperty("PeriodicInformInterval"))).value);
                //int sleepInterval = Integer.parseInt(((ConfParameter)confdb.confs.get("InternetGatewayDevice.ManagementServer.PeriodicInformInterval")).value);
                Thread.sleep(sleepInterval * 1000);

                ArrayList<EventStruct> eventKeyList = new ArrayList<EventStruct>();
                EventStruct eventStruct = new EventStruct();
                eventStruct.setEventCode("2 PERIODIC");
                eventKeyList.add(eventStruct);
                CpeActions cpeactions = new CpeActions(confdb);
                Envelope informMessage = cpeactions.doInform(eventKeyList);

                try {
                    this.acsurl = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_URL"))).value;
                    this.username = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_Username"))).value;
                    this.passwd = ((ConfParameter) confdb.confs.get(confdb.props.getProperty("MgmtServer_Password"))).value;
                } catch (NullPointerException missingProps) {
                    System.out.println(" WARNING: ACS credential switch not available, missing property for MgmtServer_Username or MgmtServer_Password mapping in " + confdb.props.toString());
                }

                System.out.println("Sending Periodic Message at " + (new Date()));
                CPEClientSession session = new CPEClientSession(cpeactions, acsurl, username, passwd, authtype, useragent, xmlFmt, strangeAcs);
                session.sendInform(informMessage);

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
