package com.example.peekeventproject;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * EventLoader is a helper class responsible for loading Event data from Firebase Realtime Database.
 * It supports:
 *   - Loading all future/today events (and removing past events).
 *   - Adding real-time listeners for continuous event updates.
 *   - Loading a single event by ID.
 *   - Removing listeners to avoid memory leaks.
 */
public class EventLoader {

    // Reference to "events" node in Firebase Database
    private DatabaseReference eventsRef;


    public interface EventLoadCallback {     //Callback interface to return events asynchronously.
        void onEventsLoaded(List<Event> events); // Called when events are successfully loaded
        void onError(String error);              // Called when an error occurs
    }


    //Constructor initializes Firebase reference to the "events" collection.
    public EventLoader() {
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
    }


    private boolean isFutureOrToday(String dateString) {  // Helper method that checks if an event date is today or in the future.
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date eventDate = sdf.parse(dateString); //The date string to validate.

            // Normalize "today" to midnight to ensure proper comparison
            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            // Event is valid if it's today or later (not before today's midnight)
            return eventDate != null && !eventDate.before(todayCal.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return true; // If date is invalid, keep the event instead of discarding it
        }
    }

    /**
     * Loads all events from Firebase that are scheduled for today or later.
     * - Filters out past events (optionally deletes them).
     * - Returns the result once via callback (single-time load).
     *
     * @param callback Callback to handle events or errors.
     */
    public void loadAllEvents(EventLoadCallback callback) {
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Event> eventList = new ArrayList<>();

                // Loop through each child in "events"
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);

                    if (event != null) {
                        // Ensure event ID is set (sometimes missing if only Firebase key exists)
                        if (event.getEventId() == null || event.getEventId().isEmpty()) {
                            event.setEventId(eventSnapshot.getKey());
                        }

                        // Add only future or today's events; remove old ones
                        if (isFutureOrToday(event.getDate())) {
                            eventList.add(event);
                        } else {
                            eventsRef.child(event.getEventId()).removeValue(); // Optional cleanup
                        }
                    }
                }

                // Return the valid event list to caller
                callback.onEventsLoaded(eventList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Return error message to caller
                callback.onError("Failed to load events: " + error.getMessage());
            }
        });
    }

    /**
     * Adds a real-time Firebase listener for events.
     * - Continuously updates the caller when data changes.
     * - Filters out past events and optionally deletes them.
     *
     * @param callback Callback to deliver live updates.
     * @return The ValueEventListener (caller must keep this reference to remove it later).
     */
    public ValueEventListener loadEventsRealtime(EventLoadCallback callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Event> eventList = new ArrayList<>();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);

                    if (event != null) {
                        if (event.getEventId() == null || event.getEventId().isEmpty()) {
                            event.setEventId(eventSnapshot.getKey());
                        }

                        if (isFutureOrToday(event.getDate())) {
                            eventList.add(event);
                        } else {
                            eventsRef.child(event.getEventId()).removeValue(); // Remove outdated events
                        }
                    }
                }

                callback.onEventsLoaded(eventList); // Send updated event list to caller
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Failed to load events: " + error.getMessage());
            }
        };

        // Attach listener to Firebase to receive continuous updates
        eventsRef.addValueEventListener(listener);
        return listener; // Return so caller can remove it later if needed
    }

    /**
     * Loads a single event by its ID.
     * - Useful for event detail pages.
     * - Callback always returns a list with either 0 or 1 event.
     *
     * @param eventId The unique ID of the event to fetch.
     * @param callback Callback to handle the single event or errors.
     */
    public void loadEvent(String eventId, EventLoadCallback callback) {
        eventsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Event event = snapshot.getValue(Event.class);

                    if (event != null) {
                        // Ensure eventId field is populated
                        if (event.getEventId() == null || event.getEventId().isEmpty()) {
                            event.setEventId(snapshot.getKey());
                        }

                        // Wrap event in a list for consistency with callback
                        List<Event> singleEventList = new ArrayList<>();
                        singleEventList.add(event);
                        callback.onEventsLoaded(singleEventList);
                    } else {
                        callback.onError("Event data not found");
                    }
                } else {
                    callback.onError("Event not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Failed to load event: " + error.getMessage());
            }
        });
    }


    public void removeListener(ValueEventListener listener) { //  Removes a previously registered real-time listener.
     // Prevents memory leaks when activity/fragment is destroyed.
        if (listener != null) {
            eventsRef.removeEventListener(listener);
        }
    }
}

