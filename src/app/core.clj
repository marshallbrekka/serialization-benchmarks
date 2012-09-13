(ns app.core
  (:require [clojure.tools.logging :as lg]
            [carbonite.api :as api]
            [carbonite.buffer :as buffer]
            [cheshire.core :as json]
            [clojurewerkz.spyglass.client :as spyglass]
            [clojure.walk :as walk]))

(defn nano-time []
  (System/nanoTime))

(defn timed [task]
  (let [t-start (nano-time)
        res     (task)
        t-end   (nano-time)]
    (double (/ (- t-end t-start) 1000000000))))

(def  mem-conn (spyglass/text-connection "localhost:2000"))


(defn set-data [k d]
  (spyglass/set mem-conn k  300 d))

(defn get-data [k]
  (count (spyglass/get-multi mem-conn k))) 

(def data [{:type "credit" 
             :id 29384738
             :points (->> (repeat 2000.45)
                          (take 200)
                          (map (fn [a] [1234567890123 a]))
                          (doall))}])


(def registry (api/default-registry))

(def roundtrips 100000)
(def roundtrips-mem 10000)

(defn bench [name f]
   (print name "  ")
   (flush)
   (printf "%.2f\n" (timed #(dotimes [_ roundtrips] (f data))))
   (flush))

(defn bench-mem [name ser]
   (let [d (ser data)
         ks (map str (take roundtrips-mem (iterate inc 0)))]
   (print name "  ")
   (flush)
   (printf "%.2f" (timed #(dotimes [i roundtrips-mem] (set-data (str i) d))))
   (flush)
   (printf "   %.2f\n" (timed #(get-data ks)))
   (flush)))
(defn run [] 
(println "Clojure version: "
   (str (:major       *clojure-version*) "."
        (:minor       *clojure-version*) "."
        (:incremental *clojure-version*)))
(println "Num roundtrips:  "  roundtrips)
(println)
(dotimes [i 2]
  (println "Trail: " (inc i))
  (bench "json"
         #(json/parse-string (json/generate-string %)))
  (bench "json-keywordize"
         #(walk/keywordize-keys (json/parse-string (json/generate-string %))))
  (bench "carbonite"
         #(buffer/read-bytes registry (buffer/write-bytes registry %)))
  (println))


(println "Send/Recieve with memcached")
(println "num roundtrips " roundtrips-mem)
(dotimes [i 2]
  (println "Trail: " (inc i))
  (bench-mem "json"
         json/generate-string)
  (bench-mem "carbonite"
          #(buffer/write-bytes registry %))
  (println))
  )
