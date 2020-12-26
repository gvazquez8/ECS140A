(defun parity-correction (msgs)
    (or (reconstruct-bits (cdr msgs) (cons (car msgs) NIL) ) (cons NIL msgs) )
)

(defun reconstruct-bits (msgs new-msgs) ; msgs = input, new-msgs = possible solution
    (cond ( (and (eq (length msgs) 0 ) (equal (position NIL (car new-msgs) ) NIL ) ) ; we've gone through all rows and filled in all NILs
                (check-msgs (reverse new-msgs)) ) ; verify if valid
          ( (not (equal (position NIL (car new-msgs) ) NIL ) )
                (or (reconstruct-bits msgs (cons (replace-element (car new-msgs) 0 (position NIL (car new-msgs) ) ) (cdr new-msgs) ) ) ; replace NIL with 0
                    (reconstruct-bits msgs (cons (replace-element (car new-msgs) 1 (position NIL (car new-msgs) ) ) (cdr new-msgs) ) ) ; replace NIL with 1
                ) )
          ( t (reconstruct-bits (cdr msgs) (cons (car msgs) new-msgs) ) ) ; there was no NIL in current row, move to next row.
    )
)

(defun check-msgs (new-msgs)
    (cond ( (and (check-all-rows new-msgs (length new-msgs) ) (check-all-columns new-msgs (length (car new-msgs) ) ) ) ; If parities are correct, return answer
                (cons t new-msgs) )
          ( t NIL ) ; else return nil
    )
)

(defun check-all-rows (msgs numRows) ; check all rows, if any return NIL -> return NIL else t
    (cond ( (= numRows 0 ) t )
          ( t (and (check-row msgs 0 ) (check-all-rows (cdr msgs) (- numRows 1) ) ) )
    )
)

(defun check-all-columns (msgs numCols) ; check all columns, if any return NIL -> return NIL else t
    (cond ( (= numCols 0 ) t )
          ( t (and (check-column msgs (- numCols 1) ) (check-all-columns msgs (- numCols 1) ) ) )
    )
)

(defun check-row (lsts rowIndex) ; if ( sum of bits (except parity bit) mod 2 ) == parity bit -> return T else nil
    (cond ( (eq (mod (sum-row-bits (elt lsts rowIndex) ) 2 ) (car (last (elt lsts rowIndex) ) ) ) t )
          ( t NIL )
    )
)

(defun check-column (lsts colIndex) ; if ( sum of bits (except parity bit) mod 2) == parity bit -> return T else nil
    (cond ( (eq (mod (sum-column-bits (butlast lsts) colIndex) 2) (elt (car (last lsts)) colIndex) ) t )
          ( t NIL )
    )
)

(defun sum-column-bits (lsts colIndex) ; sums all bits except parity bit
    (reduce (lambda (x y) (+ (elt y colIndex) x) ) lsts :initial-value 0)
)

(defun sum-row-bits (lst) ; sum all bits except parity bit
    (reduce '+ (butlast lst) )
)

(defun replace-element (lst new-element index) ; return the original list with element at index 'index' replaced with new-element
    (cond ( (> index 0) (cons (car lst) (replace-element (cdr lst) new-element (- index 1) ) ) )
          ( t (cons new-element (cdr lst) ) )
    )
)