import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Queue;
import java.text.DecimalFormat;

public class KenKenPlayer
{
    //private final String difficulty = "3x3";
    //private String difficulty;

    private int PUZZLE_SIZE;
    private int NUM_CELLS;

    int recursions;
    int[] vals;
    
    //Board board = null;


    /// --- AC-3 Constraint Satisfication --- ///
   

    ArrayList<Integer>[] globalDomains;
    ArrayList<Integer>[] neighbors;
    ArrayList<Arithmetic> regions = new ArrayList<Arithmetic>();
    Queue<Arc> globalQueue = new LinkedList<Arc>();


    private void setPuzzleSize(int dim){
        PUZZLE_SIZE = dim;
        NUM_CELLS = dim * dim;
        vals = new int[NUM_CELLS];
        globalDomains = new ArrayList[NUM_CELLS];
        neighbors = new ArrayList[NUM_CELLS];
    }

    private final void AC3Init(){
        // board.Clear();
		recursions = 0; 

        initGlobalDomains(); // initialize domains for each cell

        allDiff(); // initializes neighbors and globalQueue   
        

        // Initial call to backtrack() on cell 0 (top left)
        boolean success = backtrack(0, globalDomains);

        // Prints evaluation of run

        /*
         * sets final board values in vals
         */
        Finished(success);

    }

    private final void most_constrained_solver(){
        // board.Clear();
		recursions = 0; 

        initGlobalDomains(); // initialize domains for each cell

        allDiff(); // initializes neighbors and globalQueue   
        

        // Initial call to backtrack() on cell 0 (top left)
        boolean success = backtrack_heuristic(globalDomains);

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
            if(box.operator != '#'){ // if there is more than one value in the box
                for(int i = 0; i < box.cells.size(); i++){
                    int cell = box.cells.get(i);
                    ArrayList<Integer> neighbors = new ArrayList<Integer>();
                    for(int j = 0; j < box.cells.size(); j++){
                        if(i!=j) neighbors.add(box.cells.get(j));
                    }
                    Arc group = new Arc(cell, neighbors, box.target, box.operator);
                    globalQueue.add(group);
                }
                
            } else { // if there is only one value in the box, assign it that value
                int cell = box.cells.get(0);
                globalDomains[cell].clear();
                globalDomains[cell].add(box.target);
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

    
    
    private final boolean backtrack(int cell, ArrayList<Integer>[] Domains) {
        recursions++;
        if (cell >= NUM_CELLS){ // found a solution for the board
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
                vals[cell] = value; 
                return true;
            }
        }
        return false;
    }

        private final boolean backtrack_heuristic(ArrayList<Integer>[] Domains) {
        recursions++;
        System.out.println("Recursion count: " + recursions);  // Debugging statement

        int cell = find_most_constrained(Domains); 

        // solution is found
        if (cell == -1){
            final_assignment(Domains);
            return true;
        }

        if (cell >= NUM_CELLS){ // found a solution for the board
            return true;
        }

        // make a copy of domains so we don't modify global domains
        ArrayList<Integer>[] domain_copy = new ArrayList[PUZZLE_SIZE*PUZZLE_SIZE];
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
            boolean consistent = backtrack_heuristic(domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                return true;
            }
        }
        return false;
    }

    // finds and returns the cell with the most constrained domain 
    private final int find_most_constrained(ArrayList<Integer>[] Domains){

        int smallest_domain_size = 10; // domain size will never exceed 9
        int most_constrained_variable = -1; 

        for(int i = 0; i < Domains.length; i++){
            int domain_size = Domains[i].size();

            // if the domain hasn't been assigned a value and we find a more constrained domain
            if ( domain_size != 1 && domain_size < smallest_domain_size){
                smallest_domain_size = domain_size;
                most_constrained_variable = i;
            }
        }
        
        return most_constrained_variable;
    }

