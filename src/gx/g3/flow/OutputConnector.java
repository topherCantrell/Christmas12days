package gx.g3.flow; 


/**
 * An OutputConnector is a connector that tracks the last-connector-asker
 * and a SpecialConstructorInfo containing the NORMAL and ELSE blocks
 * attached to the output. The "toString(List)" method is specialized
 * by each derivation to return the overall code logic in the proper
 * sequence.
 */
public abstract class OutputConnector extends Connector {
    
    boolean lastAsk;
    SpecialConstructInfo info;
    
    /**
     * This method returns the name to use, and it depends on the
     * which connector was last accessed.
     * @return the name
     */
    public String getName()
    {
        return getName(lastAsk);
    }
    
    public String getName(boolean which)
    {
        String s = super.getName();
        if(which) {
            s = s + "_TRUE";
        } else {
            s = s + "_FALSE";
        }
        return s;
    }
    
    public OutputConnector(String name, SpecialConstructInfo info) {
        super(name);
        this.info = info;
    }
    
    public Connector getTrueConnector(Connector asker) {
        lastAsk = true;
        return this;
    }
    
    public Connector getFalseConnector(Connector asker) {
        lastAsk = false;
        return this;
    }
    
    public Connector getProcessConnector(Connector asker) {
        return leftInput.getProcessConnector(asker);
    }
        
}

