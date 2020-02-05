/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.explorer.shared;

public class DocumentType {
    public static final String DOC_IMAGE_URL = "document/";

    private int priority;
    private String type;
    private String displayType;
    private String iconUrl;

    public DocumentType() {
        // Default constructor necessary for GWT serialisation.
    }

    public DocumentType(final int priority, final String type, final String displayType) {
        this.priority = priority;
        this.type = type;
        this.displayType = displayType;
        this.iconUrl = getIconUrl(type);
    }

    public DocumentType(final int priority, final String type, final String displayType, final String iconUrl) {
        this.priority = priority;
        this.type = type;
        this.displayType = displayType;
        this.iconUrl = iconUrl;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String getDisplayType() {
        return displayType;
    }

    public void setDisplayType(final String displayType) {
        this.displayType = displayType;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
    }

    private String getIconUrl(final String type) {
        return DocumentType.DOC_IMAGE_URL + type + ".svg";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DocumentType that = (DocumentType) o;

        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type;
    }
}
