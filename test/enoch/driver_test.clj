(ns enoch.driver-test
  (:require [clojure.test :refer :all]
            [enoch.driver :refer :all]))

(deftest test-change-arrow
  (mapv (fn [a] (println "arrow" a)
                (change-arrow a)
		(swap! car-state assoc :mode a)
                (Thread/sleep 1000)) (keys direction-arrows))
  (println "arrow stop")
  (change-arrow :stopped)
  (Thread/sleep 500))
  
(deftest test-drive-forward
  (println "forward slow")
  (drive-forward 20)
  (Thread/sleep 2000)
  
  (println "forward fast")
  (drive-forward 60)
  (Thread/sleep 2000)

  (drive-stop))
  
(deftest test-drive-reverse
  (println "reverse slow")
  (drive-reverse 20)
  (Thread/sleep 2000)
  
  (println "reverse fast")
  (drive-reverse 60)
  (Thread/sleep 2000)

  (drive-stop))
  
(deftest test-drive-left
  (println "left slow")
  (drive-left 20)
  (Thread/sleep 2000)
  
  (println "left fast")
  (drive-left 60)
  (Thread/sleep 2000)

  (drive-stop))
    
(deftest test-drive-right
  (println "right slow")
  (drive-right 20)
  (Thread/sleep 2000)
  
  (println "forward right")
  (drive-right 60)
  (Thread/sleep 2000)

  (drive-stop))
    
(deftest test-drive-directions
  (println "forward")
  (drive-forward 40)
  (Thread/sleep 2000)

  (println "right")
  (drive-right 40)
  (Thread/sleep 2000)

  (println "reverse")
  (drive-reverse 40)
  (Thread/sleep 2000)

  (println "left")
  (drive-left 40)
  (Thread/sleep 2000)

  (drive-stop))
  