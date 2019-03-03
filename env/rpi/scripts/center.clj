#!/bin/bash lein-exec

;;;; Rebuild link counts in the Redis datastore.
(ns enoch.center)

;; Require packages.
(require '[enoch.motor-shield :refer :all])   
;'

;; Used to adjust alignment
(def halign 50)
(def valign 50)
(def step 1)
(def wait 30)

;; Horizontal
(servo-rotate 1 wait #(range 180 70 (* -1 step)))
(servo-rotate 1 wait #(range 70 130 step))
(servo-stop 1) 

;; Vertical
(servo-rotate 2 wait #(range 180 70 (* -1 step)))
(servo-rotate 2 wait #(range 70 130 step))
(servo-stop 2)

(gpio-shutdown)
  