    /**
     * Called when all variables have a domain of 1
     * Assigns all variables to their singular available value
     */
    private void final_assignment(ArrayList<Integer>[] Domains){
        for(int cell = 0; cell < PUZZLE_SIZE*PUZZLE_SIZE; cell++){
            vals[cell] = Domains[cell].get(0); 
        }
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

            boolean revised = Revise(current_arc, Domains);
                
            if(!revised) continue; // if the domain wasn't revised move to the next arc
            if (Domains[current_arc.cell].isEmpty()){ // an inconsistency was found
                return false;
            } 
        }
        
		return true;
    }

    private final boolean Revise(Arc t, ArrayList<Integer>[] Domains){
        if(t.constraintType == "diff"){
            return ReviseDiff(t, Domains);
        } else {
            return ReviseMath(t, Domains);
        }
    }

    private final boolean ReviseDiff(Arc t, ArrayList<Integer>[] Domains){
        // extract endpoints of arc
        int Xi = t.cell;
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
        switch(t.operator){
            case '+':
                return ReviseAdd(t, Domains);
            case '-':
                return ReviseSub(t, Domains);
            case '*':
                return ReviseMul(t, Domains);
            case '/':
                return ReviseDiv(t, Domains);
            default:
                System.out.println("Invalid operator");
                return false;
        }
    }

    private final boolean ReviseAdd(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];

        int neighbor = t.neighbors.get(0);
        ArrayList<Integer> neighborDom = Domains[neighbor];

        int min_sum = 0;
        int max_sum = 0;
        for(int i: t.neighbors){
            min_sum += min(Domains[i]);
            max_sum += max(Domains[i]);
        }

        // if the cage size of the add is 2
        //  complement = target - val
        // if cage cells does not contain the complement, then remove the val from domain

        for(int i=0; i<dom.size(); i++){
            if(dom.get(i) + min_sum > t.target || dom.get(i) + max_sum < t.target){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    private final boolean ReviseSub(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];
        int neighbor = t.neighbors.get(0);
        ArrayList<Integer> neighborDom = Domains[neighbor];

        for(int i=0; i<dom.size(); i++){
            int val1 = dom.get(i);
            boolean works = false;
            for(int j=0; j<neighborDom.size(); j++){
                int val2 = neighborDom.get(j);
                if(Math.abs(val1 - val2) == t.target){
                    works = true;
                }
            }
            if(!works){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    private final boolean ReviseMul(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];

        int min_prod = 1;
        int max_prod = 1;
        for(int i: t.neighbors){
            min_prod *= min(Domains[i]);
            max_prod *= max(Domains[i]);
        }
        for(int i=0; i<dom.size(); i++){
            int val = dom.get(i);
            if(t.target % val != 0 || val * min_prod > t.target || val * max_prod < t.target){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    private final boolean ReviseDiv(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];
        int neighbor = t.neighbors.get(0);
        ArrayList<Integer> neighborDom = Domains[neighbor];
        for(int i=0; i<dom.size(); i++){
            int val1 = dom.get(i);
            boolean works = false;
            for(int j=0; j<neighborDom.size(); j++){
                int val2 = neighborDom.get(j);
                if(val1 / val2 == t.target || val2 / val1 == t.target){
                    works = true;
                }
            }
            if(!works){
                dom.remove(i);
                revised = true;
            }
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

    private int max(ArrayList<Integer> L){
        // int max_index = -1;
        int max_value = 0;
        for(int i=0; i<L.size(); i++){
            if(L.get(i) > max_value){
                max_value = L.get(i);
                // max_index = i;
            }
        }  
        return max_value; 
    }

    private void Finished(boolean success){
    	
    	if(success){
            printBoard();
        } else {
            System.out.println("Failed");
        }
        System.out.println("Recursions: "+recursions);
    }

    private void printBoard(){
        for(int i=0; i<PUZZLE_SIZE; i++){
            for(int j=0; j<PUZZLE_SIZE; j++){
                int cellNum = i * PUZZLE_SIZE + j;
                System.out.print(vals[cellNum]);
            }
            System.out.println();
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
        int cell;
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
            this.cell = cell_i;
            this.neighbors = new ArrayList<Integer>(Arrays.asList(cell_j));
            constraintType = "diff";
        }

        public Arc(int cell, ArrayList<Integer> neighbors, int target, char operator){
            this.target = target;
            this.operator = operator;
            this.cell = cell;
            this.neighbors = neighbors;
            constraintType = "math";
        }

        public int compareTo(Object o){
            return this.toString().compareTo(o.toString());
        }

        public String toString(){
            return "(" + cell + "->" + neighbors + ")";
        }
    }

    public final void initialize(String difficulty){    
        
        // CREATE BOARD
        switch(difficulty){
            case "3x3":
                setPuzzleSize(3);
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(0,3))));
                regions.add(new Arithmetic(6, '*', new ArrayList<Integer>(Arrays.asList(1,2))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(4,7))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(5,8))));
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(6))));
                break;
            case "add-3":
                setPuzzleSize(3);
                regions.add(new Arithmetic(3, '+', new ArrayList<Integer>(Arrays.asList(0,1))));
                regions.add(new Arithmetic(4, '+', new ArrayList<Integer>(Arrays.asList(2,5))));
                regions.add(new Arithmetic(4, '+', new ArrayList<Integer>(Arrays.asList(3,6))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(4,7,8))));
                break;
            case "add-4":
                setPuzzleSize(4);
                regions.add(new Arithmetic(1, new ArrayList<Integer>(Arrays.asList(0))));
                regions.add(new Arithmetic(4, new ArrayList<Integer>(Arrays.asList(15))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(1,5))));
                regions.add(new Arithmetic(5, '+', new ArrayList<Integer>(Arrays.asList(2,3))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(4,8))));
                regions.add(new Arithmetic(3, '+', new ArrayList<Integer>(Arrays.asList(6,7))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(9,10,11))));
                regions.add(new Arithmetic(6, '+', new ArrayList<Integer>(Arrays.asList(12,13,14))));
                break;
            case "sub-3":
                setPuzzleSize(3);
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(4))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(0,1))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(2,5))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(3,6))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(7,8))));
                break;
            case "mul-div-4":
                setPuzzleSize(4);
                regions.add(new Arithmetic(12, '*', new ArrayList<Integer>(Arrays.asList(0,1,4))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(2,3))));
                regions.add(new Arithmetic(12, '*', new ArrayList<Integer>(Arrays.asList(5,6,9))));
                regions.add(new Arithmetic(24, '*', new ArrayList<Integer>(Arrays.asList(7,11,15))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(8,12))));
                regions.add(new Arithmetic(6, '*', new ArrayList<Integer>(Arrays.asList(10,13,14))));
                break;
            case "all-op-5":
                setPuzzleSize(5);
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(0,5))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(1,2))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(3,8))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(4,9))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(6,7))));
                regions.add(new Arithmetic(12, '*', new ArrayList<Integer>(Arrays.asList(10,11,15))));
                regions.add(new Arithmetic(8, '+', new ArrayList<Integer>(Arrays.asList(12,13,18))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(14,19))));
                regions.add(new Arithmetic(15, '*', new ArrayList<Integer>(Arrays.asList(16,17,22))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(20,21))));
                regions.add(new Arithmetic(4, '-', new ArrayList<Integer>(Arrays.asList(23,24))));
                break;
            case "9x9":
                setPuzzleSize(9);
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(0,1))));
                regions.add(new Arithmetic(4, '-', new ArrayList<Integer>(Arrays.asList(2,3))));
                regions.add(new Arithmetic(336, '*', new ArrayList<Integer>(Arrays.asList(4,5,14))));
                regions.add(new Arithmetic(19, '+', new ArrayList<Integer>(Arrays.asList(6,15,16,17))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(7,8))));
                regions.add(new Arithmetic(14, '+', new ArrayList<Integer>(Arrays.asList(9,18,27))));
                regions.add(new Arithmetic(16, '+', new ArrayList<Integer>(Arrays.asList(10,19))));
                regions.add(new Arithmetic(720, '*', new ArrayList<Integer>(Arrays.asList(11,20,28,29))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(12,13))));
                regions.add(new Arithmetic(10, '+', new ArrayList<Integer>(Arrays.asList(21,22,23))));
                regions.add(new Arithmetic(80, '*', new ArrayList<Integer>(Arrays.asList(24,25,34))));
                regions.add(new Arithmetic(8, '-', new ArrayList<Integer>(Arrays.asList(26,35))));
                regions.add(new Arithmetic(7, '-', new ArrayList<Integer>(Arrays.asList(30,31))));
                regions.add(new Arithmetic(1920, '*', new ArrayList<Integer>(Arrays.asList(32,33,42,50,51))));
                regions.add(new Arithmetic(9, new ArrayList<Integer>(Arrays.asList(36))));
                regions.add(new Arithmetic(5, '+', new ArrayList<Integer>(Arrays.asList(37,38))));
                regions.add(new Arithmetic(17, '+', new ArrayList<Integer>(Arrays.asList(39,40,41))));
                regions.add(new Arithmetic(9, '+', new ArrayList<Integer>(Arrays.asList(43,44))));
                regions.add(new Arithmetic(14, '+', new ArrayList<Integer>(Arrays.asList(45,54,63))));
                regions.add(new Arithmetic(8, '-', new ArrayList<Integer>(Arrays.asList(46,47))));
                regions.add(new Arithmetic(8, '+', new ArrayList<Integer>(Arrays.asList(48,49))));
                regions.add(new Arithmetic(108, '*', new ArrayList<Integer>(Arrays.asList(52,60,61))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(53,62))));
                regions.add(new Arithmetic(6, '-', new ArrayList<Integer>(Arrays.asList(55,64))));
                regions.add(new Arithmetic(42, '*', new ArrayList<Integer>(Arrays.asList(56,65))));
                regions.add(new Arithmetic(20, '+', new ArrayList<Integer>(Arrays.asList(57,58,59))));
                regions.add(new Arithmetic(8, '-', new ArrayList<Integer>(Arrays.asList(66,67))));
                regions.add(new Arithmetic(216, '*', new ArrayList<Integer>(Arrays.asList(68,76,77))));
                regions.add(new Arithmetic(12, '+', new ArrayList<Integer>(Arrays.asList(69,70,71))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(72,73))));
                regions.add(new Arithmetic(4, '/', new ArrayList<Integer>(Arrays.asList(74,75))));
                regions.add(new Arithmetic(12, '+', new ArrayList<Integer>(Arrays.asList(78,79,80))));
                break;
            default:
                System.out.println("Invalid difficulty selected.");
        }
        
        // INIT CELLS
        for(int cell = 0; cell < NUM_CELLS; cell++){
            vals[cell] = 0;
        }
    }
    

    public void run(String difficulty){
        initialize(difficulty);

        GUI gui = new GUI();
        gui.initVals();

        AC3Init();
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("difficulty? \teasy (e), hard (h)");

        char choice = scan.nextLine().charAt(0);

        KenKenPlayer app = new KenKenPlayer();

        if (choice == 'e'){
            app.run("3x3");
        }
        if (choice == 'h'){
            app.run("9x9");
        }
        scan.close();
    }

    class GUI {
        JFrame mainFrame;
        CellPanel[][] cellPanels;
        JPanel gamePanel, buttonPanel;
        JLabel recursionLabel;
        JButton ac3Button, heuristicButton, clearButton;
        // Assuming a maximum puzzle size for color array initialization
        Color[] regionColors = new Color[PUZZLE_SIZE * PUZZLE_SIZE]; 

        public GUI() {
            mainFrame = new JFrame("KenKen Player");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setLayout(new BorderLayout());

            gamePanel = new JPanel();
            gamePanel.setLayout(new GridLayout(PUZZLE_SIZE, PUZZLE_SIZE)); // Grid layout for the puzzle

            // Initialize the cell panels
            cellPanels = new CellPanel[PUZZLE_SIZE][PUZZLE_SIZE];
            for (int r = 0; r < PUZZLE_SIZE; r++) {
                for (int c = 0; c < PUZZLE_SIZE; c++) {
                    cellPanels[r][c] = new CellPanel();
                    gamePanel.add(cellPanels[r][c]);
                }
            }

            mainFrame.add(gamePanel, BorderLayout.CENTER);
            mainFrame.pack();
            mainFrame.setVisible(true);

        // Button panel
        buttonPanel = new JPanel();
        ac3Button = new JButton("AC3");
        heuristicButton = new JButton("Most Constrained Heuristic");
        clearButton = new JButton("Clear Board");
        recursionLabel = new JLabel("Recursions: ");
        
        // action listeners for buttons
        ac3Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AC3Init();
                recursionLabel.setText("Recursions: " + recursions);
                updateBoard();
            }
        });
        
        heuristicButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                most_constrained_solver();
                recursionLabel.setText("Recursions: " + recursions);
                updateBoard();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearBoard();
            }
        });

        buttonPanel.add(ac3Button);
        buttonPanel.add(heuristicButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(recursionLabel);

        mainFrame.add(gamePanel, BorderLayout.CENTER);
        mainFrame.add(buttonPanel, BorderLayout.SOUTH);
        mainFrame.pack();
        mainFrame.setVisible(true);

        }

        //  update the board with the final solution values
        private void updateBoard() {
            for (int i = 0; i < PUZZLE_SIZE; i++) {
                for (int j = 0; j < PUZZLE_SIZE; j++) {
                    int cellNum = i * PUZZLE_SIZE + j;
                    cellPanels[i][j].setValue(Integer.toString(vals[cellNum]));
                }
            }
        }

        private void clearBoard() {
            for (int i = 0; i < PUZZLE_SIZE; i++) {
                for (int j = 0; j < PUZZLE_SIZE; j++) {
                    cellPanels[i][j].valueField.setText(""); // Clear the text field
                    cellPanels[i][j].valueField.setEditable(true); // Make the field editable again
                }
            }
            recursionLabel.setText("Recursions: 0"); // Reset the recursion label
        }

        public void initVals() {
            // Assign colors to regions
            assignRegionColors();

            // Set the borders and constraints for each cell panel
            for (int i = 0; i < regions.size(); i++) {
                Arithmetic arithmetic = regions.get(i);
                for (int cellIndex : arithmetic.cells) {
                    int row = cellIndex / PUZZLE_SIZE;
                    int col = cellIndex % PUZZLE_SIZE;
                    CellPanel cellPanel = cellPanels[row][col];
                    cellPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    cellPanel.setRegionColor(regionColors[i]);
                    if (cellIndex == arithmetic.cells.get(0)) { // Top-left cell of the region
                        cellPanel.setConstraint(arithmetic.target + String.valueOf(arithmetic.operator));
                    }
                }
            }
        }

        private void assignRegionColors() {
            // Set a different color for each region
            for (int i = 0; i < regions.size(); i++) {
                // Cycle through a predefined array of colors
                regionColors[i] = new Color(
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256)
                );
            }
        }
        
    }

    class CellPanel extends JPanel {
        JLabel constraintLabel;
        JTextField valueField;

        public CellPanel() {
            super(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.BLACK)); // Set the border for the cell

            constraintLabel = new JLabel();
            valueField = new JTextField();
            valueField.setHorizontalAlignment(JTextField.CENTER);

            constraintLabel.setOpaque(true);
            valueField.setOpaque(true);

            // Adjust the font size for the constraint label
            Font currentFont = constraintLabel.getFont();
            constraintLabel.setFont(new Font(currentFont.getName(), Font.BOLD, 17)); 
            constraintLabel.setBorder(new EmptyBorder(0, 2, 0, 0)); // Add some padding to the label

            add(constraintLabel, BorderLayout.NORTH);
            add(valueField, BorderLayout.CENTER);

            setOpaque(true);

        }

        public void setConstraint(String text) {
            constraintLabel.setText(text);
        }

        public void setRegionColor(Color color) {
            setBackground(color);
            constraintLabel.setBackground(color); 
            valueField.setBackground(color);
        }

        public void setValue(String value) {
            valueField.setText(value);
        }
    }
}
