(defun match (pattern assert)
    (cond ( (equal pattern assert) 't )                                                                                 ; pattern is the same as assert. acts as a base case because eventually, if they do match they will result in
          ( (equal (car pattern) (car assert) ) (match (cdr pattern) (cdr assert)) )                                    ; first item in both lists match, move on to next items
          ( (equal (car pattern) '! ) (match-exclamation pattern assert) )                                              ; next pattern item contains !, evaluate it
          ( (eq (not (search "*" (string (car pattern)))) NIL)                                                          ; if true, the next pattern item has a *, evaluate it.
                (cond ( (equal (match-star (string (car pattern)) (string (car assert)) ) t )                           ; if we successfully matched the *, move on to next items otherwise return nil.
                            (match (cdr pattern) (cdr assert) ) )
                      ( t NIL ) ) )
          ( t NIL )                                                                                                     ; if none of these conditions were true, items do not match so return nil.
    )
)

(defun match-exclamation (pattern assert)
    (cond ( (equal (car (cdr pattern) ) '!) (match-exclamation (cdr pattern) assert) )                                  ; if multiple !s exist, match 0 atoms with current one and move on to next !.
          ( (eq (cdr pattern) NIL) t)                                                                                   ; '! is the last atom in pattern so it will match everything remaining even if assert is nil.
          ( (eq (not (search "*" (string (car (cdr pattern) ) ) ) ) NIL )                                               ; the element after ! is a word with * so make sure we cover it correctly.
                (cond ( (and (equal (match-star (string (car (cdr pattern) ) ) (string (car assert) ) ) t )
                             (equal (match-star (string (car (cdr pattern) ) ) (string (car (cdr assert) ) ) ) t ) )
                            (match-exclamation pattern (cdr assert) ) )
                      (  (equal (match-star (string (car (cdr pattern) ) ) (string (car assert) ) ) t )
                            (match (cdr (cdr pattern) ) (cdr assert) ) )
                      (t (match-exclamation pattern (cdr assert) ) ) ) )
          ( (and (equal (car (cdr pattern) ) (car assert) ) (equal (car (cdr pattern) ) (car (cdr assert) ) ) )
                (match-exclamation pattern (cdr assert) ) )                                                             ; handles cases where the next word in pattern is repeated twice. consume 1 of them in !. ex: (! s) , ( s s)
          ( (equal (car (cdr pattern) ) (car assert) ) (match (cdr pattern) assert) )                                   ; the 2nd word in pattern matches 1st word in assert. stop matching ! and continue.
          ( (and (not (equal (car (cdr pattern)) NIL)) (equal (car assert) NIL) ) NIL )                                 ; if assert is NIL, and there are more atoms after ! then return NIL.
          ( t (match-exclamation pattern (cdr assert) ) )                                                               ; continue matching ! until a condition is met.
    )
)

(defun match-star (pattern assert)
    ( cond ( (and   (equal (length pattern) 0) (equal (length assert) 0) ) t   )                                        ; if both assert and pattern empty, then return true since they match
           ( (equal (subseq pattern 0) "*")                                t   )                                        ; if pattern is only "*" then itll match everything. return t
           ( (or    (equal (length pattern) 0) (equal (length assert) 0) ) nil )                                        ; if one of them is empty, then something isn't matching up so return NIL
           ( (and   (equal (subseq pattern 0 1) "*") (equal (subseq pattern 1 2) "*") )
                        (match-star (subseq pattern 1) assert) )                                                            ; "**" -> "*"
           ( (equal (subseq pattern 0 1) "*")  (match-current-star pattern assert) )                                    ; if current character is * then begin matching it.
           ( (equal (subseq pattern 0 1) (subseq assert 0 1) ) (match-star (subseq pattern 1) (subseq assert 1) ) )     ; if current character is not a * and it matches with the next character in assert. move on to match the next characters.
           ( t NIL )                                                                                                    ; if it reaches to this statement, characters did not match.
    )
)

(defun match-current-star (pattern assert)
    (cond ( (> (length assert) 1)   ; cases where assert has more than 1 character
                (cond ( (and (equal (subseq pattern 1 2) (subseq assert 0 1)) (equal (subseq pattern 1 2) (subseq assert 1 2)) ) ; Ex: "a*b", "abb"
                            (match-star pattern (subseq assert 1) ) )
                        ( (equal (subseq pattern 1 2) (subseq assert 0 1)) (match-star (subseq pattern 1) assert ) )             ; Done matching current star so move on to next character.
                        ( t (match-star pattern (subseq assert 1) ) )                                                            ; Continue matching *
                )
          )
          ( (equal (subseq pattern 1 2) (subseq assert 0 1) ) (match-star (subseq pattern 1) assert) )                           ; finished matching *
          ( t (match-star pattern (subseq assert 1) ) )                                                                          ; since assert = 1 character long, match it with * and move on to next character in pattern
    )
)

