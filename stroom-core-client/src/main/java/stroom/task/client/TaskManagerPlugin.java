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

package stroom.task.client;

import stroom.core.client.ContentManager;
import stroom.core.client.MenuKeys;
import stroom.core.client.presenter.MonitoringPlugin;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.svg.client.IconColour;
import stroom.svg.shared.SvgImage;
import stroom.task.client.presenter.TaskManagerPresenter;
import stroom.widget.menu.client.presenter.IconMenuItem;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Singleton;

@Singleton
public class TaskManagerPlugin extends MonitoringPlugin<TaskManagerPresenter> {

    @Inject
    public TaskManagerPlugin(final EventBus eventBus, final ContentManager eventManager,
                             final Provider<TaskManagerPresenter> presenterProvider,
                             final ClientSecurityContext securityContext) {
        super(eventBus, eventManager, presenterProvider, securityContext);
    }

    @Override
    protected void addChildItems(final BeforeRevealMenubarEvent event) {
        if (getSecurityContext().hasAppPermission(PermissionNames.MANAGE_TASKS_PERMISSION)) {
            event.getMenuItems().addMenuItem(MenuKeys.MONITORING_MENU,
                    new IconMenuItem.Builder()
                            .priority(13)
                            .icon(SvgImage.JOBS)
                            .iconColour(IconColour.GREY)
                            .text("Server Tasks")
                            .command(this::open)
                            .build());
        }
    }
}
