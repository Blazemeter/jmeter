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

package org.apache.jmeter.gui.tree;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.swing.tree.TreePath;
import org.apache.jmeter.util.JMeterUtils;

/**
 *
 */
public class UndoHistoryTest extends junit.framework.TestCase {

    public UndoHistoryTest() {
        File propsFile = null;
        try {
            propsFile = File.createTempFile("jmeter-plugins", "testProps");
            propsFile.deleteOnExit();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        //propsFile=new File("/home/undera/NetBeansProjects/jmeter/trunk/bin/jmeter.properties");

        JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
        JMeterUtils.setLocale(new Locale("ignoreResources"));
    }

    /*
     * public void testGetTestElementCheckSum() {
     * System.out.println("getTestElementCheckSum"); TestElement el = new
     * TestAction(); int result = UndoHistory.getTestElementCheckSum(el);
     * assertTrue(result!=0); el.setProperty(new BooleanProperty());
     * assertTrue(result != UndoHistory.getTestElementCheckSum(el)); }
     *
     * public void testGetTestElementCheckSum_stable() {
     * System.out.println("getTestElementCheckSum stable"); TestElement el = new
     * ThreadGroup(); AbstractJMeterGuiComponent gui = new ThreadGroupGui();
     *
     * gui.modifyTestElement(el); int result1 =
     * UndoHistory.getTestElementCheckSum(el); gui.modifyTestElement(el); int
     * result2 = UndoHistory.getTestElementCheckSum(el); assertEquals(result1,
     * result2); el.setProperty(new BooleanProperty()); assertTrue(result1 !=
     * UndoHistory.getTestElementCheckSum(el)); }
     */
    public void testClear() {
        System.out.println("clear");
        UndoHistory instance = new UndoHistory();
        instance.clear();
    }

    public void testAdd() throws Exception {
        System.out.println("add");
        JMeterTreeModel treeModel = new JMeterTreeModel();
        UndoHistory instance = new UndoHistory();
        instance.add(treeModel, new TreePath(this), "");
    }

    public void testGetRelativeState() throws Exception {
        System.out.println("getRelativeState");
        JMeterTreeModel treeModelRecv = new JMeterTreeModel();
        UndoHistory instance = new UndoHistory();

        // safety check
        instance.getRelativeState(-1, treeModelRecv);
        instance.getRelativeState(1, treeModelRecv);


        JMeterTreeModel treeModel1 = new JMeterTreeModel();
        JMeterTreeModel treeModel2 = new JMeterTreeModel();
        JMeterTreeModel treeModel3 = new JMeterTreeModel();
        instance.add(treeModel1, new TreePath(this), "");
        instance.add(treeModel2, new TreePath(this), "");
        instance.add(treeModel3, new TreePath(this), "");

        // regular work check
        instance.getRelativeState(-1, treeModelRecv);
        instance.getRelativeState(-1, treeModelRecv);
        instance.getRelativeState(-1, treeModelRecv); // undo ignored
        instance.getRelativeState(1, treeModelRecv);
        instance.getRelativeState(1, treeModelRecv);
        instance.getRelativeState(1, treeModelRecv); // redo ignored

        // overwrite check
        instance.getRelativeState(-1, treeModelRecv);
        instance.getRelativeState(-1, treeModelRecv);
        instance.add(treeModel3, new TreePath(this), "");
    }
}
