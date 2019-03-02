#!/bin/bash lein-exec

;;;; Rebuild link counts in the Redis datastore.
(ns enoch.center)

;; Require packages.
(require '[enoch.motor-shield :refer :all])   

(servo-rotate 1 100 #(range 0 (inc 10) 1))
(servo-rotate 1 100 #(range (inc 10) 0 -1))

