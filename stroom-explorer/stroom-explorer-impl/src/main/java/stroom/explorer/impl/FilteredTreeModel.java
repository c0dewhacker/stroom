package stroom.explorer.impl;

import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerNodeKey;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FilteredTreeModel extends AbstractTreeModel<ExplorerNodeKey> {

    public FilteredTreeModel(final long id, final long creationTime) {
        super(id, creationTime);
    }

    @Override
    public void add(final ExplorerNode parent, final ExplorerNode child) {
        final ExplorerNodeKey childKey = child != null ? child.getUniqueKey() : null;
        final ExplorerNodeKey parentKey = parent != null ? parent.getUniqueKey() : null;

        parentMap.put(childKey, parent);
        childMap.computeIfAbsent(parentKey, k -> new LinkedHashSet<>()).add(child);
    }

    public ExplorerNode getParent(final ExplorerNode child) {
        if (child == null) {
            return null;
        }
        return parentMap.get(child.getUniqueKey());
    }

    public List<ExplorerNode> getChildren(final ExplorerNode parent) {
        final Set<ExplorerNode> children = childMap.get(parent != null ? parent.getUniqueKey() : null);
        return children != null ? children.stream().toList() : null;
    }
}