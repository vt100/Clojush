;;; Records the results of runs to `ici-recorder`.
;;;
;;; We want to record two types of information:
;;;   1. The initial run configuration
;;;   2. The results of a generation
;;; Each run has a UUID. Each generation is recorded with a reference to this
;;; UUID and it's index.
;;;
;;; We build up the data we want to send for the generation/run over time
;;; and then send it off.
(ns clojush.pushgp.record
  (:require [ici-recorder]
            [clj-uuid]
            [clojure.spec.test :as stest]))

(-> 'ici-recorder.parquet.add-data
  stest/enumerate-namespace
  stest/instrument)

(-> 'ici-recorder.parquet.write
  stest/enumerate-namespace
  stest/instrument)
  
(def data (atom {}))


;; Stores a configuration options for the run, for the sequence of `ks` and value `v`
(defn config-data! [ks v]
  (swap! data assoc-in (cons :configuration ks) v)
  v)

;; called at the begining of a new run.
;;
;; Resets the state and creates UUID
(defn new-run! []
  (reset! data {:uuid (clj-uuid/v1)}))
  
  ;; commented out until apache spark supports timestamp_millis
  ;; https://github.com/apache/spark/pull/15332
  ; (config-data! [:start-time] (java.time.Instant/now)))


(def p-configuration
  {:problem-file [true :string]
   :argmap [true [:string false :string]]
   :initialization-ms [false :long]
   :registered-instructions [true [true :string]]
   :versiom-numer [false :string]
   :git-hash [false :string]})


;; Records the run configuration with `ici-recorder`
(defn end-config! []
  (let [{:keys [configuration uuid]} @data]
    (ici-recorder/record-run
      p-configuration
      uuid
      configuration)))

;; Called at the begining of a generation
(defn new-generation! [index]
  (swap!
    data
    (fn [m]
      (assoc
        (select-keys m [:uuid])
        :index index))))

;; stores some data about the generation
(defn generation-data! [ks v]
  (swap! data assoc-in (cons :generation ks) v)
  v)

(def p-error :double)
(def p-errors [true p-error])
(def p-plush-instruction-map
  {:instruction [true :string]
   :uuid [false :string]
   :random-insertion [false :boolean]
   :silent [false :boolean]
   :random-closes [false :integer]
   :parent-uuid [false :string]})
(def p-genome [true p-plush-instruction-map])

(def p-individual
  {:genome [true p-genome]
   :program [true :string]
   :errors [false p-errors]
   :total-error [false p-error]
   :normalized-error [false p-error]
   :meta-errors [false p-errors]
   :history [false p-errors]
   :ancestors [false [true p-genome]]
   :uuid [false :string]
   :parent-uuids [false [true :string]]
   :genetic-operators [false :string]
   :is-random-replacement [false :boolean]
   :age [true :integer]})

(def p-generation
  {:outcome [true :string]
   :epsilons [false :double]
   :population [true [true p-individual]]})

;; records the generation with `ici-recorder`
(defn end-generation! []
  (let [{:keys [generation uuid index]} @data]
    (ici-recorder/record-generation
      p-generation
      uuid
      index
      generation)))
