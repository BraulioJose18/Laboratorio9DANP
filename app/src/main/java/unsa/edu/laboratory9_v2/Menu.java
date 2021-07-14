package unsa.edu.laboratory9_v2;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Menu extends AppCompatActivity {

    Button cameraDos, intentCamera;
    private ActivityResultLauncher <String> activityResultLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Button cameraDos = (Button) findViewById(R.id.cameraDos);
        Button intentCamera = (Button) findViewById(R.id.intentImplicito);
        this.activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if (result) {
                            cameraDos.setEnabled(true);
                            intentCamera.setEnabled(true);
                        } else {
                            Toast.makeText(Menu.this.getApplicationContext(),"Es necesario dar permiso de acceso a la camara.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        this.activityResultLauncher.launch(Manifest.permission.CAMERA);

        cameraDos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Menu.this, ActivityMain.class));
            }
        });
        intentCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Menu.this, IntentCamera.class));
            }
        });
    }

}