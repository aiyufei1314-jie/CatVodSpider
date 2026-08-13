package com.github.catvod.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

public class Class {

    @SerializedName("type_id")
    private String typeId;
    @SerializedName("type_name")
    private String typeName;
    @SerializedName("type_flag")
    private String typeFlag;

    public Class() {
    }

    public static List<Class> arrayFrom(String str) {
        try {
            Class[] array = new Gson().fromJson(str, Class[].class);
            return array == null ? null : Arrays.asList(array);
        } catch (Exception e) {
            return null;
        }
    }

    public Class(String typeId) {
        this(typeId, typeId);
    }

    public Class(String typeId, String typeName) {
        this(typeId, typeName, null);
    }

    public Class(String typeId, String typeName, String typeFlag) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.typeFlag = typeFlag;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeFlag() {
        return typeFlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Class)) return false;
        Class it = (Class) obj;
        return getTypeId().equals(it.getTypeId());
    }
}