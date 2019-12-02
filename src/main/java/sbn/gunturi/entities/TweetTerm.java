package sbn.gunturi.entities;

/**
 * This entity represents all the attributes of tweet term for our amalysis
 * @author Vamsi Gunturi
 */
public class TweetTerm implements Comparable<TweetTerm>{
    // The word
    private String word;
    // Identifies if the term is a word or a hashtag
    private String type;
    // Number of times the term is used
    private int frequency;
    // String of letters representing the trend of the term curve
    private String saxRep;
    // The trend of the curve
    private double[] timeSeries;
    // Binary representation of SAX string
    private double[] binaryRep;

    /**
     * Initialize some values of the TweetTerm
     * @param word the word related to the term
     * @param type Identifies if the term is a word or a hashtag
     * @param frequency Number of times the term is used
     * @param saxRep String of letters representing the trend of the term curve
     * @param timeSeries The trend of the curve
     */
    public TweetTerm(String word, String type, int frequency, String saxRep, double[] timeSeries) {
        this.word = word;
        this.type = type;
        this.frequency = frequency;
        this.saxRep = saxRep;
        this.timeSeries = timeSeries;
        // Set the value of binaryRep
        toBinary();
    }

    /**
     *
     * @return word
     */
    public String getWord() {
        return word;
    }

    /**
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @return frequency
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     *
     * @return SAX representation
     */
    public String getSaxRep() {
        return saxRep;
    }

    /**
     *
     * @return Time series
     */
    public double[] getTimeSeries() {
        return timeSeries;
    }

    /**
     *
     * @return binary representation of SAX string
     */
    public double[] getBinaryRep() {
        return binaryRep;
    }

    /**
     * Compare the frequency between two TweetTerm object
     * @param t the other TweetTerm
     * @return the difference between the two frequencies
     */
    @Override
    public int compareTo(TweetTerm t) {
        return t.getFrequency() - this.frequency;
    }
    
    // Transform the SAX string in a binary array and save it to binaryRep
    private void toBinary(){
        // Initialize binaryRep
        this.binaryRep = new double[saxRep.length()]; 
        // For each letter of SAX string
        for(int i = 0; i < saxRep.length(); i++){
            // If it is an A
            if(saxRep.charAt(i) == 'a')
                // Add a 0
                binaryRep[i] = 0;
            else
                // Otherwise add a 1
                binaryRep[i] = 1;
        }
    }
    
    
}
