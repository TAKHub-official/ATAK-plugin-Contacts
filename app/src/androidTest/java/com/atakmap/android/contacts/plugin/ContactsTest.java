package com.atakmap.android.contacts.plugin;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.ToolbarItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atak.plugins.impl.PluginContextProvider;

@RunWith(AndroidJUnit4.class)
public class ContactsTest {
    
    @Mock
    private IServiceController mockServiceController;
    
    @Mock
    private PluginContextProvider mockContextProvider;
    
    @Mock
    private IHostUIService mockUIService;
    
    @Mock
    private ToolbarItem mockToolbarItem;
    
    @Mock
    private Pane mockPane;
    
    private Contacts contactsPlugin;
    private Context context;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Get the context from instrumentation
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Mock service controller to return context provider and UI service
        when(mockServiceController.getService(PluginContextProvider.class)).thenReturn(mockContextProvider);
        when(mockServiceController.getService(IHostUIService.class)).thenReturn(mockUIService);
        
        // Mock context provider to return context
        when(mockContextProvider.getPluginContext()).thenReturn(context);
        
        // Create a test instance with mocked dependencies
        contactsPlugin = new Contacts(mockServiceController) {
            @Override
            protected ToolbarItem createToolbarItem() {
                return mockToolbarItem;
            }
            
            @Override
            protected void showPane() {
                // Override to avoid real showing
            }
        };
        
        // Set mocked pane
        contactsPlugin.contactsPane = mockPane;
    }
    
    @Test
    public void testOnStart() {
        // Call onStart
        contactsPlugin.onStart();
        
        // Verify that addToolbarItem was called
        verify(mockUIService).addToolbarItem(mockToolbarItem);
    }
    
    @Test
    public void testOnStop() {
        // Call onStop
        contactsPlugin.onStop();
        
        // Verify that removeToolbarItem was called
        verify(mockUIService).removeToolbarItem(mockToolbarItem);
    }
    
    @Test
    public void testShowPaneWhenPaneIsVisible() {
        // Mock pane to be visible
        when(mockUIService.isPaneVisible(mockPane)).thenReturn(true);
        
        // Call showPane
        contactsPlugin.showPane();
        
        // Verify that isPaneVisible was called
        verify(mockUIService).isPaneVisible(mockPane);
        
        // Verify that showPane was not called
        verify(mockUIService, never()).showPane(mockPane, null);
    }
    
    @Test
    public void testShowPaneWhenPaneIsNotVisible() {
        // Mock pane to be not visible
        when(mockUIService.isPaneVisible(mockPane)).thenReturn(false);
        
        // Call showPane
        contactsPlugin.showPane();
        
        // Verify that isPaneVisible was called
        verify(mockUIService).isPaneVisible(mockPane);
        
        // Verify that showPane was called
        verify(mockUIService).showPane(mockPane, null);
    }
    
    @Test
    public void testCheckPermissions() {
        // Call checkPermissions
        contactsPlugin.checkPermissions(context);
        
        // This is mostly a logging test, so we just ensure it doesn't crash
    }
} 