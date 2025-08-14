package com.example.peekeventproject;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

/**
 * RSVPManager handles all RSVP (attendee) logic for events.
 * It connects to Firebase Realtime Database and:
 * - Toggles RSVP status for the logged-in user.
 * - Checks if the user has RSVP'd to an event.
 * - Listens to attendee count changes in real-time.
 * - Removes event listeners to prevent memory leaks.
 */
public class RSVPManager { // RSVPManager handles all RSVP (attendee) logic for events.

    // Reference to the "events" node in Firebase
    private DatabaseReference eventsRef;

    // Firebase Authentication to get current user ID
    private FirebaseAuth auth;
    private String currentUserId;

    /**
     * Callback interface for RSVP actions like toggling or checking RSVP status.
     */
    public interface RSVPCallback {
        void onSuccess(boolean isRsvpd, int newAttendeeCount); // Called when operation succeeds
        void onError(String error);                            // Called if operation fails
    }

    /**
     * Listener interface for real-time attendee count changes.
     */
    public interface AttendeeCountListener {
        void onAttendeeCountChanged(int newCount, boolean userHasRsvpd);
    }

    /**
     * Constructor initializes Firebase references and retrieves current user ID.
     */
    public RSVPManager() { //
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * Toggles RSVP status for the current user on a given event.
     * If the user has RSVP'd -> remove RSVP.
     * If the user hasn't RSVP'd -> add RSVP.
     * @param eventId The ID of the event to toggle RSVP for.
     * @param callback The callback to handle success or failure.
     */
    public void toggleRSVP(String eventId, RSVPCallback callback) {
        // Ensure user is logged in
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }
        DatabaseReference eventRef = eventsRef.child(eventId);
        // Get current event data once to determine RSVP state
        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Event event = snapshot.getValue(Event.class);
                    if (event != null) {
                        boolean currentlyRsvpd = event.hasUserRsvpd(currentUserId);

                        // Toggle RSVP in the event object
                        if (currentlyRsvpd) {
                            event.removeRsvp(currentUserId); // User already RSVP'd → un-RSVP
                        } else {
                            event.addRsvp(currentUserId);    // User not RSVP'd → RSVP
                        }

                        // Save updated event back to Firebase
                        eventRef.setValue(event)
                                .addOnSuccessListener(aVoid -> {
                                    // Pass updated RSVP state and attendee count
                                    callback.onSuccess(!currentlyRsvpd, event.getAttendeeCount());
                                })
                                .addOnFailureListener(e -> {
                                    callback.onError("Failed to update RSVP: " + e.getMessage());
                                });
                    } else {
                        callback.onError("Event data not found");
                    }
                } else {
                    callback.onError("Event not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Adds a real-time listener for changes to an event's attendee count.
     * This updates UI instantly when other users RSVP/un-RSVP.
     *
     * @param eventId The ID of the event to listen for.
     * @param listener Callback to receive attendee count and user RSVP state.
     * @return The ValueEventListener to allow later removal.
     */
    public ValueEventListener listenToAttendeeCount(String eventId, AttendeeCountListener listener) {
        DatabaseReference eventRef = eventsRef.child(eventId);

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Event event = snapshot.getValue(Event.class);
                    if (event != null) {
                        // Check if current user has RSVP'd
                        boolean userHasRsvpd = currentUserId != null && event.hasUserRsvpd(currentUserId);
                        // Notify callback with current attendee count and user state
                        listener.onAttendeeCountChanged(event.getAttendeeCount(), userHasRsvpd);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Listener error is usually ignored unless debugging
            }
        };

        // Attach the listener to Firebase
        eventRef.addValueEventListener(valueEventListener);
        return valueEventListener;
    }


    public void removeListener(String eventId, ValueEventListener listener) { // Detaches a real-time listener to avoid memory leaks when activity is destroyed.
        if (listener != null) {
            eventsRef.child(eventId).removeEventListener(listener);
        }
    }


    public void checkRSVPStatus(String eventId, RSVPCallback callback) {
        if (currentUserId == null) { // Checks if the current user has RSVP'd for an event (without toggling)
            callback.onError("User not authenticated");
            return;
        }

        eventsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Event event = snapshot.getValue(Event.class);
                    if (event != null) {
                        boolean hasRsvpd = event.hasUserRsvpd(currentUserId);
                        callback.onSuccess(hasRsvpd, event.getAttendeeCount());
                    } else {
                        callback.onError("Event data not found");
                    }
                } else {
                    callback.onError("Event not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Database error: " + error.getMessage());
            }
        });
    }
}
