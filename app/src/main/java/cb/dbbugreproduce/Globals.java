package cb.dbbugreproduce;

import android.os.Environment;

import org.apache.commons.io.FilenameUtils;

public class Globals {
    private final String PrimaryDirectoryName = "CTPrimary";
    private final String DatabaseName         = "ctdb";
    private final String DatabaseExt          = ".cblite2";

    public String GetPrimaryDirectoryPath()
    {
        String envPath = Environment.getExternalStorageDirectory().toString();
        String path = FilenameUtils.concat(envPath, PrimaryDirectoryName);
        return path;
    }

    public String GetDatabaseFullPath(){
        return FilenameUtils.concat(GetPrimaryDirectoryPath(), GetDatabaseNamePlusExtension());
    }

    public String GetDatabaseNamePlusExtension(){
        return DatabaseName + DatabaseExt;
    }

    public String GetDatabaseNameOnly(){
        return DatabaseName;
    }

}
