package sbn.gunturi.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

/**
 * Abstract class that collect all the methods and attributes that an 
 * IndexManager needs to have and extends.
 * @author Vamsi Gunturi
 */
public abstract class IndexManager {

    /**
     * Relative path to the index physical location.
     */
    public String indexPath;

    /**
     * Lucene object that allow to interact with the index.
     */
    public IndexReader ir;

    /**
     * Lucene object that allow to interact with the index.
     */
    public static IndexSearcher searcher;

    /**
     * Constructor that save the location were the index is or will be created.
     * @param indexPath relative path of the index location
     */
    public IndexManager(String indexPath) throws IOException {
        this.indexPath = indexPath;
    }

    /**
     * Create a new index using data stored in sourcePath.
     * @param sourcePath Relative path to the physical location were are stored data need for index generation
     */
    public abstract void create(String sourcePath);

    /**
     * Create a new index using data stored in an other. In particoular data of a specific field that match specific values.
     * 
     * @param sourcePath Relative path to the other index physical location
     * @param fieldName field name of source index needed for the creation of the new one
     * @param fieldValues values to match in the field in order to get right data
     */
    public abstract ArrayList<String> create(String sourcePath, String fieldName, ArrayList<String> fieldValues);

    /**
     * Collect all documents that match the fieldValue for a specific fieldName.
     * 
     * @param fieldName name of the field where the match has to be verified
     * @param fieldValue value to match
     * @param range number of results to return
     * @return Return an ArrayList of matched documents.
     */
    public ArrayList<Document> searchForField(String fieldName, String fieldValue, int range) {
        try {
            
            this.setReader(this.indexPath);
            
            // Set index params (path, ir and searcher
            Query q;
            
            
            /*if (fieldValue instanceof String) {
                q = new TermQuery(new Term(fieldName, fieldValue));
            } else {
                BytesRef ref = new BytesRef();
                NumericUtils.longToPrefixCoded(Long.parseLong(fieldValue), 0, ref);
                q = new TermQuery(new Term(fieldName, ref));
            }

            TopDocs top = searcher.search(q, range);
            ScoreDoc[] hits = top.scoreDocs;

            if (hits.length == 0) {
                return null;
            }

            int resultArraySize = 0;
            // calculate the size of the result array
            if (range < hits.length) {
                resultArraySize = range;
            } else {
                resultArraySize = hits.length;
            }
            
            // Collect the documents inside the query results
            ArrayList<Document> results = new ArrayList<>();

            for (ScoreDoc entry : hits) {
                Document doc = searcher.doc(entry.doc);
                results.add(doc);
            }

            return results;*/
            
            
            //System.out.println("Current index path: " + this.indexPath);
            
            // if the field is a LongField
            if (fieldName.equals("date") || fieldName.equals("userId")) {
                //Convert the fieldValue in a ByteRef
                BytesRef ref = new BytesRef();
                NumericUtils.longToPrefixCoded(Long.parseLong(fieldValue), 0, ref);
                // Create the query
                q = new TermQuery(new Term(fieldName, ref));
            } else {
                // Create the query
                q = new TermQuery(new Term(fieldName, fieldValue));
            }

            // Execute the query
            TopDocs top = searcher.search(q, range);
            // Get query results
            ScoreDoc[] hits = top.scoreDocs;
            
            // Collect the documents inside the query results
            ArrayList<Document> results = new ArrayList<>();

            for (ScoreDoc entry : hits) {
                Document doc = searcher.doc(entry.doc);
                results.add(doc);
            }
            
            // Return the list of Docs
            return results;

        } catch (IOException ex) {
            System.out.println("---> Problems with source files: IOException <---");
            ex.printStackTrace();

            return null;
        }
    }
    
    
    /**
     * Collect all documents that match the fieldValue in any of the two fieldNames.
     * 
     * @param fieldName1 name of the first field where the match has to be verified
     * @param fieldName2 name of the second field where the match has to be verified
     * @param fieldValue value to match
     * @param range number of results to return
     * @return Return an ArrayList of matched documents.
     */
    public ArrayList<Document> searchForTwoFields(String fieldName1, String fieldName2, String fieldValue, int range) throws IOException {
            
        
            System.out.println(this.indexPath);
            
            this.setReader(this.indexPath);
          
            // Create two TermQueries
            TermQuery t1 = new TermQuery(new Term(fieldName1, fieldValue));
            TermQuery t2 = new TermQuery(new Term(fieldName2, fieldValue));

            // Create a BooleanQuery
            BooleanQuery query = new BooleanQuery();
            // Both terms MUST appears
            query.add(t1, BooleanClause.Occur.SHOULD);
            query.add(t2, BooleanClause.Occur.SHOULD);

            try {
                // Execute the query
                TopDocs hits = searcher.search(query, 1000000);
                // Collect and return the results
                ScoreDoc[] scoreDocs = hits.scoreDocs;   
            
                // Collect the documents inside the query results
                ArrayList<Document> results = new ArrayList<>();

                for (ScoreDoc entry : scoreDocs) {
                    Document doc = searcher.doc(entry.doc);
                    results.add(doc);
                }

                // Return the list of Docs
                return results;
                

            } catch (IOException ex) {
                Logger.getLogger(IndexManager.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
       
    }

    /**
     * Collect all documents that match the fieldValue for a specific list of fieldNames.
     * @param fieldName name of the field where the match has to be verified
     * @param fieldValues list of values to match
     * @param range number of results to return
     * @return Return an ArrayList of matched documents.
     */
    public ArrayList<Document> searchForField(String fieldName, ArrayList<String> fieldValues, int range) {

        ArrayList<Document> results = new ArrayList<>();
        //this.setReader(this.indexPath);
        // For each value to match...
        for (String fieldValue : fieldValues) {
            // ...compute the query and join the results
            results.addAll(searchForField(fieldName, fieldValue, range));
        }
        
        // Return the list of Docs
        return results;
    }

    /**
     * Return a list of Strings that represent the value of each document of the list for a specific field.
     * @param docs List of document to examinate
     * @param fieldName Field of interest
     * @return Return a list of Strings that represent the value of each document of the list for a specific field.
     */
    public ArrayList<String> getFieldValuesList(ArrayList<Document> docs, String fieldName) throws IOException {
        //this.setReader(this.indexPath);
        
        ArrayList<String> results = new ArrayList<String>();
        // For each document...
        for (Document doc : docs) {
            // ...append the value of fieldName to the output list
            results.add(doc.get(fieldName));
        }
        
        // return the list of values
        return results;
    }

    /**
     * Return the list of values of a specific field in a collection of documents selected by the match of fieldValues for a specific fieldName. 
     * @param filterFieldName name of field to use to match documents
     * @param filterFieldValues list of values to match in the index for a specific field name
     * @param fieldOfInterest field from which get values
     * @param range number of results
     * @return Return the list of a values of a specific field in a collection of documents selected by the match of fieldValues for a specific fieldName. 
     */
    public ArrayList<String> searchFilteredValueField(String filterFieldName,
            ArrayList<String> filterFieldValues, String fieldOfInterest, int range) throws IOException {
        return getFieldValuesList(searchForField(filterFieldName, filterFieldValues, range), fieldOfInterest);
    }

    /**
     * Return the list of values of a specific field in a collection of documents selected by the match of a single fieldValue for a specific fieldName. 
     * @param filterFieldName name of field to use to match documents
     * @param filterFieldValue value to match in the index for a specific field name
     * @param fieldOfInterest field from which get values
     * @param range number of results
     * @return Return the list of a values of a specific field in a collection of documents selected by the match of fieldValues for a specific fieldName. 
     */
    public ArrayList<String> searchFilteredValueField(String filterFieldName,
            String filterFieldValue, String fieldOfInterest, int range) throws IOException {
        return getFieldValuesList(searchForField(filterFieldName, filterFieldValue, range), fieldOfInterest);
    }

    /**
     * Return the size of the index.
     * @return
     */
    public int getIndexSizes() {
        try {
            // open the index
            Directory dir = new SimpleFSDirectory(new File(indexPath));
            IndexReader ir = DirectoryReader.open(dir);

            dir.close();

            // return the number of docs
            return ir.numDocs();

        } catch (IOException ex) {
            System.out.println("---> Problems with source files: IOException <---");
            ex.printStackTrace();
            return -1;
        }
    }
    
    public static String prevIndexPath = "";

    /**
     * Set parameters of the index.
     * @param indexPath relative path of the index
     * @throws IOException
     */
    public void setReader(String indexPath) throws IOException {
        
        //System.out.println("Prev Index path:" + prevIndexPath);
        //System.out.println("Current Index path:" + indexPath);
        
        //@TODO: Need to do this more efficiently
        if(prevIndexPath != indexPath) {
            
            //System.out.println("Before directory open");
            
            Directory dir = new SimpleFSDirectory(new File(indexPath));

            ir = DirectoryReader.open(dir);
            searcher = new IndexSearcher(ir);
            //ir.close();
            dir.close();
            
            prevIndexPath = indexPath;
        }
        
        
    }

    /**
     * Return all the documents where the term is present in a specific field.
     * @param term term to search
     * @param field field in which the term has to been searched
     * @return Return all the documents where the term is present in a specific field.
     */
    public ScoreDoc[] searchTermInAField(String term, String field) {
        try {
            // Set index params
            //setReader(this.indexPath);
            // Create a new query specifing field and term
            TermQuery t = new TermQuery(new Term(field, term));
            BooleanQuery query = new BooleanQuery();
            // the term MUST occur in the field
            query.add(t, BooleanClause.Occur.MUST);
            
            // Execute the query
            TopDocs hits = searcher.search(query, 1000000);
            
            // Collect and return the results
            ScoreDoc[] scoreDocs = hits.scoreDocs;
            return scoreDocs;

        } catch (IOException ex) {
            Logger.getLogger(IndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Return an array of docs were term1 appears in field1 and term2 appears in field2.
     * @param term1 term to search in field1
     * @param field1 field in which term1 has to been searched
     * @param term2 term to search in field2
     * @param field2 field in which term2 has to been searched
     * @return Return an array of docs were term1 appears in field1 and term2 appears in field2.
     */
    public ScoreDoc[] searchTwoTermsInFields(String term1, String field1, String term2, String field2) {
        // Create two TermQueries
        TermQuery t1 = new TermQuery(new Term(field1, term1));
        TermQuery t2 = new TermQuery(new Term(field2, term2));
        
        // Create a BooleanQuery
        BooleanQuery query = new BooleanQuery();
        // Both terms MUST appears
        query.add(t1, BooleanClause.Occur.MUST);
        query.add(t2, BooleanClause.Occur.MUST);

        try {
            // Execute the query
            TopDocs hits = searcher.search(query, 1000000);
            // Collect and return the results
            ScoreDoc[] scoreDocs = hits.scoreDocs;
            return scoreDocs;

        } catch (IOException ex) {
            Logger.getLogger(IndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Return all documents in which appears at least one term of the list.
     * @param terms list of terms to match
     * @param field field in which search terms
     * @return Return all documents in which appears at least one term of the list.
     */
    public ScoreDoc[] searchTermsInAField(ArrayList<String> terms, String field) throws IOException {
        
        setReader(this.indexPath);
        
        //System.out.println("Mentioned search path: " + this.indexPath);
        
        // Create a BooleanQuery
        BooleanQuery query = new BooleanQuery();
        // For each term in the list...
        for (String term : terms) {
            //System.out.println(term);
            // ...create a TermQuery for it with the Clause SHOULD
            query.add(new TermQuery(new Term(field, term)), BooleanClause.Occur.SHOULD);
        }

        try {
            // Execute the query
            TopDocs hits = searcher.search(query, 10000000);
            // Collect and return the results
            ScoreDoc[] scoreDocs = hits.scoreDocs;
            return scoreDocs;

        } catch (IOException ex) {
            Logger.getLogger(IndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Return all the documents of an index in a list.
     * @return 
     */
    public ArrayList<Document> getAllDocs() {
        try {
            setReader(this.indexPath);
            ArrayList<Document> results = new ArrayList<Document>();

            for (int i = 0; i < ir.numDocs(); i++) {
                results.add(ir.document(i));
            }

            return results;

        } catch (IOException ex) {
            Logger.getLogger(IndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
