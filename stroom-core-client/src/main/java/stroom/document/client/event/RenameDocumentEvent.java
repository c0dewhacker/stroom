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

package stroom.document.client.event;

import stroom.explorer.shared.ExplorerNode;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.gwtplatform.mvp.client.PresenterWidget;

public class RenameDocumentEvent extends GwtEvent<RenameDocumentEvent.Handler> {

    private static Type<Handler> TYPE;
    private final PresenterWidget<?> presenter;
    private final ExplorerNode explorerNode;
    private final String docName;

    private RenameDocumentEvent(final PresenterWidget<?> presenter,
                                final ExplorerNode explorerNode,
                                final String docName) {
        this.presenter = presenter;
        this.explorerNode = explorerNode;
        this.docName = docName;
    }

    public static void fire(final HasHandlers handlers,
                            final PresenterWidget<?> presenter,
                            final ExplorerNode docRef,
                            final String docName) {
        handlers.fireEvent(new RenameDocumentEvent(presenter, docRef, docName));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public final Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onRename(this);
    }

    public PresenterWidget<?> getPresenter() {
        return presenter;
    }

    public ExplorerNode getExplorerNode() {
        return explorerNode;
    }

    public String getDocName() {
        return docName;
    }

    public interface Handler extends EventHandler {

        void onRename(final RenameDocumentEvent event);
    }
}
