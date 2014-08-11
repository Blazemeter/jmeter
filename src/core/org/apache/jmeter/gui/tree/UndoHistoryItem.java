package org.apache.jmeter.gui.tree;

import org.apache.jorphan.collections.HashTree;

import java.io.Serializable;

/**
 * Undo history item
 */
public class UndoHistoryItem implements Serializable {

    private final HashTree tree;
    // TODO: find a way to show this comment in menu item and toolbar tooltip
    private final String comment;

    /**
     * @param copy     HashTree
     * @param acomment String
     */
    public UndoHistoryItem(HashTree copy, String acomment) {
        tree = copy;
        comment = acomment;
    }

    /**
     * @return {@link org.apache.jorphan.collections.HashTree}
     */
    public HashTree getTree() {
        return tree;
    }

    /**
     * @return String comment
     */
    public String getComment() {
        return comment;
    }
}
