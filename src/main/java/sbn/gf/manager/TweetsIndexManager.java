package sbn.gf.manager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.seninp.jmotif.sax.SAXException;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import twitter4j.TwitterException;
import sbn.gf.entities.TweetTerm;
import sbn.gf.builder.TweetTermBuilder;
import sbn.gf.builder.TweetsIndexBuilder;

/**
 * This class manage the creation and the communication with indices of twitter messages
 * @author Vamsi Gunturi
 */
public class TweetsIndexManager extends IndexManager {
    // Last tweet posted timestamp
    private static long max = 1481036346994L;
    // First tweet posted timestamp
    private static long min = 1480170614348L;
    
    public static Set<String> stopWords = null;

    /**
     * Constructor that calls the super()
     * @param indexPath index source relative path
     */
    public TweetsIndexManager(String indexPath) throws IOException {
        super(indexPath);
        
        
        if(TweetsIndexManager.stopWords == null) {
            System.out.println("Adding stop words to the Hashset....");
            this.loadStopWords();
        
        }
        
        //this.setReader(this.indexPath);
    }

    @Override
    // From stream
    public void create(String sourcePath) {
        System.out.println("Tweets Index Creation!");
        // Initialize a new TweetsIndexBuilder
        TweetsIndexBuilder tib = new TweetsIndexBuilder(sourcePath, indexPath);
        try {
            
            // Build the index
            tib.build();
            
        } catch (IOException ex) {
            System.out.println("---> Problems with source files: IOException <---");
            ex.printStackTrace();
        } catch (TwitterException ex) {
            System.out.println("---> Problems with Tweets: TwitterException <---");
            ex.printStackTrace();
        }
    }

    @Override
    //from index
    public ArrayList<String> create(String sourcePath, String fieldName, ArrayList<String> fieldValues) {
        // Initialize a new TweetsIndexBuilder
        TweetsIndexBuilder tib = new TweetsIndexBuilder(sourcePath, indexPath);
        
        ArrayList<String> filList = new ArrayList<String>();
        
        try {
            // Build the index
            filList = tib.build(fieldName, fieldValues);
        } catch (IOException ex) {
            System.out.println("---> Problems with source files: IOException <---");
            ex.printStackTrace();
        }
        
        return filList;
    }

    

    /**
     * Search relevant terms in a list of fields using a specific regex and 
     * considering its curve with a specific stepsize
     * @param relevantFields field of interest
     * @param regex regex that the term curve have to match
     * @param stepSize time interval to divide the curve
     * @return A list of relevant terms
     */
    public ArrayList<TweetTerm> getRelFieldTerms(String[] relevantFields, String regex, long stepSize) {
        try {
            // Set manager params
            setReader(indexPath);

            // Output list
            ArrayList<TweetTerm> relWords = new ArrayList<TweetTerm>();

            // Obtain index fields
            Fields fields = MultiFields.getFields(ir);
            
            // long matchCounter = 0;

            // For each relevant field
            for (String relField : relevantFields) {
                // get all the terms in that field
                Terms terms = fields.terms(relField);
                
                // Inizialize an iterator over the terms
                TermsEnum termsEnum = terms.iterator(null);

                // Term frequency
                long freq;
                // Word related to the term
                String word;
                
                // Initialize a builder
                // @TODO: Need to check the significance of this threshold
                TweetTermBuilder twb = new TweetTermBuilder(2, 0.01);

                // For each term
                while (termsEnum.next() != null) {
                    // Set its frequency
                    freq = termsEnum.totalTermFreq();
                    // Get the word and save it
                    BytesRef byteRef = termsEnum.term();
                    word = byteRef.utf8ToString();
                    // Compute its timeseries
                    double[] wordValues = getTermTimeSeries(word, relField, stepSize);
                    // Generate a new TweetTerm object
                    TweetTerm tw = twb.build(word, relField, wordValues, (int) freq);
                    
                    // If its SAX representation matches the regex add it to the relevant words
                    if (tw.getSaxRep().matches(regex) && ! TweetsIndexManager.stopWords.contains(tw.getWord())) {
                        relWords.add(tw);
                    }
                    
                    // Add only if the current term is not a stop and if the word matches a particular pattern
                    //if (tw.getSaxRep().matches(regex) && ! TweetsIndexManager.stopWords.contains(tw.getWord())) {
                    //if (! TweetsIndexManager.stopWords.contains(tw.getWord())) {
                    //    relWords.add(tw);
                    //}
                }
            }
            
            // System.out.println("Words not matching criteria: " + matchCounter );
            
            // Sort the collection by frequency
            Collections.sort(relWords);
            // Get the first 1000
            relWords = (ArrayList) relWords.stream().limit(1000).collect(Collectors.toList());
            // return the list of relevant words
            return relWords;

        } catch (IOException ex) {
            Logger.getLogger(TweetsIndexManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(TweetsIndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    private void loadStopWords() throws FileNotFoundException, IOException {
        
        TweetsIndexManager.stopWords = new HashSet<>();
        
        FileInputStream stream = new FileInputStream("input/stopwords.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String sw;
        while ((sw = br.readLine()) != null) {
            //System.out.println(sw);
            TweetsIndexManager.stopWords.add(sw);
        }
    }

    /**
     * Obtain the frequency of a term during regular time intervals
     * @param term term of interest
     * @param field field in which search the term
     * @param stepSize time interval size
     * @return the time series
     */
    public double[] getTermTimeSeries(String term, String field, double stepSize) {
        // Initialize the size of the output array
        int arraySize = (int) (((max - min) / stepSize) + 1);
        // Initialize the output array
        double[] wordValues = new double[arraySize];
        // Execute the search and get the resulting documents
        ScoreDoc[] scoreDocs = searchTermInAField(term, field);

        try {
            // For each document found
            for (ScoreDoc sd : scoreDocs) {
                int i;
                // Obtain the time interval correspondig to the timestamp of the tweet
                i = (int) ((Long.parseLong(ir.document(sd.doc).get("date")) - min) / stepSize);
                // increment the number of documents found in that interval
                wordValues[i]++;
            }
        } catch (IOException ex) {
            Logger.getLogger(TweetsIndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return wordValues;
    }

    /**
     * Search document in which appear at least one of component of compList
     * @param compList List of lists of terms which we want to consider for the search
     * @return an array of documents that match the query
     */
    public ScoreDoc[] searchORANDCondInAField(ArrayList<ArrayList<String>> compList) {
        // Create a boolean query
        BooleanQuery query = new BooleanQuery();
        // For each component in the list
        for (ArrayList<String> comp : compList) {
            // Create a new boolean query
            BooleanQuery compQuery = new BooleanQuery();
            // For each term in the component
            for (String term : comp) {
                // If it is an hashtag
                if (term.startsWith("#")) {
                    // Add a clause searching the term in the hashtag field
                    compQuery.add(new TermQuery(new Term("hashtags", term)), BooleanClause.Occur.MUST);
                } else {
                    // Otherwise add a clause searching in tweetText
                    compQuery.add(new TermQuery(new Term("tweetText", term)), BooleanClause.Occur.MUST);
                }
            }
            // Add the boolean query obtained by a component to the query
            query.add(compQuery, BooleanClause.Occur.SHOULD);
        }

        try {
            // Execute the query
            TopDocs hits = searcher.search(query, 10000000);
            // Get the resulting documents
            ScoreDoc[] scoreDocs = hits.scoreDocs;

            return scoreDocs;

        } catch (IOException ex) {
            Logger.getLogger(IndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
