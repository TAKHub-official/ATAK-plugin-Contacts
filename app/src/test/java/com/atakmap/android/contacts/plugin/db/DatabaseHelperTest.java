package com.atakmap.android.contacts.plugin.db;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.atakmap.android.contacts.plugin.model.Contact;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class DatabaseHelperTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SQLiteDatabase mockDatabase;
    
    @Mock
    private Cursor mockCursor;
    
    private DatabaseHelper databaseHelper;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock database path
        when(mockContext.getDatabasePath(anyString())).thenReturn(mock(java.io.File.class));
        
        // Use getInstance instead of constructor directly
        databaseHelper = spy(DatabaseHelper.getInstance(mockContext));
        
        // Mock getWritableDatabase and getReadableDatabase
        doReturn(mockDatabase).when(databaseHelper).getWritableDatabase();
        doReturn(mockDatabase).when(databaseHelper).getReadableDatabase();
    }
    
    @Test
    public void testAddContact() {
        // Create a test contact
        Contact contact = new Contact("Test Name", "123-456-7890", "Test Notes");
        contact.setLocation(37.7749, -122.4194);
        
        // Mock database insert
        when(mockDatabase.insert(anyString(), isNull(), any(ContentValues.class))).thenReturn(1L);
        
        // Call addContact
        long id = databaseHelper.addContact(contact);
        
        // Verify that insert was called
        verify(mockDatabase).insert(eq(DatabaseHelper.TABLE_CONTACTS), isNull(), any(ContentValues.class));
        
        // Verify that the correct ID was returned
        assertEquals(1L, id);
        
        // Capture the ContentValues to verify the data
        ArgumentCaptor<ContentValues> valuesCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mockDatabase).insert(eq(DatabaseHelper.TABLE_CONTACTS), isNull(), valuesCaptor.capture());
        
        ContentValues capturedValues = valuesCaptor.getValue();
        assertEquals("Test Name", capturedValues.getAsString(DatabaseHelper.KEY_NAME));
        assertEquals("123-456-7890", capturedValues.getAsString(DatabaseHelper.KEY_PHONE));
        assertEquals("Test Notes", capturedValues.getAsString(DatabaseHelper.KEY_NOTES));
        assertEquals(37.7749, capturedValues.getAsDouble(DatabaseHelper.KEY_LATITUDE), 0.0001);
        assertEquals(-122.4194, capturedValues.getAsDouble(DatabaseHelper.KEY_LONGITUDE), 0.0001);
    }
    
    @Test
    public void testGetContact() {
        // Mock cursor for query
        when(mockDatabase.query(anyString(), any(), anyString(), any(), isNull(), isNull(), isNull())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_ID)).thenReturn(0);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_NAME)).thenReturn(1);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_PHONE)).thenReturn(2);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_NOTES)).thenReturn(3);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_LATITUDE)).thenReturn(4);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_LONGITUDE)).thenReturn(5);
        when(mockCursor.getLong(0)).thenReturn(1L);
        when(mockCursor.getString(1)).thenReturn("Test Name");
        when(mockCursor.getString(2)).thenReturn("123-456-7890");
        when(mockCursor.getString(3)).thenReturn("Test Notes");
        when(mockCursor.isNull(4)).thenReturn(false);
        when(mockCursor.isNull(5)).thenReturn(false);
        when(mockCursor.getDouble(4)).thenReturn(37.7749);
        when(mockCursor.getDouble(5)).thenReturn(-122.4194);
        
        // Call getContact
        Contact contact = databaseHelper.getContact(1);
        
        // Verify that query was called
        verify(mockDatabase).query(eq(DatabaseHelper.TABLE_CONTACTS), any(), eq(DatabaseHelper.KEY_ID + "=?"), 
                eq(new String[]{"1"}), isNull(), isNull(), isNull());
        
        // Verify that cursor was closed
        verify(mockCursor).close();
        
        // Verify contact data
        assertNotNull(contact);
        assertEquals(1L, contact.getId());
        assertEquals("Test Name", contact.getName());
        assertEquals("123-456-7890", contact.getPhoneNumber());
        assertEquals("Test Notes", contact.getNotes());
        assertEquals(37.7749, contact.getLatitude(), 0.0001);
        assertEquals(-122.4194, contact.getLongitude(), 0.0001);
        assertTrue(contact.hasLocation());
    }
    
    @Test
    public void testUpdateContact() {
        // Create a test contact
        Contact contact = new Contact(1, "Updated Name", "987-654-3210", "Updated Notes", 34.0522, -118.2437);
        
        // Mock database update
        when(mockDatabase.update(anyString(), any(ContentValues.class), anyString(), any())).thenReturn(1);
        
        // Call updateContact
        int result = databaseHelper.updateContact(contact);
        
        // Verify that update was called
        verify(mockDatabase).update(eq(DatabaseHelper.TABLE_CONTACTS), any(ContentValues.class), 
                eq(DatabaseHelper.KEY_ID + "=?"), eq(new String[]{String.valueOf(contact.getId())}));
        
        // Verify that the correct result was returned
        assertEquals(1, result);
        
        // Capture the ContentValues to verify the data
        ArgumentCaptor<ContentValues> valuesCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(mockDatabase).update(eq(DatabaseHelper.TABLE_CONTACTS), valuesCaptor.capture(), 
                eq(DatabaseHelper.KEY_ID + "=?"), eq(new String[]{String.valueOf(contact.getId())}));
        
        ContentValues capturedValues = valuesCaptor.getValue();
        assertEquals("Updated Name", capturedValues.getAsString(DatabaseHelper.KEY_NAME));
        assertEquals("987-654-3210", capturedValues.getAsString(DatabaseHelper.KEY_PHONE));
        assertEquals("Updated Notes", capturedValues.getAsString(DatabaseHelper.KEY_NOTES));
        assertEquals(34.0522, capturedValues.getAsDouble(DatabaseHelper.KEY_LATITUDE), 0.0001);
        assertEquals(-118.2437, capturedValues.getAsDouble(DatabaseHelper.KEY_LONGITUDE), 0.0001);
    }
    
    @Test
    public void testDeleteContact() {
        // Create a test contact
        Contact contact = new Contact(1, "Test Name", "123-456-7890", "Test Notes");
        
        // Mock database delete
        when(mockDatabase.delete(anyString(), anyString(), any())).thenReturn(1);
        
        // Call deleteContact
        int result = databaseHelper.deleteContact(contact);
        
        // Verify that delete was called
        verify(mockDatabase).delete(eq(DatabaseHelper.TABLE_CONTACTS), eq(DatabaseHelper.KEY_ID + "=?"), 
                eq(new String[]{String.valueOf(contact.getId())}));
        
        // Verify that the correct result was returned
        assertEquals(1, result);
    }
    
    @Test
    public void testGetAllContacts() {
        // Mock cursor for query
        when(mockDatabase.query(anyString(), any(), isNull(), isNull(), isNull(), isNull(), anyString())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(true).thenReturn(false); // Two contacts
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_ID)).thenReturn(0);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_NAME)).thenReturn(1);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_PHONE)).thenReturn(2);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_NOTES)).thenReturn(3);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_LATITUDE)).thenReturn(4);
        when(mockCursor.getColumnIndex(DatabaseHelper.KEY_LONGITUDE)).thenReturn(5);
        when(mockCursor.getLong(0)).thenReturn(1L).thenReturn(2L);
        when(mockCursor.getString(1)).thenReturn("Test Name 1").thenReturn("Test Name 2");
        when(mockCursor.getString(2)).thenReturn("123-456-7890").thenReturn("987-654-3210");
        when(mockCursor.getString(3)).thenReturn("Test Notes 1").thenReturn("Test Notes 2");
        when(mockCursor.isNull(4)).thenReturn(false);
        when(mockCursor.isNull(5)).thenReturn(false);
        when(mockCursor.getDouble(4)).thenReturn(37.7749).thenReturn(34.0522);
        when(mockCursor.getDouble(5)).thenReturn(-122.4194).thenReturn(-118.2437);
        
        // Call getAllContacts
        List<Contact> contacts = databaseHelper.getAllContacts();
        
        // Verify that query was called
        verify(mockDatabase).query(eq(DatabaseHelper.TABLE_CONTACTS), any(), isNull(), isNull(), 
                isNull(), isNull(), anyString());
        
        // Verify that cursor was closed
        verify(mockCursor).close();
        
        // Verify contacts data
        assertNotNull(contacts);
        assertEquals(2, contacts.size());
        
        Contact contact1 = contacts.get(0);
        assertEquals(1L, contact1.getId());
        assertEquals("Test Name 1", contact1.getName());
        assertEquals("123-456-7890", contact1.getPhoneNumber());
        assertEquals("Test Notes 1", contact1.getNotes());
        assertEquals(37.7749, contact1.getLatitude(), 0.0001);
        assertEquals(-122.4194, contact1.getLongitude(), 0.0001);
        assertTrue(contact1.hasLocation());
        
        Contact contact2 = contacts.get(1);
        assertEquals(2L, contact2.getId());
        assertEquals("Test Name 2", contact2.getName());
        assertEquals("987-654-3210", contact2.getPhoneNumber());
        assertEquals("Test Notes 2", contact2.getNotes());
        assertEquals(34.0522, contact2.getLatitude(), 0.0001);
        assertEquals(-118.2437, contact2.getLongitude(), 0.0001);
        assertTrue(contact2.hasLocation());
    }
} 