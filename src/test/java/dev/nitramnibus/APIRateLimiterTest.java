package dev.nitramnibus;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class APIRateLimiterTest {


    @Test
    void testBasic() {
        APIRateLimiter apiRateLimiter = new APIRateLimiter(10);

        Date now = new Date();
        for (int i = 0; i < 10; i++) {
            assertTrue(apiRateLimiter.allowRequest(0, now));
        }
        for (int i = 0; i < 20; i++) {
            assertFalse(apiRateLimiter.allowRequest(0, now));
        }
    }

    @Test
    void testMultipleClient() {
        APIRateLimiter apiRateLimiter = new APIRateLimiter(10);

        Date now = new Date(0);
        Date later = new Date(120 * 1000); // 2 minutes later
        for (int i = 0; i < 10; i++) {
            for (int cliendId = 0; cliendId < 10; cliendId++) {
                assertTrue(apiRateLimiter.allowRequest(cliendId, now));
            }
        }

        // TOO SOON
        for (int i = 0; i < 10; i++) {
            for (int cliendId = 0; cliendId < 10; cliendId++) {
                assertFalse(apiRateLimiter.allowRequest(cliendId, now));
            }
        }

        // NOW OK
        for (int i = 0; i < 10; i++) {
            for (int cliendId = 0; cliendId < 10; cliendId++) {
                assertTrue(apiRateLimiter.allowRequest(cliendId, later));
            }
        }
    }

    @Test
    void testChronologicalOrder() {

        APIRateLimiter apiRateLimiter = new APIRateLimiter(10);
        for (int i = 0; i < 5; i++) {
            Date date = new Date(i);
            assertTrue(apiRateLimiter.allowRequest(0, date));
        }

        for (int i = 0; i < 4; i++) {
            Date date = new Date(i);
            assertFalse(apiRateLimiter.allowRequest(0, date));
        }

    }

    @Test
    void testContinuousPass() {
        // max 10 request per 60s
        APIRateLimiter apiRateLimiter = new APIRateLimiter(10);

        // request every 7s: should work
        for (int i = 0; i < 1000; i++) {
            Date date = new Date(i * 7000);
            assertTrue(apiRateLimiter.allowRequest(0, date));
        }

    }

    @Test
    void testContinuousFail() {
        // max 10 request per 60s
        APIRateLimiter apiRateLimiter = new APIRateLimiter(10);

        // request every 5s: should stop working at 11th request
        for (int i = 0; i < 10; i++) {
            Date date = new Date(i * 5000);
            assertTrue(apiRateLimiter.allowRequest(0, date));
        }

        Date date = new Date(10 * 5000);
        assertFalse(apiRateLimiter.allowRequest(0, date));
    }
}