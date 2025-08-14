package com.example.peekeventproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ValueEventListener;

/**
 * Activity that shows detailed information for a single event.
 * Users can view event data, RSVP or un-RSVP, and see live attendee counts.
 */
public class EventDetailActivity extends AppCompatActivity {

    private Event event;                    // The event being displayed (passed from previous screen)
    private TextView attendeeCountText;     // Displays total number of attendees
    private Button rsvpButton;              // RSVP button to join/leave event
    private RSVPManager rsvpManager;        // Handles all RSVP logic with Firebase
    private ValueEventListener attendeeListener; // Firebase listener for real-time updates
    private boolean userHasRsvpd = false;   // Tracks if current user has RSVPd
    private boolean isLoading = false;      // Prevents multiple clicks during network calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Enable action bar back button and set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
        }

        // Initialize RSVP manager (Firebase interaction handler)
        rsvpManager = new RSVPManager();

        // Retrieve Event object passed via Intent (must implement Serializable)
        event = (Event) getIntent().getSerializableExtra("event");

        // If no event data was passed, close the activity with a message
        if (event == null || event.getEventId() == null) {
            Toast.makeText(this, "Event data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind views and populate with event data
        initializeViews();
        setupEventData();

        // Setup RSVP button actions and state
        setupRSVPButton();

        // Setup real-time attendee count listener
        setupRealtimeListener();

        // Check whether the user has already RSVPd to update button state
        checkInitialRSVPStatus();
    }

    /**
     * Find all views, populate event data, and load image.
     */
    private void initializeViews() {
        ImageView imageView = findViewById(R.id.detail_image);
        TextView title = findViewById(R.id.detail_title);
        TextView date = findViewById(R.id.detail_date);
        TextView time = findViewById(R.id.detail_time);
        TextView location = findViewById(R.id.detail_location);
        TextView zone = findViewById(R.id.detail_zone);
        TextView description = findViewById(R.id.detail_description);
        attendeeCountText = findViewById(R.id.attendee_count_text);
        rsvpButton = findViewById(R.id.rsvp_button);
        ImageView backButton = findViewById(R.id.back_button);

        // Set event details on UI
        title.setText(event.getTitle());
        date.setText(event.getDate());
        time.setText(event.getTime());
        location.setText(event.getLocation());
        zone.setText("Tukutane Zone: " + event.getZone());
        description.setText(event.getDescription());

        // Load event image using Glide (async image loading)
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery) // temporary image while loading
                    .into(imageView);
        } else {
            // If no image URL exists, use a placeholder
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Handle custom back button click (if present in layout)
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Navigate back to MainActivity and clear duplicate instances
                Intent intent = new Intent(EventDetailActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    /**
     * Sets the initial attendee count text.
     */
    private void setupEventData() {
        updateAttendeeText(event.getAttendeeCount());
    }

    /**
     * Sets up the RSVP button to toggle RSVP state when clicked.
     */
    private void setupRSVPButton() {
        rsvpButton.setOnClickListener(v -> {
            // Ignore clicks if already performing a network request
            if (isLoading) return;

            setLoadingState(true);

            // Use RSVPManager to toggle user's RSVP state
            rsvpManager.toggleRSVP(event.getEventId(), new RSVPManager.RSVPCallback() {
                @Override
                public void onSuccess(boolean isRsvpd, int newAttendeeCount) {
                    runOnUiThread(() -> {
                        // Update UI after successful RSVP action
                        userHasRsvpd = isRsvpd;
                        updateRSVPButton();
                        updateAttendeeText(newAttendeeCount);
                        setLoadingState(false);

                        // Notify user of the change
                        String message = isRsvpd ? "Successfully RSVPd!" : "RSVP removed";
                        Toast.makeText(EventDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // Show error message and reset button state
                        setLoadingState(false);
                        Toast.makeText(EventDetailActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    /**
     * Creates a real-time listener to update attendee count dynamically.
     */
    private void setupRealtimeListener() {
        attendeeListener = rsvpManager.listenToAttendeeCount(event.getEventId(),
                new RSVPManager.AttendeeCountListener() {
                    @Override
                    public void onAttendeeCountChanged(int newCount, boolean userRsvpd) {
                        runOnUiThread(() -> {
                            // Update UI when Firebase data changes in real time
                            userHasRsvpd = userRsvpd;
                            updateAttendeeText(newCount);
                            updateRSVPButton();
                        });
                    }
                });
    }

    /**
     * Checks initial RSVP status (used when the activity first opens).
     */
    private void checkInitialRSVPStatus() {
        rsvpManager.checkRSVPStatus(event.getEventId(), new RSVPManager.RSVPCallback() {
            @Override
            public void onSuccess(boolean isRsvpd, int attendeeCount) {
                runOnUiThread(() -> {
                    // Update RSVP button and attendee count based on current status
                    userHasRsvpd = isRsvpd;
                    updateRSVPButton();
                    updateAttendeeText(attendeeCount);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Notify user if there's a problem retrieving RSVP state
                    Toast.makeText(EventDetailActivity.this,
                            "Error checking RSVP status: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Updates the attendee count text view with a formatted value.
     */
    private void updateAttendeeText(int count) {
        attendeeCountText.setText(count + " Going");
    }

    /**
     * Updates RSVP button UI based on whether user has RSVPd.
     */
    private void updateRSVPButton() {
        if (userHasRsvpd) {
            // User has RSVPd → show option to un-RSVP
            rsvpButton.setText("Un-RSVP");
            rsvpButton.setBackgroundTintList(ContextCompat.getColorStateList(this,
                    android.R.color.holo_red_light));
        } else {
            // User has not RSVPd → show RSVP option
            rsvpButton.setText("RSVP");
            rsvpButton.setBackgroundTintList(ContextCompat.getColorStateList(this,
                    android.R.color.holo_green_light));
        }
    }

    /**
     * Enables or disables button and updates label during loading.
     */
    private void setLoadingState(boolean loading) {
        isLoading = loading;
        rsvpButton.setEnabled(!loading);
        rsvpButton.setText(loading ? "Loading..." : (userHasRsvpd ? "Un-RSVP" : "RSVP"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firebase listener to avoid memory leaks
        if (attendeeListener != null && event != null) {
            rsvpManager.removeListener(event.getEventId(), attendeeListener);
        }
    }

    /**
     * Handles the action bar's back button click.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity when back arrow is pressed
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
