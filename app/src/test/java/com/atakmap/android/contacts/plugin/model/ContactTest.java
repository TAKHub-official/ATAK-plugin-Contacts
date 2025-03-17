package com.atakmap.android.contacts.plugin.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ContactTest {
    
    private Contact contact;
    private static final long TEST_ID = 1L;
    private static final String TEST_NAME = "Test Contact";
    private static final String TEST_PHONE = "123-456-7890";
    private static final String TEST_NOTES = "Test notes";
    private static final Double TEST_LATITUDE = 37.7749;
    private static final Double TEST_LONGITUDE = -122.4194;
    
    @Before
    public void setUp() {
        // Create a contact with all fields for testing
        contact = new Contact(TEST_ID, TEST_NAME, TEST_PHONE, TEST_NOTES, TEST_LATITUDE, TEST_LONGITUDE);
    }
    
    @Test
    public void testContactCreation() {
        // Test that all fields are correctly set
        assertEquals(TEST_ID, contact.getId());
        assertEquals(TEST_NAME, contact.getName());
        assertEquals(TEST_PHONE, contact.getPhoneNumber());
        assertEquals(TEST_NOTES, contact.getNotes());
        assertEquals(TEST_LATITUDE, contact.getLatitude());
        assertEquals(TEST_LONGITUDE, contact.getLongitude());
        assertTrue(contact.hasLocation());
    }
    
    @Test
    public void testContactCreationWithoutLocation() {
        // Create a contact without location
        Contact contactNoLocation = new Contact(TEST_ID, TEST_NAME, TEST_PHONE, TEST_NOTES);
        
        // Test that location fields are null
        assertNull(contactNoLocation.getLatitude());
        assertNull(contactNoLocation.getLongitude());
        assertFalse(contactNoLocation.hasLocation());
    }
    
    @Test
    public void testSetLocation() {
        // Create a contact without location
        Contact contactNoLocation = new Contact(TEST_ID, TEST_NAME, TEST_PHONE, TEST_NOTES);
        
        // Set location
        contactNoLocation.setLocation(TEST_LATITUDE, TEST_LONGITUDE);
        
        // Test that location fields are set
        assertEquals(TEST_LATITUDE, contactNoLocation.getLatitude());
        assertEquals(TEST_LONGITUDE, contactNoLocation.getLongitude());
        assertTrue(contactNoLocation.hasLocation());
    }
    
    @Test
    public void testClearLocation() {
        // Test clearing location
        contact.clearLocation();
        
        // Test that location fields are null
        assertNull(contact.getLatitude());
        assertNull(contact.getLongitude());
        assertFalse(contact.hasLocation());
    }
    
    @Test
    public void testUpdateHasLocation() {
        // Test that hasLocation is updated when latitude is set to null
        contact.setLatitude(null);
        assertFalse(contact.hasLocation());
        
        // Test that hasLocation is updated when both coordinates are set
        contact.setLatitude(TEST_LATITUDE);
        contact.setLongitude(TEST_LONGITUDE);
        assertTrue(contact.hasLocation());
        
        // Test that hasLocation is updated when longitude is set to null
        contact.setLongitude(null);
        assertFalse(contact.hasLocation());
    }
    
    @Test
    public void testToString() {
        // Test that toString contains all fields
        String toString = contact.toString();
        assertTrue(toString.contains(String.valueOf(TEST_ID)));
        assertTrue(toString.contains(TEST_NAME));
        assertTrue(toString.contains(TEST_PHONE));
        assertTrue(toString.contains(TEST_NOTES));
        assertTrue(toString.contains(String.valueOf(TEST_LATITUDE)));
        assertTrue(toString.contains(String.valueOf(TEST_LONGITUDE)));
    }
} 