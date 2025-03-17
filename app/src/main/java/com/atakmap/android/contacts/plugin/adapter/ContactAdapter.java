package com.atakmap.android.contacts.plugin.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.contacts.plugin.R;
import com.atakmap.android.contacts.plugin.model.Contact;
// Comment out error handler imports for debugging
// import com.atakmap.android.contacts.plugin.util.ErrorHandler;
// import com.atakmap.android.contacts.plugin.util.UIErrorHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying contacts in a RecyclerView
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    
    private static final String TAG = "ContactAdapter";
    private final List<Contact> contactList;
    private List<Contact> contactListFull; // Original list for filtering
    private final Context context;
    private final OnContactClickListener listener;
    // Comment out error handlers for debugging
    // private ErrorHandler errorHandler;
    // private UIErrorHandler uiErrorHandler;
    
    /**
     * Interface for click events on contacts
     */
    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }
    
    /**
     * ViewHolder for a contact entry
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView phoneTextView;
        
        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_contact_name);
            phoneTextView = itemView.findViewById(R.id.tv_contact_phone);
        }
        
        public void bind(final Contact contact, final OnContactClickListener listener) {
            try {
                if (contact != null) {
                    nameTextView.setText(contact.getName());
                    phoneTextView.setText(contact.getPhoneNumber());
                    
                    // Click listener for the entire entry
                    itemView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onContactClick(contact);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding contact to view: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Constructor
     * @param context Context
     * @param contactList List of contacts to display
     * @param listener Click listener
     */
    public ContactAdapter(Context context, List<Contact> contactList, OnContactClickListener listener) {
        Log.d(TAG, "ContactAdapter constructor started");
        this.context = context;
        this.contactList = contactList;
        this.contactListFull = new ArrayList<>(contactList); // Create a copy for filtering
        this.listener = listener;
        
        // Initialize error handlers - commented out for debugging
        // this.errorHandler = ErrorHandler.getInstance(context);
        // this.uiErrorHandler = UIErrorHandler.getInstance(context);
        Log.d(TAG, "ContactAdapter constructor completed");
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            Log.d(TAG, "onCreateViewHolder called");
            // Use standard inflate instead of UIErrorHandler
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
            return new ViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating ViewHolder: " + e.getMessage(), e);
            
            // Fallback for unexpected case
            View errorView = new View(parent.getContext());
            return new ViewHolder(errorView);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Log.d(TAG, "onBindViewHolder called for position " + position);
            if (position < contactList.size()) {
                Contact contact = contactList.get(position);
                holder.bind(contact, listener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding ViewHolder at position " + position + ": " + e.getMessage(), e);
        }
    }
    
    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }
    
    /**
     * Updates the contact list
     * @param newContacts New contact list
     */
    public void updateContacts(List<Contact> newContacts) {
        try {
            Log.d(TAG, "Updating contacts list with " + 
                 (newContacts != null ? newContacts.size() : 0) + " contacts");
            
            // Clear both lists
            contactList.clear();
            
            if (newContacts != null) {
                contactList.addAll(newContacts);
                
                // Create a deep copy of the contacts list
                contactListFull = new ArrayList<>(newContacts);
                
                Log.d(TAG, "Updated contactList with " + contactList.size() + 
                     " items and contactListFull with " + contactListFull.size() + " items");
            } else {
                Log.d(TAG, "No contacts to update with, lists are now empty");
                contactListFull = new ArrayList<>(); // Create empty list
            }
            
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error updating contacts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Filters the contact list based on the search term
     * @param query Search term
     */
    public void filter(String query) {
        try {
            Log.d(TAG, "Filtering contacts with query: '" + query + "', contactList size: " + 
                 (contactList != null ? contactList.size() : 0) + 
                 ", full list size: " + (contactListFull != null ? contactListFull.size() : 0));
                
            // Make sure we have the full list to filter from
            if (contactListFull == null) {
                Log.d(TAG, "contactListFull is null, creating new empty list");
                contactListFull = new ArrayList<>();
            }
            
            // Clear the current display list
            contactList.clear();
            
            if (query == null || query.isEmpty()) {
                // If search query is empty, show all contacts
                contactList.addAll(contactListFull);
                Log.d(TAG, "Search cleared, showing all " + contactList.size() + " contacts");
            } else {
                // Convert query to lowercase for case-insensitive search
                String lowerCaseQuery = query.toLowerCase();
                
                // Filter contacts by name only (simplified)
                for (Contact contact : contactListFull) {
                    if (contact != null && contact.getName() != null && 
                        contact.getName().toLowerCase().contains(lowerCaseQuery)) {
                        contactList.add(contact);
                        Log.d(TAG, "Adding contact to filtered results: " + contact.getName());
                    }
                }
                
                Log.d(TAG, "Filtered contacts list, showing " + contactList.size() + 
                       " results for query: " + query);
            }
            
            // Always notify adapter that data has changed
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error filtering contacts: " + e.getMessage(), e);
        }
    }
} 