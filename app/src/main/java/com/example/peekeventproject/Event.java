package com.example.peekeventproject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**

 * Implements Serializable so it can be passed between activities using Intents.
 */
public class Event implements Serializable {

    // Core event fields
    private String eventId;        // Unique ID for the event (used as Firebase key)
    private String title;          // Event title
    private String category;       // Category (e.g., Music, Tech, Food)
    private String description;    // Detailed event description
    private String date;           // Date of the event
    private String time;           // Time of the event (can be a range)
    private String location;       // Venue or address
    private String zone;           // Additional location detail (e.g., Tukatune Zone)
    private int attendeeCount;     // Current number of attendees (RSVP count)
    private String imageUrl;       // Download URL for event image stored in Firebase Storage
    private String creatorId;      // ID of the user who created the event
    private Map<String, Boolean> rsvpList; // Maps userId → true if RSVP’d

    /**
     * Default constructor required by Firebase
     * Initializes RSVP list to prevent null pointer exceptions.
     */
    public Event() {
        this.rsvpList = new HashMap<>();
    }

    /**
     * Main constructor used when creating a new event manually.
     *
     */
    public Event(String eventId, String title, String category, String description,
                 String date, String time, String location, String zone, int attendeeCount) {
        this.eventId = eventId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.date = date;
        this.time = time;
        this.location = location;
        this.zone = zone;
        this.attendeeCount = attendeeCount;
        this.imageUrl = null;              // Image is optional, can be set later
        this.rsvpList = new HashMap<>();   // Start with empty RSVP list
    }

    // ---------------------------
    // Getters for all fields
    // ---------------------------
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getLocation() { return location; }
    public String getZone() { return zone; }
    public int getAttendeeCount() { return attendeeCount; }
    public String getImageUrl() { return imageUrl; }
    public String getCreatorId() { return creatorId; }

    /**
     * Returns RSVP list safely (never null).
     */
    public Map<String, Boolean> getRsvpList() {
        return rsvpList != null ? rsvpList : new HashMap<>();
    }

    // ---------------------------
    // Setters for all fields
    // ---------------------------
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setLocation(String location) { this.location = location; }
    public void setZone(String zone) { this.zone = zone; }
    public void setAttendeeCount(int attendeeCount) { this.attendeeCount = attendeeCount; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    /**
     * Sets RSVP list safely (replaces null with an empty list).
     */
    public void setRsvpList(Map<String, Boolean> rsvpList) {
        this.rsvpList = rsvpList != null ? rsvpList : new HashMap<>();
    }

    // ---------------------------
    // RSVP utility methods
    // ---------------------------

    /**
     * Checks if a specific user has RSVP’d to this event.
     * @param userId ID of the user
     * @return true if the user RSVP’d, false otherwise
     */
    public boolean hasUserRsvpd(String userId) {
        return rsvpList != null && rsvpList.containsKey(userId) && rsvpList.get(userId);
    }

    /**
     * Adds an RSVP for a user.
     * If user hasn't RSVP’d before, mark them as attending and increase attendee count.
     * @param userId ID of the user
     */
    public void addRsvp(String userId) {
        if (rsvpList == null) {
            rsvpList = new HashMap<>();
        }
        if (!rsvpList.containsKey(userId) || !rsvpList.get(userId)) {
            rsvpList.put(userId, true);
            attendeeCount++; // Increment count when new RSVP is added
        }
    }

    /**
     * Removes RSVP for a user.
     * Marks user as not attending and decreases attendee count (cannot go below zero).
     * @param userId ID of the user
     */
    public void removeRsvp(String userId) {
        if (rsvpList != null && rsvpList.containsKey(userId) && rsvpList.get(userId)) {
            rsvpList.put(userId, false);
            attendeeCount = Math.max(0, attendeeCount - 1); // Prevent negative count
        }
    }
}
