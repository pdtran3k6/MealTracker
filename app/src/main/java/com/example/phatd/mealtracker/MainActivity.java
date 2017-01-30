package com.example.phatd.mealtracker;

import android.Manifest;
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

    private ImageView leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail;
    private ImageView[] thumbnailsArray;

    private File foodThumbnailsDir;
    private File[] directoryListing;
    private int thirdMostRecentThumbnailIndex, mostRecentThumbnailIndex,
            thumbnailCounter, numTaps_clearMem, numTaps_takePhotos;

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        numTaps_takePhotos = 1; // Initialize numTaps_takePhotos
        numTaps_clearMem = 1; // Initialize numTaps_clearMem

        // Refresh button setup
        Button syncButton = (Button) findViewById(R.id.sync);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncPhotos();
            }
        });

        // Clear-memory button setup
        Button clearMemory = (Button) findViewById(R.id.clear_memory);
        clearMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearMemory();
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
        updateMealThumbnails();
    }

    //region Functionalities for all buttons
    // Redirect to QuizActivity
    private void goToQuiz() {
        if (directoryListing.length != 0) {
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
    private void clearMemory() {
        if (numTaps_clearMem == 2) {
            for (File f : foodThumbnailsDir.listFiles())
                f.delete();
            numTaps_clearMem = 1;
            updateMealThumbnails();
        } else {
            Toast.makeText(MainActivity.this, "Tap again to clear memory",
                    Toast.LENGTH_SHORT).show();
            numTaps_clearMem = 2;
            // TODO: Add delay for resetting the numTaps_clearMem
        }
    }

    // Tap twice to take photo of meal
    private void checkNumTapsToOpenCamera() {
        if (numTaps_takePhotos == 2) {
            handleCameraPermission();
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

    // Set up indexes for the 3 most recent meal photos
    // If there are less than 3 photos, use all of them
    private void thumbnailIndexSetup() {
        thumbnailCounter = 0;
        mostRecentThumbnailIndex = directoryListing.length - 1;
        if (directoryListing.length > 3) {
            thirdMostRecentThumbnailIndex = directoryListing.length - 3;
        } else {
            thirdMostRecentThumbnailIndex = 0;
        }
    }

    // Load all photos into thumbnail holders
    // If there are no photos, display nothing
    private void updateMealThumbnails() {
        thumbnailSetup();
        sortMealPhotos();

        if (directoryListing.length != 0) {
            thumbnailIndexSetup();

            // Start from left to right, add the most recent thumbnail
            // first to the third most recent thumbnail last (if exists)
            // and display them in ImageView
            for (int i = mostRecentThumbnailIndex; i >= thirdMostRecentThumbnailIndex; i--) {
                Glide.with(this).
                        load(directoryListing[i]).
                        into(thumbnailsArray[thumbnailCounter]);
                thumbnailCounter++;
            }
        }

        // If there's nothing in the thumbnail folder, set all ImageView holders to transparent
        else {
            for (ImageView thumbnail : thumbnailsArray) {
                thumbnail.setImageResource(android.R.color.transparent);
            }
        }
    }
    //endregion

    //region Capture photos
    //Save full-sized photos on the phone
    String mCurrentPhotoPath;

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

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
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
                startActivityForResult(takePictureIntent, RESULT_OK);
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
    //endregion
}