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

package org.apache.jmeter.gui.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jorphan.collections.HashTree;

/**
 *
 */
public class UndoCommand implements Command {

    private static final Set<String> commands = new HashSet<String>();

    static {
        commands.add(ActionNames.UNDO);
        commands.add(ActionNames.REDO);
    }

    public void doAction(ActionEvent e) throws IllegalUserActionException {
        GuiPackage guiPackage = GuiPackage.getInstance();
        final String command = e.getActionCommand();

        TreePath path;
        if (command.equals(ActionNames.UNDO)) {
            path = guiPackage.getTreeModel().goInHistory(-1);
        } else if (command.equals(ActionNames.REDO)) {
            path = guiPackage.getTreeModel().goInHistory(1);
        } else {
            throw new IllegalArgumentException("Wrong action called: " + command);
        }

        // we need to go to recorded tree path
        // fixme: we have a problem with unselected tree item then
        // also the GUI reflects old GUI properties
        //final JTree tree = GuiPackage.getInstance().getMainFrame().getTree();
        //tree.setSelectionPath(path);
        guiPackage.updateCurrentGui();
        guiPackage.getMainFrame().repaint();
    }

    /**
     * @return Set<String>
     */
    public Set<String> getActionNames() {
        return commands;
    }

    // wrapper to use package-visible method
    public static void convertSubTree(HashTree tree) {
        Save executor = new Save();
        executor.convertSubTree(tree);
    }
}
