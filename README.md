# Ken-Ken-Solver

## Overview

This project formulates the KenKen puzzle as a Constraint Satisfaction Problem (CSP) and compares different heuristics of solving. 

To test: `javac KenKenPlayer && java KenKenPlayer`.

Input a difficulty (1-9). Use the GUI to select which method to run. Note that the GUI sometimes has visual bugs and may not be helpful to look at for the solutions to puzzles - it is better to look at the terminal output instead.

## Results

We tested our backtracking algorithms on expert difficulty boards taken from the official KenKen website, which ranged from 3x3 to 9x9. The methods tested were (1) forward checking, (2) AC3, (3) AC3 with MCV heuristic, and (4) AC3 with MCV and LCV. Below is the table showing the number of recursions taken to solve for each board size and each method.

|              | 3x3 | 4x4 | 5x5 | 6x6 | 7x7 | 8x8 | 9x9 |
|--------------|-----|-----|-----|-----|-----|-----|-----|
| FW-checking  | 10  | 56  | 50  | 365 | 82124 | 21820 | 1450090 |
| AC3          | 10  | 20  | 27  | 83  | 735  | 364  | 1212  |
| MCV          | 2   | 5   | 8   | 30  | 160  | 164  | 412  |
| MCV & LCV    | 2   | 4   | 7   | 23  | 30  | 78  | 312  |


