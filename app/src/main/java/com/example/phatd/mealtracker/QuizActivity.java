package com.example.phatd.mealtracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // I-don't-know button setup
        Button iDontKnowButton = (Button) findViewById(R.id.idontknow);
        iDontKnowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revealCorrectAnswer();
            }
        });
    }

    private void revealCorrectAnswer() {

    }
}
