package com.netflix.config;

/**
 * User: Mike Smith
 * Date: 11/28/15
 * Time: 12:46 AM
 */
public class DynamicPrimitiveBooleanProperty extends PropertyWrapper<Boolean>
{
    protected volatile boolean primitiveValue;

    public DynamicPrimitiveBooleanProperty(String propName, Boolean defaultValue)
    {
        super(propName, defaultValue);

        this.primitiveValue = chooseValue();

        // Add a callback to update the volatile value when the property is changed.
        this.prop.addCallback(() -> primitiveValue = chooseValue() );
    }

    private boolean chooseValue() {
        Boolean propValue = this.prop == null ? null : this.prop.getBoolean();
        return propValue == null ? defaultValue : propValue.booleanValue();
    }

    /**
     * Get the current value from the underlying DynamicProperty
     */
    public boolean get() {
        return primitiveValue;
    }

    @Override
    public Boolean getValue() {
        return get();
    }
}
