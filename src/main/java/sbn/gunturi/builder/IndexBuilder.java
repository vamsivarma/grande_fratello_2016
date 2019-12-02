package sbn.gunturi.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import static org.apache.lucene.util.Version.LUCENE_41;
import twitter4j.TwitterException;

/**
 * Abstract class that collect all the methods and attributes that an 
 * IndexBuilder needs to have and extends.
 * 
 * @author Vamsi Gunturi
 */
public abstract class IndexBuilder {

    public static CharArraySet STOPWORDS;

    /**
     * Dircetory attribute where the index is going to be saved
     */
    public Directory dir;

    /**
     * Lucene analizer
     */
    public Analyzer analyzer;

    /**
     * Lucene index writer configuration attribute
     */
    public IndexWriterConfig cfg;

    /**
     * Lucene index writer
     */
    public IndexWriter writer;
    
    /**
     * Relative path of the source file from which extract information for the new index
     */
    public String sourcePath;

    /**
     * Relative path of the new index
     */
    public String indexPath;
    
    /**
     * Abstract method that build the index
     * @throws IOException
     * @throws TwitterException
     */
    public abstract void build() throws IOException, TwitterException;
    
    /**
     * Abstract method that build the index starting from a field name and its values of interest
     * @param fieldName Field name of another index
     * @param fieldValues Field values to match in the other index
     * @throws IOException
     */
    public abstract ArrayList<String> build(String fieldName, ArrayList<String> fieldValues) throws IOException;
    
    /**
     * Set the values of the builder
     * @param dirName dir in which inizialize the directory
     * @throws IOException
     */
    public void setBuilderParams(String dirName) throws IOException {
        
        this.dir = new SimpleFSDirectory(new File(dirName));
        this.analyzer = new ItalianAnalyzer(LUCENE_41);
        this.cfg = new IndexWriterConfig(LUCENE_41, analyzer);
        this.writer = new IndexWriter(dir, cfg);
    }
}
