package com.example.visiteurope;

import static com.example.visiteurope.Login.wasLoggingIn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.checkinternet.NetworkChangeListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;


public class HomeActivity extends AppCompatActivity {

    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    DatabaseReference reference;
    Button state, city;
    String[] listItemsCity;
    Intent intent;

    private static final String FILE_NAME = "myFile";
    private FirebaseUser fus;
    private boolean ischecked;


    //private String userID;*/

    public void fillCity(){
        reference = FirebaseDatabase.getInstance().getReference();
        reference.child(state.getText().toString()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                DataSnapshot ds = task.getResult();
                listItemsCity = new String[]{"","","","",""};
                int i = 0;
                for(DataSnapshot datas : ds.getChildren()) {
                    listItemsCity[i] = datas.getKey();
                    i++;
                }
            }
        });
    }

    private void StoredDataUsingSHaredPref(boolean ischecked) {
        SharedPreferences.Editor editor = getSharedPreferences(FILE_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("ischecked", ischecked);
        editor.apply();
    }

    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        state = (Button) findViewById(R.id.state);
        city = (Button) findViewById(R.id.city);


        ImageView user_icon = (ImageView) findViewById(R.id.user_icon);
        ImageView user = (ImageView) findViewById(R.id.user);
        Button ok = (Button) findViewById(R.id.ok);


        ok.setOnClickListener(view -> {
            if (state.getText().equals("Choose state")){
                Toast.makeText(this, "You must choose state.", Toast.LENGTH_SHORT).show();
            }else if (city.getText().equals("Choose city")){
                Toast.makeText(this, "You must choose city.", Toast.LENGTH_SHORT).show();
            }else{
                intent = new Intent(HomeActivity.this, PlacesListActivity.class);
                intent.putExtra("path", state.getText()+"/"+city.getText());
                startActivity(intent);
            }
        });

        //User_signedIn
        fus = FirebaseAuth.getInstance().getCurrentUser();

        SharedPreferences sharedPreferences = getSharedPreferences(FILE_NAME, MODE_PRIVATE);
        ischecked = sharedPreferences.getBoolean("ischecked", false);

        if ((fus != null && wasLoggingIn) || ischecked) {
            user_icon.setVisibility(View.INVISIBLE);
            user.setVisibility(View.VISIBLE);
            user.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, Profile.class);
                    startActivity(intent);
                }
            });

        } else {
            user.setVisibility(View.INVISIBLE);
            user_icon.setVisibility(View.VISIBLE);
            user_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, Login.class);
                    startActivity(intent);
                }
            });
        }

        /*user_icon.setOnClickListener(view -> {
            if (fus != null && ischecked && wasLoggingIn) {
                Intent intent = new Intent(HomeActivity.this, Profile.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(HomeActivity.this, Login.class);
                startActivity(intent);
            }
        });*/

        state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*//create list of items
                reference = FirebaseDatabase.getInstance().getReference("Belgium/Antwerp");
                reference.child("Grand Place").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        DataSnapshot ds = task.getResult();
                        Toast.makeText(getApplicationContext(),String.valueOf(ds.child("history").getValue()),Toast.LENGTH_SHORT).show();
                    }
                });*/
                String[] listItems = new String[] {"Belgium", "Netherlands", "Norway", "Spain", "United Kingdom (UK)"};
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Choose state:");
                builder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        state.setText(listItems[i]);
                        dialogInterface.dismiss();
                        fillCity();
                    }
                });
                builder.setNegativeButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        state.setText("Choose state");
                    }
                });
                builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                //show AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        city.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //create list of items
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Choose city:");
                builder.setSingleChoiceItems(listItemsCity, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        city.setText(listItemsCity[i]);
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        city.setText("Choose city");
                    }
                });
                builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                //show AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}
