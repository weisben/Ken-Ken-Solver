import javax.swing.*;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Queue;
import java.text.DecimalFormat;

public class KenKenPlayer
{
    private final int PUZZLE_SIZE = 3;
    private final int NUM_CELLS = PUZZLE_SIZE * PUZZLE_SIZE;

    // final values must be assigned in vals[][]
    int[][] vals = new int[PUZZLE_SIZE][PUZZLE_SIZE];
    Cell[] cells = new Cell[NUM_CELLS];
    ArrayList<Arithmetic> regions = new ArrayList<Arithmetic>();
    //Board board = null;


        /// --- AC-3 Constraint Satisfication --- ///
   

    ArrayList<Integer>[] globalDomains = new ArrayList[NUM_CELLS];
    ArrayList<Integer>[] neighbors = new ArrayList[NUM_CELLS];
    Queue<Arc> globalQueue = new LinkedList<Arc>();


    private final void AC3Init(){
        // board.Clear();
		// recursions = 0; 

        allDiff(); // initializes neighbors and globalQueue

        initGlobalDomains(); // initialize domains for each cell

        // Initial call to backtrack() on cell 0 (top left)
        boolean success = backtrack(0,globalDomains);

        // Prints evaluation of run

        /*
         * sets final board values in vals
         */
        Finished(success);

    }

    /**
     * This method sets up the global domains for all cells
     */
    private void initGlobalDomains(){
        
        for (int i = 0; i < NUM_CELLS; i++){
            globalDomains[i] = new ArrayList<Integer>();
            for(int j = 1; j <= PUZZLE_SIZE; j++){
                globalDomains[i].add(j);
            }
        }
    }

    /*
     *  This method defines constraints between a set of variables.
     */
    private final void allDiff(){
        //initialize neighbors
        for (int i = 0; i < NUM_CELLS; i++){
            neighbors[i] = new ArrayList<Integer>();

            int row = i / PUZZLE_SIZE;
            int col = i % PUZZLE_SIZE;

            //populate rows
            for(int j = 0; j < PUZZLE_SIZE; j++){
                int neighbor_index = (row * PUZZLE_SIZE) + j;
                if (neighbor_index != i){
                    neighbors[i].add(neighbor_index);
                }
            }

            //populate columns
            for (int j = 0; j < PUZZLE_SIZE; j++){
                int neighbor_index = (j * PUZZLE_SIZE) + col;
                if (neighbor_index != i){
                    neighbors[i].add(neighbor_index);
                }
            }
            
            //use neighbors to create arcs and initialize global queue
            for (int k: neighbors[i]){
                Arc pair = new Arc(i, k, "diff");
                globalQueue.add(pair);
            }
        }

        for (Arithmetic box: regions){
            ArrayList<Integer> box_cells = box.cells;
            
            
        }
    }
    
    private final boolean backtrack(int cell, ArrayList<Integer>[] Domains) {

        if (cell >= NUM_CELLS){ // found a solution for the board
            return true;
        }

        int row = cell / PUZZLE_SIZE;
        int col = cell % PUZZLE_SIZE;

        // make a copy of domains so we don't modify global domains
        ArrayList<Integer>[] domain_copy = new ArrayList[81];
        for (int i = 0; i < NUM_CELLS; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }
        
        // checks if previous cell assignment is consistent
        if (!AC3(domain_copy)) { // AC3 found an empty domain
            return false; // backtrack and find another value
        } 

        // copy of cell's domain to be iterated through
        ArrayList<Integer> domain_values = new ArrayList<Integer>();
        for(int val : domain_copy[cell]){
            domain_values.add(val);
        }

        // find a value for this cell
        for (int value : domain_values) {
            // assign a value to current cell
            domain_copy[cell].clear();
            domain_copy[cell].add(value);

            // check if value works by recursively calling backtrack on next cell
            boolean consistent = backtrack(cell + 1, domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                vals[row][col] = value; 
                return true;
            }
        }
        return false;
    }

