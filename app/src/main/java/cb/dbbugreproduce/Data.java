package cb.dbbugreproduce;

import com.couchbase.lite.Database;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import cb.dbbugreproduce.models.CadecDataModel;

public class Data {

    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    //Database instance CouchDBLite
    public static Database database = null;

    public static CadecDataModel CadecObjModel;

    public static final AtomicBoolean StopDebugLoop = new AtomicBoolean(false);
}
