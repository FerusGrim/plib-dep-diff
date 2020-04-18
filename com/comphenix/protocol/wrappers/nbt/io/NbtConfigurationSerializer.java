package com.comphenix.protocol.wrappers.nbt.io;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import com.google.common.primitives.Ints;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtType;
import org.bukkit.configuration.file.YamlConfiguration;
import com.comphenix.protocol.wrappers.nbt.NbtWrapper;
import com.google.common.collect.Lists;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.List;
import com.comphenix.protocol.wrappers.nbt.NbtVisitor;
import org.bukkit.configuration.ConfigurationSection;
import com.comphenix.protocol.wrappers.nbt.NbtBase;

public class NbtConfigurationSerializer
{
    public static final String TYPE_DELIMITER = "$";
    public static final NbtConfigurationSerializer DEFAULT;
    private String dataTypeDelimiter;
    
    public NbtConfigurationSerializer() {
        this.dataTypeDelimiter = "$";
    }
    
    public NbtConfigurationSerializer(final String dataTypeDelimiter) {
        this.dataTypeDelimiter = dataTypeDelimiter;
    }
    
    public String getDataTypeDelimiter() {
        return this.dataTypeDelimiter;
    }
    
    public <TType> void serialize(final NbtBase<TType> value, final ConfigurationSection destination) {
        value.accept(new NbtVisitor() {
            private ConfigurationSection current = destination;
            private List<Object> currentList;
            private Map<ConfigurationSection, Integer> workingIndex = Maps.newHashMap();
            
            @Override
            public boolean visitEnter(final NbtCompound compound) {
                this.current = this.current.createSection(compound.getName());
                return true;
            }
            
            @Override
            public boolean visitEnter(final NbtList<?> list) {
                final Integer listIndex = this.getNextIndex();
                final String name = this.getEncodedName(list, listIndex);
                if (list.getElementType().isComposite()) {
                    this.current = this.current.createSection(name);
                    this.workingIndex.put(this.current, 0);
                }
                else {
                    this.currentList = (List<Object>)Lists.newArrayList();
                    this.current.set(name, (Object)this.currentList);
                }
                return true;
            }
            
            @Override
            public boolean visitLeave(final NbtCompound compound) {
                this.current = this.current.getParent();
                return true;
            }
            
            @Override
            public boolean visitLeave(final NbtList<?> list) {
                if (this.currentList != null) {
                    this.currentList = null;
                }
                else {
                    this.workingIndex.remove(this.current);
                    this.current = this.current.getParent();
                }
                return true;
            }
            
            @Override
            public boolean visit(final NbtBase<?> node) {
                if (this.currentList == null) {
                    final Integer listIndex = this.getNextIndex();
                    final String name = this.getEncodedName(node, listIndex);
                    this.current.set(name, NbtConfigurationSerializer.this.fromNodeValue(node));
                }
                else {
                    this.currentList.add(NbtConfigurationSerializer.this.fromNodeValue(node));
                }
                return true;
            }
            
            private Integer getNextIndex() {
                final Integer listIndex = this.workingIndex.get(this.current);
                if (listIndex != null) {
                    return this.workingIndex.put(this.current, listIndex + 1);
                }
                return null;
            }
            
            private String getEncodedName(final NbtBase<?> node, final Integer index) {
                if (index != null) {
                    return index + NbtConfigurationSerializer.this.dataTypeDelimiter + node.getType().getRawID();
                }
                return node.getName() + NbtConfigurationSerializer.this.dataTypeDelimiter + node.getType().getRawID();
            }
            
            private String getEncodedName(final NbtList<?> node, final Integer index) {
                if (index != null) {
                    return index + NbtConfigurationSerializer.this.dataTypeDelimiter + node.getElementType().getRawID();
                }
                return node.getName() + NbtConfigurationSerializer.this.dataTypeDelimiter + node.getElementType().getRawID();
            }
        });
    }
    
    public <TType> NbtWrapper<TType> deserialize(final ConfigurationSection root, final String nodeName) {
        return (NbtWrapper<TType>)this.readNode(root, nodeName);
    }
    
