package dev.nitramnibus;

import java.util.*;

public class APIRateLimiter {

    private final int maxRequestCount;
    private final HashMap<Long, HandledRequestQueue> requestQueues = new HashMap<>();

    // keep track of chronological order.
    private Date lastTimestamp = null;

    public APIRateLimiter(int maxRequestCount) {
        this.maxRequestCount = maxRequestCount;
    }


    /**
     * Checks if clientId has reached the maximum allowed API rate limit.
     *
     * @param clientId The ID of the client making a request.
     * @param timestamp The time at which the clients make the requests.
     *                  the timestamp MUST come in CHRONOLOGICAL ORDER.
     *
     * @return true if client is allowed to make request and timestamp are in
     *              chronological order, else false.
     */
    public boolean allowRequest(long clientId, Date timestamp) {
        if (!checkChronological(timestamp)) {
            return false;
        }
        lastTimestamp = timestamp;

        clearOldRequests(timestamp, clientId);

        HandledRequestQueue requestQueue = requestQueues.get(clientId);

        if (requestQueue == null) {
            // New clientId: Add them to the list.
            requestQueue = new HandledRequestQueue();
            requestQueues.put(clientId, requestQueue);
        }

        return requestQueue.allowRequest(timestamp);
    }

    private void clearOldRequests(Date timestamp, long currentClientId) {
        ArrayList<Long> toRemove = new ArrayList<>();
        for (long clientId : requestQueues.keySet()) {
            HandledRequestQueue requestQueue = requestQueues.get(clientId);
            if (requestQueue.removeRequestIfTooOld(timestamp) && clientId != currentClientId) {
                toRemove.add(clientId);
            }
        }
        toRemove.forEach(requestQueues.keySet()::remove);
    }

    private boolean checkChronological(Date timestamp) {
        if (lastTimestamp == null) {
            lastTimestamp = timestamp;
        }
        return !lastTimestamp.after(timestamp);
    }



    private class HandledRequestQueue {

        private final static long MAX_TIME_MS = 60 * 1000;

        private final Queue<Date> handledRequests = new ArrayDeque<>(maxRequestCount);


        /**
         * Check if a request is allowed for this client's queue.
         * @param timestamp The time of the request. MUST COME IN
         *                  CHRONOLOGICAL ORDER, but this is not enforced here.
         * @return true if allowed else false.
         */
        public boolean allowRequest(Date timestamp) {

            if (handledRequests.size() < maxRequestCount) {
                // There is less than N requests in the sliding window: We can allow the new request
                handledRequests.add(timestamp);
                return true;
            }
            // Too many requests: Do not Allow
            return false;
        }

        private boolean removeRequestIfTooOld(Date timestamp) {
            Date limit = new Date(timestamp.getTime() - MAX_TIME_MS);
            while ( !handledRequests.isEmpty() && handledRequests.element().before(limit)) {
                // Remove request older than MAX_TIME_MS
                handledRequests.poll();
            }
            return handledRequests.isEmpty();
        }

    }

}
