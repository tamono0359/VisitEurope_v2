package com.example.visiteurope;

import static com.example.visiteurope.Login.wasLoggingIn;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.checkinternet.NetworkChangeListener;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.UUID;

public class PlacesListActivity extends AppCompatActivity  {

    private static final String FILE_NAME = "myFile";
    private FirebaseUser fus;
    private boolean ischecked;
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
    private String filePath;
    Uri selectedImageUri;


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

    private void StoredDataUsingSHaredPref(boolean ischecked) {
        SharedPreferences.Editor editor = getSharedPreferences(FILE_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("ischecked", ischecked);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_list);

        final TextView[] sight = new TextView[10];

        Intent i = getIntent();

        String path = i.getStringExtra("path");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child(path).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                DataSnapshot ds = task.getResult();
                LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
                int l = -1;
                for(DataSnapshot datas : ds.getChildren()) {
                    l++;
                    sight[l] = new TextView(getApplicationContext());
                    sight[l].setText(datas.getKey());
                    ll.addView(sight[l]);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    params.setMargins(0, 50, 0, 50);
                    ll.setPadding(80, 40, 80, 40);
                    sight[l].setGravity(Gravity.CENTER);
                    sight[l].setHeight(150);
                    sight[l].setTextSize(20);
                    sight[l].setTextColor(getResources().getColor(R.color.white));
                    sight[l].setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    sight[l].setBackgroundResource(R.drawable.shape_general);
                    sight[l].setLayoutParams(params);
                    sight[l].setPadding(50, 10, 50, 10);
                    int finalL = l;
                    sight[l].setOnClickListener(view -> {
                        Intent intent = new Intent(PlacesListActivity.this, Sight.class);
                        intent.putExtra("path", path + "/" + sight[finalL].getText());
                        startActivity(intent);
                    });
                }
            }
        });


        ImageView addIcon = (ImageView) findViewById(R.id.addIcon);

        //User_signedIn
        fus = FirebaseAuth.getInstance().getCurrentUser();

        SharedPreferences sharedPreferences = getSharedPreferences(FILE_NAME, MODE_PRIVATE);
        ischecked = sharedPreferences.getBoolean("ischecked", false);

        addIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fus != null || wasLoggingIn || ischecked) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlacesListActivity.this);
                    builder.setTitle("Add place:");

                    LinearLayout layout = new LinearLayout(PlacesListActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(70, 30, 70, 30);

                    final EditText place_title = new EditText(PlacesListActivity.this);
                    place_title.setHint("Title");
                    layout.addView(place_title);

                    final EditText place_location = new EditText(PlacesListActivity.this);
                    place_location.setHint("Location");
                    layout.addView(place_location);

                    final EditText place_information = new EditText(PlacesListActivity.this);
                    place_information.setHint("Information");
                    layout.addView(place_information);

                    final EditText place_history = new EditText(PlacesListActivity.this);
                    place_history.setHint("History");
                    layout.addView(place_history);

                    final ImageView clip = new ImageView(PlacesListActivity.this);
                    clip.setImageResource(R.drawable.clip);
                    //clip.setMinimumWidth(30);
                    //clip.setMinimumHeight(30);
                    layout.addView(clip);

                    clip.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                                ActivityCompat.requestPermissions(PlacesListActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        REQUEST_CODE_STORAGE_PERMISSION);
                            }else{
                                selectImage();
                            }
                        }
                    });

                    builder.setView(layout);

                    builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String title, information, location, history;
                            title = place_title.getText().toString();
                            location = place_location.getText().toString();
                            information = place_information.getText().toString();
                            history = place_history.getText().toString();

                            if (title.isEmpty() && location.isEmpty() && information.isEmpty() && history.isEmpty()){
                                Toast.makeText(PlacesListActivity.this, "You need to fill informations.", Toast.LENGTH_LONG).show();
                            }else{
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                                reference.child(path).child(title).child("location").setValue(location);

                                reference.child(path).child(title).child("info").setValue(information);

                                reference.child(path).child(title).child("history").setValue(history);

                                Toast.makeText(PlacesListActivity.this, "Saved sucessfully.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // what ever you want to do with No option.
                        }
                    });
                    builder.show();
                } else {
                    Toast.makeText(PlacesListActivity.this, "You need to be logged in if you want to add place.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void selectImage() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        File selectedImageFile;

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && requestCode == RESULT_OK){
            if (data != null){
                selectedImageUri = data.getData();
                if (selectedImageUri != null){
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        selectedImageFile = new File(getPathFromUri(selectedImageUri));


                        /*// Create a Cloud Storage reference from the app
                        FirebaseStorage storage = FirebaseStorage.getInstance("gs://visiteurope-8fccf.appspot.com");
                        StorageReference storageRef = storage.getReference();

                        // Create a reference to "mountains.jpg"
                        StorageReference mountainsRef = storageRef.child("skuska");

                        storageRef.child("skuska").putFile(Uri.fromFile(selectedImageFile));*/

                    }catch (Exception exception){
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri){

        String filePath;
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null){
            filePath = contentUri.getPath();
        }else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }
}