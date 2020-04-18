package com.comphenix.protocol.injector;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;
import com.comphenix.protocol.events.ListenerPriority;

public class PrioritizedListener<TListener> implements Comparable<PrioritizedListener<TListener>>
{
    private TListener listener;
    private ListenerPriority priority;
    
    public PrioritizedListener(final TListener listener, final ListenerPriority priority) {
        this.listener = listener;
        this.priority = priority;
    }
    
    @Override
    public int compareTo(final PrioritizedListener<TListener> other) {
        return Ints.compare(this.getPriority().getSlot(), other.getPriority().getSlot());
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PrioritizedListener) {
            final PrioritizedListener<TListener> other = (PrioritizedListener<TListener>)obj;
            return Objects.equal((Object)this.listener, (Object)other.listener);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[] { this.listener });
    }
    
    public TListener getListener() {
        return this.listener;
    }
    
    public ListenerPriority getPriority() {
        return this.priority;
    }
}
