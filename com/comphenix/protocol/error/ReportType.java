package com.comphenix.protocol.error;

import com.comphenix.protocol.reflect.fuzzy.AbstractFuzzyMatcher;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.FuzzyReflection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.comphenix.protocol.reflect.FieldAccessException;
import java.lang.reflect.Field;

public class ReportType
{
    private final String errorFormat;
    protected String reportName;
    
    public ReportType(final String errorFormat) {
        this.errorFormat = errorFormat;
    }
    
    public String getMessage(final Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return this.toString();
        }
        return String.format(this.errorFormat, parameters);
    }
    
    @Override
    public String toString() {
        return this.errorFormat;
    }
    
    public static Class<?> getSenderClass(final Object sender) {
        if (sender == null) {
            throw new IllegalArgumentException("sender cannot be null.");
        }
        if (sender instanceof Class) {
            return (Class<?>)sender;
        }
        return sender.getClass();
    }
    
    public static String getReportName(final Object sender, final ReportType type) {
        if (sender == null) {
            throw new IllegalArgumentException("sender cannot be null.");
        }
        return getReportName(getSenderClass(sender), type);
    }
    
    private static String getReportName(final Class<?> sender, final ReportType type) {
        if (sender == null) {
            throw new IllegalArgumentException("sender cannot be null.");
        }
        if (type.reportName == null) {
            for (final Field field : getReportFields(sender)) {
                try {
                    field.setAccessible(true);
                    if (field.get(null) == type) {
                        return type.reportName = field.getDeclaringClass().getCanonicalName() + "#" + field.getName();
                    }
                    continue;
                }
                catch (IllegalAccessException e) {
                    throw new FieldAccessException("Unable to read field " + field, e);
                }
            }
            throw new IllegalArgumentException("Cannot find report name for " + type);
        }
        return type.reportName;
    }
    
    public static ReportType[] getReports(final Class<?> sender) {
        if (sender == null) {
            throw new IllegalArgumentException("sender cannot be NULL.");
        }
        final List<ReportType> result = new ArrayList<ReportType>();
        for (final Field field : getReportFields(sender)) {
            try {
                field.setAccessible(true);
                result.add((ReportType)field.get(null));
            }
            catch (IllegalAccessException e) {
                throw new FieldAccessException("Unable to read field " + field, e);
            }
        }
        return result.toArray(new ReportType[0]);
    }
    
    private static List<Field> getReportFields(final Class<?> clazz) {
        return FuzzyReflection.fromClass(clazz, true).getFieldList(FuzzyFieldContract.newBuilder().requireModifier(8).typeDerivedOf(ReportType.class).build());
    }
}
