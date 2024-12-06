package com.paraam.cpeagent;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.bazaarvoice.dropwizard.assets.ConfiguredAssetsBundle;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.paraam.cpeagent.core.CPEHttpServer;
import com.paraam.cpeagent.core.CPEPeriodicInform;
import com.paraam.cpeagent.core.CPEUtil;
import com.paraam.cpeagent.core.CPEWorker;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;

public class SimulatorService extends Service<SimulatorConfiguration> {
    // names of environment variables provided for Dockerfile.
    private static final String PI_INTERVAL = "PI_INTERVAL";
    private static final String AUTH_USER_NAME = "AUTH_USER_NAME";
    private static final String AUTH_CREDENTIAL = "AUTH_CREDENTIAL";
    private static final String AUTH_TYPE = "AUTH_TYPE";
    private static final String CR_PATH = "CR_PATH";
    private static final String CR_PORT = "CR_PORT";
    private static final String SIMULATOR = "SIMULATOR";
    private static final String ACS_URL = "ACS_URL";
    private static final String DUMP_PATH_FORMAT = "/dump/%s/";
    private static final String SERIAL_NUMBER_FORMAT = "SERIAL_NUMBER_FMT";
    private static final String SERIAL_NUMBER = "SERIAL_NUMBER";
    private static final String IP_ADDRESS = "IP_ADDRESS";
    private static final String STRANGE_ACS = "STRANGE_ACS";

    public static void main(final String[] args) throws Exception {
        try {
            new SimulatorService().run(args);
        } catch (final Exception e) {
            System.out.println("Error : " + e.getMessage());
        }
    }

    @Override
    public void initialize(final Bootstrap<SimulatorConfiguration> bootstrap) {
        bootstrap.setName("TR069-Simulator");
        bootstrap.addBundle(new ConfiguredAssetsBundle("/assets/", "/dashboard/"));
        bootstrap.addBundle(new ViewBundle());
        bootstrap.addBundle(GuiceBundle.newBuilder()
                .addModule(new SimulatorModule())
                .enableAutoConfig(getClass().getPackage().getName())
                .build());
    }

    @Override
    public void run(final SimulatorConfiguration configuration, final Environment environment) {
        final ArrayList<AgentConfig> agentConfigs = this.readAgentConfig();
        CPEUtil util = new CPEUtil();
        int threadcnt = 0;

        for (AgentConfig config : agentConfigs) {
            ArrayList<String> iplist = new ArrayList<String>();
            if (!util.isValidIPAddress(config.getStartIpAddress())) {
                continue;
            }
            if (!util.isValidIPAddress(config.getEndIpAddress())) {
                iplist.add(config.getStartIpAddress());
            } else {
                iplist = util.range2iplist(config.getStartIpAddress(), config.getEndIpAddress());
            }

            for (String ipaddr : iplist) {
                final CPEWorker worker = new CPEWorker(ipaddr, config.getConnectionRequestPort() + threadcnt, config.getAcsUrl(),
                        config.getConnectionRequestPath(), config.getPeriodicInformInterval(),
                        config.getSimulatorLocation(), config.getAuthUserName(), config.getAuthPassword(),
                        config.getAuthType(), config.getUserAgent(), config.getXmlFormat(),
                        config.getSerialNumberFmt(), config.getSerialNumber() + threadcnt, config.getStrangeAcs());
                final CPEHttpServer httpserver = worker.getNewHttpServer();
                Thread serverthread = new Thread(httpserver, "Http_Server");
                serverthread.start();
                final CPEPeriodicInform periodicInform = worker.getNewPeriodicInform();
                Thread informthread = new Thread(periodicInform, "Periodic_Inform");
                informthread.start();
                final Thread cpthread = new Thread(worker, "WorkerThread_" + threadcnt);
                cpthread.start();
                threadcnt++;
            }
        }
    }

    private ArrayList<AgentConfig> readAgentConfig() {
        final String confDir = "." + File.separator + "conf";
        final String filepath = confDir + File.separator + "agent.csv";
        // System.out.println("Current Filepath Checking >>>> " + filepath );
        final File userfile = new File(filepath);
        ArrayList<AgentConfig> config = userfile.exists() ? this.readAgentFile(filepath) : null;
        if (config == null) {
            config.add(this.readAgentEnvironment());
        }
        return config;
    }

