package com.example.peekeventproject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * RecyclerView Adapter to display a list of Event objects in card format.
 * Uses ViewHolder pattern for performance.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<Event> eventList;
    private final Context context;          // Needed to start new activities
    private final boolean showEditButton;

    /**
     * Constructor
     * @param context context from the calling Activity or Fragment
     * @param events list of Event objects to display
     * @param showEditButton whether edit button should be visible
     */
    public EventAdapter(Context context, List<Event> events, boolean showEditButton) {
        this.context = context;  //
        this.eventList = events;
        this.showEditButton = showEditButton;
    }

    /**
     * ViewHolder holds references to each view inside an item card layout.
     * Improves performance by avoiding repeated findViewById calls.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, location, zone, editButton, shareButton;
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.event_title);
            date = itemView.findViewById(R.id.event_date);
            location = itemView.findViewById(R.id.event_location);
            zone = itemView.findViewById(R.id.event_zone);
            image = itemView.findViewById(R.id.eventImageView);
            editButton = itemView.findViewById(R.id.event_edit);
            shareButton = itemView.findViewById(R.id.event_share);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate a single event card layout for each item in the RecyclerView
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(v);
    }

    //Each event data is bound to a view
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        if (event == null) return; // Safety check

        // Populate event title (fallback if null)
        holder.title.setText(event.getTitle() != null ? event.getTitle() : "No Title");

        // Display date and time together (with fallbacks)
        holder.date.setText((event.getDate() != null ? event.getDate() : "No Date")
                + " | " + (event.getTime() != null ? event.getTime() : "No Time"));

        // Populate location and zone
        holder.location.setText(event.getLocation() != null ? event.getLocation() : "No Location");
        holder.zone.setText(event.getZone() != null ? "Tukutane Zone: " + event.getZone() : "Tukutane Zone: Not Set");

        // Load event image using Glide (with placeholder and error fallback)
        if (holder.image != null) {
            String imageUrl = event.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery) // temporary image while loading
                        .error(android.R.drawable.ic_menu_gallery)       // fallback if load fails
                        .into(holder.image);
            } else {
                // Show default placeholder if no image URL is available
                holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // Open detailed event view when the whole card is clicked
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailActivity.class);
            intent.putExtra("event", event); // Pass entire event object (Serializable)
            context.startActivity(intent);
        });

        // Show or hide edit button depending on the flag
        if (showEditButton) {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.editButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditEventActivity.class);
                intent.putExtra("event", event);
                context.startActivity(intent);
            });
        } else {
            holder.editButton.setVisibility(View.GONE);
        }

        // Share button - allows sharing event details using any app
        holder.shareButton.setOnClickListener(v -> {
            String shareText = "Check out this event: " + event.getTitle() + "\n" +
                    "Date: " + event.getDate() + " at " + event.getTime() + "\n" +
                    "Location: " + event.getLocation() + "\n" +
                    "Zone: " + event.getZone();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(Intent.createChooser(shareIntent, "Share Event via"));
        });
    }

    @Override
    public int getItemCount() {
        // Return total number of events (safe against null list)
        return eventList != null ? eventList.size() : 0;
    }

    /**
     * Allows dynamic refresh of RecyclerView data
     * @param newEventList updated list of events
     */
    public void updateData(List<Event> newEventList) {
        this.eventList = newEventList;
        notifyDataSetChanged(); // Refresh entire list
    }
}
