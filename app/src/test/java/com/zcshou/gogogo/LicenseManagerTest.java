package com.zcshou.gogogo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LicenseManagerTest {
    @Test
    public void acceptsConfiguredPassword() {
        assertTrue(LicenseManager.verifyPassword("GoGoGo@2026"));
    }

    @Test
    public void rejectsWrongPassword() {
        assertFalse(LicenseManager.verifyPassword("WrongPassword"));
    }

    @Test
    public void rejectsEmptyPassword() {
        assertFalse(LicenseManager.verifyPassword(""));
        assertFalse(LicenseManager.verifyPassword(null));
    }
}
