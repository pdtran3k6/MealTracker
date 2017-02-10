package com.example.phatd.mealtracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class QuizActivity extends AppCompatActivity {

    private ImageView[] thumbnailsArray;
    private ImageView leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail;
    private TextView question_DayAndTime;

    private File[] directoryListing;
    private File foodThumbnailsDir;
    private File correctPhoto;
    private int correctIndex;
    private boolean answered;

    private int green;
    private int red;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_quiz);

        answered = false;

        // I-don't-know button setup
        Button iDontKnowButton = (Button) findViewById(R.id.idontknow);
        iDontKnowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(-1);
                updatePoints();
            }
        });

        // question_DayAndTime TextView setup
        question_DayAndTime = (TextView) findViewById(R.id.question_DayAndTime);

        // Populate question_DayAndTime
        generateQuestions();

        // Populate all ImageView food thumbnails
        generateThumbnails();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        answered = false;
    }

    private void revealCorrectAnswer(int chosenIndex) {
        int displayPos = correctIndex + 1;
        red = Color.argb(70, 255, 61, 61);
        green = Color.argb(70, 26, 255, 0);

        if (!answered) {
            if (chosenIndex == -1) {
                Toast.makeText(this, "This is the correct answer. " +
                                "Be more aware of what you eat next time",
                        Toast.LENGTH_SHORT).show();
                thumbnailsArray[correctIndex].setColorFilter(green);
            } else if (chosenIndex == correctIndex) {
                Toast.makeText(this, "Congratulations. It's the correct answer",
                        Toast.LENGTH_SHORT).show();
                thumbnailsArray[chosenIndex].setColorFilter(green);
            } else {
                Toast.makeText(this, "The correct answer was photo number " + displayPos,
                        Toast.LENGTH_SHORT).show();
                thumbnailsArray[chosenIndex].setColorFilter(red);
                thumbnailsArray[correctIndex].setColorFilter(green);
            }
            answered = true;
            Snackbar.make(findViewById(android.R.id.content), "Points updated",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("BACK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent goToMainActivity = new Intent(getApplicationContext(),
                                    MainActivity.class);
                            startActivity(goToMainActivity);
                        }
                    }).show();
        }
    }

    private void updatePoints() {

    }

    private void generateQuestions() {
        // Get directory path & generate a File[] that contains all meal thumbnails
        foodThumbnailsDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        directoryListing = foodThumbnailsDir.listFiles();

        correctIndex = ThreadLocalRandom.current().nextInt(0, directoryListing.length);
        correctPhoto = directoryListing[correctIndex];

        long millisec = correctPhoto.lastModified();
        Date mealCapturedTime = new Date(millisec);
        String dayName = new SimpleDateFormat("EEE").format(mealCapturedTime);
        String date = new SimpleDateFormat("dd/MM").format(mealCapturedTime);
        String hourMin = new SimpleDateFormat("hh:mm aaa").format(mealCapturedTime);

        question_DayAndTime.setText("on " + dayName + " " + date + " \nat " + hourMin + "?");
    }

    // Set up display thumbnail
    private void generateThumbnails() {
        setupThumbnails();

        int totalNumPhotos = directoryListing.length;
        Integer[] randomPhotoIndexes = new Integer[totalNumPhotos];

        // Populate all meal photos' indexes to the array
        for (int i = 0; i < totalNumPhotos; i++) {
            randomPhotoIndexes[i] = i;
        }

        // Randomize all elements
        Collections.shuffle(Arrays.asList(randomPhotoIndexes));

        for (int k = 0; k < 3; k++) {
            // Loop through the first 3 values of randomPhotoIndexes
            // If the none of them is the correctIndex,
            // put correctIndex in any of the first 3 values
            if (randomPhotoIndexes[k] == correctIndex) {
                correctIndex = k;
                break;
            } else if (k == 2) {
                int pos = ThreadLocalRandom.current().nextInt(0, 3);
                randomPhotoIndexes[pos] = correctIndex;
                correctIndex = pos;
            }
        }

        // Populate thumbnailsArray with random meal photos
        for (int i = 0; i < 3; i++) {
            Glide.with(this).
                    load(directoryListing[randomPhotoIndexes[i]]).
                    into(thumbnailsArray[i]);
        }
    }

    private void setupThumbnails() {
        // Set each thumbnail to its corresponding variable
        leftFoodThumbnail = (ImageView) findViewById(R.id.leftThumbnail);
        leftFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(0);
                updatePoints();
            }
        });
        centerFoodThumbnail = (ImageView) findViewById(R.id.centerThumbnail);
        centerFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(1);
                updatePoints();
            }
        });
        rightFoodThumbnail = (ImageView) findViewById(R.id.rightThumbnail);
        rightFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(2);
                updatePoints();
            }
        });

        // Put all thumbnails into an array
        thumbnailsArray = new ImageView[]
                {leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail};
    }
}
