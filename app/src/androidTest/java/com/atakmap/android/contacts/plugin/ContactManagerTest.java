package com.atakmap.android.contacts.plugin;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.atakmap.android.contacts.plugin.adapter.ContactAdapter;
import com.atakmap.android.contacts.plugin.db.DatabaseHelper;
import com.atakmap.android.contacts.plugin.model.Contact;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ContactManagerTest {
    
    private Context context;
    
    @Mock
    private View mockMainView;
    
    @Mock
    private RecyclerView mockRecyclerView;
    
    @Mock
    private TextView mockEmptyView;
    
    @Mock
    private EditText mockSearchEditText;
    
    @Mock
    private Button mockAddButton;
    
    @Mock
    private DatabaseHelper mockDatabaseHelper;
    
    @Mock
    private ContactAdapter mockAdapter;
    
    private ContactManager contactManager;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Get the context from instrumentation
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Mock findViewById calls
        when(mockMainView.findViewById(R.id.rv_contacts)).thenReturn(mockRecyclerView);
        when(mockMainView.findViewById(R.id.tv_empty_view)).thenReturn(mockEmptyView);
        when(mockMainView.findViewById(R.id.et_search_contacts)).thenReturn(mockSearchEditText);
        when(mockMainView.findViewById(R.id.btn_add_contact)).thenReturn(mockAddButton);
        
        // Create a test instance with mocked dependencies
        contactManager = new ContactManager(context, mockMainView) {
            @Override
            protected void setupViews() {
                // Override to avoid real setup
            }
            
            @Override
            public void loadContacts() {
                // Override to avoid real loading
            }
        };
        
        // Set mocked adapter
        contactManager.setAdapter(mockAdapter);
        
        // Set mocked database helper
        contactManager.setDatabaseHelper(mockDatabaseHelper);
    }
    
    @Test
    public void testLoadContacts() {
        // Create test contacts
        List<Contact> testContacts = new ArrayList<>();
        testContacts.add(new Contact(1, "John Doe", "123-456-7890", "Test notes"));
        testContacts.add(new Contact(2, "Jane Smith", "987-654-3210", "Another note"));
        
        // Mock database helper to return test contacts
        when(mockDatabaseHelper.getAllContacts()).thenReturn(testContacts);
        
        // Call loadContacts
        contactManager.loadContacts();
        
        // Verify that getAllContacts was called
        verify(mockDatabaseHelper).getAllContacts();
        
        // Verify that adapter was updated
        verify(mockAdapter).updateContacts(any());
    }
    
    @Test
    public void testFilterContacts() {
        // Call filterContacts
        contactManager.filterContacts("John");
        
        // Verify that adapter filter was called
        verify(mockAdapter).filter("John");
    }
    
    @Test
    public void testUpdateContactsUI_WithContacts() {
        // Mock adapter to return contacts
        when(mockAdapter.getItemCount()).thenReturn(2);
        
        // Call updateContactsUI
        contactManager.updateContactsUI();
        
        // Verify that recyclerView visibility was set to VISIBLE
        verify(mockRecyclerView).setVisibility(View.VISIBLE);
        
        // Verify that emptyView visibility was set to GONE
        verify(mockEmptyView).setVisibility(View.GONE);
    }
    
    @Test
    public void testUpdateContactsUI_WithoutContacts() {
        // Mock adapter to return no contacts
        when(mockAdapter.getItemCount()).thenReturn(0);
        
        // Call updateContactsUI
        contactManager.updateContactsUI();
        
        // Verify that recyclerView visibility was set to GONE
        verify(mockRecyclerView).setVisibility(View.GONE);
        
        // Verify that emptyView visibility was set to VISIBLE
        verify(mockEmptyView).setVisibility(View.VISIBLE);
        
        // Verify that emptyView text was set
        verify(mockEmptyView).setText("No contacts added yet");
    }
    
    @Test
    public void testReset() {
        // Call reset
        contactManager.reset();
        
        // Verify that loadContacts was called
        verify(mockAdapter).notifyDataSetChanged();
    }
    
    @Test
    public void testOnContactClick() {
        // Create test contact
        Contact testContact = new Contact(1, "John Doe", "123-456-7890", "Test notes");
        
        // Call onContactClick
        contactManager.onContactClick(testContact);
        
        // This is a visual test that would show a dialog, so we can't verify much
        // Just ensure it doesn't crash
    }
} 