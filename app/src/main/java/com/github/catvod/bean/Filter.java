package com.github.catvod.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Filter {

    @SerializedName("key")
    private String key;
    @SerializedName("name")
    private String name;
    @SerializedName("init")
    private String init;
    @SerializedName("value")
    private List<Value> value;

    public Filter() {
    }

    public Filter(String key, String name, List<Value> value) {
        this.key = key;
        this.name = name;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getInit() {
        return init;
    }

    public List<Value> getValue() {
        return value;
    }

    public static class Value {

        @SerializedName("n")
        private String n;
        @SerializedName("v")
        private String v;

        public Value() {
        }

        public Value(String value) {
            this.n = value;
            this.v = value;
        }

        public Value(String n, String v) {
            this.n = n;
            this.v = v;
        }

        public String getN() {
            return n;
        }

        public String getV() {
            return v;
        }
    }
}
