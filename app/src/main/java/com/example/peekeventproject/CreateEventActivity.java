package com.example.peekeventproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    // Request codes for image picking and permissions
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Selected image URI
    private Uri imageUri;

    // UI components
    private ImageView eventImageView;
    private EditText titleInput, descriptionInput, dateInput, timeInput, locationInput, zoneInput, attendeesInput;
    private Spinner categorySpinner;
    private Button submitButton;
    private EditText eventTimeInput; // Used for selecting start-end time

    // Firebase references
    private DatabaseReference eventRef;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Bind UI elements to variables
        bindViews();
        setupBottomNavigation();

        // Initialize Firebase references
        eventRef = FirebaseDatabase.getInstance().getReference("events");
        storageRef = FirebaseStorage.getInstance().getReference("event_images");

        // Set click listeners
        eventImageView.setOnClickListener(v -> checkStoragePermissionAndOpenFileChooser()); // Select image
        dateInput.setOnClickListener(v -> showDatePicker());                                // Select date
        submitButton.setOnClickListener(v -> uploadEventData());                            // Submit event
        eventTimeInput = findViewById(R.id.event_time_input);
        eventTimeInput.setOnClickListener(v -> showStartTimePicker());                      // Select time range
    }

    /**
     * Binds all view elements to variables
     */
    private void bindViews() {
        titleInput = findViewById(R.id.event_title_input);
        descriptionInput = findViewById(R.id.event_description_input);
        dateInput = findViewById(R.id.event_date_input);
        timeInput = findViewById(R.id.event_time_input);
        locationInput = findViewById(R.id.location_input);
        zoneInput = findViewById(R.id.tukatune_zone_input);
        attendeesInput = findViewById(R.id.attendee_count_input);
        categorySpinner = findViewById(R.id.event_category_spinner);
        eventImageView = findViewById(R.id.eventImageView);
        submitButton = findViewById(R.id.submit_button);
    }

    /**
     * Checks permission to read images and opens file chooser if granted
     */
    private void checkStoragePermissionAndOpenFileChooser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                openFileChooser();
            }
        } else {
            // For older Android versions
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                openFileChooser();
            }
        }
    }

    /**
     * Opens an image picker to select an event image
     */
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Handles result after selecting an image
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            eventImageView.setImageURI(imageUri); // Show image preview on ImageView
        }
    }

    /**
     * Handles permission result for reading storage
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser();
            } else {
                Toast.makeText(this, "Permission denied to access images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Uploads event data and image to Firebase Storage and Database
     */
    private void uploadEventData() {
        // Collect input values
        String title = titleInput.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String description = descriptionInput.getText().toString().trim();
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String zone = zoneInput.getText().toString().trim();
        String attendeesStr = attendeesInput.getText().toString().trim();

        // Validate fields
        if (title.isEmpty() || category.isEmpty() || description.isEmpty() || date.isEmpty() ||
                time.isEmpty() || location.isEmpty() || zone.isEmpty() || attendeesStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        int attendeeCount;
        try {
            attendeeCount = Integer.parseInt(attendeesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid attendee count", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique event ID using firebase
        String eventId = eventRef.push().getKey();
        if (eventId == null) {
            Toast.makeText(this, "Failed to generate event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state on button
        submitButton.setEnabled(false);
        submitButton.setText("Creating Event...");

        // Upload image to Firebase Storage (organized by eventId folder)
        StorageReference fileRef = storageRef.child(eventId + "/" + System.currentTimeMillis() + "." + getFileExtension(imageUri));

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            // Create Event object with details
                            Event event = new Event(eventId, title, category, description, date, time, location, zone, attendeeCount);
                            event.setImageUrl(imageUrl);
                            event.setCreatorId(FirebaseAuth.getInstance().getCurrentUser().getUid()); // Store user ID

                            // Save event to Firebase Database
                            eventRef.child(eventId).setValue(event)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();

                                            // Send event back to MainActivity
                                            Intent resultIntent = new Intent();
                                            resultIntent.putExtra("event", event);
                                            setResult(RESULT_OK, resultIntent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                                            resetSubmitButton();
                                        }
                                    });
                        })
                )
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
    }

    /**
     * Resets submit button state if upload fails
     */
    private void resetSubmitButton() {
        submitButton.setEnabled(true);
        submitButton.setText("Create Event");
    }

    /**
     * Determines file extension for selected image
     */
    private String getFileExtension(Uri uri) {
        String extension = null;

        // Try using content resolver first
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(cr.getType(uri));
        }

        // Fallback using file path if content resolver fails
        if (extension == null) {
            String path = uri.getPath();
            if (path != null) {
                int dotIndex = path.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < path.length() - 1) {
                    extension = path.substring(dotIndex + 1);
                }
            }
        }

        return extension != null ? extension : "jpg"; // Default extension
    }

    /**
     * Sets up bottom navigation for switching screens
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_create); // Highlight current tab

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    /**
     * Shows date picker dialog to select event date
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month1 + 1, year1);
                    dateInput.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // Prevent past dates
        datePickerDialog.show();
    }

    /**
     * Shows start time picker dialog
     */
    private void showStartTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, startHour, startMinute) -> {
            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.HOUR_OF_DAY, startHour);
            startTime.set(Calendar.MINUTE, startMinute);
            String formattedStart = formatTime(startTime);

            showEndTimePicker(formattedStart); // After picking start time, ask for end time

        }, hour, minute, false).show(); // false = 12-hour format with AM/PM
    }

    /**
     * Shows end time picker dialog and sets full time range
     */
    private void showEndTimePicker(String formattedStart) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, endHour, endMinute) -> {
            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.HOUR_OF_DAY, endHour);
            endTime.set(Calendar.MINUTE, endMinute);
            String formattedEnd = formatTime(endTime);

            eventTimeInput.setText(formattedStart + " - " + formattedEnd); // Show time range

        }, hour, minute, false).show();
    }

    /**S
     * Helper method to format time into "hh:mm AM/PM"
     */
    private String formatTime(Calendar time) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(time.getTime());
    }
}
