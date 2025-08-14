package com.example.peekeventproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class ProfileActivity extends AppCompatActivity {

    // UI fields for displaying profile info
    private TextView profileName, profileEmail, profilePhone, profileDate;

    // Firebase references
    private FirebaseAuth auth;                // For authentication and user sessions
    private DatabaseReference userRef;        // To retrieve current user's profile data

    // RecyclerView to display user events
    private RecyclerView recyclerView;
    private List<Event> myEvents = new ArrayList<>();       // Events created by the user
    private List<Event> rsvpedEvents = new ArrayList<>();   // Events user RSVP'd for (but didn't create)
    private EventAdapter eventAdapter;                     // Adapter for RecyclerView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase authentication
        auth = FirebaseAuth.getInstance();

        // Bind profile text views
        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        profilePhone = findViewById(R.id.profile_phone);
        profileDate = findViewById(R.id.profile_date);

        // Bind RecyclerView
        recyclerView = findViewById(R.id.user_events_recycler);

        // UI controls for switching event types
        ImageView menuButton = findViewById(R.id.menu_button);
        Button btnMyEvents = findViewById(R.id.btn_my_events);
        Button btnRsvpedEvents = findViewById(R.id.btn_rsvped_events);

        // Initialize adapter with empty list and set layout
        eventAdapter = new EventAdapter(this, new ArrayList<>(), true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(eventAdapter);


        btnMyEvents.setOnClickListener(v -> {
            btnMyEvents.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lavender));
            btnRsvpedEvents.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));

            eventAdapter = new EventAdapter(ProfileActivity.this, myEvents, true);
            recyclerView.setAdapter(eventAdapter);  //Reload adapter with only events created by user.
        });


        btnRsvpedEvents.setOnClickListener(v -> {
            btnRsvpedEvents.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lavender));
            btnMyEvents.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));

            eventAdapter = new EventAdapter(ProfileActivity.this, rsvpedEvents, false);
            recyclerView.setAdapter(eventAdapter); //Reload adapter with RSVP events only
        });


        menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(ProfileActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_about_us) {
                    // Navigate to AboutUsActivity
                    startActivity(new Intent(ProfileActivity.this, AboutUsActivity.class));
                    return true;
                } else if (id == R.id.menu_logout) {
                    // Logout user and return to Welcome Page
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, Welcome_page.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        /**
         * Load current user profile data from Firebase.
         */
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

            // Fetch and display user profile fields
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        profileName.setText(snapshot.child("name").getValue(String.class));
                        profileEmail.setText(snapshot.child("email").getValue(String.class));
                        profilePhone.setText(snapshot.child("phone").getValue(String.class));
                        profileDate.setText(snapshot.child("dateCreated").getValue(String.class));
                    } else {
                        Toast.makeText(ProfileActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            });

            // Load user's events (both created and RSVP'd)
            loadUserEvents(uid);
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        // Setup bottom navigation
        setupBottomNavigation();
    }

    /**
     * Loads events related to the user:
     * - myEvents: events created by this user.
     * - rsvpedEvents: events user RSVP'd for (but didn't create).
     */
    private void loadUserEvents(String userId) { // Loads events related to the user:
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myEvents.clear();
                rsvpedEvents.clear();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event == null) continue;

                    // Check if the current user is the creator
                    boolean isCreator = userId.equals(event.getCreatorId());

                    // Check if the user RSVP'd to this event
                    boolean hasRsvpd = event.getRsvpList() != null
                            && Boolean.TRUE.equals(event.getRsvpList().get(userId));

                    if (isCreator) myEvents.add(event);
                    if (hasRsvpd && !isCreator) rsvpedEvents.add(event);
                }

                // Default view: show "My Events" first
                eventAdapter.updateData(myEvents);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupBottomNavigation() { // Configures the BottomNavigationView to allow switching between:
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_profile); // Highlight current tab

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_create) {
                startActivity(new Intent(ProfileActivity.this, CreateEventActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true; // Already on profile
            }
            return false;
        });
    }
}
