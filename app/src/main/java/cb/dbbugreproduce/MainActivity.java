package cb.dbbugreproduce;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import cb.dbbugreproduce.models.CadecDataModel;
import cb.dbbugreproduce.models.eventbus.EventBusPostTextviewUpdate;
import cb.dbbugreproduce.models.eventbus.EventBusStartTestAsync;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";
    private Activity activity;
    private Helper h;
    private Globals g;

    private TextView txtDebugText;
    private EditText txtIterations;
    private Button btnBeginTest;

    private static final AtomicInteger totalIterations = new AtomicInteger(0);

    private boolean PermissionsAccepted = false;
    private final int REQUEST_PERMISSIONS_CODE = 1;
    private String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.h = new Helper();
        this.g = new Globals();
        this.activity = this;

        final int permission  = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            PermissionsAccepted = false;
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_PERMISSIONS_CODE);
        }else{
            //Start application process
            PermissionsAccepted = true;
            onResume();
            beginStartProcedure();

        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int totalGrantedCount = 0;
        for(int x=0; x < grantResults.length; x++){
            if (grantResults[x] == PackageManager.PERMISSION_GRANTED){
                totalGrantedCount++;
            }
        }

        if(totalGrantedCount == grantResults.length){
            // We don't have permission so prompt the user
            PermissionsAccepted = true;
            //Start application process
            beginStartProcedure();
            onResume();
        }else{
            boolean somePermissionsForeverDenied = false;
            for(String permission: permissions){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                    //denied
                    Log.d(TAG, "onRequestPermissionsResult: PERMISSIONS: DENIED " + permission);
                }else{
                    if(ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
                        //allowed
                        Log.d(TAG, "onRequestPermissionsResult: PERMISSIONS: ALLOWED "+ permission);
                    } else{
                        //set to never ask again
                        Log.d(TAG, "onRequestPermissionsResult: PERMISSIONS: SET TO NEVER ASKED AGAIN - "+ permission);
                        somePermissionsForeverDenied = true;
                    }
                }
            }
            if(somePermissionsForeverDenied){
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Permissions Required")
                        .setMessage("You have forcefully denied some of the required permissions " +
                                "for this action. Please open settings, go to permissions and allow them manually.")
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }else{
                new AlertDialog.Builder(this)
                        .setTitle("PERMISSIONS DENIED")
                        .setMessage("Unable to proceed, this application requires that permissions be accepted to operate correctly. \r\n\r\n YOU MUST APPROVE ALL PERMISSIONS BEFORE CONTINUING.")
                        .setPositiveButton("TAP HERE TO CLOSE AND TRY AGAIN.", new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which){
                                activity.finish();
                                System.exit(0);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .show();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void beginStartProcedure(){
        txtDebugText  = (TextView)findViewById(R.id.mainactivity_debugtext);
        txtIterations = (EditText)findViewById(R.id.mainactivity_txtiterations);
        btnBeginTest  = (Button)findViewById(R.id.mainactivity_btnbegintest);
        btnBeginTest.setOnClickListener(this);

        if(!h.DoesPrimaryFolderExist()){
            h.CreateDirectory(g.GetPrimaryDirectoryPath());
        }

        switch (DBHelper.AnalyzeAndCreate(getApplicationContext(), false)){
            case EXCEPTION:
               appendText("**DBHelper.AnalyzeAndCreate() returned Exception");
            break;

            case POPULATED:
                appendText("Loaded previous data from DB successfully.");
                break;

            case NO_DB:
                if (Data.database == null){
                    DBHelper.AnalyzeAndCreate(getApplicationContext(), true);
                    appendText("Created new DB");
                    Data.CadecObjModel = new CadecDataModel();
                    Data.CadecObjModel.OutOfStockDelivery = new ArrayList<>();

                    DBHelper.PopulateDocuments(Data.CadecObjModel);
                }else{
                    appendText("**Cant find a DB instance..");
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void handleEventBusTestRun(final EventBusStartTestAsync empty){
        for(int x = 0; x <= totalIterations.get(); x++) {
            if(Data.StopDebugLoop.get()){
                break;
            }

            if (DBHelper.PopulateDocuments(Data.CadecObjModel)) {
                EventBus.getDefault().post(new EventBusPostTextviewUpdate("(" + x + ")" + " Saving - OK."));
            } else {
                if(!Data.StopDebugLoop.get())
                    EventBus.getDefault().post(new EventBusPostTextviewUpdate("(" + x + ")" + " Saving - FAIL."));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void handleEventBusUpdateTextView(final EventBusPostTextviewUpdate model){
        appendText(model.GetText());
    }

    public void appendText(String text){
        try {
            txtDebugText.setText("");
            Thread.sleep((long) 0.4);
            txtDebugText.setText(txtDebugText.getText().toString() + "\n" + text);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.mainactivity_btnbegintest) {
            Data.StopDebugLoop.set(false);
            totalIterations.set(Integer.parseInt(txtIterations.getText().toString()));
            EventBus.getDefault().post(new EventBusStartTestAsync());
        }
    }
}
