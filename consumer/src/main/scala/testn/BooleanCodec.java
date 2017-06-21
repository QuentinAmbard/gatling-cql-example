package testn;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import java.nio.ByteBuffer;

public class BooleanCodec extends TypeCodec<String> {

    public static final BooleanCodec instance = new BooleanCodec();

    private BooleanCodec() {
        super(DataType.cboolean(), String.class);
    }

    @Override
    public ByteBuffer serialize(String value, ProtocolVersion protocolVersion) {
        return value == null ? null : cboolean().serializeNoBoxing(Boolean.valueOf(value), protocolVersion);
    }

    @Override
    public String deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) {
        return bytes == null || bytes.remaining() == 0 ? null : cboolean().deserializeNoBoxing(bytes, protocolVersion)+"";
    }

    @Override
    public String format(String value) {
        return value.toString();
    }

    @Override
    public String parse(String value) {
        try {
            return value == null || value.isEmpty() || value.equalsIgnoreCase("NULL") ? null : value;
        } catch (IllegalArgumentException e) {
            throw new InvalidTypeException(String.format("Cannot parse UUID value from \"%s\"", value), e);
        }
    }

}
