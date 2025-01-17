package stroom.query.client.presenter;

import stroom.datasource.api.v2.FindFieldInfoCriteria;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.docref.StringMatch;
import stroom.item.client.SelectionListModel;
import stroom.query.shared.QueryHelpRequest;
import stroom.query.shared.QueryHelpRow;
import stroom.query.shared.QueryHelpType;
import stroom.query.shared.QueryResource;
import stroom.util.shared.CriteriaFieldSort;
import stroom.util.shared.PageRequest;
import stroom.util.shared.PageResponse;
import stroom.util.shared.ResultPage;

import com.google.gwt.core.client.GWT;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class DynamicQueryHelpSelectionListModel implements SelectionListModel<QueryHelpRow, QueryHelpSelectionItem> {

    private static final QueryResource QUERY_RESOURCE = GWT.create(QueryResource.class);

    private final RestFactory restFactory;

    private DocRef dataSourceRef;
    private String query;
    private boolean showAll = true;
    private QueryHelpRequest lastRequest;

    @Inject
    public DynamicQueryHelpSelectionListModel(final RestFactory restFactory) {
        this.restFactory = restFactory;
    }

    @Override
    public void onRangeChange(final QueryHelpSelectionItem parent,
                              final String filter,
                              final PageRequest pageRequest,
                              final Consumer<ResultPage<QueryHelpSelectionItem>> consumer) {
        final String parentId;
        if (parent != null) {
            parentId = unwrap(parent).getId() + ".";
        } else {
            parentId = "";
        }

        final StringMatch stringMatch = StringMatch.contains(filter);
        final CriteriaFieldSort sort = new CriteriaFieldSort(
                FindFieldInfoCriteria.SORT_BY_NAME,
                false,
                true);
        final QueryHelpRequest request = new QueryHelpRequest(
                pageRequest,
                Collections.singletonList(sort),
                query,
                dataSourceRef,
                parentId,
                stringMatch,
                showAll);

        // Only fetch if the request has changed.
        if (!request.equals(lastRequest)) {
            lastRequest = request;

            restFactory.builder()
                    .forResultPageOf(QueryHelpRow.class)
                    .onSuccess(response -> {
                        // Only update if the request is still current.
                        if (request == lastRequest) {
                            final ResultPage<QueryHelpSelectionItem> resultPage;
                            if (response.getValues().size() > 0) {
                                List<QueryHelpSelectionItem> items = response
                                        .getValues()
                                        .stream()
                                        .map(this::wrap)
                                        .collect(Collectors.toList());

                                resultPage = new ResultPage<>(items, response.getPageResponse());
                            } else {
                                final List<QueryHelpSelectionItem> rows = Collections
                                        .singletonList(new QueryHelpSelectionItem(QueryHelpRow
                                                .builder()
                                                .type(QueryHelpType.TITLE)
                                                .id(parentId + "none")
                                                .title("[ none ]")
                                                .build()));
                                resultPage = new ResultPage<>(rows, new PageResponse(0, 1, 1L, true));
                            }

                            consumer.accept(resultPage);
                        }
                    })
                    .call(QUERY_RESOURCE)
                    .fetchQueryHelpItems(request);
        }
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    @Override
    public void reset() {
        lastRequest = null;
    }

    @Override
    public boolean displayFilter() {
        return true;
    }

    @Override
    public boolean displayPath() {
        return true;
    }

    @Override
    public boolean displayPager() {
        return true;
    }

    @Override
    public QueryHelpSelectionItem wrap(final QueryHelpRow item) {
        return new QueryHelpSelectionItem(item);
    }

    @Override
    public QueryHelpRow unwrap(final QueryHelpSelectionItem selectionItem) {
        if (selectionItem == null) {
            return null;
        }
        return selectionItem.getQueryHelpRow();
    }

    public void setDataSourceRef(final DocRef dataSourceRef) {
        this.dataSourceRef = dataSourceRef;
    }

    public void setShowAll(final boolean showAll) {
        this.showAll = showAll;
    }
}
