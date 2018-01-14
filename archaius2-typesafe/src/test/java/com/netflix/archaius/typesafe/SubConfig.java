package com.netflix.archaius.typesafe;

import com.netflix.archaius.api.annotations.PropertyName;

import java.util.List;
import java.util.Map;

/**
 * Created by Milan Baran on 5/26/17.
 */
public interface SubConfig {

    @PropertyName(name = "var1")
    String getVar1();

    @PropertyName(name = "var2")
    Boolean getVar2();
}