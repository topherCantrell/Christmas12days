package gx.g3.flow; 

import java.util.*;

/**
 * This class represents a node in a binary expression tree.
 */
public class ExpressionNode {

    String expression;        // The expression at this node
    boolean connectedToLeft;  // True if connected to the left side of parent
    ExpressionNode parent;    // This node's parent
    ExpressionNode left;      // The child to the left (if any)
    ExpressionNode right;     // The child to the right (if any)

    /**
     * This method builds an expression tree from the single root node.
     * @param master coming in the list contains the single root node and
     *   going out it contains the nodes in the expanded expression tree
     *   (the first node is still the root)
     */
    public static void processExpressionList(List<ExpressionNode> master) {
        boolean changed = true;
        while(changed) {
            changed = false;
            for(int x=0;x<master.size();++x) {
                ExpressionNode n = (ExpressionNode)master.get(x);                
                boolean b = processExpressionNode(master,n);
                if(b) {
                    changed=true;
                    break;
                }
            }
        }
    }
    
    /**
     * This method turns the input node into a parent-and-two-children adding
     * the new children to the growing list of nodes.
     * @param master the list of nodes
     * @param node the node to expand
     */
    public static boolean processExpressionNode(List<ExpressionNode> master, ExpressionNode node) {
        
        String e = node.expression;
        
        // Don't do anything if this node has already been processed
        if(e.equals("||") || e.equals("&&")) {
            return false;
        }

        // Convert the String expression into left-operator-right form
        ParseLeftRightOpInfo pi = CodeParser.parseLogicConnector(e);        

        // Don't do anything if this node is a leaf 
        if(pi.right==null) {
            return false;
        }
                
        ExpressionNode nLeft = new ExpressionNode();
        ExpressionNode nRight = new ExpressionNode();
        
        nLeft.expression = pi.left;
        nLeft.connectedToLeft = true;
        nLeft.parent = node;
        
        nRight.expression = pi.right;
        nRight.connectedToLeft = false;
        nRight.parent = node;
        
        node.left = nLeft;
        node.right = nRight;
        
        node.expression = pi.operator;
        
        master.add(nLeft);
        master.add(nRight);
        
        return true;
        
    }


}
