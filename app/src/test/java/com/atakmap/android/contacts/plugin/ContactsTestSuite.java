package com.atakmap.android.contacts.plugin;

import com.atakmap.android.contacts.plugin.adapter.ContactAdapterTest;
import com.atakmap.android.contacts.plugin.db.DatabaseHelperTest;
import com.atakmap.android.contacts.plugin.model.ContactTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite to run all unit tests for the Contacts plugin
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ContactTest.class,
    ContactAdapterTest.class,
    DatabaseHelperTest.class,
    ContactsNativeLoaderTest.class
})
public class ContactsTestSuite {
    // This class remains empty, it is used only as a holder for the above annotations
} 