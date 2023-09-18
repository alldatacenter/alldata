package com.datasophon.api.utils;

import org.apache.sshd.client.session.ClientSession;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MinaUtilsTest {

    @Test
    public void testOpenConnection() {
        // Setup
        // Run the test
        final ClientSession result = MinaUtils.openConnection("sshHost", 0, "sshUser", "privateKey");

        // Verify the results
    }

    @Test
    public void testCloseConnection() {
        // Setup
        final ClientSession session = null;

        // Run the test
        MinaUtils.closeConnection(session);

        // Verify the results
    }

    @Test
    public void testGetKeyPairFromString() {
        // Setup
        // Run the test
        final KeyPair result = MinaUtils.getKeyPairFromString("pk");

        // Verify the results
    }

    @Test
    public void testExecCmdWithResult() {
        // Setup
        final ClientSession session = null;

        // Run the test
        final String result = MinaUtils.execCmdWithResult(session, "command");

        // Verify the results
        assertEquals("failed", result);
    }

    @Test
    public void testUploadFile() {
        // Setup
        final ClientSession session = null;

        // Run the test
        final boolean result = MinaUtils.uploadFile(session, "remotePath", "inputFile");

        // Verify the results
        assertFalse(result);
    }

    @Test
    public void testCreateDir() {
        // Setup
        final ClientSession session = null;

        // Run the test
        final boolean result = MinaUtils.createDir(session, "path");

        // Verify the results
        assertFalse(result);
    }

    @Test
    public void testMain() throws Exception {
        // Setup
        // Run the test
        MinaUtils.main(new String[]{"args"});

        // Verify the results
    }

    @Test(expected = IOException.class)
    public void testMain_ThrowsIOException() throws Exception {
        // Setup
        // Run the test
        MinaUtils.main(new String[]{"args"});
    }

    @Test(expected = InterruptedException.class)
    public void testMain_ThrowsInterruptedException() throws Exception {
        // Setup
        // Run the test
        MinaUtils.main(new String[]{"args"});
    }
}
