package org.owwlo.watchcat.utils.EventBus;

public class VideoPlayerStateChangeEvent {
    public enum State
    {
        PLAYING,
        EXITING
    };

    private State state;

    public VideoPlayerStateChangeEvent(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
