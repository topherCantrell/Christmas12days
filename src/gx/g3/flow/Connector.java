package gx.g3.flow; 

import java.util.*;

/**
 * This class is the base functionallity for all logic conenctors.
 */
public abstract class Connector {
    
    String name;      // Unique name
    int polarity;     // (Possibly) several forms
    
    Connector leftInput;   // The connector to the left dumping into us
    Connector rightInput;  // The connector to the right dumping into us
    Connector output;      // The connector we dump into
    
    public Connector(String name) {
        this.name = name;
    }
    
    public String getName(boolean which)
    {
        // For all connectors except the OutputNodes, there is only one name.
        return getName();
    }
    
    public String getName()
    {
        return name;
    }
    
    // The target connector depends on who is asking (left, right, or bottom)
    
    public abstract Connector getTrueConnector(Connector asker);
    public abstract Connector getFalseConnector(Connector asker);
    public abstract Connector getProcessConnector(Connector asker);
    
    public void toAssembly(List<Line> out) {}   
    
}
