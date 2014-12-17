/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
 * This class is used for  re-initializing remote engines in case if
 * there were fails
 * Settings are picked up from jmeter.properties:
 *
 # whether or not retry to connect to remote engine if it is unavailable
 rmi.continue_on_fail=true
 # how many retries should we do
 rmi.retries_number=5
 # how large is interval in ms between retries
 rmi.retries_delay=15000

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
                log.warn(String.valueOf(attemptNumber) + "/"
                        + String.valueOf(rmiRetriesNumber) + " retry to connect to " + engine + "...");


                jMeterEngine = new ClientJMeterEngine(engine);

                log.warn("Successfull re-connection with " + String.valueOf(attemptNumber) + "/"
                    + String.valueOf(rmiRetriesNumber) + " retry");
                jMeterEngine.configure(this.tree);
                engineList.add(jMeterEngine);
            } catch (Exception e) {
                log.warn("Failed to re-connect to remote host " + engine + ": " + String.valueOf(attemptNumber)
                        + " retry", e);
        }
        }
        return;
    }

    public List<JMeterEngine> getEngineList() {
        return engineList;
    }
}
