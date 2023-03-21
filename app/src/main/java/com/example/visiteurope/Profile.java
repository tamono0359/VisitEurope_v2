package com.example.visiteurope;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.checkinternet.NetworkChangeListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Profile extends AppCompatActivity {

    private ImageView logout, back;
    private TextView email, name, change_password;

    private FirebaseUser user;
    private DatabaseReference reference;
    private String userID;
    private static final String FILE_NAME = "myFile";
    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;
    private DatabaseReference mdatabaseReference;

    ImageView profile_photo;

    private  boolean is8char=false, hasUpper=false, hasnum=false;

    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

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
        setContentView(R.layout.activity_profile);

        logout = (ImageView) findViewById(R.id.logout);
        back = (ImageView) findViewById(R.id.back);
        profile_photo = findViewById(R.id.profile_photo);
        storageReference = FirebaseStorage.getInstance().getReference();

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(Profile.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                StoredDataUsingSHaredPref(false);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Profile.this, HomeActivity.class));
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        userID = user.getUid();

        email = (TextView) findViewById(R.id.email_info);
        name = (TextView) findViewById(R.id.name_info);

        StorageReference profileRef = storageReference.child("users/"+user.getUid()+"/profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(profile_photo);
            }
        });

        reference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user_profile = snapshot.getValue(User.class);

                if (user_profile != null) {
                    String fullname = user_profile.fullName;
                    String mail = user_profile.email;

                    email.setText(mail);
                    name.setText(fullname);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Profile.this, "Something went wrong.", Toast.LENGTH_LONG).show();
            }
        });

        Button change_password = findViewById(R.id.changepass);
        change_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText changepass = new EditText(view.getContext());
                final AlertDialog.Builder changepassdialog = new AlertDialog.Builder(view.getContext());
                changepassdialog.setTitle("Change password:");
                changepassdialog.setMessage("Enter new password.");
                changepassdialog.setView(changepass);

                changepassdialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newpass = changepass.getText().toString();
                        if (newpass.isEmpty()) {
                            changepass.setError("Password is required!");
                            changepass.requestFocus();
                            return;
                        }

                        // 8 character
                        if (newpass.length() >= 8) {
                            is8char = true;
                        } else {
                            is8char = false;
                            changepass.setError("Min password length should be 6 characters!");
                            changepass.requestFocus();
                            return;
                        }
                        //number
                        if (newpass.matches("(.*[0-9].*)")) {
                            hasnum = true;
                        } else {
                            hasnum = false;
                            changepass.setError("Password must contain at least one number!");
                            changepass.requestFocus();
                            return;
                        }
                        //upper case
                        if (newpass.matches("(.*[A-Z].*)")) {
                            hasUpper = true;
                        } else {
                            hasUpper = false;
                            changepass.setError("Password must contain at least one capital letter!");
                            changepass.requestFocus();
                            return;
                        }
                        user.updatePassword(newpass).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(Profile.this, "Password has been changed succesfully.", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(Profile.this, "Password has not been changed.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                changepassdialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                changepassdialog.create().show();
            }
        });

        final TextView[] sight = new TextView[50];

        //show user's wishlist
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(userID).child("wishlist").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                DataSnapshot ds = task.getResult();
                LinearLayout ll_profile = (LinearLayout) findViewById(R.id.ll_profile);
                int l = -1;
                for (DataSnapshot datas : ds.getChildren()) {
                    l++;
                    sight[l] = new TextView(getApplicationContext());
                    sight[l].setText(datas.getKey());
                    ll_profile.addView(sight[l]);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 10, 0, 15);
                    sight[l].setGravity(Gravity.CENTER);
                    sight[l].setTextSize(17);
                    sight[l].setTextColor(getResources().getColor(R.color.white));
                    sight[l].setLayoutParams(params);
                    int finalL = l;
                    sight[l].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(Profile.this);
                            builder.setTitle("Edit " + sight[finalL].getText());

                            //deleting place from wishlist
                            builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ll_profile.removeView(sight[finalL]);
                                    String actual_title = (String) sight[finalL].getText();
                                    DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("Users").child(userID);
                                    commentsRef.child("wishlist").addListenerForSingleValueEvent(new ValueEventListener() {

                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            for (DataSnapshot snap : dataSnapshot.getChildren()) {
                                                String id = snap.getKey();

                                                if (id.equals(actual_title)) {
                                                    DatabaseReference removeRef = dataSnapshot.getRef();
                                                    removeRef.child(id).removeValue();
                                                }
                                                Toast.makeText(Profile.this, "Place deleted successfully.", Toast.LENGTH_SHORT).show();

                                            }

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            });
                            builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            builder.show();
                        }
                    });
                }
            }
        });

        profile_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGalleryIntent, 1000);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Profile.this, MainActivity.class);
        startActivity(intent);
    }

    private void StoredDataUsingSHaredPref(boolean ischecked) {
        SharedPreferences.Editor editor = getSharedPreferences(FILE_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("ischecked", ischecked);
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = data.getData();
                profile_photo.setImageURI(imageUri);

                uploadImageToFirebase(imageUri);

            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        StorageReference fileRef = storageReference.child("users/" + user.getUid() + "/profile.jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(Profile.this, "Image uploaded", Toast.LENGTH_LONG).show();
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).into(profile_photo);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Profile.this, "Failed", Toast.LENGTH_LONG).show();
            }
        });
    }
    
}
