package com.comphenix.protocol;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.comphenix.protocol.events.ConnectionSide;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import java.util.Locale;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.Deque;
import com.google.common.collect.Range;

class PacketTypeParser
{
    public static final Range<Integer> DEFAULT_MAX_RANGE;
    private PacketType.Sender side;
    private PacketType.Protocol protocol;
    
    PacketTypeParser() {
        this.side = null;
        this.protocol = null;
    }
    
    public Set<PacketType> parseTypes(final Deque<String> arguments, final Range<Integer> defaultRange) {
        final Set<PacketType> result = (Set<PacketType>)Sets.newHashSet();
        this.side = null;
        this.protocol = null;
        while (this.side == null) {
            final String arg = arguments.poll();
            if (this.side == null) {
                final ConnectionSide connection = this.parseSide(arg);
                if (connection != null) {
                    this.side = connection.getSender();
                    continue;
                }
            }
            if (this.protocol == null && (this.protocol = this.parseProtocol(arg)) != null) {
                continue;
            }
            throw new IllegalArgumentException("Specify connection side (CLIENT or SERVER).");
        }
        List<Range<Integer>> ranges = RangeParser.getRanges(arguments, PacketTypeParser.DEFAULT_MAX_RANGE);
        if (this.protocol != null) {
            final Iterator<String> it = arguments.iterator();
            while (it.hasNext()) {
                final String name = it.next().toUpperCase(Locale.ENGLISH);
                final Collection<PacketType> names = PacketType.fromName(name);
                for (final PacketType type : names) {
                    if (type.getProtocol() == this.protocol && type.getSender() == this.side) {
                        result.add(type);
                        it.remove();
                    }
                }
            }
        }
        if (ranges.isEmpty() && result.isEmpty()) {
            ranges = (List<Range<Integer>>)Lists.newArrayList();
            ranges.add(defaultRange);
        }
        for (final Range<Integer> range : ranges) {
            for (final Integer id : ContiguousSet.create((Range)range, DiscreteDomain.integers())) {
                if (this.protocol == null) {
                    if (!PacketType.hasLegacy(id)) {
                        continue;
                    }
                    result.add(PacketType.findLegacy(id, this.side));
                }
                else {
                    if (!PacketType.hasCurrent(this.protocol, this.side, id)) {
                        continue;
                    }
                    result.add(PacketType.findCurrent(this.protocol, this.side, id));
                }
            }
        }
        return result;
    }
    
    public PacketType.Protocol getLastProtocol() {
        return this.protocol;
    }
    
    public PacketType.Sender getLastSide() {
        return this.side;
    }
    
    public ConnectionSide parseSide(final String text) {
        if (text == null) {
            return null;
        }
        final String candidate = text.toLowerCase();
        if ("client".startsWith(candidate)) {
            return ConnectionSide.CLIENT_SIDE;
        }
        if ("server".startsWith(candidate)) {
            return ConnectionSide.SERVER_SIDE;
        }
        return null;
    }
    
    public PacketType.Protocol parseProtocol(final String text) {
        if (text == null) {
            return null;
        }
        final String candidate = text.toLowerCase();
        if ("handshake".equals(candidate) || "handshaking".equals(candidate)) {
            return PacketType.Protocol.HANDSHAKING;
        }
        if ("login".equals(candidate)) {
            return PacketType.Protocol.LOGIN;
        }
        if ("play".equals(candidate) || "game".equals(candidate)) {
            return PacketType.Protocol.PLAY;
        }
        if ("status".equals(candidate)) {
            return PacketType.Protocol.STATUS;
        }
        return null;
    }
    
    static {
        DEFAULT_MAX_RANGE = Range.closed((Comparable)0, (Comparable)255);
    }
}
