
package com.netflix.archaius.api;

/**
 * Key used to group and order {@link Config}s into layers (@see Layers).
 * Layers are ordered by natural order such that lower order values have 
 * precedence over a higher value.  Within a layer configurations are prioritized
 * such that the last config wins.
 */
public final class Layer {
    private final String name;
    private final int order;
    
    /**
     * Construct an Layer key.  
     * 
     * @param name
     * @param order
     * @return
     */
    public static Layer of(String name, int order) {
        return new Layer(name, order);
    }
    
    private Layer(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public Layer withOrder(int order) {
        return new Layer(name, order);
    }
    
    public int getOrder() {
        return order;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + order;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Layer other = (Layer) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (order != other.order)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Key [layerName=" + name + ", layerOrder=" + order + "]";
    }
}