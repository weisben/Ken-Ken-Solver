import javax.swing.*;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Queue;
import java.text.DecimalFormat;

public class KenKenPlayer
{
    private final int PUZZLE_SIZE = 4;
    private final int NUM_CELLS = PUZZLE_SIZE * PUZZLE_SIZE;
    private final String difficulty = "add-only4";

    Cell[] cells = new Cell[NUM_CELLS];
    
    //Board board = null;


    /// --- AC-3 Constraint Satisfication --- ///
   

    ArrayList<Integer>[] globalDomains = new ArrayList[NUM_CELLS];
    ArrayList<Integer>[] neighbors = new ArrayList[NUM_CELLS];
    ArrayList<Arithmetic> regions = new ArrayList<Arithmetic>();
    Queue<Arc> globalQueue = new LinkedList<Arc>();


    private final void AC3Init(){
        // board.Clear();
		// recursions = 0; 

        initGlobalDomains(); // initialize domains for each cell

        allDiff(); // initializes neighbors and globalQueue   
        
        for(Arc a: globalQueue){
            System.out.println(a);
        }

        // Initial call to backtrack() on cell 0 (top left)
        boolean success = backtrack(0,globalDomains);

        // Prints evaluation of run

        /*
         * sets final board values in vals
         */
        Finished(success);

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
                Arc pair = new Arc(i, k);
                globalQueue.add(pair);
            }
        }

        for (Arithmetic box: regions){      
            if(box.operator != '#'){
                for(int i = 0; i < box.cells.size(); i++){
                    int cell = box.cells.get(i);
                    ArrayList<Integer> neighbors = new ArrayList<Integer>();
                    for(int j = 0; j < box.cells.size(); j++){
                        if(i!=j) neighbors.add(box.cells.get(j));
                    }
                    Arc group = new Arc(cell, neighbors, box.target, box.operator);
                    globalQueue.add(group);
                }
                
            } else {
                int cell_num = box.cells.get(0);
                globalDomains[cell_num].clear();
                globalDomains[cell_num].add(box.target);
            }
        }
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

    
    
    private final boolean backtrack(int cell_num, ArrayList<Integer>[] Domains) {
        if (cell_num >= NUM_CELLS){ // found a solution for the board
            return true;
        }

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
        for(int val : domain_copy[cell_num]){
            domain_values.add(val);
        }

        // find a value for this cell
        for (int value : domain_values) {
            // assign a value to current cell
            domain_copy[cell_num].clear();
            domain_copy[cell_num].add(value);

            // check if value works by recursively calling backtrack on next cell
            boolean consistent = backtrack(cell_num + 1, domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                cells[cell_num].val = value; 
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

            if(current_arc.constraintType == "diff"){
                int Xi = current_arc.cell_num;
                int Xj = current_arc.neighbors.get(0);

                boolean revised = ReviseDiff(current_arc, Domains);
                
                if(!revised) continue; // if the domain wasn't revised move to the next arc
                if (Domains[Xi].isEmpty()){ // an inconsistency was found
                    return false;
                }
                // add other neighbors to queue
                for (int k: neighbors[Xi]){
                    if (k != Xj){
                        Arc neighbor = new Arc(k, Xi);
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
            } else { //constraintType == "math"
                boolean revised = ReviseMath(current_arc, Domains);
                if(!revised) continue; // if the domain wasn't revised move to the next arc
                if (Domains[current_arc.cell_num].isEmpty()){ // an inconsistency was found
                    return false;
                }
            }
            
        }
        
		return true;
    }

    private final boolean ReviseDiff(Arc t, ArrayList<Integer>[] Domains){
        // extract endpoints of arc
        int Xi = t.cell_num;
        int Xj = t.neighbors.get(0);

        // Domain of Xi needs to be revised only if the domain of Xj is a
        // singular value which is contained in the domain of Xi
        if(Domains[Xj].size() == 1 && Domains[Xi].contains(Domains[Xj].get(0))){
            Domains[Xi].remove(Domains[Xj].get(0));
            return true; 
        } else {
            return false;
        }       
 	}

    private final boolean ReviseMath(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;
        ArrayList<Integer> dom = Domains[t.cell_num];
        switch(t.operator){
            case '+':
                int sum = 0;
                for(int i: t.neighbors){
                    sum += min(Domains[i]);
                }
                for(int i=0; i<dom.size(); i++){
                    if(dom.get(i) + sum > t.target){
                        dom.remove(i);
                        revised = true;
                    }
                }
                break;
            case '-':

                break;
            case '*':
                int prod = 1;
                for(int i: t.neighbors){
                    prod *= min(Domains[i]);
                }
                for(int i=0; i<dom.size(); i++){
                    if(dom.get(i) * prod > t.target){
                        dom.remove(i);
                        revised = true;
                    }
                }
                break;
            case '/':

                break;
        }
        return revised;
    }

    private int min(ArrayList<Integer> L){
        // int min_index = -1;
        int min_value = PUZZLE_SIZE;
        for(int i=0; i<L.size(); i++){
            if(L.get(i) < min_value){
                min_value = L.get(i);
                // min_index = i;
            }
        }  
        return min_value; 
    }

    private void Finished(boolean success){
    	
    	if(success){
            printBoard();
        } else {
            System.out.println("Failed");
        }
    }

    private void printBoard(){
        for(int i=0; i<PUZZLE_SIZE; i++){
            for(int j=0; j<PUZZLE_SIZE; j++){
                int cellNum = i * PUZZLE_SIZE + j;
                System.out.print(cells[cellNum].val);
            }
            System.out.println();
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
            this.operator = '#';
        }
    }

    class Arc implements Comparable<Object>{
        int cell_num;
        ArrayList<Integer> neighbors;
        int target;
        char operator;
        String constraintType;

        public Arc(int cell_i, int cell_j){
            if (cell_i == cell_j){
                try {
                    throw new Exception(cell_i+ "=" + cell_j);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            this.cell_num = cell_i;
            this.neighbors = new ArrayList<Integer>(Arrays.asList(cell_j));
            constraintType = "diff";
        }

        public Arc(int cell, ArrayList<Integer> neighbors, int target, char operator){
            this.target = target;
            this.operator = operator;
            this.cell_num = cell;
            this.neighbors = neighbors;
            constraintType = "math";
        }

        public int compareTo(Object o){
            return this.toString().compareTo(o.toString());
        }

        public String toString(){
            return "(" + cell_num + "->" + neighbors + ")";
        }
    }

    public final void initialize(String difficulty){
        for(int cell_num = 0; cell_num < NUM_CELLS; cell_num++){
            cells[cell_num] = new Cell(cell_num, 0);
        }
        
        switch(difficulty){
            case "3x3":
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(0,3))));
                regions.add(new Arithmetic(6, 'x', new ArrayList<Integer>(Arrays.asList(1,2))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(4,7))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(5,8))));
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(6))));
                break;
            case "add-only3":
                regions.add(new Arithmetic(3, '+', new ArrayList<Integer>(Arrays.asList(0,1))));
                regions.add(new Arithmetic(4, '+', new ArrayList<Integer>(Arrays.asList(2,5))));
                regions.add(new Arithmetic(4, '+', new ArrayList<Integer>(Arrays.asList(3,6))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(4,7,8))));
                break;
            case "add-only4":
                regions.add(new Arithmetic(1, new ArrayList<Integer>(Arrays.asList(0))));
                regions.add(new Arithmetic(4, new ArrayList<Integer>(Arrays.asList(15))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(1,5))));
                regions.add(new Arithmetic(5, '+', new ArrayList<Integer>(Arrays.asList(2,3))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(4,8))));
                regions.add(new Arithmetic(3, '+', new ArrayList<Integer>(Arrays.asList(6,7))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(9,10,11))));
                regions.add(new Arithmetic(6, '+', new ArrayList<Integer>(Arrays.asList(12,13,14))));
                break;
            case "trivial":
                regions.add(new Arithmetic(3, new ArrayList<Integer>(Arrays.asList(0))));
                regions.add(new Arithmetic(1, new ArrayList<Integer>(Arrays.asList(1))));
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(2))));
                regions.add(new Arithmetic(1, new ArrayList<Integer>(Arrays.asList(3))));
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(4))));
                regions.add(new Arithmetic(3, new ArrayList<Integer>(Arrays.asList(5))));
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(6))));
                regions.add(new Arithmetic(3, new ArrayList<Integer>(Arrays.asList(7))));
                regions.add(new Arithmetic(1, new ArrayList<Integer>(Arrays.asList(8))));
                break;
            default:
                System.out.println("Invalid difficulty selected.");
        }
        
        


        for (Arithmetic box: regions){
            ArrayList<Integer> box_cells = box.cells;
            for (int i: box_cells){
                cells[i].setArithmeticBox(box);
            }
        }
    }
    
    public void run(){
        initialize(difficulty);
        AC3Init();
    }

    public static void main(String[] args) {
        KenKenPlayer app = new KenKenPlayer();
        app.run();
    }
}
