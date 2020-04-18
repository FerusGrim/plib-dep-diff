package com.comphenix.protocol.reflect.instances;

import javax.annotation.Nullable;

public interface InstanceProvider
{
    Object create(@Nullable final Class<?> p0);
}
