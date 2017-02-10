package com.example.phatd.mealtracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1234567;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 1;

    private ImageView leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail;
    private ImageView[] thumbnailsArray;

    private File foodThumbnailsDir;
    private File[] directoryListing;
    private int mostRecentThumbnailIndex, thumbnailCounter, numTaps_takePhotos;

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        numTaps_takePhotos = 1; // Initialize numTaps_takePhotos

        // Sync button setup
        Button syncButton = (Button) findViewById(R.id.sync);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: add user auth verification
                if (false) {
                    syncPhotos();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("I'm working on this feature. Thanks for your interest")
                            .setTitle("Thank you!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });

        // Delete-all-photos button setup
        Button deletePhotos = (Button) findViewById(R.id.delete_all_photos);
        deletePhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //region Create dialog to confirm deletion
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Delete all photos");
                builder.setMessage("Are you sure you want to delete all meal photos?");
                builder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteAllPhotos();
                                thumbnailSetup();
                                sortMealPhotos();
                                updateMealThumbnails();
                            }
                        });
                builder.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                //endregion
            }
        });

        // Take-photo button setup
        FloatingActionButton takePhotosButton =
                (FloatingActionButton) findViewById(R.id.takePhotos);
        takePhotosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkNumTapsToOpenCamera();
            }
        });

        // Get-quiz button setup
        FloatingActionButton getQuizButton = (FloatingActionButton) findViewById(R.id.getQuiz);
        getQuizButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToQuiz();
            }
        });

        // Get all images in folder and put it into ImageView holder
        thumbnailSetup();
        sortMealPhotos();
        updateMealThumbnails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sortMealPhotos();
        updateMealThumbnails();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        sortMealPhotos();
        updateMealThumbnails();
    }

    //region Functionalities for all buttons
    // Redirect to QuizActivity
    private void goToQuiz() {
        if (directoryListing.length >= 3) {
            Intent goToQuizActivity = new Intent(this, QuizActivity.class);
            startActivity(goToQuizActivity);
        } else {
            Toast.makeText(this, "Not enough meal photos to generate questions",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Upload missing photos (if any) to firebase. Otherwise, pop up a toast
    private void syncPhotos() {
        if (foodThumbnailsDir.listFiles().length != 0) {
            for (File f : foodThumbnailsDir.listFiles()) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.phatd.mealtracker.fileprovider",
                        f);
                uploadPhotoToFirebase(f, photoURI);
            }
        } else {
            Toast.makeText(this, "You don't have any meal photos", Toast.LENGTH_LONG).show();
        }
    }

    // Delete all photos in the directory
    private void deleteAllPhotos() {
        for (File f : foodThumbnailsDir.listFiles())
            f.delete();
    }

    // Tap twice to take photo of meal
    private void checkNumTapsToOpenCamera() {
        if (numTaps_takePhotos == 2) {
            handleCameraPermission();
            thumbnailSetup();
            sortMealPhotos();
            updateMealThumbnails();
        } else {
            Toast.makeText(MainActivity.this, "Tap again to take photo of a meal",
                    Toast.LENGTH_SHORT).show();
            numTaps_takePhotos = 2;
        }
    }
    //endregion

    //region Display meals thumbnails
    // Set up display thumbnail
    private void thumbnailSetup() {
        // Set each thumbnail to its corresponding variable
        leftFoodThumbnail = (ImageView) findViewById(R.id.leftThumbnail);
        centerFoodThumbnail = (ImageView) findViewById(R.id.centerThumbnail);
        rightFoodThumbnail = (ImageView) findViewById(R.id.rightThumbnail);

        // Put all thumbnails into an array
        thumbnailsArray = new ImageView[]
                {leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail};
    }

    // Find the directory that stores photos, then
    // proceed to sort all photos (if they exist)
    private void sortMealPhotos() {
        // Get directory path & generate a File[] that contains all meal thumbnails
        foodThumbnailsDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        directoryListing = foodThumbnailsDir.listFiles();
        Arrays.sort(directoryListing, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
    }

    // Load all photos into thumbnail holders
    // If there are no photos, display nothing
    private void updateMealThumbnails() {
        if (directoryListing.length != 0) {
            thumbnailCounter = 0;
            mostRecentThumbnailIndex = directoryListing.length - 1;

            // Start from left to right, add the most recent thumbnail
            // first to the third most recent thumbnail last (if exists)
            // and display them in ImageView
            while (thumbnailCounter <= 2 && directoryListing[mostRecentThumbnailIndex].exists()) {
                // The file doesn't contain any photos (hence size 0), delete it
                if (directoryListing[mostRecentThumbnailIndex].length() != 0) {
                    Glide.with(this).
                            load(directoryListing[mostRecentThumbnailIndex]).
                            into(thumbnailsArray[thumbnailCounter]);
                } else {
                    // After the file gets deleted, go to the next most recent file
                    // without incrementing the thumbnailCounter
                    directoryListing[mostRecentThumbnailIndex].delete();
                    mostRecentThumbnailIndex--;
                    continue;
                }
                mostRecentThumbnailIndex--;
                thumbnailCounter++;
            }
        } else {
            // If there's nothing in the thumbnail folder,
            // set all ImageView holders to transparent
            for (ImageView thumbnail : thumbnailsArray) {
                thumbnail.setImageResource(android.R.color.transparent);
            }
        }
    }
    //endregion

    //region Capture photos
    // Create image file for meal
    private File createImageFile() throws IOException {
        // Create an image file name that is collision-resistant
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String imageFileName = "Meal_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,   //prefix
                ".jpg",          //suffix
                storageDir       //directory
        );
        return image;
    }

    // Capture image of meal
    private void invokeTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.phatd.mealtracker.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
            }
        }
    }

    private void uploadPhotoToFirebase(File photoFile, Uri photoURI) {
        StorageReference mealRef = mStorageRef.child("meals/" + photoFile.getName());

        mealRef.putFile(photoURI)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Log.i("download url", downloadUrl.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle unsuccessful uploads
                        Toast.makeText(getApplicationContext(),
                                e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleCameraPermission() {
        // If app has permission to use camera, take pictures of meals
        if (checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            invokeTakePictureIntent();
        }

        // If app doesn't have permission, ask for camera permission from user
        else {
            String[] permissionRequest = {Manifest.permission.CAMERA};
            requestPermissions(permissionRequest, CAMERA_PERMISSION_REQUEST_CODE);
        }

        // Reset numTaps_takePhotos to 1
        numTaps_takePhotos = 1;
    }
    //endregion

    // region Set up menu, permissions, etc.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Contact me option
        if (id == R.id.contact_me) {
            String[] addresses = {"phatdtran3k6@gmail.com"};
            Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
            sendEmail.setData(Uri.parse("mailto:"));
            sendEmail.putExtra(Intent.EXTRA_EMAIL, addresses);
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, "SUGGESTION");
            sendEmail.putExtra(Intent.EXTRA_TEXT, "Hi Phat, you're cute.");
            if (sendEmail.resolveActivity(getPackageManager()) != null) {
                startActivity(sendEmail);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                invokeTakePictureIntent();
            } else {
                Toast.makeText(this, "Can't take photo without permission",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}