package com.example.phatd.mealtracker;

import android.content.Intent;
import android.content.SharedPreferences;
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

    ImageView[] thumbnailsArray;
    ImageView leftFoodThumbnail;
    ImageView centerFoodThumbnail;
    ImageView rightFoodThumbnail;
    TextView question_DayAndTime;
    TextView pointsTV;

    File[] directoryListing;
    int correctIndex;
    int points;
    boolean answered;
    boolean correct;

    static int LEFT_THUMBNAIL = 0;
    static int MID_THUMBNAIL = 1;
    static int RIGHT_THUMBNAIL = 2;
    static int NO_ANSWER = -1;
    static String POINT_SHARED_PREFRENCES = "points";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_quiz);
        question_DayAndTime = (TextView) findViewById(R.id.question_DayAndTime);
        pointsTV = (TextView) findViewById(R.id.points);
        answered = false;
        points = getCurrPoints();

        // I-don't-know button setup
        Button iDontKnowButton = (Button) findViewById(R.id.idontknow);
        iDontKnowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(NO_ANSWER);
                genSnackbar();
                updatePoints(correct);
            }
        });

        generateQuestions();
        generateThumbnails();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        answered = false;
    }

    private void revealCorrectAnswer(int chosenIndex) {
        int displayPos = correctIndex + 1;
        int green;
        int red;
        red = Color.argb(70, 255, 61, 61);
        green = Color.argb(70, 26, 255, 0);

        if (!answered) {
            if (chosenIndex == NO_ANSWER) {
                Toast.makeText(this, "This is the correct answer. " +
                                "Be more aware of what you eat next time",
                        Toast.LENGTH_SHORT).show();
                thumbnailsArray[correctIndex].setColorFilter(green);
                correct = false;
            } else if (chosenIndex == correctIndex) {
                Toast.makeText(this, "Congratulations. It's the correct answer",
                        Toast.LENGTH_SHORT).show();
                thumbnailsArray[chosenIndex].setColorFilter(green);
                correct = true;
            } else {
                Toast.makeText(this, "The correct answer was photo number " + displayPos,
                        Toast.LENGTH_SHORT).show();
                thumbnailsArray[chosenIndex].setColorFilter(red);
                thumbnailsArray[correctIndex].setColorFilter(green);
                correct = false;
            }
            answered = true;
        }
    }


    private void genSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), "",
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

    private int getCurrPoints() {
        int localPoints;
        SharedPreferences myPref = getSharedPreferences(POINT_SHARED_PREFRENCES, 0);
        if (myPref.contains(POINT_SHARED_PREFRENCES)) {
            localPoints = Integer.parseInt(myPref.getString(POINT_SHARED_PREFRENCES, ""));
        } else {
            localPoints = 0;
        }

        String currentPoints = Integer.toString(localPoints);
        pointsTV.setText("Current point is: " + currentPoints);
        return localPoints;
    }

    private void updatePoints(boolean correct) {
        SharedPreferences myPref = getSharedPreferences(POINT_SHARED_PREFRENCES, 0);
        SharedPreferences.Editor editor = myPref.edit();

        if (correct) {
            editor.putString(POINT_SHARED_PREFRENCES, Integer.toString(++points));
            editor.commit();
            getCurrPoints();
            return;
        }

        editor.putString(POINT_SHARED_PREFRENCES, Integer.toString(--points));
        editor.commit();
        getCurrPoints();
    }

    private void generateQuestions() {
        File foodThumbnailsDir;
        File correctPhoto;
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

        for (int i = 0; i < totalNumPhotos; i++) {
            randomPhotoIndexes[i] = i;
        }

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

        for (int i = 0; i < 3; i++) {
            Glide.with(this).
                    load(directoryListing[randomPhotoIndexes[i]]).
                    into(thumbnailsArray[i]);
        }
    }

    private void setupThumbnails() {
        leftFoodThumbnail = (ImageView) findViewById(R.id.leftThumbnail);
        leftFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(LEFT_THUMBNAIL);
                genSnackbar();
                updatePoints(correct);
            }
        });
        centerFoodThumbnail = (ImageView) findViewById(R.id.centerThumbnail);
        centerFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(MID_THUMBNAIL);
                genSnackbar();
                updatePoints(correct);
            }
        });
        rightFoodThumbnail = (ImageView) findViewById(R.id.rightThumbnail);
        rightFoodThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer(RIGHT_THUMBNAIL);
                genSnackbar();
                updatePoints(correct);
            }
        });

        thumbnailsArray = new ImageView[]
                {leftFoodThumbnail, centerFoodThumbnail, rightFoodThumbnail};
    }
}