    public NbtCompound deserializeCompound(final YamlConfiguration root, final String nodeName) {
        return (NbtCompound)this.readNode((ConfigurationSection)root, nodeName);
    }
    
    public <T> NbtList<T> deserializeList(final YamlConfiguration root, final String nodeName) {
        return (NbtList<T>)(NbtList)this.readNode((ConfigurationSection)root, nodeName);
    }
    
    private NbtWrapper<?> readNode(final ConfigurationSection parent, final String name) {
        String[] decoded = getDecodedName(name);
        Object node = parent.get(name);
        NbtType type = NbtType.TAG_END;
        if (node == null) {
            for (final String key : parent.getKeys(false)) {
                decoded = getDecodedName(key);
                if (decoded[0].equals(name)) {
                    node = parent.get(decoded[0]);
                    break;
                }
            }
            if (node == null) {
                throw new IllegalArgumentException("Unable to find node " + name + " in " + parent);
            }
        }
        if (decoded.length > 1) {
            type = NbtType.getTypeFromID(Integer.parseInt(decoded[1]));
        }
        if (node instanceof ConfigurationSection) {
            if (type != NbtType.TAG_END) {
                final NbtList<Object> list = NbtFactory.ofList(decoded[0], new Object[0]);
                final ConfigurationSection section = (ConfigurationSection)node;
                final List<String> sorted = this.sortSet(section.getKeys(false));
                for (final String key2 : sorted) {
                    final NbtBase<Object> base = (NbtBase<Object>)this.readNode(section, key2.toString());
                    base.setName("");
                    list.getValue().add(base);
                }
                return (NbtWrapper<?>)(NbtWrapper)list;
            }
            final NbtCompound compound = NbtFactory.ofCompound(decoded[0]);
            final ConfigurationSection section = (ConfigurationSection)node;
            for (final String key3 : section.getKeys(false)) {
                compound.put(this.readNode(section, key3));
            }
            return (NbtWrapper<?>)compound;
        }
        else {
            if (type == NbtType.TAG_END) {
                throw new IllegalArgumentException("Cannot find encoded type of " + decoded[0] + " in " + name);
            }
            if (node instanceof List) {
                final NbtList<Object> list = NbtFactory.ofList(decoded[0], new Object[0]);
                list.setElementType(type);
                for (final Object value : (List)node) {
                    list.addClosest(this.toNodeValue(value, type));
                }
                return (NbtWrapper<?>)(NbtWrapper)list;
            }
            return NbtFactory.ofWrapper(type, decoded[0], this.toNodeValue(node, type));
        }
    }
    
    private List<String> sortSet(final Set<String> unsorted) {
        final List<String> sorted = new ArrayList<String>(unsorted);
        Collections.sort(sorted, new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                final int index1 = Integer.parseInt(getDecodedName(o1)[0]);
                final int index2 = Integer.parseInt(getDecodedName(o2)[0]);
                return Ints.compare(index1, index2);
            }
        });
        return sorted;
    }
    
    private Object fromNodeValue(final NbtBase<?> base) {
        if (base.getType() == NbtType.TAG_INT_ARRAY) {
            return toByteArray((int[])(Object)base.getValue());
        }
        return base.getValue();
    }
    
    public Object toNodeValue(final Object value, final NbtType type) {
        if (type == NbtType.TAG_INT_ARRAY) {
            return toIntegerArray((byte[])value);
        }
        return value;
    }
    
    private static byte[] toByteArray(final int[] data) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
        final IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);
        return byteBuffer.array();
    }
    
    private static int[] toIntegerArray(final byte[] data) {
        final IntBuffer source = ByteBuffer.wrap(data).asIntBuffer();
        final IntBuffer copy = IntBuffer.allocate(source.capacity());
        copy.put(source);
        return copy.array();
    }
    
    private static String[] getDecodedName(final String nodeName) {
        final int delimiter = nodeName.lastIndexOf(36);
        if (delimiter > 0) {
            return new String[] { nodeName.substring(0, delimiter), nodeName.substring(delimiter + 1) };
        }
        return new String[] { nodeName };
    }
    
    static {
        DEFAULT = new NbtConfigurationSerializer();
    }
}
