package stroom.annotation.impl;

import stroom.annotation.api.AnnotationCreator;
import stroom.annotation.api.AnnotationFields;
import stroom.annotation.shared.AnnotationDetail;
import stroom.annotation.shared.CreateEntryRequest;
import stroom.annotation.shared.EventId;
import stroom.annotation.shared.EventLink;
import stroom.annotation.shared.SetAssignedToRequest;
import stroom.annotation.shared.SetStatusRequest;
import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.FieldInfo;
import stroom.datasource.api.v2.FindFieldInfoCriteria;
import stroom.docref.DocRef;
import stroom.entity.shared.ExpressionCriteria;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.common.v2.FieldInfoResultPageBuilder;
import stroom.query.language.functions.FieldIndex;
import stroom.query.language.functions.ValuesConsumer;
import stroom.search.extraction.ExpressionFilter;
import stroom.searchable.api.Searchable;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.security.user.api.UserNameService;
import stroom.util.shared.PermissionException;
import stroom.util.shared.ResultPage;
import stroom.util.shared.UserName;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

public class AnnotationService implements Searchable, AnnotationCreator {

    private static final DocRef ANNOTATIONS_PSEUDO_DOC_REF = new DocRef("Searchable", "Annotations", "Annotations");

    private final AnnotationDao annotationDao;
    private final SecurityContext securityContext;
    private final UserNameService userNameService;

    @Inject
    AnnotationService(final AnnotationDao annotationDao,
                      final SecurityContext securityContext,
                      final UserNameService userNameService) {
        this.annotationDao = annotationDao;
        this.securityContext = securityContext;
        this.userNameService = userNameService;
    }

    @Override
    public DocRef getDocRef() {
        try {
            checkPermission();
            return ANNOTATIONS_PSEUDO_DOC_REF;
        } catch (final PermissionException e) {
            return null;
        }
    }

    @Override
    public ResultPage<FieldInfo> getFieldInfo(final FindFieldInfoCriteria criteria) {
        return FieldInfoResultPageBuilder.builder(criteria).addAll(AnnotationFields.FIELDS).build();
    }

    @Override
    public Optional<String> fetchDocumentation(final DocRef docRef) {
        return Optional.empty();
    }

    @Override
    public DateField getTimeField() {
        return AnnotationFields.UPDATED_ON_FIELD;
    }

    @Override
    public void search(final ExpressionCriteria criteria,
                       final FieldIndex fieldIndex,
                       final ValuesConsumer consumer) {
        checkPermission();

        final ExpressionFilter expressionFilter = ExpressionFilter.builder()
                .addReplacementFilter(
                        AnnotationFields.CURRENT_USER_FUNCTION,
                        securityContext.getUserIdentityForAudit())
                .build();

        ExpressionOperator expression = criteria.getExpression();
        expression = expressionFilter.copy(expression);
        criteria.setExpression(expression);

        annotationDao.search(criteria, fieldIndex, consumer);
    }

    private UserName getCurrentUser() {
        return securityContext.getUserName();
    }

    AnnotationDetail getDetail(Long annotationId) {
        checkPermission();
        return annotationDao.getDetail(annotationId);
    }

    public AnnotationDetail createEntry(final CreateEntryRequest request) {
        checkPermission();
        return annotationDao.createEntry(request, getCurrentUser());
    }

    List<EventId> getLinkedEvents(final Long annotationId) {
        checkPermission();
        return annotationDao.getLinkedEvents(annotationId);
    }

    List<EventId> link(final EventLink eventLink) {
        checkPermission();
        return annotationDao.link(eventLink, getCurrentUser());
    }

    List<EventId> unlink(final EventLink eventLink) {
        checkPermission();
        return annotationDao.unlink(eventLink, getCurrentUser());
    }

    Integer setStatus(SetStatusRequest request) {
        checkPermission();
        return annotationDao.setStatus(request, getCurrentUser());
    }

    Integer setAssignedTo(SetAssignedToRequest request) {
        checkPermission();
        return annotationDao.setAssignedTo(request, getCurrentUser());
    }

    private void checkPermission() {
        if (!securityContext.hasAppPermission(PermissionNames.ANNOTATIONS)) {
            throw new PermissionException(
                    securityContext.getUserIdentityForAudit(),
                    "You do not have permission to use annotations");
        }
    }
}
