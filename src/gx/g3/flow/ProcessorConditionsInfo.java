package gx.g3.flow; 

import java.util.*;

public class ProcessorConditionsInfo
{
    
    boolean leftRequired;
    boolean rightRequired;
    
    List<ProcessorCompareInfo> compares = new ArrayList<ProcessorCompareInfo>();
    List<ProcessorConditionInfo> conditions = new ArrayList<ProcessorConditionInfo>();
    
}
