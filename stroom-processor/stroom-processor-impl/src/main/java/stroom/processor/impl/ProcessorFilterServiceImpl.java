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
 */

package stroom.processor.impl;

import stroom.analytics.shared.AnalyticRuleDoc;
import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docrefinfo.api.DocRefInfoService;
import stroom.entity.shared.ExpressionCriteria;
import stroom.meta.api.MetaService;
import stroom.meta.shared.FindMetaCriteria;
import stroom.meta.shared.MetaFields;
import stroom.pipeline.shared.PipelineDoc;
import stroom.processor.api.ProcessorFilterService;
import stroom.processor.api.ProcessorService;
import stroom.processor.shared.CreateProcessFilterRequest;
import stroom.processor.shared.FetchProcessorRequest;
import stroom.processor.shared.Processor;
import stroom.processor.shared.ProcessorFields;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.ProcessorFilterFields;
import stroom.processor.shared.ProcessorFilterRow;
import stroom.processor.shared.ProcessorListRow;
import stroom.processor.shared.ProcessorRow;
import stroom.processor.shared.ProcessorType;
import stroom.processor.shared.QueryData;
import stroom.processor.shared.ReprocessDataInfo;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.security.api.DocumentPermissionService;
import stroom.security.api.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.PermissionNames;
import stroom.security.user.api.UserNameService;
import stroom.util.AuditUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.Expander;
import stroom.util.shared.PermissionException;
import stroom.util.shared.ResultPage;
import stroom.util.shared.Severity;
import stroom.util.shared.UserName;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
class ProcessorFilterServiceImpl implements ProcessorFilterService {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ProcessorFilterServiceImpl.class);

    private static final String PERMISSION = PermissionNames.MANAGE_PROCESSORS_PERMISSION;

    private final ProcessorService processorService;
    private final ProcessorFilterDao processorFilterDao;
    private final ProcessorTaskDao processorTaskDao;
    private final MetaService metaService;
    private final SecurityContext securityContext;
    private final DocRefInfoService docRefInfoService;
    private final UserNameService userNameService;
    private final DocumentPermissionService documentPermissionService;

    @Inject
    ProcessorFilterServiceImpl(final ProcessorService processorService,
                               final ProcessorFilterDao processorFilterDao,
                               final ProcessorTaskDao processorTaskDao,
                               final MetaService metaService,
                               final SecurityContext securityContext,
                               final DocRefInfoService docRefInfoService,
                               final UserNameService userNameService,
                               final DocumentPermissionService documentPermissionService) {
        this.processorService = processorService;
        this.processorFilterDao = processorFilterDao;
        this.processorTaskDao = processorTaskDao;
        this.metaService = metaService;
        this.securityContext = securityContext;
        this.docRefInfoService = docRefInfoService;
        this.userNameService = userNameService;
        this.documentPermissionService = documentPermissionService;
    }

    @Override
    public ProcessorFilter create(final CreateProcessFilterRequest request) {
        // Check the user has read permissions on the pipeline.
        if (!securityContext.hasDocumentPermission(request.getPipeline().getUuid(), DocumentPermissionNames.READ)) {
            throw new PermissionException(securityContext.getUserIdentityForAudit(),
                    "You do not have permission to create this processor filter");
        }

        final Processor processor = processorService.create(
                request.getProcessorType(),
                request.getPipeline(),
                request.isEnabled());
        return create(processor, request);
    }

    @Override
    public ProcessorFilter create(final Processor processor,
                                  final CreateProcessFilterRequest request) {
        // Check the user has read permissions on the pipeline.
        if (!securityContext.hasDocumentPermission(processor.getPipelineUuid(), DocumentPermissionNames.READ)) {
            throw new PermissionException(securityContext.getUserIdentityForAudit(),
                    "You do not have permission to create this processor filter");
        }

        // If we are using auto priority then try and get a priority.
        final int calculatedPriority = getAutoPriority(processor, request.getPriority(), request.isAutoPriority());

        // now create the filter and tracker
        final ProcessorFilter processorFilter = new ProcessorFilter();
        // Blank tracker
        processorFilter.setReprocess(request.isReprocess());
        processorFilter.setEnabled(request.isEnabled());
        processorFilter.setPriority(calculatedPriority);
        processorFilter.setProcessor(processor);
        processorFilter.setQueryData(request.getQueryData());
        processorFilter.setMinMetaCreateTimeMs(request.getMinMetaCreateTimeMs());
        processorFilter.setMaxMetaCreateTimeMs(request.getMaxMetaCreateTimeMs());
        return create(processorFilter);
    }

    @Override
    public ProcessorFilter importFilter(final Processor processor,
                                        final DocRef processorFilterDocRef,
                                        final CreateProcessFilterRequest request) {
        // Check the user has read permissions on the pipeline.
        if (!securityContext.hasDocumentPermission(processor.getPipelineUuid(), DocumentPermissionNames.READ)) {
            throw new PermissionException(securityContext.getUserIdentityForAudit(),
                    "You do not have permission to create this processor filter");
        }

        // If we are using auto priority then try and get a priority.
        final int calculatedPriority = getAutoPriority(processor, request.getPriority(), request.isAutoPriority());

        if (request.getQueryData() != null && request.getQueryData().getDataSource() == null) {
            request.getQueryData().setDataSource(MetaFields.STREAM_STORE_DOC_REF);
        }

        // now create the filter and tracker
        ProcessorFilter processorFilter = new ProcessorFilter();
        AuditUtil.stamp(securityContext, processorFilter);
        processorFilter.setReprocess(request.isReprocess());
        processorFilter.setEnabled(request.isEnabled());
        processorFilter.setPriority(calculatedPriority);
        processorFilter.setProcessor(processor);
        processorFilter.setQueryData(request.getQueryData());
        processorFilter.setMinMetaCreateTimeMs(request.getMinMetaCreateTimeMs());
        processorFilter.setMaxMetaCreateTimeMs(request.getMaxMetaCreateTimeMs());
        if (processorFilterDocRef != null) {
            processorFilter.setUuid(processorFilterDocRef.getUuid());
        }

        return create(processorFilter);
    }

    @Override
    public ProcessorFilter create(final ProcessorFilter processorFilter) {
        ProcessorFilter createdFilter = securityContext.secureResult(PERMISSION, () ->
                processorFilterDao.create(ensureValid(processorFilter)));

        // The creator of the filter becomes the owner and the processor tasks are run as the owner
        // of the filter (see stroom.processor.impl.ProcessorTaskCreatorImpl.createNewTasks)
        documentPermissionService.setDocumentOwner(createdFilter.getUuid(), securityContext.getUserUuid());
        createdFilter.setProcessor(processorFilter.getProcessor());

        return createdFilter;
    }

    @Override
    public Optional<ProcessorFilter> fetch(final int id) {
        return securityContext.secureResult(PERMISSION, () ->
                processorFilterDao.fetch(id));
    }

    @Override
    public ProcessorFilter update(final ProcessorFilter processorFilter) {
        // Check the user has update permissions on the pipeline.
        if (!securityContext.hasDocumentPermission(
                processorFilter.getProcessor().getPipelineUuid(),
                DocumentPermissionNames.UPDATE)) {

            throw new PermissionException(securityContext.getUserIdentityForAudit(),
                    "You do not have permission to update this processor filter");
        }

        if (processorFilter.getUuid() == null) {
            processorFilter.setUuid(UUID.randomUUID().toString());
        }

        AuditUtil.stamp(securityContext, processorFilter);
        return securityContext.secureResult(PERMISSION, () ->
                processorFilterDao.update(processorFilter));
    }

    @Override
    public boolean delete(final int id) {
        return securityContext.secureResult(PERMISSION, () -> {
            if (processorFilterDao.logicalDeleteByProcessorFilterId(id) > 0) {
                // Logically delete any associated tasks that have not yet finished processing.
                // Once the filter is logically deleted no new tasks will be created for it, but we may still have
                // active tasks for 'deleted' filters.
                processorTaskDao.logicalDeleteByProcessorFilterId(id);
                return true;
            }
            return false;
        });
    }

    @Override
    public void setPriority(final Integer id, final Integer priority) {
        fetch(id).ifPresent(processorFilter -> {
            processorFilter.setPriority(priority);
            update(processorFilter);
        });
    }

    @Override
    public void setEnabled(final Integer id, final Boolean enabled) {
        fetch(id).ifPresent(processorFilter -> {
            processorFilter.setEnabled(enabled);
            update(processorFilter);
        });
    }

    @Override
    public ResultPage<ProcessorFilter> find(final ExpressionCriteria criteria) {
        return securityContext.secureResult(PERMISSION, () ->
                processorFilterDao.find(criteria));
    }

    // TODO : The following method combines results from the processor and processor filter services so should possibly
    //  be in another class that controls the collaboration.
    @Override
    public ResultPage<ProcessorListRow> find(final FetchProcessorRequest request) {
        return securityContext.secureResult(PERMISSION, () -> {
            final List<ProcessorListRow> values = new ArrayList<>();

            final ExpressionCriteria criteria = new ExpressionCriteria(request.getExpression());
            final ResultPage<ProcessorFilter> processorFilters = find(criteria);

            final String processorIds = processorFilters
                    .getValues()
                    .stream()
                    .map(ProcessorFilter::getProcessor)
                    .filter(Objects::nonNull)
                    .map(Processor::getId)
                    .distinct()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            if (!processorIds.isBlank()) {
                final ExpressionOperator processorExpression = ExpressionOperator.builder()
                        .addTerm(ProcessorFields.ID.getName(), Condition.IN, processorIds)
                        .build();
                final ResultPage<Processor> streamProcessors = processorService.find(new ExpressionCriteria(
                        processorExpression));

                // Get unique processors.
                final Set<Processor> processors = new HashSet<>(streamProcessors.getValues());

                final List<Processor> sorted = new ArrayList<>(processors);
                sorted.sort((o1, o2) -> {
                    if (o1.getPipelineUuid() != null && o2.getPipelineUuid() != null) {
                        return o1.getPipelineUuid().compareTo(o2.getPipelineUuid());
                    }
                    if (o1.getPipelineUuid() != null) {
                        return -1;
                    }
                    if (o2.getPipelineUuid() != null) {
                        return 1;
                    }
                    return o1.getId().compareTo(o2.getId());
                });

                for (final Processor processor : sorted) {
                    final Expander processorExpander = new Expander(0, false, false);

                    updatePipelineName(processor);

                    final ProcessorRow processorRow = new ProcessorRow(processorExpander,
                            processor);
                    values.add(processorRow);

                    // If the job row is open then add child rows.
                    final String userUuid = securityContext.getUserUuid();
                    if (request.getExpandedRows() == null || request.isRowExpanded(processorRow)) {
                        processorExpander.setExpanded(true);

                        // Add filters.
                        for (final ProcessorFilter processorFilter : processorFilters.getValues()) {
                            if (processor.equals(processorFilter.getProcessor())) {

                                // If the user is not an admin then only show them filters that are owned by them.
                                boolean include = false;
                                if (securityContext.isAdmin()) {
                                    include = true;
                                } else {
                                    try {
                                        final String ownerUuid = securityContext
                                                .getDocumentOwnerUuid(processorFilter.asDocRef());
                                        if (ownerUuid.equals(userUuid)) {
                                            include = true;
                                        }
                                    } catch (final RuntimeException e) {
                                        LOGGER.debug(e::getMessage, e);
                                    }
                                }

                                if (include) {
                                    // Decorate the expression with resolved dictionaries etc.
                                    final QueryData queryData = processorFilter.getQueryData();
                                    if (queryData != null && queryData.getExpression() != null) {
                                        queryData.setExpression(decorate(queryData.getExpression()));
                                    }

                                    if (processorFilter.getPipelineName() == null) {
                                        if (processor.getPipelineName() == null) {
                                            updatePipelineName(processor);
                                        }
                                        processorFilter.setPipelineName(processor.getPipelineName());
                                    }

                                    final ProcessorFilterRow processorFilterRow = getRow(processorFilter);
                                    values.add(processorFilterRow);
                                }
                            }
                        }
                    }
                }
            }

            return ResultPage.createUnboundedList(values);
        });
    }

    @Override
    public ProcessorFilterRow getRow(final ProcessorFilter processorFilter) {
        String userDisplayName;
        try {
            final String ownerUuid = securityContext
                    .getDocumentOwnerUuid(processorFilter.asDocRef());
            userDisplayName = Optional.ofNullable(ownerUuid)
                    .flatMap(userNameService::getByUuid)
                    .map(UserName::getUserIdentityForAudit)
                    .orElse(null);
        } catch (final RuntimeException e) {
            userDisplayName = e.getMessage();
            LOGGER.debug(e::getMessage, e);
        }

        return new ProcessorFilterRow(processorFilter, userDisplayName);
    }

    private void updatePipelineName(final Processor processor) {
        if (processor.getPipelineName() == null && processor.getPipelineUuid() != null) {
            final Optional<String> pipelineName = getPipelineName(
                    processor.getProcessorType(),
                    processor.getPipelineUuid());
            processor.setPipelineName(pipelineName.orElseGet(() -> {
                LOGGER.warn("Unable to find Pipeline " +
                        processor.getPipelineUuid() +
                        " associated with Processor " +
                        processor.getUuid() +
                        " (id: " +
                        processor.getId() +
                        ")" +
                        " Has it been deleted?");
                return null;
            }));
        }
    }

    @Override
    public Optional<String> getPipelineName(final ProcessorType processorType,
                                            final String uuid) {
        try {
            String docType = PipelineDoc.DOCUMENT_TYPE;
            if (ProcessorType.STREAMING_ANALYTIC.equals(processorType)) {
                docType = AnalyticRuleDoc.DOCUMENT_TYPE;
            }

            final DocRef pipelineDocRef = DocRef.builder()
                    .type(docType)
                    .uuid(uuid)
                    .build();
            return docRefInfoService.name(pipelineDocRef);
        } catch (final RuntimeException e) {
            // This error is expected in tests and the pipeline name isn't essential
            // as it is only used in here for logging purposes.
            LOGGER.trace(e::getMessage, e);
        }
        return Optional.empty();
    }

    @Override
    public ResultPage<ProcessorFilter> find(final DocRef pipelineDocRef) {
        if (pipelineDocRef == null) {
            throw new IllegalArgumentException("Supplied pipeline reference cannot be null");
        }

        if (!PipelineDoc.DOCUMENT_TYPE.equals(pipelineDocRef.getType())) {
            throw new IllegalArgumentException("Supplied pipeline reference cannot be of type " +
                    pipelineDocRef.getType());
        }

        // First try to find the associated processors
        final ExpressionOperator processorExpression = ExpressionOperator.builder()
                .addTerm(ProcessorFields.PIPELINE, Condition.IS_DOC_REF, pipelineDocRef).build();
        ResultPage<Processor> processorResultPage = processorService.find(new ExpressionCriteria(processorExpression));
        if (processorResultPage.size() == 0) {
            return new ResultPage<>(new ArrayList<>());
        }

        final ArrayList<ProcessorFilter> filters = new ArrayList<>();
        // Now find all the processor filters
        for (Processor processor : processorResultPage.getValues()) {
            final ExpressionOperator filterExpression = ExpressionOperator.builder()
                    .addTerm(ProcessorFilterFields.PROCESSOR_ID,
                            ExpressionTerm.Condition.EQUALS,
                            processor.getId()).build();
            ResultPage<ProcessorFilter> filterResultPage = find(new ExpressionCriteria(filterExpression));
            filters.addAll(filterResultPage.getValues());
        }

        return new ResultPage<>(filters);
    }

    private ExpressionOperator decorate(final ExpressionOperator operator) {
        final ExpressionOperator.Builder builder = ExpressionOperator.builder()
                .op(operator.op())
                .enabled(operator.enabled());

        if (operator.getChildren() != null) {
            for (final ExpressionItem child : operator.getChildren()) {
                if (child instanceof ExpressionOperator expressionOperator) {
                    builder.addOperator(decorate(expressionOperator));

                } else if (child instanceof ExpressionTerm expressionTerm) {
                    DocRef docRef = expressionTerm.getDocRef();

                    try {
                        if (docRef != null) {
                            final Optional<DocRefInfo> optionalDocRefInfo = docRefInfoService.info(docRef);
                            if (optionalDocRefInfo.isPresent()) {
                                expressionTerm = ExpressionTerm.builder()
                                        .enabled(expressionTerm.enabled())
                                        .field(expressionTerm.getField())
                                        .condition(expressionTerm.getCondition())
                                        .value(expressionTerm.getValue())
                                        .docRef(optionalDocRefInfo.get().getDocRef())
                                        .build();
                            }
                        }
                    } catch (final RuntimeException e) {
                        LOGGER.debug(e::getMessage, e);
                    }

                    builder.addTerm(expressionTerm);
                }
            }
        }

        return builder.build();
    }

    @Override
    public List<ReprocessDataInfo> reprocess(final CreateProcessFilterRequest request) {
        return securityContext.secureResult(PERMISSION, () -> {
            final List<ReprocessDataInfo> info = new ArrayList<>();

            try {
                // We want to find all processors that need reprocessing filters.
                final List<String> processorUuidList = metaService.getProcessorUuidList(
                        new FindMetaCriteria(request.getQueryData().getExpression()));
                processorUuidList.forEach(processorUuid -> {
                    try {
                        processorService.fetchByUuid(processorUuid).ifPresent(processor -> {
                            final ProcessorFilter processorFilter = create(processor, request);

                            info.add(new ReprocessDataInfo(Severity.INFO, "Added reprocess filter to " +
                                    getPipelineDetails(processor.getPipelineUuid()) +
                                    " with priority " +
                                    processorFilter.getPriority(),
                                    null));
                        });
                    } catch (final RuntimeException e) {
                        info.add(new ReprocessDataInfo(Severity.ERROR,
                                "Unable to add reprocess filter for processor " +
                                        processorUuid, e.getMessage()));
                    }
                });
            } catch (final RuntimeException e) {
                info.add(new ReprocessDataInfo(Severity.ERROR, e.getMessage(), null));
            }

            return info;
        });
    }

    private int getAutoPriority(final Processor processor,
                                final int defaultPriority,
                                final boolean autoPriority) {
        if (!autoPriority) {
            return defaultPriority;
        }

        int priority = defaultPriority;

        final ExpressionOperator filterExpression = ExpressionOperator.builder()
                .addTerm(ProcessorFilterFields.PROCESSOR_ID, ExpressionTerm.Condition.EQUALS, processor.getId())
                .addTerm(ProcessorFilterFields.DELETED, ExpressionTerm.Condition.EQUALS, false)
                .build();
        final List<ProcessorFilter> list = processorFilterDao.find(
                new ExpressionCriteria(filterExpression)).getValues();
        for (final ProcessorFilter filter : list) {
            // Ignore reprocess filters.
            if (!filter.isReprocess()) {
                if (filter.isEnabled()) {
                    // If it's enabled then just return the priority.
                    return filter.getPriority();
                } else {
                    priority = filter.getPriority();
                }
            }
        }

        return priority;
    }

    private String getPipelineDetails(final String uuid) {
        try {
            final DocRef pipelineDocRef = new DocRef("Pipeline", uuid);
            final Optional<DocRefInfo> optionalDocRefInfo = docRefInfoService.info(pipelineDocRef);
            return optionalDocRefInfo
                    .map(DocRefInfo::getDocRef)
                    .map(DocRef::getName)
                    .map(name -> name + " (" + uuid + ")")
                    .orElse(uuid);
        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return uuid;
    }

    private ProcessorFilter ensureValid(final ProcessorFilter processorFilter) {
        if (processorFilter == null) {
            return null;
        }

        if (processorFilter.getUuid() == null) {
            processorFilter.setUuid(UUID.randomUUID().toString());
        }

        if (processorFilter.getQueryData() == null) {
            throw new IllegalArgumentException("QueryData cannot be null creating ProcessorFilter" + processorFilter);
        }

        if (processorFilter.getQueryData().getDataSource() == null) {
            processorFilter.getQueryData().setDataSource(MetaFields.STREAM_STORE_DOC_REF);
        }

        AuditUtil.stamp(securityContext, processorFilter);
        return processorFilter;
    }
}
