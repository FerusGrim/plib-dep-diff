package com.comphenix.protocol.reflect.cloning;

import com.comphenix.protocol.reflect.instances.ExistingGenerator;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.comphenix.protocol.reflect.instances.InstanceProvider;
import com.comphenix.protocol.reflect.instances.DefaultInstances;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.lang.ref.WeakReference;
import java.util.List;

public class AggregateCloner implements Cloner
{
    public static final AggregateCloner DEFAULT;
    private List<Cloner> cloners;
    private WeakReference<Object> lastObject;
    private int lastResult;
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private AggregateCloner() {
    }
    
    public List<Cloner> getCloners() {
        return Collections.unmodifiableList((List<? extends Cloner>)this.cloners);
    }
    
    private void setCloners(final Iterable<? extends Cloner> cloners) {
        this.cloners = (List<Cloner>)Lists.newArrayList((Iterable)cloners);
    }
    
    @Override
    public boolean canClone(final Object source) {
        this.lastResult = this.getFirstCloner(source);
        this.lastObject = new WeakReference<Object>(source);
        return this.lastResult >= 0 && this.lastResult < this.cloners.size();
    }
    
    private int getFirstCloner(final Object source) {
        for (int i = 0; i < this.cloners.size(); ++i) {
            if (this.cloners.get(i).canClone(source)) {
                return i;
            }
        }
        return this.cloners.size();
    }
    
    @Override
    public Object clone(final Object source) {
        if (source == null) {
            throw new IllegalAccessError("source cannot be NULL.");
        }
        int index = 0;
        if (this.lastObject != null && this.lastObject.get() == source) {
            index = this.lastResult;
        }
        else {
            index = this.getFirstCloner(source);
        }
        if (index < this.cloners.size()) {
            return this.cloners.get(index).clone(source);
        }
        throw new IllegalArgumentException("Cannot clone " + source + ": No cloner is suitable.");
    }
    
    static {
        DEFAULT = newBuilder().instanceProvider(DefaultInstances.DEFAULT).andThen(BukkitCloner.class).andThen(ImmutableDetector.class).andThen(CollectionCloner.class).andThen(FieldCloner.class).build();
    }
    
    public static class BuilderParameters
    {
        private InstanceProvider instanceProvider;
        private Cloner aggregateCloner;
        private InstanceProvider typeConstructor;
        
        private BuilderParameters() {
        }
        
        public InstanceProvider getInstanceProvider() {
            return this.instanceProvider;
        }
        
        public Cloner getAggregateCloner() {
            return this.aggregateCloner;
        }
    }
    
    public static class Builder
    {
        private List<Function<BuilderParameters, Cloner>> factories;
        private BuilderParameters parameters;
        
        public Builder() {
            this.factories = (List<Function<BuilderParameters, Cloner>>)Lists.newArrayList();
            this.parameters = new BuilderParameters();
        }
        
        public Builder instanceProvider(final InstanceProvider provider) {
            this.parameters.instanceProvider = provider;
            return this;
        }
        
        public Builder andThen(final Class<? extends Cloner> type) {
            return this.andThen((Function<BuilderParameters, Cloner>)new Function<BuilderParameters, Cloner>() {
                public Cloner apply(@Nullable final BuilderParameters param) {
                    final Object result = param.typeConstructor.create(type);
                    if (result == null) {
                        throw new IllegalStateException("Constructed NULL instead of " + type);
                    }
                    if (type.isAssignableFrom(result.getClass())) {
                        return (Cloner)result;
                    }
                    throw new IllegalStateException("Constructed " + result.getClass() + " instead of " + type);
                }
            });
        }
        
        public Builder andThen(final Function<BuilderParameters, Cloner> factory) {
            this.factories.add(factory);
            return this;
        }
        
        public AggregateCloner build() {
            final AggregateCloner newCloner = new AggregateCloner(null);
            final Cloner paramCloner = new NullableCloner(newCloner);
            final InstanceProvider paramProvider = this.parameters.instanceProvider;
            this.parameters.aggregateCloner = paramCloner;
            this.parameters.typeConstructor = DefaultInstances.fromArray(ExistingGenerator.fromObjectArray(new Object[] { paramCloner, paramProvider }));
            final List<Cloner> cloners = (List<Cloner>)Lists.newArrayList();
            for (int i = 0; i < this.factories.size(); ++i) {
                final Cloner cloner = (Cloner)this.factories.get(i).apply((Object)this.parameters);
                if (cloner == null) {
                    throw new IllegalArgumentException(String.format("Cannot create cloner from %s (%s)", this.factories.get(i), i));
                }
                cloners.add(cloner);
            }
            newCloner.setCloners(cloners);
            return newCloner;
        }
    }
}
