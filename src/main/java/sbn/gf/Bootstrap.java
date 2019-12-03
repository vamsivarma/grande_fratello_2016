package sbn.gf;

import java.io.IOException;
import sbn.gf.entities.Participant;


/**
 * Bootstrap point for the application 
 * @author Vamsi Gunturi
 */
public class Bootstrap {
    
    private static Participant p = new Participant();

    
    
    /**
     * Main method from which the whole analysis starts 
     * This method invokes all the analysis we performed
     * 1) Temporal Analysis 
     * 2) Supporter Analysis 
     * 3) Spread of Influence
     * @param args
     */
    public static void main(String[] args) throws IOException {
        
        // Step 1
        // Temporal analysis
        TemporalAnalysis.temporal_analysis();
        
       
        // Step 2
        // For performing supporter analysis
        SupporterAnalysis.supporter_analysis();

        // Step 3    
        // For performing spread of influence
        SOIAnalysis.spread_of_influence();
    }   
}
