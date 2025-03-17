package com.atakmap.android.contacts.plugin;

import java.io.File;
import android.content.Context;
import android.util.Log;

/**
 * Boilerplate code for loading native.
 */
public class ContactsNativeLoader {

    private static final String TAG = "ContactsNativeLoader";
    private static String ndl = null;

    /**
    * If a plugin wishes to make use of this class, they will need to copy it into their plugin.
    * The classloader that loads this class is a key component of getting System.load to work 
    * properly.   If it is desirable to use this in a plugin, it will need to be a direct copy in a
    * non-conflicting package name.
    */
    synchronized static public void init(final Context context) {
        Log.d(TAG, "Initializing ContactsNativeLoader");
        if (ndl == null) {
            try {
                ndl = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(),
                                0).nativeLibraryDir;
                Log.d(TAG, "Native library dir: " + ndl);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing native library directory", e);
                throw new IllegalArgumentException(
                        "native library loading will fail, unable to grab the nativeLibraryDir from the package name");
            }

        }
    }

    /**
    * Security guidance from our recent audit:
    * Pass an absolute path to System.load(). Avoid System.loadLibrary() because its behavior 
    * depends upon its implementation which often relies on environmental features that can be 
    * manipulated. Use only validated, sanitized absolute paths.
    */

    public static void loadLibrary(final String name) {
        if (ndl != null) {
            final String lib = ndl + File.separator
                    + System.mapLibraryName(name);
            Log.d(TAG, "Loading library: " + lib);
            if (new File(lib).exists()) {
                try {
                    System.load(lib);
                    Log.d(TAG, "Library loaded successfully: " + lib);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading library: " + lib, e);
                    throw e;
                }
            } else {
                Log.w(TAG, "Library file does not exist: " + lib);
            }
        } else {
            Log.e(TAG, "NativeLoader not initialized");
            throw new IllegalArgumentException("NativeLoader not initialized");
        }
    }
} 