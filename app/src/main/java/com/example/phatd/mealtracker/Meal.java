package com.example.phatd.mealtracker;

import android.provider.ContactsContract.CommonDataKinds.Photo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Meal {
    public String timeOfMeal;
    public String dayOfMeal;
    public Photo mealThumbnail;
    //public

    // Default Constructor
    public Meal() {
        timeOfMeal = new SimpleDateFormat("HH:mm a").format(new Date());
        dayOfMeal = new SimpleDateFormat("EE").format(new Date());
        mealThumbnail = null;
    }

    // Constructor
    // Passing in date only
    public Meal(Date dateInput) {
        timeOfMeal = new SimpleDateFormat("HH:mm a").format(dateInput);
        dayOfMeal = new SimpleDateFormat("EE").format(dateInput);
        mealThumbnail = null;
    }
}