    private ArrayList<AgentConfig> readAgentFile(final String filepath) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(false)
                .build();
        final CPEUtil util = new CPEUtil();
        final ArrayList<String> csvlist = util.parseFile(filepath);
        final ArrayList<String[]> tokenized = new ArrayList<String[]>();
        for (final String line : csvlist) {
            try {
                String[] tokens = parser.parseLine(line);
                if ((tokens.length > 0) && util.isValidIPAddress(tokens[0].trim())) {
                    tokenized.add(tokens);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (tokenized.isEmpty()) {
            return null;
        }

        final ArrayList<AgentConfig> agentConfigs = new ArrayList<AgentConfig>();

        for (String[] csvline : tokenized) {
            final AgentConfig config = new AgentConfig();

            // System.out.println("CSV Line >>>>>>>>> " + csvline);
            if (csvline.length >= 7) {
                config.setStartIpAddress(csvline[0].trim());
                config.setEndIpAddress(csvline[1].trim());
                config.setAcsUrl(csvline[2].trim());
                config.setConnectionRequestPath(csvline[3].trim());
                final String crPortVal = csvline[4].trim();
                config.setConnectionRequestPort(Integer.parseInt(crPortVal));
                final String informInterval = csvline[5].trim();
                config.setPeriodicInformInterval(Integer.parseInt(informInterval));
                config.setSimulatorLocation(csvline[6].trim());
            }
            // defaults if not set in the file
            config.setUserAgent("tr069-simulator");
            config.setXmlFormat("");
            config.setAuthType("");
            config.setAuthUserName("user");
            config.setAuthPassword("");

            if (csvline.length >= 10) {
                config.setAuthUserName(csvline[7].trim());
                config.setAuthPassword(csvline[8].trim());
                config.setAuthType(csvline[9].trim());
            }
            if (csvline.length >= 11) {
                config.setUserAgent(csvline[10].trim());
            }
            if (csvline.length >= 12) {
                config.setXmlFormat(csvline[11].trim());
            }
            if (csvline.length >= 13) {
                config.setSerialNumberFmt(csvline[12].trim());
                config.setSerialNumber(0);
            }
            if (csvline.length >= 14) {
                config.setSerialNumber(Integer.parseInt(csvline[13].trim()));
            }
            if (csvline.length >= 15) {
                config.setStrangeAcs(Boolean.parseBoolean(csvline[14].trim()));
            }
            agentConfigs.add(config);
        }
        return agentConfigs;
    }

    /**
     * Intended to read external properties from environment variables. Mostly
     * Useful in docker containers where this is an easier way to convey
     * configuration.
     *
     * @return Agent configuration object.
     */
    private AgentConfig readAgentEnvironment() {
        final AgentConfig config = new AgentConfig();
        config.setUserAgent("tr069-simulator");
        config.setXmlFormat("");

        final Map<String, String> environment = System.getenv();
        config.setStartIpAddress(this.getOrDefault(SimulatorService.IP_ADDRESS, environment, "127.0.0.1"));
        final String piIntervalInSec = this.getOrDefault(SimulatorService.PI_INTERVAL, environment, "600");
        config.setPeriodicInformInterval(Integer.parseInt(piIntervalInSec));
        config.setAuthType(this.getOrDefault(SimulatorService.AUTH_TYPE, environment, ""));
        config.setAuthUserName(this.getOrDefault(SimulatorService.AUTH_USER_NAME, environment, "user"));
        config.setAuthPassword(this.getOrDefault(SimulatorService.AUTH_CREDENTIAL, environment, ""));
        final String crPortVal = this.getOrDefault(SimulatorService.CR_PORT, environment, "8035");
        config.setConnectionRequestPort(Integer.parseInt(crPortVal));
        config.setConnectionRequestPath(this.getOrDefault(SimulatorService.CR_PATH, environment, "/"));
        final String simulator = this.getOrDefault(SimulatorService.SIMULATOR, environment, "calix_ont");
        config.setSimulatorLocation(String.format(SimulatorService.DUMP_PATH_FORMAT, simulator));
        config.setAcsUrl(this.getOrDefault(SimulatorService.ACS_URL, environment,
                "http://localhost:8080/services/acs"));
        config.setSerialNumberFmt(this.getOrDefault(SimulatorService.SERIAL_NUMBER_FORMAT, environment, "%08d"));
        final String serialNumberValue = this.getOrDefault(SimulatorService.SERIAL_NUMBER, environment, "0");
        config.setSerialNumber(Integer.parseInt(serialNumberValue));
        final String strangeAcsValue = this.getOrDefault(SimulatorService.STRANGE_ACS, environment, "false");
        config.setStrangeAcs(Boolean.getBoolean(strangeAcsValue));

        return config;
    }

    private String getOrDefault(final String key, final Map<String, String> map, final String defaultValue) {
        if (map.containsKey(key)) {
            return map.get(key);
        } else {
            return defaultValue;
        }
    }
}