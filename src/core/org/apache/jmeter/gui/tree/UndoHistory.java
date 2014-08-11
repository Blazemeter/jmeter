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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.gui.action.UndoCommand;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * Users expected record situations: initial empty tree; before node deletion;
 * before node insertion; after each walk off edited node (modifyTestElement)
 */
public class UndoHistory implements TreeModelListener, Serializable {
    private ArrayList<Integer> savedExpanded = new ArrayList<Integer>();
    private int savedSelected = 0;

    /**
     * Avoid storing too many elements
     *
     * @param <T>
     */
    private static class LimitedArrayList<T> extends ArrayList<T> {
        /**
         *
         */
        private static final long serialVersionUID = -6574380490156356507L;
        private int limit;

        public LimitedArrayList(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(T item) {
            if (this.size() + 1 > limit) {
                this.remove(0);
            }
            return super.add(item);
        }
    }

    private static final int INITIAL_POS = -1;
    private static final Logger log = LoggingManager.getLoggerForClass();

    private List<UndoHistoryItem> history = new LimitedArrayList<UndoHistoryItem>(25); // TODO Make this configurable or too many properties ?
    private int position = INITIAL_POS;

    /**
     * flag to prevent recursive actions
     */
    private boolean working = false;

    public UndoHistory() {
    }

    /**
     * @return true if must not put in history
     */
    private boolean noop() {
        return working;
    }

    /**
     *
     */
    public void clear() {
        if (noop()) {
            return;
        }
        log.debug("Clearing undo history", new Throwable());
        history.clear();
        position = INITIAL_POS;
    }

    /**
     * this method relies on the rule that the record in history made AFTER
     * change has been made to test plan
     *
     * @param treeModel JMeterTreeModel
     * @param comment   String
     */
    public void add(JMeterTreeModel treeModel, String comment) {
        // don't add element if we are in the middle of undo/redo or a big loading
        if (noop()) {
            log.debug("Not adding history because of noop");
            return;
        }
        JMeterTreeNode root = (JMeterTreeNode) treeModel.getRoot();
        if (root.getChildCount() < 1) {
            log.debug("Not adding history because of no children", new Throwable());
            return;
        }

        String name = ((JMeterTreeNode) treeModel.getRoot()).getName();

        if (log.isDebugEnabled()) {
            log.debug("Adding history element " + name + ": " + comment, new Throwable());
        }

        working = true;
        // get test plan tree
        HashTree tree = treeModel.getCurrentSubTree((JMeterTreeNode) treeModel.getRoot());
        // first clone to not convert original tree
        tree = (HashTree) tree.getTree(tree.getArray()[0]).clone();

        position++;
        while (history.size() > position) {
            log.debug("Removing further record, position: " + position + ", size: " + history.size());
            history.remove(history.size() - 1);
        }

        // cloning is required because we need to immute stored data
        HashTree copy = UndoCommand.convertSubTree(tree);

        history.add(new UndoHistoryItem(copy, comment));

        log.debug("Added history element, position: " + position + ", size: " + history.size());
        working = false;
    }

    public void getRelativeState(int offset, JMeterTreeModel acceptorModel) {
        log.debug("Moving history from position " + position + " with step " + offset + ", size is " + history.size());
        if (offset < 0 && !canUndo()) {
            log.warn("Can't undo, we're already on the last record");
            return;
        }

        if (offset > 0 && !canRedo()) {
            log.warn("Can't redo, we're already on the first record");
            return;
        }

        if (history.isEmpty()) {
            log.warn("Can't proceed, the history is empty");
            return;
        }

        position += offset;

        final GuiPackage guiInstance = GuiPackage.getInstance();

        saveTreeState(guiInstance);

        loadHistoricalTree(acceptorModel, guiInstance);

        log.debug("Current position " + position + ", size is " + history.size());

        restoreTreeState(guiInstance);

        guiInstance.updateCurrentGui();
        guiInstance.getMainFrame().repaint();
    }

    private void loadHistoricalTree(JMeterTreeModel acceptorModel, GuiPackage guiInstance) {
        HashTree newModel = history.get(position).getTree();
        acceptorModel.removeTreeModelListener(this);
        working = true;
        try {
            guiInstance.getTreeModel().clearTestPlan();
            guiInstance.addSubTree(newModel);
        } catch (Exception ex) {
            log.error("Failed to load from history", ex);
        }
        acceptorModel.addTreeModelListener(this);
        working = false;
    }

    /**
     * @return true if remaing items
     */
    public boolean canRedo() {
        return position < history.size() - 1;
    }

    /**
     * @return true if not at first element
     */
    public boolean canUndo() {
        return position > INITIAL_POS + 1;
    }

    public void treeNodesChanged(TreeModelEvent tme) {
        String name = ((JMeterTreeNode) tme.getTreePath().getLastPathComponent()).getName();
        log.debug("Nodes changed " + name);
        final JMeterTreeModel sender = (JMeterTreeModel) tme.getSource();
        add(sender, "Node changed " + name);
    }

    /**
     *
     */
    // FIXME: is there better way to record test plan load events? currently it records each node added separately
    public void treeNodesInserted(TreeModelEvent tme) {
        String name = ((JMeterTreeNode) tme.getTreePath().getLastPathComponent()).getName();
        log.debug("Nodes inserted " + name);
        final JMeterTreeModel sender = (JMeterTreeModel) tme.getSource();
        add(sender, "Add " + name);
    }

    /**
     *
     */
    public void treeNodesRemoved(TreeModelEvent tme) {
        String name = ((JMeterTreeNode) tme.getTreePath().getLastPathComponent()).getName();
        log.debug("Nodes removed: " + name);
        add((JMeterTreeModel) tme.getSource(), "Remove " + name);
    }

    /**
     *
     */
    public void treeStructureChanged(TreeModelEvent tme) {
        log.debug("Nodes struct changed");
        add((JMeterTreeModel) tme.getSource(), "Complex Change");
    }

    /**
     * @param guiPackage
     * @return int[]
     */
    private void saveTreeState(GuiPackage guiPackage) {
        savedExpanded.clear();

        MainFrame mainframe = guiPackage.getMainFrame();
        if (mainframe != null) {
            final JTree tree = mainframe.getTree();
            savedSelected = tree.getMinSelectionRow();

            for (int rowN = 0; rowN < tree.getRowCount(); rowN++) {
                if (tree.isExpanded(rowN)) {
                    savedExpanded.add(rowN);
                }
            }
        }
    }

    private void restoreTreeState(GuiPackage guiInstance) {
        final JTree tree = guiInstance.getMainFrame().getTree();

        if (savedExpanded.size() > 0) {
            for (int rowN : savedExpanded) {
                tree.expandRow(rowN);
            }
        } else {
            tree.expandRow(0);
        }
        tree.setSelectionRow(savedSelected);
    }

}
