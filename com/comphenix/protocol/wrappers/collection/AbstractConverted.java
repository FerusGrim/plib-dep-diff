package com.comphenix.protocol.wrappers.collection;

import javax.annotation.Nullable;
import com.google.common.base.Function;

public abstract class AbstractConverted<VInner, VOuter>
{
    private Function<VOuter, VInner> innerConverter;
    private Function<VInner, VOuter> outerConverter;
    
    public AbstractConverted() {
        this.innerConverter = (Function<VOuter, VInner>)new Function<VOuter, VInner>() {
            public VInner apply(@Nullable final VOuter param) {
                return AbstractConverted.this.toInner(param);
            }
        };
        this.outerConverter = (Function<VInner, VOuter>)new Function<VInner, VOuter>() {
            public VOuter apply(@Nullable final VInner param) {
                return AbstractConverted.this.toOuter(param);
            }
        };
    }
    
    protected abstract VOuter toOuter(final VInner p0);
    
    protected abstract VInner toInner(final VOuter p0);
    
    protected Function<VOuter, VInner> getInnerConverter() {
        return this.innerConverter;
    }
    
    protected Function<VInner, VOuter> getOuterConverter() {
        return this.outerConverter;
    }
}
