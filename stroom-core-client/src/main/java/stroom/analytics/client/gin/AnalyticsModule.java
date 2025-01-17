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

package stroom.analytics.client.gin;

import stroom.analytics.client.AnalyticsPlugin;
import stroom.analytics.client.presenter.AnalyticDataShardsPresenter;
import stroom.analytics.client.presenter.AnalyticDataShardsPresenter.AnalyticDataShardsView;
import stroom.analytics.client.presenter.AnalyticEmailDestinationPresenter;
import stroom.analytics.client.presenter.AnalyticEmailDestinationPresenter.AnalyticEmailDestinationView;
import stroom.analytics.client.presenter.AnalyticNotificationPresenter;
import stroom.analytics.client.presenter.AnalyticNotificationPresenter.AnalyticNotificationView;
import stroom.analytics.client.presenter.AnalyticProcessingPresenter;
import stroom.analytics.client.presenter.AnalyticProcessingPresenter.AnalyticProcessingView;
import stroom.analytics.client.presenter.AnalyticRulePresenter;
import stroom.analytics.client.presenter.AnalyticStreamDestinationPresenter;
import stroom.analytics.client.presenter.AnalyticStreamDestinationPresenter.AnalyticStreamDestinationView;
import stroom.analytics.client.presenter.ScheduledQueryProcessingPresenter;
import stroom.analytics.client.presenter.ScheduledQueryProcessingPresenter.ScheduledQueryProcessingView;
import stroom.analytics.client.presenter.StreamingProcessingPresenter;
import stroom.analytics.client.presenter.StreamingProcessingPresenter.StreamingProcessingView;
import stroom.analytics.client.presenter.TableBuilderProcessingPresenter;
import stroom.analytics.client.presenter.TableBuilderProcessingPresenter.TableBuilderProcessingView;
import stroom.analytics.client.view.AnalyticDataShardsViewImpl;
import stroom.analytics.client.view.AnalyticEmailDestinationViewImpl;
import stroom.analytics.client.view.AnalyticNotificationViewImpl;
import stroom.analytics.client.view.AnalyticProcessingViewImpl;
import stroom.analytics.client.view.AnalyticStreamDestinationViewImpl;
import stroom.analytics.client.view.ScheduledQueryProcessingViewImpl;
import stroom.analytics.client.view.StreamingProcessingViewImpl;
import stroom.analytics.client.view.TableBuilderProcessingViewImpl;
import stroom.core.client.gin.PluginModule;

public class AnalyticsModule extends PluginModule {

    @Override
    protected void configure() {
        bindPlugin(AnalyticsPlugin.class);

        bind(AnalyticRulePresenter.class);

        bindPresenterWidget(AnalyticProcessingPresenter.class,
                AnalyticProcessingView.class,
                AnalyticProcessingViewImpl.class);
        bindPresenterWidget(AnalyticNotificationPresenter.class,
                AnalyticNotificationView.class,
                AnalyticNotificationViewImpl.class);
        bindPresenterWidget(AnalyticEmailDestinationPresenter.class,
                AnalyticEmailDestinationView.class,
                AnalyticEmailDestinationViewImpl.class);
        bindPresenterWidget(AnalyticStreamDestinationPresenter.class,
                AnalyticStreamDestinationView.class,
                AnalyticStreamDestinationViewImpl.class);
        bindPresenterWidget(AnalyticDataShardsPresenter.class,
                AnalyticDataShardsView.class,
                AnalyticDataShardsViewImpl.class);
        bindPresenterWidget(ScheduledQueryProcessingPresenter.class,
                ScheduledQueryProcessingView.class,
                ScheduledQueryProcessingViewImpl.class);
        bindPresenterWidget(TableBuilderProcessingPresenter.class,
                TableBuilderProcessingView.class,
                TableBuilderProcessingViewImpl.class);
        bindPresenterWidget(StreamingProcessingPresenter.class,
                StreamingProcessingView.class,
                StreamingProcessingViewImpl.class);
    }
}
