# ContactsPlugin for ATAK

Version 1.2.1

_________________________________________________________________
## PURPOSE AND CAPABILITIES

ContactsPlugin is an ATAK plugin that allows users to manage contacts within the ATAK environment. This plugin provides a simple and efficient way to store, view, edit, and delete contact information for team members, points of contact, or any individuals relevant to your mission.

### Key Features:
- Create and store contacts with name, phone number, notes, and location data
- View all contacts in an alphabetically sorted list
- Edit existing contact information
- Delete contacts with confirmation
- Simple and intuitive user interface
- Works in offline environments
- International phone number format support
- Interactive phone numbers for direct calling or copying
- Location data with options to use current location or enter coordinates manually
- Show contact locations on the ATAK map with markers
- Copy coordinates to clipboard for use in other applications
- Version information and publisher details
- Optimized UI layout for maximum contact visibility
- Robust error handling and recovery system

_________________________________________________________________
## STATUS

Released: Version 1.2.1 (March 2025)  
Compatibility: ATAK-CIV 5.3.0.12 and higher

_________________________________________________________________
## INSTALLATION

### Prerequisites
- ATAK-CIV 5.3.0.12 or higher installed on your device
- Storage permissions granted to ATAK
- Location permissions granted to ATAK (for location features)

### Installation Steps
1. Download the ContactsPlugin APK file
2. In ATAK, go to "Settings" > "Tool Preferences" > "Plugin Manager"
3. Tap the "+" button in the bottom right
4. Navigate to the downloaded APK file and select it
5. Follow the on-screen instructions to complete installation
6. Restart ATAK when prompted

### Verification
After restarting ATAK, you should see a "Contacts" icon in the ATAK toolbar. If you don't see it, ensure the plugin is enabled in the Plugin Manager.

_________________________________________________________________
## USAGE GUIDE

### Accessing the Plugin
Tap the "Contacts" icon in the ATAK toolbar to open the Contacts panel.

### Adding a Contact
1. Open the Contacts panel
2. Tap the "Add Contact" button
3. Fill in the required information:
   - Name (required)
   - Country Code (defaults to +49)
   - Phone Number (optional)
   - Notes (optional)
   - Location (optional):
     - Tap "Current Location" to use your device's current location
     - Tap "Enter Coordinates" to manually input latitude and longitude
     - Use "Clear Location" to remove location data
4. Tap "Save"

### Viewing Contact Details
1. Open the Contacts panel
2. Tap on any contact in the list to view details
3. If the contact has location data, it will be displayed in the details view

### Using Phone Numbers
1. **Direct Calling**: Tap on the phone number in the contact details to open your device's dialer app with the number pre-filled
2. **Copy to Clipboard**: Long-press on the phone number to copy it to your clipboard for use in other apps

### Using Location Data
1. **View on Map**: Tap "Show on Map" to center the ATAK map on the contact's location and create a marker
2. **Copy Coordinates**: Tap "Copy Coordinates" to copy the latitude and longitude to your clipboard
3. **Copy Individual Coordinates**: Long-press on either latitude or longitude to copy just that value

### Editing a Contact
1. Open the contact details by tapping on a contact
2. Tap the "Edit" button
3. Modify the information as needed, including location data
4. Tap "Save"

### Deleting a Contact
1. Open the contact details by tapping on a contact
2. Tap the "Delete" button
3. Confirm deletion when prompted

### Searching Contacts
1. Open the Contacts panel
2. Enter text in the search field to filter contacts by name

### Viewing Plugin Information
1. Open the Contacts panel
2. Tap the info button (i) next to the search field
3. View version information and publisher details
4. Tap "Close" to dismiss the dialog

_________________________________________________________________
## TECHNICAL INFORMATION

### Data Storage
- Contact information is stored in a SQLite database on the device
- The database is stored in the app's private storage or external storage if necessary
- No data is transmitted to external servers

### Security
- All data is stored locally on the device
- No network connections required for functionality
- Standard Android security practices implemented

### Permissions
- READ_EXTERNAL_STORAGE: Required for database storage
- WRITE_EXTERNAL_STORAGE: Required for database storage
- ACCESS_FINE_LOCATION: Required for current location functionality
- ACCESS_COARSE_LOCATION: Required for current location functionality

### Versioning
This project uses Semantic Versioning (SemVer):
- MAJOR version for incompatible API changes
- MINOR version for new functionality in a backward compatible manner
- PATCH version for backward compatible bug fixes

_________________________________________________________________
## TROUBLESHOOTING

### Common Issues
- **"Failed to add contact"**: This may occur if storage permissions are not granted. Go to Android Settings > Apps > ATAK > Permissions and ensure storage permissions are granted.
- **Contacts not showing**: Refresh the contacts list by closing and reopening the plugin.
- **Contact list not sorting correctly**: Contact list should sort alphabetically by name. If sorting issues occur, restart the plugin.
- **Phone dialer not opening**: If tapping on a phone number doesn't open the dialer, ensure your device has a phone app installed and set as the default dialer.
- **Current location not working**: Ensure location permissions are granted to ATAK and that your device's location services are enabled.
- **Contacts disappearing after search**: If contacts disappear after searching, try closing and reopening the plugin to refresh the list.

### Reporting Issues
If you encounter issues not covered in this documentation, please report them to the development team.

_________________________________________________________________
## DEVELOPER NOTES

Built with:
- Android SDK
- ATAK Plugin Development Kit
- SQLite for data storage

For developers interested in extending or modifying this plugin, the source code follows a standard Android architecture with:
- Model-View-Controller design pattern
- Singleton database helper
- Transaction-based database operations
- Comprehensive error handling and recovery system
- Detailed logging for debugging and troubleshooting

_________________________________________________________________
## VERSION HISTORY

### Version 1.2.1 (March 2025)
- Implemented robust error handling and recovery system
- Added centralized error management with severity levels
- Improved error logging with detailed information
- Added automatic recovery strategies for common errors
- Enhanced UI error handling for better user experience
- Improved plugin stability and reliability
- Fixed various minor bugs and issues

### Version 1.2.0 (March 2025)
- Added location functionality to store and display contact locations
- Added options to use current device location or enter coordinates manually
- Added ability to show contact locations on the ATAK map with markers
- Added coordinate copying functionality (both individual and combined)
- Improved contact details view with optimized layout
- Enhanced search functionality with better filtering and list restoration
- Fixed issue with contacts disappearing after search
- Added location permissions for accessing device location
- Improved database structure to support location data
- Optimized UI layout for better space utilization
- Improved error handling and logging throughout the application

### Version 1.1.0 (March 2025)
- Added interactive phone numbers for direct calling or copying
- Improved documentation with usage instructions for new features
- Added info button and dialog with version and publisher information
- Optimized UI layout for better space utilization
- Improved button styling for consistency across the application

### Version 1.0.0 (March 2025)
- Initial release
- Core contact management functionality
- Alphabetical sorting of contacts by name
- Storage permission handling
- Improved UI for better visibility and usability
