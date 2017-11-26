package gx.g3.flow; 

/**
 * This class implements the AND logic connector. Processing always
 * goes to our left input. Any request on the FALSE connector leads
 * to the output's FALSE connector.
 */
public class AndConnector extends Connector {
    
    
    public AndConnector(String name) {
        super(name);
    }
    
    public Connector getTrueConnector(Connector asker) {
        if(asker==leftInput) {
            return rightInput.getProcessConnector(this);
        } else if(asker==rightInput) {
            return output.getTrueConnector(this);
        }
        return null;
    }
    
    public Connector getFalseConnector(Connector asker) {
        if(asker==leftInput) {
            return output.getFalseConnector(this);
        } else if(asker==rightInput) {
            return output.getFalseConnector(this);
        }
        return null;
    }
    
    public Connector getProcessConnector(Connector asker) {
        return leftInput.getProcessConnector(this);
    }
    
   
}
