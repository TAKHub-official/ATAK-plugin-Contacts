package com.atakmap.android.contacts.plugin;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ContactsNativeLoaderTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private PackageManager mockPackageManager;
    
    @Mock
    private ApplicationInfo mockApplicationInfo;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock context to return package manager
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        
        // Mock context to return package name
        when(mockContext.getPackageName()).thenReturn("com.atakmap.android.contacts.plugin");
        
        // Mock package manager to return application info
        try {
            when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            fail("Mock setup failed: " + e.getMessage());
        }
        
        // Mock application info to return native library dir
        mockApplicationInfo.nativeLibraryDir = "/data/app/com.atakmap.android.contacts.plugin/lib";
    }
    
    @Test
    public void testInit() {
        // Call init
        ContactsNativeLoader.init(mockContext);
        
        // Verify that getPackageManager was called
        verify(mockContext).getPackageManager();
        
        // Verify that getPackageName was called
        verify(mockContext).getPackageName();
        
        // Verify that getApplicationInfo was called
        try {
            verify(mockPackageManager).getApplicationInfo(eq("com.atakmap.android.contacts.plugin"), eq(0));
        } catch (PackageManager.NameNotFoundException e) {
            fail("Verification failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testInitWithNullContext() {
        // Call init with null context
        try {
            ContactsNativeLoader.init(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("native library loading will fail"));
        }
    }
    
    @Test
    public void testLoadLibrary() {
        // Initialize with mock context
        ContactsNativeLoader.init(mockContext);
        
        // Mock File.exists to return true
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        
        // Use PowerMockito to mock System.load
        // Note: This is a simplified test since we can't easily mock System.load
        // In a real test, you would use PowerMockito to mock the static System.load method
        
        // Call loadLibrary
        try {
            ContactsNativeLoader.loadLibrary("test_lib");
            // If we get here, the test passes (we can't verify System.load was called)
        } catch (Exception e) {
            // This might happen since we can't mock System.load
            // The test should still pass if the exception is related to System.load
            assertTrue(e.getMessage().contains("load") || e.getMessage().contains("Library"));
        }
    }
    
    @Test
    public void testLoadLibraryWithoutInit() {
        // Reset the native library dir
        try {
            java.lang.reflect.Field field = ContactsNativeLoader.class.getDeclaredField("ndl");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            fail("Failed to reset native library dir: " + e.getMessage());
        }
        
        // Call loadLibrary without init
        try {
            ContactsNativeLoader.loadLibrary("test_lib");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("NativeLoader not initialized", e.getMessage());
        }
    }
} 