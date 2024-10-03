public class LamportClock {
    private int clock;

    public LamportClock() {
        clock = 0;
    }

    // Increment the clock for local events (e.g., sending a message)
    public synchronized void tick() {
        clock++;
    }

    // Update the clock when receiving a message, based on the received timestamp
    public synchronized void update(int receivedClock) {
        clock = Math.max(clock, receivedClock) + 1;
    }

    // Get the current clock value
    public synchronized int getClock() {
        return clock;
    }
}
