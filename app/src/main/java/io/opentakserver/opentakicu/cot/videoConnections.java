package io.opentakserver.opentakicu.cot;

public class videoConnections {
    private feed Feed;

    public videoConnections(feed Feed) {
        this.Feed = Feed;
    }

    public feed getFeed() {
        return Feed;
    }

    public void setFeed(feed Feed) {
        this.Feed = Feed;
    }
}
