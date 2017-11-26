package gx.g3.flow; 

/**
 * This class implements the AND logic connector. 
 */
public class NotConnector extends Connector {
    
    
    public NotConnector(String name) {
        super(name);
    }
    
    public Connector getTrueConnector(Connector asker) {
        if(asker==leftInput) {
            return output.getFalseConnector(this);
        }
        return null;
    }
    
    
    public Connector getFalseConnector(Connector asker) {
        if(asker==leftInput) {
            return output.getTrueConnector(this);
        }
        return null;
    }
    
    public Connector getProcessConnector(Connector asker) {
        return leftInput.getProcessConnector(this);
    }
    
}
