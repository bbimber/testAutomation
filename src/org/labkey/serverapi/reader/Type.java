package org.labkey.serverapi.reader;

import java.io.File;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public enum Type
{
    StringType("Text (String)", "xsd:string", "varchar", String.class, ByteBuffer.class),
    IntType("Integer", "xsd:int", "integer", Integer.class, Integer.TYPE, Short.class, Short.TYPE, Byte.class, Byte.TYPE),
    LongType("Long", "xsd:long", "bigint", Long.class, long.class),
    DoubleType("Number (Double)", "xsd:double", "double", Double.class, Double.TYPE, BigDecimal.class), // Double.TYPE is here because manually created datasets with required doubles return Double.TYPE as Class
    FloatType("Number (Float)", "xsd:float", "float", Float.class, Float.TYPE),
    DateTimeType("DateTime", "xsd:dateTime", "timestamp", Date.class, Timestamp.class, java.sql.Time.class, java.sql.Date.class),
    BooleanType("Boolean", "xsd:boolean", "boolean", Boolean.class, Boolean.TYPE),
    AttachmentType("Attachment", "xsd:attachment", "varchar", String.class, File.class);

    private final String label;
    private final String xsd;
    private final Class clazz;
    private final Set<Class> allClasses = new HashSet<>();
    private final String sqlTypeName;

    Type(String label, String xsd, String sqlTypeName, Class clazz, Class... additionalClasses)
    {
        this.label = label;
        this.xsd = xsd;
        this.clazz = clazz;
        this.allClasses.add(clazz);
        this.allClasses.addAll(Arrays.asList(additionalClasses));
        this.sqlTypeName = sqlTypeName;
    }

    public String getLabel()
    {
        return label;
    }

    public String getXsdType()
    {
        return xsd;
    }

    public Class getJavaClass()
    {
        return clazz;
    }

    public boolean matches(Class clazz)
    {
        return allClasses.contains(clazz);
    }

    public String getSqlTypeName()
    {
        return sqlTypeName;
    }

    public boolean isNumeric()
    {
        return this == IntType || this == DoubleType;
    }

    public static Type getTypeByXsdType(String xsd)
    {
        for (Type type : values())
        {
            if (type.getXsdType().equals(xsd))
                return type;
        }
        return null;
    }

    public static Type getTypeByClass(Class clazz)
    {
        for (Type type : values())
        {
            if (type.matches(clazz))
                return type;
        }
        return null;
    }
}

