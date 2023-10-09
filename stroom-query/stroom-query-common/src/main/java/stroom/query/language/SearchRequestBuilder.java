package stroom.query.language;

import stroom.expression.api.ExpressionContext;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Filter;
import stroom.query.api.v2.Format;
import stroom.query.api.v2.HoppingWindow;
import stroom.query.api.v2.ParamSubstituteUtil;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.ResultRequest.Fetch;
import stroom.query.api.v2.ResultRequest.ResultStyle;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.Sort;
import stroom.query.api.v2.Sort.SortDirection;
import stroom.query.api.v2.TableSettings;
import stroom.query.common.v2.DateExpressionParser;
import stroom.query.common.v2.DateExpressionParser.DatePoint;
import stroom.query.language.functions.Expression;
import stroom.query.language.functions.ExpressionParser;
import stroom.query.language.functions.FieldIndex;
import stroom.query.language.functions.ParamFactory;
import stroom.query.language.token.AbstractToken;
import stroom.query.language.token.KeywordGroup;
import stroom.query.language.token.StructureBuilder;
import stroom.query.language.token.Token;
import stroom.query.language.token.TokenException;
import stroom.query.language.token.TokenGroup;
import stroom.query.language.token.TokenType;
import stroom.query.language.token.Tokeniser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class SearchRequestBuilder {

    public static final String TABLE_COMPONENT_ID = "table";
    public static final String VIS_COMPONENT_ID = "vis";

    private final VisualisationTokenConsumer visualisationTokenConsumer;

    private ExpressionContext expressionContext;
    private final FieldIndex fieldIndex;
    private final Map<String, Expression> expressionMap;
    private final Set<String> addedFields = new HashSet<>();
    private final List<AbstractToken> havingTokens = new ArrayList<>();

    @Inject
    public SearchRequestBuilder(final VisualisationTokenConsumer visualisationTokenConsumer) {
        this.visualisationTokenConsumer = visualisationTokenConsumer;
        this.fieldIndex = new FieldIndex();
        this.expressionMap = new HashMap<>();
    }

    public void extractDataSourceNameOnly(final String string, final Consumer<String> consumer) {
        // Get a list of tokens.
        final List<Token> tokens = Tokeniser.parse(string);
        if (tokens.size() == 0) {
            throw new TokenException(null, "No tokens");
        }

        // Create structure.
        final TokenGroup tokenGroup = StructureBuilder.create(tokens);

        // Assume we have a root bracket group.
        final List<AbstractToken> childTokens = tokenGroup.getChildren();

        // Add data source.
        addDataSourceName(childTokens, consumer, true);
    }

    public SearchRequest create(final String string,
                                final SearchRequest in,
                                final ExpressionContext expressionContext) {
        Objects.requireNonNull(in, "Null sample request");
        Objects.requireNonNull(expressionContext, "Null expression context");
        Objects.requireNonNull(expressionContext.getDateTimeSettings(), "Null date time settings");

        // Set the expression context.
        this.expressionContext = expressionContext;

        // Get a list of tokens.
        final List<Token> tokens = Tokeniser.parse(string);
        if (tokens.size() == 0) {
            throw new TokenException(null, "No tokens");
        }

        // Create structure.
        final TokenGroup tokenGroup = StructureBuilder.create(tokens);

        // Assume we have a root bracket group.
        final List<AbstractToken> childTokens = tokenGroup.getChildren();

        final Query.Builder queryBuilder = Query.builder();
        if (in.getQuery() != null) {
            queryBuilder.params(in.getQuery().getParams());
            queryBuilder.timeRange(in.getQuery().getTimeRange());
        }

        // Keep track of consumed tokens to check token order.
        final List<TokenType> consumedTokens = new ArrayList<>();

        // Create result requests.
        final List<ResultRequest> resultRequests = new ArrayList<>();
        addTableSettings(childTokens, consumedTokens, resultRequests, queryBuilder);

        // Try to make a query.
        Query query = queryBuilder.build();
        if (query.getDataSource() == null) {
            throw new TokenException(null, "No data source has been specified.");
        }

        // Make sure there is a non-null expression.
        if (query.getExpression() == null) {
            query = query.copy().expression(ExpressionOperator.builder().build()).build();
        }

        return new SearchRequest(
                in.getSearchRequestSource(),
                in.getKey(),
                query,
                resultRequests,
                in.getDateTimeSettings(),
                in.incremental(),
                in.getTimeout());
    }

    private List<AbstractToken> addDataSourceName(final List<AbstractToken> tokens,
                                                  final Consumer<String> consumer) {
        return addDataSourceName(tokens, consumer, false);
    }

    /**
     * @param isLenient Ignores any additional child tokens found. This is to support getting just
     *                  the data source from a partially written query, which is likely to not be
     *                  a valid query. For example, you may type
     *                  <pre>{@code from View sel}</pre> and at this point do code completion on 'sel'
     *                  so if this method is called it will treat 'sel as an extra child.
     */
    private List<AbstractToken> addDataSourceName(final List<AbstractToken> tokens,
                                                  final Consumer<String> consumer,
                                                  final boolean isLenient) {
        AbstractToken token = tokens.get(0);

        // The first token must be `FROM`.
        if (!TokenType.FROM.equals(token.getTokenType())) {
            throw new TokenException(token, "Expected from");
        }

        final KeywordGroup keywordGroup = (KeywordGroup) token;
        if (keywordGroup.getChildren().size() == 0) {
            throw new TokenException(token, "Expected data source value");
        } else if (keywordGroup.getChildren().size() > 1 && !isLenient) {
            throw new TokenException(keywordGroup.getChildren().get(1),
                    "Unexpected data source child tokens: " + keywordGroup.getChildren()
                            .stream()
                            .map(token2 -> token2.getTokenType() + ":[" + token2.getText() + "]")
                            .collect(Collectors.joining(", ")));
        }

        final AbstractToken dataSourceToken = keywordGroup.getChildren().get(0);
        if (!TokenType.isString(dataSourceToken)) {
            throw new TokenException(dataSourceToken, "Expected a token of type string");
        }
        final String dataSourceName = dataSourceToken.getUnescapedText();
        consumer.accept(dataSourceName);

        return tokens.subList(1, tokens.size());
    }

    private List<AbstractToken> addExpression(final List<AbstractToken> tokens,
                                              final List<TokenType> consumedTokens,
                                              final Set<TokenType> allowConsumed,
                                              final TokenType keyword,
                                              final Consumer<ExpressionOperator> expressionConsumer) {
        final List<AbstractToken> whereGroup = new ArrayList<>();
        final List<TokenType> localConsumedTokens = new ArrayList<>(consumedTokens);
        final Set<TokenType> allowFollowing = new HashSet<>(allowConsumed);
        int i = 0;
        for (; i < tokens.size(); i++) {
            final AbstractToken token = tokens.get(i);
            if (token instanceof final KeywordGroup keywordGroup) {
                final TokenType tokenType = keywordGroup.getTokenType();
                if (keyword.equals(tokenType)) {
                    // Make sure we haven't already consumed the keyword token.
                    checkTokenOrder(token, localConsumedTokens, Set.of(TokenType.FROM), allowConsumed);
                    localConsumedTokens.add(tokenType);
                    consumedTokens.add(tokenType);
                    allowFollowing.add(tokenType);
                    allowFollowing.addAll(Set.of(TokenType.AND, TokenType.OR, TokenType.NOT));
                    whereGroup.add(keywordGroup);
                } else if (TokenType.AND.equals(tokenType) ||
                        TokenType.OR.equals(tokenType) ||
                        TokenType.NOT.equals(tokenType)) {
                    // Check we have already consumed the expected keyword token.
                    checkTokenOrder(token, localConsumedTokens, Set.of(TokenType.FROM, keyword), allowFollowing);
                    localConsumedTokens.add(tokenType);
                    whereGroup.add(keywordGroup);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if (whereGroup.size() > 0) {
            final ExpressionOperator expressionOperator = processLogic(whereGroup);
            expressionConsumer.accept(expressionOperator);
        } else {
            expressionConsumer.accept(ExpressionOperator.builder().build());
        }

        if (i < tokens.size()) {
            return tokens.subList(i, tokens.size());
        }
        return Collections.emptyList();
    }

    private void addTerm(
            final List<AbstractToken> tokens,
            final ExpressionOperator.Builder parentBuilder) {
        if (tokens.size() < 3) {
            throw new TokenException(tokens.get(0), "Incomplete term");
        }

        final AbstractToken fieldToken = tokens.get(0);
        final AbstractToken conditionToken = tokens.get(1);

        if (!TokenType.isString(fieldToken) && !TokenType.PARAM.equals(fieldToken.getTokenType())) {
            throw new TokenException(fieldToken, "Expected field string");
        }
        if (!TokenType.CONDITIONS.contains(conditionToken.getTokenType())) {
            throw new TokenException(conditionToken, "Expected condition token");
        }

        final String field = fieldToken.getUnescapedText();
        final StringBuilder value = new StringBuilder();
        int end = addValue(tokens, value, 2);

        if (TokenType.BETWEEN.equals(conditionToken.getTokenType())) {
            if (tokens.size() == end) {
                throw new TokenException(tokens.get(tokens.size() - 1), "Expected between and");
            }

            final AbstractToken betweenAnd = tokens.get(end);
            if (!TokenType.BETWEEN_AND.equals(betweenAnd.getTokenType())) {
                throw new TokenException(betweenAnd, "Expected between and");
            }
            value.append(", ");
            end = addValue(tokens, value, end + 1);
        }

        // Check we consumed all tokens to get the value(s).
        if (end < tokens.size()) {
            throw new TokenException(tokens.get(end), "Unexpected token");
        }

        // If we have a where clause then we expect the next token to contain an expression.
        Condition cond;
        boolean not = false;
        switch (conditionToken.getTokenType()) {
            case EQUALS -> cond = Condition.EQUALS;
            case NOT_EQUALS -> {
                cond = Condition.EQUALS;
                not = true;
            }
            case GREATER_THAN -> cond = Condition.GREATER_THAN;
            case GREATER_THAN_OR_EQUAL_TO -> cond = Condition.GREATER_THAN_OR_EQUAL_TO;
            case LESS_THAN -> cond = Condition.LESS_THAN;
            case LESS_THAN_OR_EQUAL_TO -> cond = Condition.LESS_THAN_OR_EQUAL_TO;
            case IS_NULL -> cond = Condition.IS_NULL;
            case IS_NOT_NULL -> cond = Condition.IS_NOT_NULL;
            case BETWEEN -> cond = Condition.BETWEEN;
            default -> throw new TokenException(conditionToken, "Unknown condition: " + conditionToken);
        }

        final ExpressionTerm expressionTerm = ExpressionTerm
                .builder()
                .field(field)
                .condition(cond)
                .value(value.toString().trim())
                .build();

        if (not) {
            parentBuilder
                    .addOperator(ExpressionOperator
                            .builder()
                            .op(Op.NOT)
                            .addTerm(expressionTerm)
                            .build());
        } else {
            parentBuilder.addTerm(expressionTerm);
        }
    }

    private int addValue(final List<AbstractToken> tokens,
                         final StringBuilder value,
                         final int start) {
        for (int i = start; i < tokens.size(); i++) {
            final AbstractToken token = tokens.get(i);
            if (TokenType.BETWEEN_AND.equals(token.getTokenType())) {
                value.append(parseValueTokens(tokens.subList(start, i)));
                return i;
            }
        }

        value.append(parseValueTokens(tokens.subList(start, tokens.size())));
        return tokens.size();
    }

    private String parseValueTokens(final List<AbstractToken> tokens) {
        if (tokens.size() == 0) {
            return "";
        }

        boolean dateExpression = false;
        boolean numericExpression = false;
        final StringBuilder sb = new StringBuilder();
        for (final AbstractToken token : tokens) {
            if (TokenType.FUNCTION_GROUP.equals(token.getTokenType())) {
                DatePoint foundFunction = null;
                final String function = token.getUnescapedText();
                for (final DatePoint datePoint : DatePoint.values()) {
                    if (datePoint.getFunction().equals(function)) {
                        foundFunction = datePoint;
                        break;
                    }
                }
                if (foundFunction == null) {
                    throw new TokenException(token, "Unexpected function in value");
                } else {
                    dateExpression = true;
                }
            } else if (TokenType.DURATION.equals(token.getTokenType())) {
                dateExpression = true;
            } else if (TokenType.NUMBER.equals(token.getTokenType())) {
                numericExpression = true;
            }

            sb.append(token.getUnescapedText());
        }

        final String expression = sb.toString();
        if (dateExpression) {
            try {
                DateExpressionParser.parse(expression, expressionContext.getDateTimeSettings());
            } catch (final RuntimeException e) {
                throw new TokenException(tokens.get(0), "Unexpected token");
            }
        } else if (numericExpression) {
            boolean seenSign = false;
            boolean seenNumber = false;
            for (final AbstractToken token : tokens) {
                if (TokenType.PLUS.equals(token.getTokenType()) ||
                        TokenType.MINUS.equals(token.getTokenType())) {
                    if (seenSign || seenNumber) {
                        throw new TokenException(token, "Unexpected token");
                    }
                    seenSign = true;
                } else if (TokenType.NUMBER.equals(token.getTokenType())) {
                    if (seenNumber) {
                        throw new TokenException(token, "Unexpected token");
                    }
                    seenNumber = true;
                } else {
                    throw new TokenException(token, "Unexpected token");
                }
            }
        } else if (tokens.size() > 1) {
            throw new TokenException(tokens.get(1), "Unexpected token");
        }

        return expression;
    }

    private ExpressionOperator processLogic(final List<AbstractToken> tokens) {
        ExpressionOperator.Builder builder = ExpressionOperator.builder().op(Op.AND);
        final List<AbstractToken> termTokens = new ArrayList<>();

        int i = 0;
        for (; i < tokens.size(); i++) {
            final AbstractToken token = tokens.get(i);
            final TokenType tokenType = token.getTokenType();

            final boolean logic =
                    TokenType.AND.equals(tokenType) ||
                            TokenType.OR.equals(tokenType) ||
                            TokenType.NOT.equals(tokenType);

            if (!(token instanceof KeywordGroup) &&
                    !(token instanceof TokenGroup) &&
                    !logic) {

                // Treat token as part of a term.
                termTokens.add(token);

            } else {
                // Add current term.
                if (termTokens.size() > 0) {
                    addTerm(termTokens, builder);
                    termTokens.clear();
                }

                if (token instanceof final KeywordGroup keywordGroup) {
                    switch (tokenType) {
                        case WHERE, HAVING, FILTER, AND -> builder = addAnd(builder, keywordGroup.getChildren());
                        case OR -> builder = addOr(builder, keywordGroup.getChildren());
                        case NOT -> builder = addNot(builder, keywordGroup.getChildren());
                        default -> throw new TokenException(token, "Unexpected pipe operation in query");
                    }

                } else if (token instanceof final TokenGroup tokenGroup) {
                    builder = addAnd(builder, tokenGroup.getChildren());

                } else if (logic) {
                    final List<AbstractToken> remaining = tokens.subList(i + 1, tokens.size());
                    i = tokens.size();

                    switch (tokenType) {
                        case AND -> builder = addAnd(builder, remaining);
                        case OR -> builder = addOr(builder, remaining);
                        case NOT -> builder = addNot(builder, remaining);
                        default -> throw new TokenException(token, "Unexpected token");
                    }
                }
            }
        }

        // Add remaining term.
        if (termTokens.size() > 0) {
            addTerm(termTokens, builder);
            termTokens.clear();
        }

        return builder.build();
    }

    private ExpressionOperator.Builder addAnd(final ExpressionOperator.Builder builder,
                                              final List<AbstractToken> tokens) {
        ExpressionOperator.Builder nextBuilder = builder;
        final ExpressionOperator childOperator = processLogic(tokens);
        if (childOperator.hasChildren()) {
            nextBuilder = ExpressionOperator.builder().op(Op.AND);
            final ExpressionOperator current = builder.build();
            if (current.hasChildren()) {
                nextBuilder.addOperator(current);
            }
            nextBuilder.addOperator(childOperator);
        }
        return nextBuilder;
    }

    private ExpressionOperator.Builder addOr(final ExpressionOperator.Builder builder,
                                             final List<AbstractToken> tokens) {
        ExpressionOperator.Builder nextBuilder = builder;
        final ExpressionOperator childOperator = processLogic(tokens);
        if (childOperator.hasChildren()) {
            nextBuilder = ExpressionOperator.builder().op(Op.OR);
            final ExpressionOperator current = builder.build();
            if (current.hasChildren()) {
                nextBuilder.addOperator(current);
            }
            nextBuilder.addOperator(childOperator);
        }
        return nextBuilder;
    }

    private ExpressionOperator.Builder addNot(final ExpressionOperator.Builder builder,
                                              final List<AbstractToken> tokens) {
        final ExpressionOperator childOperator = processLogic(tokens);
        if (childOperator.hasChildren()) {
            final ExpressionOperator not = ExpressionOperator
                    .builder()
                    .op(Op.NOT)
                    .addOperator(childOperator)
                    .build();
            builder.addOperator(not);
        }
        return builder;
    }

    private void addTableSettings(final List<AbstractToken> tokens,
                                  final List<TokenType> consumedTokens,
                                  final List<ResultRequest> resultRequests,
                                  final Query.Builder queryBuilder) {
        final Map<String, Sort> sortMap = new HashMap<>();
        final Map<String, Integer> groupMap = new HashMap<>();
        final Map<String, Filter> filterMap = new HashMap<>();
        int groupDepth = 0;

        final TableSettings.Builder tableSettingsBuilder = TableSettings.builder();
        TableSettings visTableSettings = null;

        List<AbstractToken> remaining = new LinkedList<>(tokens);
        while (remaining.size() > 0) {
            final AbstractToken token = remaining.get(0);

            if (token instanceof final KeywordGroup keywordGroup) {
                switch (keywordGroup.getTokenType()) {
                    case FROM -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(),
                                Set.of());
                        remaining =
                                addDataSourceName(remaining,
                                        queryBuilder::dataSourceName);
                    }
                    case WHERE -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                Set.of(TokenType.FROM));
                        remaining =
                                addExpression(remaining,
                                        consumedTokens,
                                        Set.of(TokenType.FROM),
                                        TokenType.WHERE,
                                        queryBuilder::expression);
                    }
                    case EVAL -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                Set.of(TokenType.FROM, TokenType.WHERE, TokenType.EVAL));
                        processEval(keywordGroup);
                        remaining.remove(0);
                    }
                    case WINDOW -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                Set.of(TokenType.FROM, TokenType.WHERE, TokenType.EVAL));
                        processWindow(
                                keywordGroup,
                                tableSettingsBuilder);
                        remaining.remove(0);
                    }
                    case FILTER -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                Set.of(TokenType.FROM, TokenType.WHERE, TokenType.EVAL, TokenType.WINDOW));
                        remaining =
                                addExpression(remaining,
                                        consumedTokens,
                                        Set.of(TokenType.FROM, TokenType.WHERE, TokenType.EVAL, TokenType.WINDOW),
                                        TokenType.FILTER,
                                        tableSettingsBuilder::valueFilter);
                    }
                    case SORT -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                inverse(Set.of(TokenType.LIMIT, TokenType.SELECT, TokenType.HAVING, TokenType.VIS)));
                        processSortBy(
                                keywordGroup,
                                sortMap);
                        remaining.remove(0);
                    }
                    case GROUP -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                inverse(Set.of(TokenType.LIMIT, TokenType.SELECT, TokenType.HAVING, TokenType.VIS)));
                        processGroupBy(
                                keywordGroup,
                                groupMap,
                                groupDepth);
                        groupDepth++;
                        remaining.remove(0);
                    }
                    case HAVING -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                inverse(Set.of(TokenType.LIMIT, TokenType.SELECT, TokenType.VIS)));
                        // Remember the tokens used for having clauses as we need to ensure they are added.
                        if (keywordGroup.getChildren() != null &&
                                keywordGroup.getChildren().size() > 0) {
                            havingTokens.add(keywordGroup.getChildren().get(0));
                        }
                        remaining =
                                addExpression(remaining,
                                        consumedTokens,
                                        inverse(Set.of(TokenType.LIMIT, TokenType.SELECT, TokenType.VIS)),
                                        TokenType.HAVING,
                                        tableSettingsBuilder::aggregateFilter);
                    }
                    case SELECT -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                inverse(Set.of(TokenType.SELECT, TokenType.VIS)));
                        processSelect(
                                keywordGroup,
                                sortMap,
                                groupMap,
                                filterMap,
                                tableSettingsBuilder);
                        remaining.remove(0);
                    }
                    case LIMIT -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                inverse(Set.of(TokenType.SELECT, TokenType.VIS)));
                        processLimit(
                                keywordGroup,
                                tableSettingsBuilder);
                        remaining.remove(0);
                    }
                    case VIS -> {
                        checkTokenOrder(token,
                                consumedTokens,
                                Set.of(TokenType.FROM),
                                inverse(Set.of(TokenType.VIS)));
                        final TableSettings parentTableSettings = tableSettingsBuilder.build();
                        visTableSettings = visualisationTokenConsumer
                                .processVis(keywordGroup, parentTableSettings);
                        remaining.remove(0);
                    }
                    default -> throw new TokenException(token, "Unexpected token");
                }
            } else {
                throw new TokenException(token, "Unexpected token");
            }

            consumedTokens.add(token.getTokenType());
        }

        // Ensure StreamId and EventId fields exist if there is no grouping.
        if (groupDepth == 0) {
            if (!addedFields.contains(FieldIndex.FALLBACK_STREAM_ID_FIELD_NAME)) {
                tableSettingsBuilder.addFields(buildSpecialField(FieldIndex.FALLBACK_STREAM_ID_FIELD_NAME));
            }
            if (!addedFields.contains(FieldIndex.FALLBACK_EVENT_ID_FIELD_NAME)) {
                tableSettingsBuilder.addFields(buildSpecialField(FieldIndex.FALLBACK_EVENT_ID_FIELD_NAME));
            }
        }

        // Add missing HAVING fields.
        for (final AbstractToken token : havingTokens) {
            final String fieldName = token.getUnescapedText();
            if (!addedFields.contains(fieldName)) {
                final String id = "__" + fieldName.replaceAll("\\s", "_") + "__";
                addField(token,
                        id,
                        fieldName,
                        fieldName,
                        false,
                        true,
                        sortMap,
                        groupMap,
                        filterMap,
                        tableSettingsBuilder);
            }
        }

        final TableSettings tableSettings = tableSettingsBuilder
                .extractValues(true)
                .build();

        final ResultRequest tableResultRequest = new ResultRequest(TABLE_COMPONENT_ID,
                Collections.singletonList(tableSettings),
                null,
                null,
                null,
                ResultStyle.TABLE,
                Fetch.ALL);
        resultRequests.add(tableResultRequest);

        if (visTableSettings != null) {
            final List<TableSettings> tableSettingsList = new ArrayList<>();
            tableSettingsList.add(tableSettings);
            tableSettingsList.add(visTableSettings);
            final ResultRequest qlVisResultRequest = new ResultRequest(VIS_COMPONENT_ID,
                    tableSettingsList,
                    null,
                    null,
                    null,
                    ResultStyle.QL_VIS,
                    Fetch.ALL);
            resultRequests.add(qlVisResultRequest);
        }
    }

    public Field buildSpecialField(final String name) {
        addedFields.add(name);
        return Field.builder()
                .id(name)
                .name(name)
                .expression(ParamSubstituteUtil.makeParam(name))
                .visible(false)
                .special(true)
                .build();
    }

    private void processWindow(final KeywordGroup keywordGroup,
                               final TableSettings.Builder builder) {
        final List<AbstractToken> children = keywordGroup.getChildren();

        String field;
        String durationString;
        String advanceString = null;

        // Get field name.
        if (children.size() > 0) {
            final AbstractToken token = children.get(0);
            if (!TokenType.isString(token)) {
                throw new TokenException(token, "Syntax exception");
            }
            field = token.getUnescapedText();
        } else {
            throw new TokenException(keywordGroup, "Expected field");
        }

        // Get BY.
        if (children.size() > 1) {
            final AbstractToken token = children.get(1);
            if (!TokenType.BY.equals(token.getTokenType())) {
                throw new TokenException(token, "Syntax exception, expected by");
            }
        } else {
            throw new TokenException(keywordGroup, "Syntax exception, expected by");
        }

        // Get duration.
        if (children.size() > 2) {
            final AbstractToken token = children.get(2);
            if (!TokenType.DURATION.equals(token.getTokenType())) {
                throw new TokenException(token, "Syntax exception, expected valid window duration");
            }
            durationString = token.getUnescapedText();
        } else {
            throw new TokenException(keywordGroup, "Syntax exception, expected window duration");
        }

        // Get advance.
        if (children.size() > 3) {
            final AbstractToken token = children.get(3);
            if (!TokenType.isString(token) || !token.getUnescapedText().equals("advance")) {
                throw new TokenException(token, "Syntax exception, expected advance");
            }
        }

        // If advance then get advance duration.
        if (children.size() > 3) {
            if (children.size() > 4) {
                final AbstractToken token = children.get(4);
                if (!TokenType.DURATION.equals(token.getTokenType())) {
                    throw new TokenException(token, "Syntax exception, expected valid advance duration");
                }
                advanceString = token.getUnescapedText();
            } else {
                throw new TokenException(keywordGroup, "Syntax exception, expected advance duration");
            }
        }

        if (children.size() > 5) {
            throw new TokenException(children.get(5), "Unexpected token");
        }

        builder.window(HoppingWindow.builder()
                .timeField(field)
                .windowSize(durationString)
                .advanceSize(advanceString == null
                        ? durationString
                        : advanceString)
                .build());
    }

    private void processEval(final KeywordGroup keywordGroup) {
        final List<AbstractToken> children = keywordGroup.getChildren();

        // Check we have a variable name.
        if (children.size() == 0) {
            throw new TokenException(keywordGroup, "Expected variable name following eval");
        }
        final AbstractToken variableToken = children.get(0);
        if (!TokenType.isString(variableToken)) {
            throw new TokenException(variableToken, "Expected variable name");
        }
        final String variable = variableToken.getUnescapedText();

        // Check we have equals operator.
        if (children.size() == 1) {
            throw new TokenException(variableToken, "Expected equals");
        }
        final AbstractToken equalsToken = children.get(1);
        if (!TokenType.EQUALS.equals(equalsToken.getTokenType())) {
            throw new TokenException(equalsToken, "Expected equals");
        }

        // Check we have a function expression.
        if (children.size() <= 2) {
            throw new TokenException(equalsToken, "Expected eval expression");
        }

        // Parse the expression to check it is valid.
        final List<AbstractToken> expressionTokens = children.subList(2, children.size());
        final ExpressionParser expressionParser = new ExpressionParser(new ParamFactory(expressionMap));
        try {
            final Expression expression = expressionParser.parse(expressionContext, fieldIndex, expressionTokens);
            expressionMap.put(variable, expression);
        } catch (final ParseException e) {
            throw new TokenException(keywordGroup, e.getMessage());
        }
    }

    private void processSelect(final KeywordGroup keywordGroup,
                               final Map<String, Sort> sortMap,
                               final Map<String, Integer> groupMap,
                               final Map<String, Filter> filterMap,
                               final TableSettings.Builder tableSettingsBuilder) {
        final List<AbstractToken> children = keywordGroup.getChildren();
        AbstractToken fieldToken = null;
        String columnName = null;
        boolean afterAs = false;

        for (final AbstractToken token : children) {
            if (TokenType.isString(token)) {
                if (afterAs) {
                    if (columnName != null) {
                        throw new TokenException(token, "Syntax exception, duplicate column name");
                    } else {
                        columnName = token.getUnescapedText();
                    }
                } else if (fieldToken == null) {
                    fieldToken = token;
                } else {
                    throw new TokenException(token, "Syntax exception, expected AS");
                }
            } else if (TokenType.AS.equals(token.getTokenType())) {
                if (fieldToken == null) {
                    throw new TokenException(token, "Syntax exception, expected field name");
                }

                afterAs = true;

            } else if (TokenType.COMMA.equals(token.getTokenType())) {
                if (fieldToken == null) {
                    throw new TokenException(token, "Syntax exception, expected field name");
                }

                addField(fieldToken,
                        fieldToken.getUnescapedText(),
                        columnName,
                        sortMap,
                        groupMap,
                        filterMap,
                        tableSettingsBuilder);

                fieldToken = null;
                columnName = null;
                afterAs = false;
            }
        }

        // Add final field if we have one.
        if (fieldToken != null) {
            addField(fieldToken,
                    fieldToken.getUnescapedText(),
                    columnName,
                    sortMap,
                    groupMap,
                    filterMap,
                    tableSettingsBuilder);
        }
    }

    private void addField(final AbstractToken token,
                          final String fieldName,
                          final String columnName,
                          final Map<String, Sort> sortMap,
                          final Map<String, Integer> groupMap,
                          final Map<String, Filter> filterMap,
                          final TableSettings.Builder tableSettingsBuilder) {
        addField(token,
                fieldName,
                fieldName,
                columnName,
                true,
                false,
                sortMap,
                groupMap,
                filterMap,
                tableSettingsBuilder);
    }

    private void addField(final AbstractToken token,
                          final String id,
                          final String fieldName,
                          final String columnName,
                          final boolean visible,
                          final boolean special,
                          final Map<String, Sort> sortMap,
                          final Map<String, Integer> groupMap,
                          final Map<String, Filter> filterMap,
                          final TableSettings.Builder tableSettingsBuilder) {
        addedFields.add(fieldName);
        Expression expression = expressionMap.get(fieldName);
        if (expression == null) {
            ExpressionParser expressionParser = new ExpressionParser(new ParamFactory(expressionMap));
            try {
                expression = expressionParser.parse(expressionContext, fieldIndex, Collections.singletonList(token));
            } catch (final ParseException e) {
                throw new TokenException(token, e.getMessage());
            }
        }

        Format format = Format.GENERAL;
        switch (expression.getCommonReturnType()) {
            case DATE -> format = Format.DATE_TIME;
            case LONG, INTEGER, DOUBLE, FLOAT -> format = Format.NUMBER;
            default -> {
            }
        }

        final String expressionString = expression.toString();
        final Field field = Field.builder()
                .id(id)
                .name(columnName != null
                        ? columnName
                        : fieldName)
                .expression(expressionString)
                .sort(sortMap.get(fieldName))
                .group(groupMap.get(fieldName))
                .filter(filterMap.get(fieldName))
                .format(format)
                .visible(visible)
                .special(special)
                .build();
        tableSettingsBuilder.addFields(field);
    }

    private void processLimit(final KeywordGroup keywordGroup,
                              final TableSettings.Builder tableSettingsBuilder) {
        final List<AbstractToken> children = keywordGroup.getChildren();
        for (final AbstractToken t : children) {
            if (TokenType.isString(t) || TokenType.NUMBER.equals(t.getTokenType())) {
                try {
                    tableSettingsBuilder.addMaxResults(Long.parseLong(t.getUnescapedText()));
                } catch (final NumberFormatException e) {
                    throw new TokenException(t, "Syntax exception, expected number");
                }
            } else if (!TokenType.COMMA.equals(t.getTokenType())) {
                // We expect numbers and commas.
                throw new TokenException(t, "Syntax exception, expected number");
            }
        }
    }

    private void processSortBy(final KeywordGroup keywordGroup,
                               final Map<String, Sort> sortMap) {
        String fieldName = null;
        SortDirection direction = null;
        final List<AbstractToken> children = keywordGroup.getChildren();
        boolean first = true;
        for (final AbstractToken t : children) {
            if (first && TokenType.BY.equals(t.getTokenType())) {
                // Ignore
            } else {
                if (TokenType.isString(t)) {
                    if (fieldName == null) {
                        fieldName = t.getUnescapedText();
                    } else if (direction == null) {
                        try {
                            if (t.getUnescapedText().toLowerCase(Locale.ROOT).equalsIgnoreCase("asc")) {
                                direction = SortDirection.ASCENDING;
                            } else if (t.getUnescapedText().toLowerCase(Locale.ROOT).equalsIgnoreCase("desc")) {
                                direction = SortDirection.DESCENDING;
                            } else {
                                direction = SortDirection.valueOf(t.getUnescapedText());
                            }
                        } catch (final IllegalArgumentException e) {
                            throw new TokenException(t, "Syntax exception, expected sort direction 'asc' or 'desc'");
                        }
                    }
                } else if (TokenType.COMMA.equals(t.getTokenType())) {
                    if (fieldName == null) {
                        throw new TokenException(t, "Syntax exception, expected field name");
                    }
                    final Sort sort = new Sort(sortMap.size(),
                            direction != null
                                    ? direction
                                    : SortDirection.ASCENDING);
                    sortMap.put(fieldName, sort);
                    fieldName = null;
                    direction = null;
                } else {
                    throw new TokenException(t, "Syntax exception, expected string");
                }
            }

            first = false;
        }

        if (fieldName != null) {
            final Sort sort = new Sort(sortMap.size(),
                    direction != null
                            ? direction
                            : SortDirection.ASCENDING);
            sortMap.put(fieldName, sort);
        }
    }

    private void processGroupBy(final KeywordGroup keywordGroup,
                                final Map<String, Integer> groupMap,
                                final int groupDepth) {
        String fieldName = null;
        final List<AbstractToken> children = keywordGroup.getChildren();
        boolean first = true;
        for (final AbstractToken t : children) {
            if (first && TokenType.BY.equals(t.getTokenType())) {
                // Ignore
            } else {
                if (TokenType.isString(t)) {
                    if (fieldName == null) {
                        fieldName = t.getUnescapedText();
                    } else {
                        throw new TokenException(t, "Syntax exception, expected comma");
                    }
                } else if (TokenType.COMMA.equals(t.getTokenType())) {
                    if (fieldName == null) {
                        throw new TokenException(t, "Syntax exception, expected field name");
                    }
                    groupMap.put(fieldName, groupDepth);
                    fieldName = null;
                } else {
                    throw new TokenException(t, "Syntax exception, expected field name");
                }
            }

            first = false;
        }
        if (fieldName != null) {
            groupMap.put(fieldName, groupDepth);
        }
    }

    private void checkTokenOrder(final AbstractToken token,
                                 final List<TokenType> consumedTokens,
                                 final Set<TokenType> requireConsumed,
                                 final Set<TokenType> allowConsumed) {
        for (final TokenType tokenType : requireConsumed) {
            if (!consumedTokens.contains(tokenType)) {
                throw new TokenException(token,
                        "Required token " + tokenType + " before " + token.getTokenType());
            }
        }
        for (final TokenType tokenType : consumedTokens) {
            if (!allowConsumed.contains(tokenType)) {
                throw new TokenException(token,
                        "Unexpected token " + token.getTokenType() + " after " + tokenType);
            }
        }
    }

    private Set<TokenType> inverse(final Set<TokenType> tokenTypes) {
        final Set<TokenType> set = new HashSet<>(TokenType.ALL);
        set.removeAll(tokenTypes);
        return set;
    }
}
