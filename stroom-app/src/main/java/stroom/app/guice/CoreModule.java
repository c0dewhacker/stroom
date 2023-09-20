package stroom.app.guice;

import stroom.analytics.rule.impl.AnalyticRuleModule;
import stroom.query.impl.datasource.DataSourceModule;

import com.google.inject.AbstractModule;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new stroom.activity.impl.ActivityModule());
        install(new stroom.activity.impl.db.ActivityDaoModule());
//        install(new stroom.analytics.impl.AlertModule());
        install(new stroom.analytics.impl.AnalyticsModule());
        install(new stroom.analytics.impl.db.AnalyticsDaoModule());
        install(new stroom.annotation.impl.AnnotationModule());
        install(new stroom.annotation.impl.db.AnnotationDaoModule());
        install(new stroom.annotation.pipeline.AnnotationPipelineModule());
        install(new stroom.cache.impl.CacheModule());
        install(new stroom.cache.impl.CacheResourceModule());
        install(new stroom.cluster.lock.impl.db.ClusterLockModule());
        install(new stroom.cluster.task.impl.ClusterTaskModule());
        install(new stroom.config.global.impl.ConfigProvidersModule());
        install(new stroom.config.global.impl.GlobalConfigModule());
        install(new stroom.core.dataprocess.PipelineStreamTaskModule());
        install(new stroom.core.db.DbStatusModule());
        install(new stroom.core.entity.event.EntityEventModule());
        install(new stroom.core.meta.MetaModule());
        install(new stroom.core.receive.ReceiveDataModule());
        install(new stroom.core.servlet.ServletModule());
        install(new stroom.core.sysinfo.SystemInfoModule());
        install(new stroom.core.welcome.SessionInfoModule());
        install(new stroom.core.welcome.WelcomeModule());
        install(new stroom.dashboard.impl.DashboardModule());
        install(new DataSourceModule());
        install(new stroom.dashboard.impl.logging.LoggingModule());
        install(new stroom.dashboard.impl.script.ScriptModule());
        install(new stroom.dashboard.impl.visualisation.VisualisationModule());
        install(new stroom.data.retention.impl.DataRetentionModule());
        install(new stroom.data.store.impl.DataStoreModule());
        install(new stroom.data.store.impl.fs.FsDataStoreModule());
        install(new stroom.data.store.impl.fs.FsDataStoreTaskHandlerModule());
        install(new stroom.data.store.impl.fs.db.FsDataStoreDaoModule());
        install(new stroom.dictionary.impl.DictionaryHandlerModule());
        install(new stroom.dictionary.impl.DictionaryModule());
        install(new stroom.documentation.impl.DocumentationHandlerModule());
        install(new stroom.documentation.impl.DocumentationModule());
        install(new stroom.docstore.impl.DocStoreModule());
        install(new stroom.docstore.impl.db.DocStoreDbPersistenceModule());
        install(new stroom.dropwizard.common.DropwizardModule());
        install(new stroom.explorer.impl.ExplorerFavModule());
        install(new stroom.explorer.impl.db.ExplorerFavDbModule());
        install(new stroom.event.logging.impl.EventLoggingModule());
        install(new stroom.event.logging.rs.impl.RestResourceAutoLoggerModule());
        install(new stroom.explorer.impl.ExplorerModule());
        install(new stroom.explorer.impl.db.ExplorerDaoModule());
        install(new stroom.feed.impl.FeedModule());
        install(new stroom.importexport.impl.ExportConfigResourceModule());
        install(new stroom.importexport.impl.ImportExportHandlerModule());
        install(new stroom.importexport.impl.ImportExportModule());
        install(new stroom.index.impl.IndexElementModule());
        install(new stroom.index.impl.IndexModule());
        install(new stroom.index.impl.db.IndexDaoModule());
        install(new stroom.job.impl.JobSystemModule());
        install(new stroom.job.impl.db.JobDaoModule());
        install(new stroom.jdbc.impl.JDBCConfigHandlerModule());
        install(new stroom.jdbc.impl.JDBCConfigModule());
        install(new stroom.kafka.impl.KafkaConfigHandlerModule());
        install(new stroom.kafka.impl.KafkaConfigModule());
        install(new stroom.kafka.pipeline.KafkaPipelineModule());
        install(new stroom.legacy.db.LegacyModule());
        install(new stroom.legacy.impex_6_1.LegacyImpexModule());
        install(new stroom.meta.impl.MetaModule());
        install(new stroom.meta.impl.db.MetaDaoModule());
        install(new stroom.node.impl.NodeModule());
        install(new stroom.node.impl.db.NodeDaoModule());
        install(new stroom.pipeline.PipelineModule());
        install(new stroom.pipeline.cache.PipelineCacheModule());
        install(new stroom.pipeline.factory.CommonPipelineElementModule());
        install(new stroom.pipeline.factory.DataStorePipelineElementModule());
        install(new stroom.pipeline.factory.PipelineFactoryModule());
        install(new stroom.pipeline.refdata.ReferenceDataModule());
        install(new stroom.pipeline.stepping.PipelineSteppingModule());
        install(new stroom.pipeline.xsltfunctions.CommonXsltFunctionModule());
        install(new stroom.pipeline.xsltfunctions.DataStoreXsltFunctionModule());
        install(new stroom.processor.impl.ProcessorModule());
        install(new stroom.processor.impl.db.ProcessorDaoModule());
        install(new stroom.suggestions.impl.SuggestModule());
        install(new stroom.query.impl.QueryModule());
        install(new stroom.receive.common.RemoteFeedModule());
        install(new stroom.receive.rules.impl.ReceiveDataRuleSetModule());
        install(new stroom.search.extraction.ExtractionModule());
        install(new stroom.search.impl.SearchModule());
        install(new stroom.search.impl.shard.ShardModule());
        install(new stroom.search.elastic.ElasticSearchModule());
        install(new stroom.search.solr.SolrSearchModule());
        install(new stroom.searchable.impl.SearchableModule());
        install(new stroom.security.identity.IdentityModule());
        install(new stroom.security.identity.db.IdentityDaoModule());
        install(new stroom.security.impl.SecurityModule());
        install(new stroom.security.impl.SessionSecurityModule());
        install(new stroom.security.impl.db.SecurityDaoModule());
        install(new stroom.servicediscovery.impl.ServiceDiscoveryModule());
        install(new AnalyticRuleModule());
        install(new stroom.statistics.impl.InternalStatisticsModule());
        install(new stroom.statistics.impl.hbase.entity.StroomStatsStoreModule());
        install(new stroom.statistics.impl.hbase.internal.InternalModule());
        install(new stroom.statistics.impl.hbase.pipeline.StatisticsElementModule());
        install(new stroom.statistics.impl.hbase.rollup.StroomStatsRollupModule());
        install(new stroom.statistics.impl.sql.SqlStatisticsModule());
        install(new stroom.statistics.impl.sql.entity.StatisticStoreModule());
        install(new stroom.statistics.impl.sql.filter.StatisticsElementsModule());
        install(new stroom.statistics.impl.sql.internal.InternalModule());
        install(new stroom.statistics.impl.sql.rollup.SQLStatisticRollupModule());
        install(new stroom.statistics.impl.sql.search.SQLStatisticSearchModule());
        install(new stroom.storedquery.impl.StoredQueryModule());
        install(new stroom.storedquery.impl.db.StoredQueryDaoModule());
        install(new stroom.task.impl.TaskModule());
        install(new stroom.util.pipeline.scope.PipelineScopeModule());
        install(new stroom.view.impl.ViewModule());
    }
}
