package com.example.peekeventproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    // Input fields for event data
    private EditText titleEditText, categoryEditText, dateEditText, timeEditText,
            locationEditText, zoneEditText;
    private Button saveChangesButton;
    private EditText editEventTime; // Used for picking start/end time

    private Event event; // Event object passed from previous activity
    private DatabaseReference databaseRef; // Firebase Database reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        // Bind all input views to variables
        titleEditText = findViewById(R.id.edit_event_title);
        categoryEditText = findViewById(R.id.edit_event_category);
        dateEditText = findViewById(R.id.edit_event_date);
        timeEditText = findViewById(R.id.edit_event_time);
        locationEditText = findViewById(R.id.edit_event_location);
        zoneEditText = findViewById(R.id.edit_event_zone);
        saveChangesButton = findViewById(R.id.save_changes_button);
        editEventTime = findViewById(R.id.edit_event_time);

        // Allow user to pick a start/end time when clicking time field
        editEventTime.setOnClickListener(v -> showStartTimePicker());

        // Retrieve the Event object sent from the adapter
        event = (Event) getIntent().getSerializableExtra("event");
        if (event == null) {
            // If no event is passed, show an error and close the screen
            Toast.makeText(this, "No event data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Populate UI fields with existing event data
        titleEditText.setText(event.getTitle());
        categoryEditText.setText(event.getCategory());
        dateEditText.setText(event.getDate());
        timeEditText.setText(event.getTime());
        locationEditText.setText(event.getLocation());
        zoneEditText.setText(event.getZone());

        // Get reference to Firebase "events" node
        databaseRef = FirebaseDatabase.getInstance().getReference("events");

        // Open date picker when user clicks on the date field
        dateEditText.setOnClickListener(v -> showDatePicker());

        // Save button updates event in Firebase when clicked
        saveChangesButton.setOnClickListener(v -> updateEvent());
    }

    /**
     * Displays a date picker dialog to allow user to select a new event date.
     */
    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) ->
                        dateEditText.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * Shows a time picker for selecting start time,
     * then automatically opens an end-time picker.
     */
    private void showStartTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog startTimeDialog = new TimePickerDialog(this, (view, startHour, startMinute) -> {
            // Format selected start time
            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.HOUR_OF_DAY, startHour);
            startTime.set(Calendar.MINUTE, startMinute);
            String formattedStart = formatTime(startTime);

            // After choosing start time, prompt for end time
            showEndTimePicker(formattedStart);

        }, hour, minute, false); // false = use 12-hour format with AM/PM
        startTimeDialog.show();
    }

    /**
     * Shows a time picker for selecting event end time,
     * then updates the time input field with a range.
     */
    private void showEndTimePicker(String formattedStart) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog endTimeDialog = new TimePickerDialog(this, (view, endHour, endMinute) -> {
            // Format selected end time
            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.HOUR_OF_DAY, endHour);
            endTime.set(Calendar.MINUTE, endMinute);
            String formattedEnd = formatTime(endTime);

            // Show full range in time field
            editEventTime.setText(formattedStart + " - " + formattedEnd);

        }, hour, minute, false); // false = use 12-hour format
        endTimeDialog.show();
    }

    /**
     * Helper method to format a Calendar object into "hh:mm AM/PM".
     */
    private String formatTime(Calendar time) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(time.getTime());
    }

    /**
     * Validates input, updates event object, and saves changes to Firebase.
     */
    private void updateEvent() {
        // Get user inputs from fields
        String title = titleEditText.getText().toString().trim();
        String category = categoryEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String zone = zoneEditText.getText().toString().trim();

        // Basic validation to ensure required fields are not empty
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) ||
                TextUtils.isEmpty(time) || TextUtils.isEmpty(location)) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update event object with new details
        event.setTitle(title);
        event.setCategory(category);
        event.setDate(date);
        event.setTime(time);
        event.setLocation(location);
        event.setZone(zone);

        // Push updated event object back to Firebase using its ID
        databaseRef.child(event.getEventId()) // eventId must exist in Event class
                .setValue(event)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditEventActivity.this, "Event updated", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity after saving
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditEventActivity.this, "Failed to update event", Toast.LENGTH_SHORT).show());
    }
}
