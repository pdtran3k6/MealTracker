package com.example.phatd.mealtracker;

import android.graphics.Color;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // I-don't-know button setup
        Button iDontKnowButton = (Button) findViewById(R.id.idontknow);
        iDontKnowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(-1);
            }
        });

        // question_DayAndTime TextView setup
        question_DayAndTime = (TextView) findViewById(R.id.question_DayAndTime);

        // Populate question_DayAndTime
        generateQuestions();

        // Populate all ImageView food thumbnails
        generateThumbnails();
    }

    private void revealCorrectAnswer(int chosenIndex) {
        int displayPos = correctIndex + 1;
        if (chosenIndex == -1) {
            Toast.makeText(this, "This is the correct answer. " +
                    "Be more aware of what you eat next time",
                    Toast.LENGTH_LONG).show();
            thumbnailsArray[correctIndex].setColorFilter(Color.argb(70, 26, 255, 0));
        } else if (chosenIndex == correctIndex) {
            Toast.makeText(this, "Congratulations. It's the correct answer",
                    Toast.LENGTH_LONG).show();
            thumbnailsArray[chosenIndex].setColorFilter(Color.argb(70, 26, 255, 0));
        } else {
            Toast.makeText(this, "The correct answer was photo number " + displayPos,
                    Toast.LENGTH_LONG).show();
            thumbnailsArray[chosenIndex].setColorFilter(Color.argb(70, 255, 61, 61));
            thumbnailsArray[correctIndex].setColorFilter(Color.argb(70, 26, 255, 0));
        }
        updatePoints();
    }

    private void updatePoints() {

    }

    private void generateQuestions() {
        // Get directory path & generate a File[] that contains all meal thumbnails
        foodThumbnailsDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        directoryListing = foodThumbnailsDir.listFiles();
        if (directoryListing.length != 0) {
            correctIndex = ThreadLocalRandom.current().nextInt(0, directoryListing.length);
            correctPhoto = directoryListing[correctIndex];

            long millisec = correctPhoto.lastModified();
            Date mealCapturedTime = new Date(millisec);
            String dayName = new SimpleDateFormat("EEE").format(mealCapturedTime);
            String date = new SimpleDateFormat("dd/MM").format(mealCapturedTime);
            String hourMin = new SimpleDateFormat("hh:mm aaa").format(mealCapturedTime);

            question_DayAndTime.setText("On " + dayName + " " + date + " \nat " + hourMin + "?");
        } else {
            Toast.makeText(this, "Not enough photos to generate questions",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Set up display thumbnail
    private void generateThumbnails() {
        // Set each thumbnail to its corresponding variable
        leftFoodThumbnail = (ImageView) findViewById(R.id.leftThumbnail);
        leftFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(0);
            }
        });
        centerFoodThumbnail = (ImageView) findViewById(R.id.centerThumbnail);
        centerFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(1);
            }
        });
        rightFoodThumbnail = (ImageView) findViewById(R.id.rightThumbnail);
        rightFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(2);
            }
        });

        // Put all thumbnails into an array
        thumbnailsArray = new ImageView[]
                {leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail};

        int totalNumPhotos = directoryListing.length;
        if (totalNumPhotos >= 3) {
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
        } else {
            Toast.makeText(this, "Not enough photos to generate questions",
                    Toast.LENGTH_LONG).show();
        }
    }
}
