package com.atakmap.android.contacts.plugin.model;

/**
 * Datenmodell für einen Kontakt
 */
public class Contact {
    private long id;
    private String name;
    private String phoneNumber;
    private String notes;
    // Neue Felder für Standortinformationen
    private Double latitude;
    private Double longitude;
    private boolean hasLocation;

    // Standard-Konstruktor
    public Contact() {
        this.hasLocation = false;
    }

    // Konstruktor für neue Kontakte (ohne ID)
    public Contact(String name, String phoneNumber, String notes) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.hasLocation = false;
    }

    // Konstruktor für neue Kontakte mit Standort
    public Contact(String name, String phoneNumber, String notes, Double latitude, Double longitude) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasLocation = (latitude != null && longitude != null);
    }

    // Vollständiger Konstruktor
    public Contact(long id, String name, String phoneNumber, String notes) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.hasLocation = false;
    }

    // Vollständiger Konstruktor mit Standort
    public Contact(long id, String name, String phoneNumber, String notes, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasLocation = (latitude != null && longitude != null);
    }

    // Getter und Setter
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

    // Neue Getter und Setter für Standortinformationen
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

    // Hilfsmethode, um hasLocation zu aktualisieren
    private void updateHasLocation() {
        this.hasLocation = (latitude != null && longitude != null);
    }

    // Methode zum Setzen des Standorts
    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        updateHasLocation();
    }

    // Methode zum Löschen des Standorts
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