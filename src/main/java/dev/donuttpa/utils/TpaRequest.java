package dev.donuttpa.utils;

import java.util.UUID;

/**
 * Immutable data object representing a single pending TPA or TPAHere request.
 */
public class TpaRequest {

    private final UUID sender;
    private final UUID target;
    private final RequestType type;
    private final long timestamp;

    public TpaRequest(UUID sender, UUID target, RequestType type) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public RequestType getType() { return type; }
    public long getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TpaRequest other)) return false;
        return sender.equals(other.sender) && target.equals(other.target) && type == other.type;
    }

    @Override
    public int hashCode() {
        return 31 * sender.hashCode() + target.hashCode() + type.hashCode();
    }
}
