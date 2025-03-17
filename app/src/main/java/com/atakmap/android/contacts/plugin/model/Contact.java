package com.atakmap.android.contacts.plugin.model;

/**
 * Data model for a contact
 */
public class Contact {
    private long id;
    private String name;
    private String phoneNumber;
    private String notes;
    // New fields for location information
    private Double latitude;
    private Double longitude;
    private boolean hasLocation;

    // Default constructor
    public Contact() {
        this.hasLocation = false;
    }

    // Constructor for new contacts (without ID)
    public Contact(String name, String phoneNumber, String notes) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.hasLocation = false;
    }

    // Constructor for new contacts with location
    public Contact(String name, String phoneNumber, String notes, Double latitude, Double longitude) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasLocation = (latitude != null && longitude != null);
    }

    // Complete constructor
    public Contact(long id, String name, String phoneNumber, String notes) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.hasLocation = false;
    }

    // Complete constructor with location
    public Contact(long id, String name, String phoneNumber, String notes, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasLocation = (latitude != null && longitude != null);
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // New getters and setters for location information
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
        updateHasLocation();
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
        updateHasLocation();
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    // Helper method to update hasLocation
    private void updateHasLocation() {
        this.hasLocation = (latitude != null && longitude != null);
    }

    // Method to set location
    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        updateHasLocation();
    }

    // Method to clear location
    public void clearLocation() {
        this.latitude = null;
        this.longitude = null;
        this.hasLocation = false;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", notes='" + notes + '\'' +
                ", hasLocation=" + hasLocation +
                (hasLocation ? ", latitude=" + latitude + ", longitude=" + longitude : "") +
                '}';
    }
} 