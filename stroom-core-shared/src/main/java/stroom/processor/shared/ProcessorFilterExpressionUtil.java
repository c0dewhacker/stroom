package stroom.processor.shared;

import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;

public final class ProcessorFilterExpressionUtil {

    private ProcessorFilterExpressionUtil() {
        // Utility class.
    }

    public static ExpressionOperator createBasicExpression() {
        return ExpressionOperator.builder()
                .addTerm(ProcessorFields.DELETED, Condition.EQUALS, false)
                .addTerm(ProcessorFilterFields.DELETED, Condition.EQUALS, false)
                .build();
    }

    public static ExpressionOperator createFolderExpression(final DocRef folder) {
        return createFoldersExpression(folder);
    }

    public static ExpressionOperator createFoldersExpression(final DocRef... folders) {
        final ExpressionOperator.Builder builder = ExpressionOperator.builder();

        if (folders != null) {
            final ExpressionOperator.Builder or = ExpressionOperator.builder().op(Op.OR);
            for (final DocRef folder : folders) {
                or.addTerm(ProcessorFields.PIPELINE, Condition.IN_FOLDER, folder);
                or.addTerm(ProcessorFields.ANALYTIC_RULE, Condition.IN_FOLDER, folder);
            }
            builder.addOperator(or.build());
        }

        return builder.addTerm(ProcessorFields.DELETED, Condition.EQUALS, false)
                .addTerm(ProcessorFilterFields.DELETED, Condition.EQUALS, false)
                .build();
    }

    public static ExpressionOperator createPipelineExpression(final DocRef pipelineRef) {
        return ExpressionOperator.builder()
//                .addTerm(
//                        ProcessorFields.PROCESSOR_TYPE,
//                        Condition.EQUALS,
//                        ProcessorType.PIPELINE.getDisplayValue())
                .addTerm(ProcessorFields.PIPELINE, Condition.IS_DOC_REF, pipelineRef)
                .addTerm(ProcessorFields.DELETED, Condition.EQUALS, false)
                .addTerm(ProcessorFilterFields.DELETED, Condition.EQUALS, false)
                .build();
    }

    public static ExpressionOperator createAnalyticRuleExpression(final DocRef analyticRuleRef) {
        return ExpressionOperator.builder()
//                .addTerm(
//                        ProcessorFields.PROCESSOR_TYPE,
//                        Condition.EQUALS,
//                        ProcessorType.STREAMING_ANALYTIC.getDisplayValue())
                .addTerm(ProcessorFields.ANALYTIC_RULE, Condition.IS_DOC_REF, analyticRuleRef)
                .addTerm(ProcessorFields.DELETED, Condition.EQUALS, false)
                .addTerm(ProcessorFilterFields.DELETED, Condition.EQUALS, false)
                .build();
    }
}
