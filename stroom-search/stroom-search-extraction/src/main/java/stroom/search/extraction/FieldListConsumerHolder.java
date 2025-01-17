package stroom.search.extraction;

import stroom.util.pipeline.scope.PipelineScoped;

import jakarta.inject.Inject;

import java.util.List;

@PipelineScoped
public class FieldListConsumerHolder implements FieldListConsumer {

    private final ExtractionState extractionState;
    private FieldListConsumer fieldListConsumer;

    @Inject
    FieldListConsumerHolder(final ExtractionState extractionState) {
        this.extractionState = extractionState;
    }

    @Override
    public void accept(final List<FieldValue> fieldValues) {
        fieldListConsumer.accept(fieldValues);
        extractionState.incrementCount();
    }

    public void setFieldListConsumer(final FieldListConsumer fieldListConsumer) {
        this.fieldListConsumer = fieldListConsumer;
    }
}
