package sbn.gf.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import twitter4j.TwitterException;
import sbn.gf.builder.SupportersIndexBuilder;

/**
 * This class manage the creation and the comunication with indices of
 * supporters
 * @author Vamsi Gunturi
 */
public class SupportersIndexManager extends IndexManager {


    public SupportersIndexManager(String indexPath) throws IOException {
        
        super(indexPath);
        
        //this.setReader(this.indexPath);

    }

    @Override
    public void create(String sourcePath) {
        try {
            // Create a new supporters index builder
            SupportersIndexBuilder sib = new SupportersIndexBuilder(indexPath, sourcePath);
            sib.build();

        } catch (IOException ex) {
            Logger.getLogger(SupportersIndexManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TwitterException ex) {
            Logger.getLogger(SupportersIndexManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public ArrayList<String> create(String sourcePath, String fieldName, ArrayList<String> fieldValues) {
        // Not implemented version
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
