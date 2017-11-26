package gx.g3.flow; 

/**
 * This class implements the OR logic connector. Processing always
 * goes to our left input.
 */
public class OrConnector extends Connector {
    
    
    public OrConnector(String name) {
        super(name);
    }
    
    public Connector getTrueConnector(Connector asker) {
        if(asker==leftInput) {
            return output.getTrueConnector(this);
        } else if(asker==rightInput) {
            return output.getTrueConnector(this);
        }
        return null;
    }
    
    public Connector getFalseConnector(Connector asker) {
        if(asker==leftInput) {
            return rightInput.getProcessConnector(this);
        } else if(asker==rightInput) {
            return output.getFalseConnector(this);
        }
        return null;
    }
    
    public Connector getProcessConnector(Connector asker) {
        return leftInput.getProcessConnector(this);
    }
    
}

