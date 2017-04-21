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

package stroom.node.client.presenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import stroom.dispatch.client.ClientDispatchAsync;

public class NodeEditPresenter extends MyPresenterWidget<NodeEditPresenter.NodeEditView> {
    public interface NodeEditView extends View {
        void setName(String name);

        void setClusterUrl(String clusterUrl);

        String getClusterUrl();
    }

    @Inject
    public NodeEditPresenter(final EventBus eventBus, final NodeEditView view, final ClientDispatchAsync dispatcher) {
        super(eventBus, view);
    }

    public void setName(final String name) {
        getView().setName(name);
    }

    public void setClusterUrl(final String clusterUrl) {
        getView().setClusterUrl(clusterUrl);
    }

    public String getClusterUrl() {
        return getView().getClusterUrl();
    }
}
