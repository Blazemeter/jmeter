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

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import org.apache.jmeter.engine.TreeCloner;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.Load;
import org.apache.jmeter.gui.action.UndoCommand;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * Users expected record situations: initial empty tree; before node deletion;
 * before node insertion; after each walk off edited node (modifyTestElement)
 */
public class UndoHistory implements TreeModelListener, Serializable {
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

    /**
     * History item
     */
    private static class HistoryItem implements Serializable {

        private final HashTree tree;
        private final TreePath path;
        // maybe the comment should be removed since it is not used yet
        private final String comment;

        /**
         * @param copy
         * @param apath
         * @param acomment
         */
        public HistoryItem(HashTree copy, TreePath apath, String acomment) {
            tree = copy;
            path = apath;
            comment = acomment;
        }

        /**
         * @return {@link HashTree}
         */
        public HashTree getKey() {
            return tree;
        }

        /**
         * @return {@link TreePath}
         */
        public TreePath getValue() {
            return path;
        }

        /**
         * @return String comment
         */
        public String getComment() {
            return comment;
        }
    }

    private List<HistoryItem> history = new LimitedArrayList<HistoryItem>(25); // TODO Make this configurable or too many properties ?
    private int position = INITIAL_POS;
    /**
     * flag to prevent recursive actions
     */
    private boolean working = false;
    private boolean recording = true;

    public UndoHistory() {
    }

    /**
     * @return true if must not put in history
     */
    private boolean noop() {
        return !recording || working;
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
     * @param treeModel
     * @param path
     * @param comment
     */
    void add(JMeterTreeModel treeModel, TreePath path, String comment) {
        // don't add element if we are in the middle of undo/redo or a big loading
        if (noop()) {
            log.debug("Not adding history because of noop", new Throwable());
            return;
        }
        JMeterTreeNode root = (JMeterTreeNode) ((JMeterTreeNode) treeModel.getRoot());
        if (root.getChildCount() < 1) {
            log.debug("Not adding history because of no children", new Throwable());
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Adding history element: " + comment, new Throwable());
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

        // convert before clone
        UndoCommand.convertSubTree(tree);
        // cloning is required because we need to immute stored data
        TreeCloner cloner = new TreeCloner(false);
        tree.traverse(cloner);
        HashTree copy = cloner.getClonedTree();

        history.add(new HistoryItem(copy, path, comment));

        log.debug("Added history element, position: " + position + ", size: " + history.size());
        working = false;
    }

    public TreePath getRelativeState(int offset, JMeterTreeModel acceptorModel) {
        log.debug("Moving history from position " + position + " with step " + offset + ", size is " + history.size());
        if (offset < 0 && !canUndo()) {
            log.warn("Can't undo, we're already on the last record");
            return null;
        }

        if (offset > 0 && !canRedo()) {
            log.warn("Can't redo, we're already on the first record");
            return null;
        }

        position += offset;

        if (!history.isEmpty()) {
            HashTree newModel = history.get(position).getKey();
            acceptorModel.removeTreeModelListener(this);
            working = true;
            try {
                final GuiPackage guiInstance = GuiPackage.getInstance();
                guiInstance.clearTestPlan();
                HashTree newTree = guiInstance.addSubTree(newModel);
            } catch (Exception ex) {
                log.error("Failed to load from history", ex);
            }
            acceptorModel.addTreeModelListener(this);
            working = false;
        }
        log.debug("Current position " + position + ", size is " + history.size());
        // select historical path
        return history.get(position).getValue();
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
        log.debug("Nodes changed");
    }

    /**
     *
     */
    // FIXME: is there better way to record test plan load events? currently it records each node added separately
    public void treeNodesInserted(TreeModelEvent tme) {
        String name = tme.toString();
        log.debug("Nodes inserted " + name);
        final JMeterTreeModel sender = (JMeterTreeModel) tme.getSource();
        add(sender, getTreePathToRecord(tme), "Add " + name);
    }

    /**
     *
     */
    public void treeNodesRemoved(TreeModelEvent tme) {
        String name = tme.toString();
        log.debug("Nodes removed: " + name);
        add((JMeterTreeModel) tme.getSource(), getTreePathToRecord(tme), "Remove " + name);
    }

    /**
     *
     */
    public void treeStructureChanged(TreeModelEvent tme) {
        log.debug("Nodes struct changed");
        add((JMeterTreeModel) tme.getSource(), getTreePathToRecord(tme), "Complex Change");
    }

    /**
     * @param tme TreeModelEvent
     * @return TreePath
     */
    private TreePath getTreePathToRecord(TreeModelEvent tme) {
        TreePath path;
        if (GuiPackage.getInstance() != null) {
            path = GuiPackage.getInstance().getMainFrame().getTree().getSelectionPath();
        } else {
            path = tme.getTreePath();
        }
        return path;
    }

    /**
     * Resume inserting in UndoHistory
     */
    public void resumeRecording() {
        this.recording = true;
    }

    /**
     * Stop inserting in UndoHistory
     */
    public void pauseRecording() {
        this.recording = false;
    }
}
