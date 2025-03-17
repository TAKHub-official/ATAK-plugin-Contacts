package com.atakmap.android.contacts.plugin.adapter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.atakmap.android.contacts.plugin.R;
import com.atakmap.android.contacts.plugin.model.Contact;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ContactAdapterTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private ContactAdapter.OnContactClickListener mockListener;
    
    @Mock
    private View mockItemView;
    
    @Mock
    private TextView mockNameTextView;
    
    @Mock
    private TextView mockPhoneTextView;
    
    private ContactAdapter adapter;
    private List<Contact> contactList;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test contacts
        contactList = new ArrayList<>();
        contactList.add(new Contact(1, "John Doe", "123-456-7890", "Test notes"));
        contactList.add(new Contact(2, "Jane Smith", "987-654-3210", "Another note"));
        
        // Create adapter
        adapter = new ContactAdapter(mockContext, contactList, mockListener);
        
        // Mock findViewById for ViewHolder
        when(mockItemView.findViewById(R.id.tv_contact_name)).thenReturn(mockNameTextView);
        when(mockItemView.findViewById(R.id.tv_contact_phone)).thenReturn(mockPhoneTextView);
    }
    
    @Test
    public void testGetItemCount() {
        // Test that getItemCount returns the correct number of items
        assertEquals(2, adapter.getItemCount());
        
        // Test with empty list
        adapter.updateContacts(new ArrayList<>());
        assertEquals(0, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateContacts() {
        // Create new list of contacts
        List<Contact> newContacts = new ArrayList<>();
        newContacts.add(new Contact(3, "New Contact", "555-555-5555", "New note"));
        
        // Update adapter with new contacts
        adapter.updateContacts(newContacts);
        
        // Test that getItemCount returns the correct number of items
        assertEquals(1, adapter.getItemCount());
    }
    
    @Test
    public void testFilter() {
        // Test filtering by name
        adapter.filter("John");
        assertEquals(1, adapter.getItemCount());
        
        // Test filtering with no matches
        adapter.filter("XYZ");
        assertEquals(0, adapter.getItemCount());
        
        // Test clearing filter
        adapter.filter("");
        assertEquals(2, adapter.getItemCount());
    }
    
    @Test
    public void testBindViewHolder() {
        // Create ViewHolder
        ContactAdapter.ViewHolder viewHolder = new ContactAdapter.ViewHolder(mockItemView);
        
        // Bind contact to ViewHolder
        viewHolder.bind(contactList.get(0), mockListener);
        
        // Verify that setText was called with correct values
        verify(mockNameTextView).setText("John Doe");
        verify(mockPhoneTextView).setText("123-456-7890");
        
        // Verify that setOnClickListener was called
        verify(mockItemView).setOnClickListener(any());
    }
    
    @Test
    public void testClickListener() {
        // Create ViewHolder
        ContactAdapter.ViewHolder viewHolder = new ContactAdapter.ViewHolder(mockItemView);
        
        // Bind contact to ViewHolder
        viewHolder.bind(contactList.get(0), mockListener);
        
        // Capture click listener
        View.OnClickListener clickListener = null;
        verify(mockItemView).setOnClickListener(argThat(listener -> {
            // Simulate click
            listener.onClick(mockItemView);
            // Verify that listener was called with correct contact
            verify(mockListener).onContactClick(contactList.get(0));
            return true;
        }));
    }
} 