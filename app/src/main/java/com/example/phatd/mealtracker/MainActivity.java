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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.R.attr.button;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1234567;
    private int numTap = 1;
    private int thumbnailCounter = 0;
    private ImageView leftFoodThumbnail;
    private ImageView centerFoodThumbnail;
    private ImageView rightFoodThumbnail;
    private ImageView[] thumbnailsArray;
    private File foodThumbnailsDir;
    private File[] directoryListing;
    private int lastThumbnailPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set each thumbnail to its corresponding variable
        leftFoodThumbnail = (ImageView) findViewById(R.id.leftThumbnail);
        centerFoodThumbnail = (ImageView) findViewById(R.id.centerThumbnail);
        rightFoodThumbnail = (ImageView) findViewById(R.id.rightThumbnail);

        // Put all thumbnails into an array
        thumbnailsArray = new ImageView[]
                {leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail};

        // Get directory path of existing thumbnails
        foodThumbnailsDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        directoryListing = foodThumbnailsDir.listFiles();

        // Put all thumbnails into thumbnail holder
        putThumbnailIntoHolder(thumbnailsArray, directoryListing);

        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                putThumbnailIntoHolder(thumbnailsArray, directoryListing);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.takePhotos);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (numTap == 2) {
                    if (checkSelfPermission(Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED){
                        invokeTakePictureIntent();
                    } else {
                        String[] permissionRequest ={Manifest.permission.CAMERA};
                        requestPermissions(permissionRequest, CAMERA_PERMISSION_REQUEST_CODE);
                    }
                    numTap = 1;
                } else {
                    Toast.makeText(MainActivity.this, "Tap again to take photo of a meal",
                            Toast.LENGTH_SHORT).show();
                    numTap = 2;
                }
            }
        });
    }

    private void putThumbnailIntoHolder(ImageView[] thumbnailsArray, File[] directoryListing) {
        if (directoryListing != null) {
            if (directoryListing.length > 3) {
                lastThumbnailPosition = directoryListing.length - 3;
            }
            for (int i = directoryListing.length - 1; i >= lastThumbnailPosition; i--) {
                Glide.with(this).load(directoryListing[i]).into(thumbnailsArray[thumbnailCounter]);
                thumbnailCounter++;
            }
        } else {
            leftFoodThumbnail.setImageResource(R.drawable.me_swimming);
            rightFoodThumbnail.setImageResource(R.drawable.me_swimming);
            centerFoodThumbnail.setImageResource(R.drawable.me_swimming);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                invokeTakePictureIntent();
            } else {
                Toast.makeText(this, "Can't take photo without permission", Toast.LENGTH_LONG).show();
            }
        }
    }

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

        //noinspection SimplifiableIfStatement
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

    //Request to use camera app for taking photos
    static final int REQUEST_IMAGE_CAPTURE = 1;

    //Save full-sized photos on the phone
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException, IOException {
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
}
