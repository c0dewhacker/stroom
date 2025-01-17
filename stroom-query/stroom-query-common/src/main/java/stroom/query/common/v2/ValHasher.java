package stroom.query.common.v2;

import stroom.query.language.functions.Val;
import stroom.query.language.functions.ValSerialiser;
import stroom.query.language.functions.ref.ErrorConsumer;
import stroom.query.language.functions.ref.OutputFactory;

import com.esotericsoftware.kryo.io.Output;
import net.openhft.hashing.LongHashFunction;

public class ValHasher {

    private final OutputFactory outputFactory;
    private final ErrorConsumer errorConsumer;
    private int bufferSize = 16;

    public ValHasher(final OutputFactory outputFactory,
                     final ErrorConsumer errorConsumer) {
        this.outputFactory = outputFactory;
        this.errorConsumer = errorConsumer;
    }

    public long hash(final Val[] values) {
        if (values == null) {
            return -1;
        } else if (values.length == 0) {
            return 0;
        }
        try (final Output output = outputFactory.createHashOutput(bufferSize, errorConsumer)) {
            ValSerialiser.writeArray(output, values);
            output.flush();
            final byte[] bytes = output.toBytes();
            bufferSize = Math.max(bufferSize, output.getBuffer().length);
            return LongHashFunction.xx3().hashBytes(bytes);
        }
    }
}
