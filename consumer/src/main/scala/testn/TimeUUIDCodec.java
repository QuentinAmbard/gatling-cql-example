package testn;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import java.nio.ByteBuffer;
import java.util.UUID;

public class TimeUUIDCodec extends TypeCodec<String> {

    public static final TimeUUIDCodec instance = new TimeUUIDCodec();

    private TimeUUIDCodec() {
        super(DataType.timeuuid(), String.class);
    }


    @Override
    public ByteBuffer serialize(String valueStr, ProtocolVersion protocolVersion) {
        if (valueStr == null) {
            return null;
        }
        UUID value = UUID.fromString(valueStr);
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(0, value.getMostSignificantBits());
        bb.putLong(8, value.getLeastSignificantBits());
        return bb;
    }

    @Override
    public String deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) {
        return bytes == null || bytes.remaining() == 0 ? null : new UUID(bytes.getLong(bytes.position()), bytes.getLong(bytes.position() + 8)).toString();
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
