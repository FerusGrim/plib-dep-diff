package com.comphenix.protocol.wrappers;

import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

public final class ComponentConverter
{
    private ComponentConverter() {
    }
    
    public static BaseComponent[] fromWrapper(final WrappedChatComponent wrapper) {
        return ComponentSerializer.parse(wrapper.getJson());
    }
    
    public static WrappedChatComponent fromBaseComponent(final BaseComponent... components) {
        return WrappedChatComponent.fromJson(ComponentSerializer.toString(components));
    }
}
