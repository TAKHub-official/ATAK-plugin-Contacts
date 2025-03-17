package com.atakmap.android.contacts.plugin;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite to run all instrumentation tests for the Contacts plugin
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ContactManagerTest.class,
    ContactsTest.class
})
public class ContactsInstrumentationTestSuite {
    // This class remains empty, it is used only as a holder for the above annotations
} 