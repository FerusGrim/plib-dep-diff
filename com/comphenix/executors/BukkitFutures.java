package com.comphenix.executors;

import java.lang.reflect.Method;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.event.EventException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.EventExecutor;
import com.google.common.util.concurrent.SettableFuture;
import org.bukkit.event.EventPriority;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;

public class BukkitFutures
{
    private static Listener EMPTY_LISTENER;
    
    public static <TEvent extends Event> ListenableFuture<TEvent> nextEvent(final Plugin plugin, final Class<TEvent> eventClass) {
        return nextEvent(plugin, eventClass, EventPriority.NORMAL, false);
    }
    
    public static <TEvent extends Event> ListenableFuture<TEvent> nextEvent(final Plugin plugin, final Class<TEvent> eventClass, final EventPriority priority, final boolean ignoreCancelled) {
        final HandlerList list = getHandlerList(eventClass);
        final SettableFuture<TEvent> future = (SettableFuture<TEvent>)SettableFuture.create();
        final EventExecutor executor = (EventExecutor)new EventExecutor() {
            private final AtomicBoolean once = new AtomicBoolean();
            
            public void execute(final Listener listener, final Event event) throws EventException {
                if (!future.isCancelled() && !this.once.getAndSet(true)) {
                    future.set((Object)event);
                }
            }
        };
        final RegisteredListener listener = new RegisteredListener(BukkitFutures.EMPTY_LISTENER, executor, priority, plugin, ignoreCancelled) {
            public void callEvent(final Event event) throws EventException {
                super.callEvent(event);
                list.unregister((RegisteredListener)this);
            }
        };
        PluginDisabledListener.getListener(plugin).addFuture((ListenableFuture<?>)future);
        list.register(listener);
        return (ListenableFuture<TEvent>)future;
    }
    
    public static void registerEventExecutor(final Plugin plugin, final Class<? extends Event> eventClass, final EventPriority priority, final EventExecutor executor) {
        getHandlerList(eventClass).register(new RegisteredListener(BukkitFutures.EMPTY_LISTENER, executor, priority, plugin, false));
    }
    
    private static HandlerList getHandlerList(Class<? extends Event> clazz) {
        while (clazz.getSuperclass() != null && Event.class.isAssignableFrom(clazz.getSuperclass())) {
            try {
                final Method method = clazz.getDeclaredMethod("getHandlerList", (Class<?>[])new Class[0]);
                method.setAccessible(true);
                return (HandlerList)method.invoke(null, new Object[0]);
            }
            catch (NoSuchMethodException e2) {
                clazz = clazz.getSuperclass().asSubclass(Event.class);
                continue;
            }
            catch (Exception e) {
                throw new IllegalPluginAccessException(e.getMessage());
            }
            break;
        }
        throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName());
    }
    
    static {
        BukkitFutures.EMPTY_LISTENER = (Listener)new Listener() {};
    }
}
