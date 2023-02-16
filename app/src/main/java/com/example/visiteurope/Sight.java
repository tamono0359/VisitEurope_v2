package com.example.visiteurope;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.checkinternet.NetworkChangeListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class Sight extends AppCompatActivity {

    private DatabaseReference mdatabaseReference;
    private TextView location, info, history;
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();
    ImageView mic;
    TextToSpeech textToSpeech;


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

        ImageView sight = findViewById(R.id.sight);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        mdatabaseReference = firebaseDatabase.getReference();

        DatabaseReference getImage = mdatabaseReference.child(path).child("image");
        getImage.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String link = dataSnapshot.getValue(String.class);
                        Picasso.get().load(link).into(sight);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(Sight.this, "Error Loading Image", Toast.LENGTH_SHORT).show();
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
}