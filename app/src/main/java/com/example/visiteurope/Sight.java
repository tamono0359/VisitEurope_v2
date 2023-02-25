package com.example.visiteurope;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.checkinternet.NetworkChangeListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


public class Sight extends AppCompatActivity {

    private DatabaseReference mdatabaseReference;
    private TextView location, info, history;
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();
    ImageView mic, sight;
    TextToSpeech textToSpeech;
    Button imageUpload, imageSelect;
    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;

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
        setContentView(R.layout.activity_sight);

        Intent i = getIntent();

        String path = i.getStringExtra("path");

        sight = findViewById(R.id.sight);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        mdatabaseReference = firebaseDatabase.getReference();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        DatabaseReference getImage = mdatabaseReference.child(path).child("image");
        getImage.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String link = dataSnapshot.getValue(String.class);

                        if (link == null){
                            imageUpload = findViewById(R.id.uploadImage);
                            imageSelect = findViewById(R.id.selectImage);
                            imageUpload.setVisibility(View.VISIBLE);
                            imageSelect.setVisibility(View.VISIBLE);

                            imageSelect.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    selectImage();
                                }
                            });

                            imageUpload.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    uploadImage();
                                }
                            });

                        }else {
                            Picasso.get().load(link).into(sight);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(Sight.this, "Error loading Image", Toast.LENGTH_SHORT).show();
                    }
                });


        mdatabaseReference = FirebaseDatabase.getInstance().getReference().child(path);
        location = findViewById(R.id.location);
        info = findViewById(R.id.info);
        history = findViewById(R.id.history);
        mdatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                location.setText(snapshot.child("location").getValue(String.class));
                info.setText(snapshot.child("info").getValue(String.class));
                history.setText(snapshot.child("history").getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        CheckBox heart = findViewById(R.id.star);

        FirebaseUser fus = FirebaseAuth.getInstance().getCurrentUser();
        String userID = null;

        if (fus != null){
            userID = fus.getUid();
            heart.setVisibility(View.VISIBLE);
        }else
            heart.setVisibility(View.INVISIBLE);

        String finalUserID = userID;
        heart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                Intent i = getIntent();

                String path = i.getStringExtra("path");

                String[] full_path = path.split("/");
                String wishlist_item = full_path[2];

                Map<String, Object> wishlist_items = new HashMap<>();
                wishlist_items.put(wishlist_item, "");



                    mdatabaseReference = FirebaseDatabase.getInstance().getReference("Users");
                    mdatabaseReference.child(finalUserID).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            mdatabaseReference.child(finalUserID).child("wishlist").updateChildren(wishlist_items);
                            Toast.makeText(Sight.this, "Place saved successfully.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            }
        });

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        mic = findViewById(R.id.mic);
        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = info.getText().toString() + history.getText().toString();
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }

    // Select Image method
    private void selectImage() {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    // Override onActivityResult method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Get the Uri of data
            filePath = data.getData();
            try {
                // Setting image on image view using Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                sight.setImageBitmap(bitmap);
            }

            catch (IOException e) {
                // Log the exception
                e.printStackTrace();
            }
        }
    }

    // UploadImage method
    private void uploadImage()
    {
        if (filePath != null) {

            // Code for showing progressDialog while uploading
            ProgressDialog progressDialog
                    = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            // Defining the child of storageReference
            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
            // adding listeners on upload
            // or failure of image
            ref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    // Image uploaded successfully
                    // Dismiss dialog
                    progressDialog.dismiss();
                    Toast.makeText(Sight.this, "Image Uploaded!!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Error, Image not uploaded
                    progressDialog.dismiss();
                    Toast.makeText(Sight.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                // Progress Listener for loading
                // percentage on the dialog box
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploaded " + (int)progress + "%");
                }
            });
        }
    }
}