    private final boolean AC3(ArrayList<Integer>[] Domains) {

        // copy queue, initially all the arcs in csp
        Queue<Arc> Q = new LinkedList<Arc>();
        for(Arc a : globalQueue){
            Q.add(a);
        }
        
        // iterate through queue until there are no more arcs to revise over
        while(!Q.isEmpty()){
            Arc current_arc = Q.poll();
            int Xi = current_arc.Xi;
            int Xj = current_arc.Xj;

            boolean revised = Revise(current_arc, Domains);
            
            if(!revised) continue; // if the domain wasn't revised move to the next arc
            if (Domains[Xi].isEmpty()){ // an inconsistency was found
                return false;
            }
            // add other neighbors to queue
            for (int k: neighbors[Xi]){
                if (k != Xj){
                    Arc neighbor = new Arc(k, Xi, "diff");
                    boolean inQ = false;
                    // check if the arc is already in queue
                    for(Arc a : Q){ 
                        if(a.compareTo(neighbor) == 0) {
                            inQ = true;
                            break;
                        }
                    }
                    if (!inQ){ //add the arc if it was not already in the queue
                        Q.add(neighbor);
                    }
                }
            }
        }
        
		return true;
    }

    private final boolean Revise(Arc t, ArrayList<Integer>[] Domains){
        // extract endpoints of arc
    	int Xi = t.Xi;
        int Xj = t.Xj;

        // Domain of Xi needs to be revised only if the domain of Xj is a
        // singular value which is contained in the domain of Xi
        if(Domains[Xj].size() == 1 && Domains[Xi].contains(Domains[Xj].get(0))){
            Domains[Xi].remove(Domains[Xj].get(0));
            return true; 
        } else {
            return false;
        }
 	}

    private void Finished(boolean success){
    	
    	if(success){
            for(int i=0; i<NUM_CELLS; i++){
                int row = i / PUZZLE_SIZE;
                int col = i % PUZZLE_SIZE;
                System.out.println(vals[row][col]);
            }
        } else {
            System.out.println("Failed");
        }
    }

    class Cell {
        int cell_num;
        int val;
        Arithmetic arithmetic_box;

        public Cell(int cell_num, int val){
            this.cell_num = cell_num;
            this.val = val;
        }
        
        public void setArithmeticBox(Arithmetic box) {
            this.arithmetic_box = box;
        }

         public Arithmetic getArithmeticBox(){
            return this.arithmetic_box;
        }
        
    }

    class Arithmetic {
        int target;
        char operator;
        ArrayList<Integer> cells;
        
        public Arithmetic(int target, char operator, ArrayList<Integer> cells){
            this.target = target;
            this.operator = operator;
            this.cells = cells;
        }

        public Arithmetic(int target, ArrayList<Integer> cells){
            this.target = target;
            this.cells = cells;
        }
    }

    class Arc implements Comparable<Object>{
        int Xi, Xj;
        String constraintType;
        public Arc(int cell_i, int cell_j, String constraint_type){
            if (cell_i == cell_j){
                try {
                    throw new Exception(cell_i+ "=" + cell_j);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            Xi = cell_i;
            Xj = cell_j;
            constraintType = constraint_type;
        }

        public int compareTo(Object o){
            return this.toString().compareTo(o.toString());
        }

        public String toString(){
            return "(" + Xi + "," + Xj + ")";
        }
    }

    public final void initialize(){
        vals[0] = new int[] {0,0,0};
        vals[1] = new int[] {0,0,0};
        vals[2] = new int[] {0,0,0};
        
        regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(0,3))));
        regions.add(new Arithmetic(6, 'x', new ArrayList<Integer>(Arrays.asList(1,2))));
        regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(4,7))));
        regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(5,8))));
        regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(6))));
        
        for (Arithmetic box: regions){
            ArrayList<Integer> box_cells = box.cells;
            for (int i: box_cells){
                cells[i].setArithmeticBox(box);
            }
        }
    }
    
    public void run(){
        AC3Init();
    }

    public static void main(String[] args) {
        KenKenPlayer app = new KenKenPlayer();
        app.run();
    }
}
