#!/bin/bash lein-exec

;;;; Rebuild link counts in the Redis datastore.
(ns enoch.center)

;; Require packages.
(require '[enoch.motor-shield :refer :all])   
;'

;; Used to adjust alignment
(def halign -20)
(def valign -10)
(def step 1)

(defn arc [deg] (+ 2.5 (/ deg 18.0)))

;; Horizontal
(servo-rotate 1 100 #(range 90 30 (* -1 step)))
(servo-rotate 1 100 #(range 30 (+ 90 halign) step))

(servo-stop 1)
