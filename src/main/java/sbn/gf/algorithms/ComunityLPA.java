package sbn.gf.algorithms;

import it.stilo.g.structures.WeightedGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class compute comunityLPA
 * @author Vamsi Gunturi
 */
public class ComunityLPA implements Runnable {

    private static final Logger logger = LogManager.getLogger(ComunityLPA.class);

    private static Random rnd;
    private WeightedGraph g;

    private int chunk;
    private int runner;
    private CountDownLatch barrier;

    private int[] labels;
    private int[] list = null;
    
    //private static ArrayList<Integer> nList = new ArrayList<Integer>();

    private ComunityLPA(WeightedGraph g, CountDownLatch cb, int[] labels, int chunk, int runner) {
        this.g = g;
        this.runner = runner;
        this.barrier = cb;
        this.labels = labels;
        this.chunk = chunk;
        
        //Initialize the array list with 0's for getting the neightbourhood value
        //nList = initNList();
    }

    // Initialize labels list
    private boolean initList() {
        // Check that the list is not been initialized yet
        if (list == null) {
            // Partitioning over worker
            list = new int[(g.in.length / runner) + runner];
            
            // Iterator over the neighbours
            int j = 0;
            
            // For each node
            for (int i = chunk; i < g.in.length; i += runner) {
                // If the node has positive indegree and the label already assigned is 0
                // MEMO: The interest is in nodes with indegree positive that have
                //       not a yes or no label yet, that are not our authorities, 
                //       hubs or brokers. 
                if (g.in[i] != null && labels[i] == 0) {
                    // Save this node in the list of nodes that need a label
                    list[j] = i;
                    j++;
                }
            }
            // Resize the list
            list = Arrays.copyOf(list, j);

            //Shuffle
            for (int i = 0; i < list.length; i++) {
                for (int z = 0; z < 10; z++) {
                    int randomPosition = rnd.nextInt(list.length);
                    int temp = list[i];
                    list[i] = list[randomPosition];
                    list[randomPosition] = temp;
                }
            }

            return true;
        }
        return false;
    }

    public void run() {
        if (!initList()) {
            for (int i = 0; i < list.length; i++) {
                
                int[] near = g.in[list[i]];
                int[] nearLabs = new int[near.length];
                for (int x = 0; x < near.length; x++) {
                    nearLabs[x] = labels[near[x]];
                }

                int bl = bestLabel(nearLabs);

                if (bl != -1) {
                    //System.out.println("Best label: " + bl);
                    labels[list[i]] = bl;
                } else {
                    //System.out.println("No change");
                }
            }
        }
        barrier.countDown();
    }

    // Detect the label of a node looking at its neighbours
    public static int bestLabel(int[] neighborhood) {
        
        //Default to no change in current label which is -1
        int labelIndex = -1;
        
        //System.out.println("In the best label function");
        
        try {
            
            //Initialize the counters of 5 participants with 0's
            List<Integer> nList = Arrays.asList(0,0,0,0,0);

            // Each neighbour of the node
            for (int i = 0; i < neighborhood.length; i++) {

                int curLabel = neighborhood[i] - 1;
                
                if(curLabel > 0 && curLabel < nList.size()) {
                    int curC = nList.get(curLabel);
                    nList.set(curLabel, curC + 1);
                }

            }
            
            //System.out.println(nList);

            int maxVal = Collections.max(nList); 


            if(maxVal != 0) {
                // since labelling starts from 1 not 0
                labelIndex = nList.indexOf(maxVal) + 1;
            } 

            // System.out.println("Current neighbourhood label: " + labelIndex);
        
        } catch(Exception e) {
            System.out.println(e);
        }
       
        
        return labelIndex;
        
    }

    public static int[] compute(final WeightedGraph g, double threshold, int runner, int[] initLabels) {

        ComunityLPA.rnd = new Random(123454L);
        // Get initial label values
        int[] labels = initLabels;
        int[] newLabels = labels;
        int iter = 0;

        long time = System.nanoTime();
        CountDownLatch latch = null;

        ComunityLPA[] runners = new ComunityLPA[runner];

        for (int i = 0; i < runner; i++) {
            runners[i] = new ComunityLPA(g, latch, labels, i, runner);
        }

        ExecutorService ex = Executors.newFixedThreadPool(runner);

        do {
            iter++;
            labels = newLabels;
            newLabels = Arrays.copyOf(labels, labels.length);
            latch = new CountDownLatch(runner);

            //Label Propagation
            for (int i = 0; i < runner; i++) {
                runners[i].barrier = latch;
                runners[i].labels = newLabels;
                ex.submit(runners[i]);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                logger.debug(e);
            }
            
        } while (smoothEnd(labels, newLabels, iter, threshold));

        ex.shutdown();

        logger.info(((System.nanoTime() - time) / 1000000000d) + "\ts");
        return labels;
    }

    private static boolean smoothEnd(int[] labels, int[] newLabels, int iter, double threshold) {
        if (iter < 2) {
            return true;
        }

        int k = 3;

        if (iter > k) {
            int equality = 0;

            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == newLabels[i]) {
                    equality++;
                }
            }
            double currentT = (equality / ((double) labels.length));

            return !(currentT >= threshold);
        }
        return !Arrays.equals(labels, newLabels);
    }
    
    public static ArrayList<Integer> initNList() {
        
        //Initialize the counters of 5 participants with 0's
        List<Integer> temp = Arrays.asList(0,0,0,0,0);
        
        
        //Create a 
        ArrayList<Integer> initList = new ArrayList<Integer>();
         
        //Copy all items from list 1 to list 2
        initList.addAll(temp);
        
        return initList;
    
    }
}
