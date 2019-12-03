package sbn.gf.builder;

import net.seninp.jmotif.sax.SAXException;
import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;
import net.seninp.jmotif.sax.datastructure.SAXRecords;
import sbn.gf.entities.TweetTerm;

/**
 * This builder create a new TweetTerm computing also its SAX
 * @author Vamsi Gunturi
 */
public class TweetTermBuilder {
    // Size of SAX representation Alphabet
    int alphabetSize;
    // Sax threshold
    double nThreshold;
    // alphabet component for SAX
    NormalAlphabet na;
    
    SAXProcessor sp;

    /**
     * Initialize Builder params
     * @param alphabetSize
     * @param nThreshold
     */
    public TweetTermBuilder(int alphabetSize, double nThreshold) {
        this.alphabetSize = alphabetSize;
        this.nThreshold = nThreshold;
        this.na = new NormalAlphabet();
        this.sp = new SAXProcessor();
    }

    /**
     * Build a new TweetTerm
     * @param word word related to the TweetTerm
     * @param type TweetTerm type: text term or hashtag term
     * @param timeSeries TweetTerm time series
     * @param frequency TweetTerm frequency
     * @return a new TweetTerm
     * @throws SAXException
     */
    public TweetTerm build(String word, String type, double[] timeSeries, int frequency) throws SAXException {
        // Compute SAX
        SAXRecords res = sp.ts2saxByChunking(timeSeries, timeSeries.length, na.getCuts(alphabetSize), nThreshold);
        // Get sax representation
        String sax = res.getSAXString("");
        // Return a new TweetTerm
        return new TweetTerm(word, type, frequency, sax, timeSeries);
    }
}
