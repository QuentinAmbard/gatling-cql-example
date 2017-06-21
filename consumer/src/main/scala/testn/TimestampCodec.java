package testn;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimestampCodec extends TypeCodec<String> {

    public static final TimestampCodec instance = new TimestampCodec();

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");

    private TimestampCodec() {
        super(DataType.timestamp(), String.class);
    }


    @Override
    public ByteBuffer serialize(String valueStr, ProtocolVersion protocolVersion) {
        try {
            Date value = formatter.parse(valueStr);
            return value == null ? null : bigint().serializeNoBoxing(value.getTime(), protocolVersion);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) {
        return bytes == null || bytes.remaining() == 0 ? null : formatter.format(new Date(bigint().deserializeNoBoxing(bytes, protocolVersion)));
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
