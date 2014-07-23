package org.apache.jmeter;

import org.apache.jmeter.engine.ClientJMeterEngine;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dzmitrykashlach on 4/30/14.
 */
public class EngineReInitializer extends Thread {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final String RMI_RETRIES_NUMBER = "rmi.retries_number";
    private static final String RMI_RETRIES_DELAY = "rmi.retries_delay";
    private static final String RMI_CONTINUE_ON_FAIL = "rmi.continue_on_fail"; // $NON-NLS-1$
    private static final boolean rmiContinueOnFail;
    private static int rmiRetriesDelay;
    private static int rmiRetriesNumber;

    private List<String> failedEnginesStr;
    private List<JMeterEngine> engineList = new ArrayList<JMeterEngine>();
    private HashTree tree;
    private int attemptNumber;
    static {
        rmiContinueOnFail = JMeterUtils.getPropDefault(RMI_CONTINUE_ON_FAIL, true);
        rmiRetriesDelay = JMeterUtils.getPropDefault(RMI_RETRIES_DELAY, 0);
        rmiRetriesNumber = JMeterUtils.getPropDefault(RMI_RETRIES_NUMBER, 0);
    }


    public EngineReInitializer(List<String> failedEnginesStr, HashTree tree, int attemptNumber) {
        this.failedEnginesStr = failedEnginesStr;
        this.tree = tree;
        this.attemptNumber = attemptNumber;
    }

    public static String getRmiRetriesDelayName() {
        return RMI_RETRIES_DELAY;
    }

    public static String getRmiContinueOnFailName() {
        return RMI_CONTINUE_ON_FAIL;
    }

    public static String getRmiRetriesNumberName() {
        return RMI_RETRIES_NUMBER;
    }

    public static int getRmiRetriesNumberValue() {
        return rmiRetriesNumber;
    }


    public static boolean isRmiContinueOnFailValue() {
        return rmiContinueOnFail;
    }

    public static int getRmiRetriesDelayValue() {
        return rmiRetriesDelay;
    }

    @Override
    public void run() {
        JMeterEngine jMeterEngine = null;
        for (String engine : failedEnginesStr) {
            try {
                sleep(rmiRetriesDelay);
                log.debug(String.valueOf(attemptNumber) + "/"
                        + String.valueOf(rmiRetriesNumber) + " retry to connect to " + engine + "...");
                System.err.println(String.valueOf(attemptNumber) + "/"
                        + String.valueOf(rmiRetriesNumber) + " retry to connect to " + engine + "...");


                jMeterEngine = new ClientJMeterEngine(engine);

                log.debug("Successfull re-connection with " + String.valueOf(attemptNumber) + "/"
                    + String.valueOf(rmiRetriesNumber) + " retry");
                System.err.println("Successfull re-connection with " + String.valueOf(attemptNumber) + "/"
                    + String.valueOf(rmiRetriesNumber) + " retry");
                jMeterEngine.configure(this.tree);
                engineList.add(jMeterEngine);
            } catch (Exception e) {
                log.fatalError("Failed to re-connect to remote host " + engine + ": " + String.valueOf(attemptNumber)
                        + " retry", e);
                System.err.println("Failed to re-connect to remote host " + engine + ": " + String.valueOf(attemptNumber)
                        + " retry");
        }
        }
        return;
    }

    public List<JMeterEngine> getEngineList() {
        return engineList;
    }
}

