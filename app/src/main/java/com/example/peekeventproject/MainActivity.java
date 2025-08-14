package com.example.peekeventproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CREATE_EVENT = 1; // Request code to identify event creation result

    // RecyclerView components
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;           // List displayed to the user (filtered)
    private List<Event> originalEventList;   // Full unfiltered list of events from Firebase

    // Search and filter components
    private EditText searchEditText;
    private String currentCategory = "All";   // Default filter is "All categories"
    private String currentSearchQuery = "";   // Current text from search bar

    // Firebase loader
    private EventLoader eventLoader;          // Handles retrieving events from Firebase
    private ValueEventListener realtimeListener; // Optional listener for real-time updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize event loader class (custom helper for Firebase)
        eventLoader = new EventLoader();

        // Setup UI components
        setupBottomNavigation();  // Bottom navigation bar (home, create event, profile)
        setupSearchBar();         // Search bar to filter events by text
        setupCategoryButtons();   // Category buttons (All, Music, Art, etc.)
        setupRecyclerView();      // RecyclerView for listing events

        // Load events from Firebase and display them
        loadEventsFromFirebase();
    }

    /**
     * Sets up the RecyclerView to display events
     */
    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.events_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventList = new ArrayList<>();
        originalEventList = new ArrayList<>();
        adapter = new EventAdapter(MainActivity.this, eventList, false);

        recyclerView.setAdapter(adapter);
    }

    /**
     * Loads events from Firebase using EventLoader
     */
    private void loadEventsFromFirebase() {
        eventLoader.loadAllEvents(new EventLoader.EventLoadCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                // Runs on UI thread because Firebase callbacks are asynchronous
                runOnUiThread(() -> {
                    originalEventList.clear();
                    originalEventList.addAll(events); // Store the full list
                    applySearchAndFilter();          // Apply any filters immediately
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Failed to load events: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });

        //   listen for real-time updates
        /*
        realtimeListener = eventLoader.loadEventsRealtime(new EventLoader.EventLoadCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                runOnUiThread(() -> {
                    originalEventList.clear();
                    originalEventList.addAll(events);
                    applySearchAndFilter();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "Real-time update error: " + error,
                        Toast.LENGTH_LONG).show();
                });
            }
        });
        */
    }

    /**
     * Handles clicking an event card to open details
     */
    private void onEventClick(Event event) {
        // Ensure event has a valid ID
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            Toast.makeText(this,
                    "Event ID missing. Cannot open details.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass event object to detail screen
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra("event", event);
        startActivity(intent);
    }

    /**
     * Sets up search bar to filter events as user types
     */
    private void setupSearchBar() {
        searchEditText = findViewById(R.id.search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action required
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim(); // Update current query
                applySearchAndFilter();                   // Filter list immediately
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action required
            }
        });
    }

    /**
     * Applies both search text filter and category filter
     */
    private void applySearchAndFilter() {
        List<Event> filteredList = new ArrayList<>();

        for (Event event : originalEventList) {
            boolean matchesCategory = currentCategory.equals("All") ||
                    (event.getCategory() != null && event.getCategory().equalsIgnoreCase(currentCategory));

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (event.getTitle() != null && event.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (event.getDescription() != null && event.getDescription().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (event.getLocation() != null && event.getLocation().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            // Add event if it matches both filters
            if (matchesCategory && matchesSearch) {
                filteredList.add(event);
            }
        }

        // Refresh RecyclerView with filtered events
        eventList.clear();
        eventList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    /**
     * Sets up the bottom navigation bar for switching activities
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home); // Highlight current tab

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // Already on home
                    return true;
                } else if (itemId == R.id.nav_create) {
                    // Start CreateEventActivity and expect result
                    Intent intent = new Intent(MainActivity.this, CreateEventActivity.class);
                    startActivityForResult(intent, REQUEST_CREATE_EVENT);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // Open profile screen
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Sets up click listeners for category filter buttons
     */
    private void setupCategoryButtons() {
        int[] categoryIds = {
                R.id.category_all,
                R.id.category_music,
                R.id.category_art,
                R.id.category_technology,
                R.id.category_food,
                R.id.category_sports
        };

        for (int id : categoryIds) {
            TextView category = findViewById(id);
            category.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    highlightCategory((TextView) v); // Change UI highlight and filter
                }
            });
        }
    }

    /**
     * Highlights the selected category and applies filter
     */
    private void highlightCategory(TextView selectedCategory) {
        int[] categoryIds = {
                R.id.category_all,
                R.id.category_music,
                R.id.category_art,
                R.id.category_technology,
                R.id.category_food,
                R.id.category_sports
        };

        // Update current filter
        currentCategory = selectedCategory.getText().toString();

        // Update button styles for selected/unselected
        for (int id : categoryIds) {
            TextView category = findViewById(id);
            if (category.getId() == selectedCategory.getId()) {
                category.setBackgroundResource(R.drawable.category_button_selected);
                category.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                category.setBackgroundResource(R.drawable.category_button_unselected);
                category.setTextColor(getResources().getColor(R.color.dark_text));
            }
        }

        applySearchAndFilter(); // Refresh list by category
        Toast.makeText(this, selectedCategory.getText() + " selected", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles result after creating a new event
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CREATE_EVENT && resultCode == RESULT_OK && data != null) {
            Event newEvent = (Event) data.getSerializableExtra("event");
            if (newEvent != null && newEvent.getEventId() != null) {
                // Insert at the top of list
                originalEventList.add(0, newEvent);
                applySearchAndFilter();
                recyclerView.scrollToPosition(0);

                Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Invalid event data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Cleanup listeners to avoid memory leaks
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeListener != null && eventLoader != null) {
            eventLoader.removeListener(realtimeListener);
        }
    }
}
