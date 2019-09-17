package cb.dbbugreproduce;

import android.content.Context;
import android.util.Log;
import com.couchbase.lite.Blob;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.MutableDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import org.greenrobot.eventbus.EventBus;
import java.io.File;
import java.util.List;
import cb.dbbugreproduce.models.CadecDataModel;
import cb.dbbugreproduce.models.ManifestDelivery;
import cb.dbbugreproduce.models.eventbus.EventBusPostTextviewUpdate;

public class DBHelper {
    private static final String TAG = "DBHelper";
    private static final Helper h = new Helper();
    private static final Globals g = new Globals();

    private static void log(String logText){
        Log.d(TAG, logText);
    }

    public enum DatabaseLoadResult {
        EXCEPTION,
        NO_DB,
        POPULATED,
        NEW
    }

    public static DatabaseLoadResult AnalyzeAndCreate(Context context, boolean forceNewDB){
        try{
            DatabaseConfiguration config = new DatabaseConfiguration(context);
            config.setDirectory(g.GetPrimaryDirectoryPath());

            if(forceNewDB){
                log("DBHelper: Forcing fresh DB, deleting old one.");
                DeleteDatabase();
                Data.database = new Database(g.GetDatabaseNameOnly(), config);
                return DatabaseLoadResult.NEW;
            }else{

                if(DoesDBExist()){
                    log("DBHelper: Database folder exists.");
                    Data.database = new Database(g.GetDatabaseNameOnly(), config);
                    if(IsDBPopulated()){
                        //DB has content
                        log("DBHelper: Database appears to be populated.");
                        DBHelper.RestoreMemoryFromDB(Data.CadecObjModel);
                        return DatabaseLoadResult.POPULATED;
                    }else{
                        //DB Folder exists but DB has no documents.
                        log("DBHelper: DB not populated, deleting folder.");
                        DeleteDatabase();
                    }
                }
                return DatabaseLoadResult.NO_DB;
            }

        }catch (Exception e){
            log("DBHelper: Exception in AnalyzeAndCreate() unable to continue, returning false." + e.getMessage());
            return DatabaseLoadResult.EXCEPTION;
        }
    }

    public static boolean PopulateDocuments(CadecDataModel jsonModel){
        if(Data.database == null){
            log("DB Instance is NULL");
            return false;
        }

        Helper h = new Helper();
        try {
            MutableDocument outofstock = new MutableDocument("outofstock");
            String outOfStockJson = h.ObjectToJson(jsonModel.OutOfStockDelivery, new TypeReference<List<ManifestDelivery>>(){});
            Blob outOfStockJsonBlob = new Blob("application/json", outOfStockJson.getBytes(Data.UTF8_CHARSET));
            outofstock.setBlob("outofstock", outOfStockJsonBlob);

            log("DBHelper: OK, Saving 'outofstock' ");

            /***
             * Issue takes place on line 87
             */

            Data.database.save(outofstock);

            return true;

        } catch (Exception gException){
            Data.StopDebugLoop.set(true);
            EventBus.getDefault().post(new EventBusPostTextviewUpdate(gException.getMessage()));
            log("General Exception found when trying to CreateDocuments(), unable to Data.database.save(). Returning false. - " + gException.getMessage());
            return false;
        }

    }

    public static boolean RestoreMemoryFromDB(CadecDataModel cadecDataModel){

        try{
            if(cadecDataModel == null){
                log("CadecDataModel is Null, creating new one.");
                cadecDataModel = new CadecDataModel();
            }

            log("DBHelper - Restoring DB Documents to memory");
            cadecDataModel.OutOfStockDelivery  = GetOutOfStock();
            Data.CadecObjModel = cadecDataModel;
            return true;

        } catch (Exception gException){
            log("DBHelper - General Exception found when trying to RestoreMemoryFromDB(), unable to populate CadecDataModel. Returning false. " +  gException.getMessage());
            return false;
        }


    }

    private static List<ManifestDelivery> GetOutOfStock(){
        try{

            if(Data.database == null){
                log("DBHelper - Attempting to GetOutOfStock() but Data.database is NULL. Unable to proceed, returning null.");
                return null;
            }
            List<ManifestDelivery> outOfStockModel = null;

            MutableDocument outofstock = Data.database.getDocument("outofstock").toMutable();
            Blob outOfStockBlob = outofstock.getBlob("outofstock");
            if(outOfStockBlob == null){
                log("DBHelper - GetOutOfStock() outOfStockBlob is null");
                return null;
            }

            String jsonRaw =  h.decodeUTF8(outOfStockBlob.getContent());
            outOfStockModel = h.JsonToObject(jsonRaw,  new TypeReference<List<ManifestDelivery>>(){});

            log("DBHelper - GetOutOfStock() fetched.");
            return outOfStockModel;

        }catch (Exception e){
            log("DBHelper - GetOutOfStock() Exception, returning null - " + e.getMessage());
            return null;
        }

    }

    private static boolean DoesDBExist(){
        return Database.exists("ctdb", new File(g.GetPrimaryDirectoryPath()));
    }

    private static boolean IsDBPopulated(){
        if(Data.database != null){
            if(Data.database.getCount() > 0)
                return true;
        }
        return false;
    }

    private static void DeleteDatabase(){
        try{
            String pathToDB = g.GetDatabaseFullPath();
            if (Data.database != null)
                Data.database.delete();

            h.DeleteContentOfFolder(pathToDB);
            Data.database = null;

           log("DBHelper: Database Deleted Successfully.");

        }catch (Exception e){
            log("DBHelper: Exception when attempting to delete database. " + e.getMessage());
        }

    }
}
