package core.properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import utilities.Utils;

import java.awt.*;

public class PropertyColor extends Property
{
    public String valueStr;
    @JsonIgnore
    private Color value;

    @JsonCreator
    public PropertyColor(@JsonProperty("hashString") String hashString, @JsonProperty("valueStr") String valStr)
    {
        super(hashString);
        this.value = Utils.stringToColor(valStr);
        this.valueStr = valStr;
    }

    private PropertyColor(String hashString, int hashKey, Color value, String valueStr)
    {
        super(hashString, hashKey);
        this.value = value;
        this.valueStr = valueStr;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof PropertyColor)
            return value.equals(((PropertyColor)(other)).value);
        return false;
    }

    @Override
    protected Property _copy() {
        return new PropertyColor(hashString, hashKey, value, valueStr);
    }
}
