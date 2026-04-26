package com.penca.lapenca.dto;

import java.util.List;
import java.util.Map;

public class ThirdPlaceRule {

    private int option;
    private List<String> qualifiedGroups;
    private Map<String, String> mapping;

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public List<String> getQualifiedGroups() {
        return qualifiedGroups;
    }

    public void setQualifiedGroups(List<String> qualifiedGroups) {
        this.qualifiedGroups = qualifiedGroups;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }
}