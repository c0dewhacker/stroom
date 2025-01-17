package stroom.core.meta;

import stroom.docref.DocRef;
import stroom.docrefinfo.api.DocRefInfoService;
import stroom.feed.api.FeedStore;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexShardFields;
import stroom.meta.api.MetaService;
import stroom.meta.shared.MetaFields;
import stroom.meta.shared.Status;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.ReferenceDataFields;
import stroom.processor.shared.ProcessorTaskFields;
import stroom.query.shared.FetchSuggestionsRequest;
import stroom.query.shared.Suggestions;
import stroom.security.api.SecurityContext;
import stroom.suggestions.api.SuggestionsService;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.util.filter.QuickFilterPredicateFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class MetaSuggestionsQueryHandlerImpl implements MetaSuggestionsQueryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaSuggestionsQueryHandlerImpl.class);
    private static final int LIMIT = 20;

    private final MetaService metaService;
    private final PipelineStore pipelineStore;
    private final SecurityContext securityContext;
    private final FeedStore feedStore;
    private final DocRefInfoService docRefInfoService;
    private final TaskContextFactory taskContextFactory;

    // This may need changing if we have suggestions that are not for the stream store data source
    private final Map<String, Function<String, List<String>>> metaFieldNameToFunctionMap = Map.of(
            MetaFields.FEED.getName(), this::createFeedList,
            MetaFields.PIPELINE.getName(), this::createPipelineList,
            MetaFields.PIPELINE_NAME.getName(), this::createPipelineList,
            MetaFields.TYPE.getName(), this::createStreamTypeList,
            MetaFields.STATUS.getName(), this::createStatusList);

    private final Map<String, Function<String, List<String>>> indexShardsFieldNameToFunctionMap = Map.of(
            IndexShardFields.FIELD_INDEX.getName(), filter ->
                    getNonUniqueDocRefNames(IndexDoc.DOCUMENT_TYPE, filter));

    private final Map<String, Function<String, List<String>>> processorTaskFieldNameToFunctionMap = Map.of(
            ProcessorTaskFields.PIPELINE_NAME.getName(), filter ->
                    getNonUniqueDocRefNames(PipelineDoc.DOCUMENT_TYPE, filter));

    private final Map<String, Function<String, List<String>>> refDataFieldNameToFunctionMap = Map.of(
            ReferenceDataFields.PIPELINE_FIELD.getName(), filter ->
                    getNonUniqueDocRefNames(PipelineDoc.DOCUMENT_TYPE, filter));

    @SuppressWarnings("unused")
    @Inject
    MetaSuggestionsQueryHandlerImpl(final MetaService metaService,
                                    final PipelineStore pipelineStore,
                                    final SecurityContext securityContext,
                                    final FeedStore feedStore,
                                    final TaskContextFactory taskContextFactory,
                                    final DocRefInfoService docRefInfoService,
                                    final SuggestionsService suggestionsService) {
        this.metaService = metaService;
        this.pipelineStore = pipelineStore;
        this.securityContext = securityContext;
        this.feedStore = feedStore;
        this.docRefInfoService = docRefInfoService;
        this.taskContextFactory = taskContextFactory;
    }

    @Override
    public Suggestions getSuggestions(final FetchSuggestionsRequest request) {
        return securityContext.secureResult(() -> {
            List<String> result = Collections.emptyList();

            final String fieldName = request.getField().getFieldName();
            final Function<String, List<String>> suggestionFunc = getSuggestionFunc(request, fieldName);
            if (suggestionFunc != null) {
                result = suggestionFunc.apply(request.getText());
            }

            // Determine if we are going to allow the client to cache the suggestions.
            final boolean cache = ((request.getText() == null || request.getText().isBlank()) &&
                    (request.getField().getFieldName().equals(MetaFields.TYPE.getName()) ||
                            request.getField().getFieldName().equals(MetaFields.STATUS.getName())));

            return new Suggestions(result, cache);
        });
    }

    @Nullable
    private Function<String, List<String>> getSuggestionFunc(final FetchSuggestionsRequest request,
                                                             final String fieldName) {
        final Function<String, List<String>> suggestionFunc;
        if (MetaFields.STREAM_STORE_DOC_REF.equals(request.getDataSource())) {
            suggestionFunc = metaFieldNameToFunctionMap.get(fieldName);
        } else if (IndexShardFields.INDEX_SHARDS_PSEUDO_DOC_REF.equals(request.getDataSource())) {
            suggestionFunc = indexShardsFieldNameToFunctionMap.get(fieldName);
        } else if (ProcessorTaskFields.PROCESSOR_TASK_PSEUDO_DOC_REF.equals(request.getDataSource())) {
            suggestionFunc = processorTaskFieldNameToFunctionMap.get(fieldName);
        } else if (ReferenceDataFields.REF_STORE_PSEUDO_DOC_REF.equals(request.getDataSource())) {
            suggestionFunc = refDataFieldNameToFunctionMap.get(fieldName);
        } else {
            suggestionFunc = null;
        }
        return suggestionFunc;
    }

    @NotNull
    private List<String> createPipelineList(final String userInput) {
        final List<String> result;
        final Stream<String> stream = pipelineStore.list().stream()
                .map(DocRef::getName);
        result = QuickFilterPredicateFactory.filterStream(userInput, stream)
                .sorted()
                .limit(LIMIT)
                .collect(Collectors.toList());

        return result;
    }

    @NotNull
    private List<String> createStatusList(final String userInput) {
        final List<String> result;
        Stream<String> stream = Arrays.stream(Status.values())
                .map(Status::getDisplayValue);
        result = QuickFilterPredicateFactory.filterStream(userInput, stream)
                .sorted()
                .limit(LIMIT)
                .collect(Collectors.toList());
        return result;
    }

    private List<String> createFeedList(final String userInput) {
        // TODO this seems pretty inefficient as each call hits the db to get ALL feeds
        //   then limits/filters in java.  Needs to work off a cached feed name list

        return taskContextFactory.contextResult("Get all feed names", parentTaskContext ->
                createFeedList(parentTaskContext, userInput)).get();
    }

    private List<String> createFeedList(final TaskContext parentTaskContext, final String userInput) {
        // To get a list of feed names we need to combine the names from the meta service
        // and the feed store. Meta service only has feeds which have data, but may contain
        // feeds that have been deleted as docs.
        final CompletableFuture<Set<String>> metaFeedsFuture = CompletableFuture.supplyAsync(
                taskContextFactory.contextResult(
                        "Get meta feed names",
                        taskContext -> metaService.getFeeds()));

        final CompletableFuture<List<String>> docFeedsFuture = CompletableFuture.supplyAsync(
                taskContextFactory.contextResult(
                        "Get doc feed names",
                        taskContext ->
                                feedStore.list()
                                        .stream()
                                        .map(DocRef::getName)
                                        .collect(Collectors.toList())));

        try {
            // Make async calls to get the two lists then combine
            return metaFeedsFuture
                    .thenCombine(docFeedsFuture, (metaFeedNames, docFeedNames) ->
                            QuickFilterPredicateFactory.filterStream(
                                            userInput,
                                            Stream.concat(metaFeedNames.stream(), docFeedNames.stream())
                                                    .parallel())
                                    .distinct()
                                    .sorted()
                                    .limit(LIMIT)
                                    .collect(Collectors.toList()))
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted", e);
            return Collections.emptyList();
        } catch (ExecutionException e) {
            throw new RuntimeException("Error getting feed name suggestions: " + e.getMessage(), e);
        }
    }

    private List<String> createStreamTypeList(final String userInput) {
        return QuickFilterPredicateFactory.filterStream(
                        userInput, metaService.getTypes().stream().sorted())
                .limit(LIMIT)
                .collect(Collectors.toList());
    }

    private List<String> getNonUniqueDocRefNames(final String docRefType,
                                                 final String userInput) {
        return QuickFilterPredicateFactory.filterStream(
                        userInput, docRefInfoService.findByType(docRefType)
                                .stream()
                                .map(DocRef::getName))
                .limit(LIMIT)
                .collect(Collectors.toList());
    }
}
