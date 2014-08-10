package org.apache.jmeter.gui.tree;

import org.apache.jorphan.collections.HashTree;

import java.io.Serializable;

/**
 * History item
 */
public class UndoHistoryItem implements Serializable {

    private final HashTree tree;
    private final int[] expandedRows;
    // maybe the comment should be removed since it is not used yet
    private final String comment;
    private int selectionRow;

    /**
     * @param copy     HashTree
     * @param expRows  TreePath
     * @param acomment String
     */
    public UndoHistoryItem(HashTree copy, int[] expRows, int aselectionRow, String acomment) {
        tree = copy;
        expandedRows = expRows;
        comment = acomment;
        selectionRow = aselectionRow;
    }

    /**
     * @return {@link org.apache.jorphan.collections.HashTree}
     */
    public HashTree getTree() {
        return tree;
    }

    /**
     * @return {@link int[]}
     */
    public int[] getExpandedRows() {
        return expandedRows;
    }

    /**
     * @return String comment
     */
    public String getComment() {
        return comment;
    }

    public int getSelectionRow() {
        return selectionRow;
    }
}
