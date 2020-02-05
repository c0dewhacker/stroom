/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.explorer.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import stroom.docref.DocRef;
import stroom.docref.HasDisplayValue;

import java.util.List;
import java.util.Objects;

public class ExplorerNode implements HasDisplayValue {
    private String type;
    private String uuid;
    private String name;
    private String tags;

    private int depth;
    private String iconUrl;
    private NodeState nodeState;

    private List<ExplorerNode> children;

    public ExplorerNode() {
        // Default constructor necessary for GWT serialisation.
    }

    public ExplorerNode(final String type, final String uuid, final String name, final String tags) {
        this.type = type;
        this.uuid = uuid;
        this.name = name;
        this.tags = tags;
    }

    public static ExplorerNode create(final DocRef docRef) {
        if (docRef == null) {
            return null;
        }
        return new ExplorerNode(docRef.getType(), docRef.getUuid(), docRef.getName(), null);
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(final String tags) {
        this.tags = tags;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(final int depth) {
        this.depth = depth;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    public void setNodeState(final NodeState nodeState) {
        this.nodeState = nodeState;
    }

    public List<ExplorerNode> getChildren() {
        return children;
    }

    public void setChildren(final List<ExplorerNode> children) {
        this.children = children;
    }

    @JsonIgnore
    public ExplorerNode copy() {
        final ExplorerNode copy = new ExplorerNode(type, uuid, name, tags);
        copy.depth = depth;
        copy.iconUrl = iconUrl;
        copy.nodeState = nodeState;
        return copy;
    }

    @JsonIgnore
    public DocRef getDocRef() {
        return new DocRef(type, uuid, name);
    }

    @JsonIgnore
    @Override
    public String getDisplayValue() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ExplorerNode that = (ExplorerNode) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }

    public static class Builder {
        private final ExplorerNode instance;

        public Builder() {
            this.instance = new ExplorerNode();
        }

        public Builder type(final String type) {
            this.instance.type = type;
            return this;
        }

        public Builder uuid(final String uuid) {
            this.instance.uuid = uuid;
            return this;
        }

        public Builder name(final String name) {
            this.instance.name = name;
            return this;
        }

        public Builder tags(final String tags) {
            this.instance.tags = tags;
            return this;
        }

        public Builder depth(final int depth) {
            this.instance.depth = depth;
            return this;
        }

        public Builder iconUrl(final String iconUrl) {
            this.instance.iconUrl = iconUrl;
            return this;
        }

        public Builder nodeState(final NodeState nodeState) {
            this.instance.nodeState = nodeState;
            return this;
        }

        public ExplorerNode build() {
            return instance;
        }
    }

    public enum NodeState {
        OPEN, CLOSED, LEAF
    }
}