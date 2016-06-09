package com.netflix.archaius.test;

@TestPropertyOverride("parentClassLevelProperty=present")
public class Archaius2RuleTestParentTest extends SuperParent {

}

@TestPropertyOverride("parentClassLevelProperty=super")
class SuperParent {
    
}
